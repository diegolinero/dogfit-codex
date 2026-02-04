package com.astralimit.dogfit

import android.app.*
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.*

class DogFitBleService : Service() {
    private val TAG = "DogFitBleService"
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var isScanning = false
    private var isConnecting = false
    private var fallbackScanEnabled = false
    private val scanHandler = Handler(Looper.getMainLooper())
    private var notificationQueue: ArrayDeque<UUID> = ArrayDeque()
    private var notificationService: BluetoothGattService? = null

    private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    private val ACTIVITY_CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    private val GPS_CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a9")
    private val BATTERY_CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26aa")
    private val DEVICE_NAME_PREFIX = "DogFit"
    private val SCAN_FALLBACK_DELAY_MS = 10000L

    companion object {
        const val ACTION_NEW_DATA = "com.astralimit.dogfit.NEW_DATA"
        const val ACTION_BLE_STATUS = "com.astralimit.dogfit.BLE_STATUS"
        const val EXTRA_STATUS = "status"
        const val STATUS_CONNECTED = "connected"
        const val STATUS_DISCONNECTED = "disconnected"
    }

    override fun onCreate() {
        super.onCreate()
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "DogFitChannel")
            .setContentTitle("Collar DogFit")
            .setContentText("Buscando conexión...")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .build()
        startForeground(1, notification)
        startScanning()
        return START_STICKY
    }

    private fun startScanning() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        Log.d(TAG, "Iniciando escaneo BLE con filtro de servicio...")
        isScanning = true
        fallbackScanEnabled = false
        scanner.startScan(listOf(filter), settings, scanCallback)
        scanHandler.removeCallbacksAndMessages(null)
        scanHandler.postDelayed({ startFallbackScan() }, SCAN_FALLBACK_DELAY_MS)
    }

    private fun startFallbackScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        if (!isScanning || isConnecting) {
            return
        }
        Log.w(TAG, "Sin resultados con filtro. Activando escaneo general...")
        scanner.stopScan(scanCallback)
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        fallbackScanEnabled = true
        scanner.startScan(emptyList(), settings, scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (isConnecting) return
            val deviceName = result.device.name ?: result.scanRecord?.deviceName
            val hasService = result.scanRecord?.serviceUuids?.contains(ParcelUuid(SERVICE_UUID)) == true
            if (fallbackScanEnabled && !hasService && deviceName?.contains(DEVICE_NAME_PREFIX, ignoreCase = true) != true) {
                return
            }
            Log.d(TAG, "Dispositivo encontrado: ${deviceName ?: "Desconocido"}")
            isConnecting = true
            isScanning = false
            scanHandler.removeCallbacksAndMessages(null)
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(this)
            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                result.device.connectGatt(this@DogFitBleService, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                result.device.connectGatt(this@DogFitBleService, false, gattCallback)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Escaneo BLE falló con código $errorCode. Reintentando...")
            isScanning = false
            isConnecting = false
            scanHandler.removeCallbacksAndMessages(null)
            Handler(Looper.getMainLooper()).postDelayed({ startScanning() }, 3000)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Error GATT ($status). Reintentando conexión...")
                isConnecting = false
                gatt.close()
                broadcastStatus(STATUS_DISCONNECTED)
                Handler(Looper.getMainLooper()).postDelayed({ startScanning() }, 5000)
                return
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "GATT Conectado. Descubriendo servicios...")
                isConnecting = false
                broadcastStatus(STATUS_CONNECTED)
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e(TAG, "GATT Desconectado. Reintentando...")
                isConnecting = false
                gatt.close()
                broadcastStatus(STATUS_DISCONNECTED)
                Handler(Looper.getMainLooper()).postDelayed({ startScanning() }, 5000)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.i(TAG, "Servicios OK. Solicitando MTU...")
            gatt.requestMtu(256)
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.i(TAG, "MTU listo: $mtu. Activando Notificaciones...")
            val service = gatt.getService(SERVICE_UUID)
            if (service == null) {
                Log.e(TAG, "Servicio BLE no encontrado")
                return
            }
            notificationService = service
            notificationQueue = ArrayDeque(
                listOf(ACTIVITY_CHAR_UUID, GPS_CHAR_UUID, BATTERY_CHAR_UUID)
            )
            enableNextNotification(gatt)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Error escribiendo descriptor BLE ($status)")
            }
            enableNextNotification(gatt)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.getStringValue(0)
            val intent = Intent(ACTION_NEW_DATA)
            intent.putExtra("data", data)
            sendBroadcast(intent)
        }
    }

    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic?) {
        if (characteristic == null) {
            Log.e(TAG, "Característica BLE no encontrada")
            return
        }
        val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        gatt.setCharacteristicNotification(characteristic, true)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        } else {
            Log.e(TAG, "Descriptor BLE no encontrado")
        }
    }

    private fun enableNextNotification(gatt: BluetoothGatt) {
        val service = notificationService ?: return
        val nextUuid = notificationQueue.removeFirstOrNull() ?: return
        val characteristic = service.getCharacteristic(nextUuid)
        if (characteristic == null) {
            Log.e(TAG, "Característica BLE $nextUuid no encontrada")
            enableNextNotification(gatt)
            return
        }
        val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        gatt.setCharacteristicNotification(characteristic, true)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        } else {
            Log.e(TAG, "Descriptor BLE no encontrado para $nextUuid")
            enableNextNotification(gatt)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("DogFitChannel", "DogFit", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun broadcastStatus(status: String) {
        val intent = Intent(ACTION_BLE_STATUS)
        intent.putExtra(EXTRA_STATUS, status)
        sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?) = null
}
