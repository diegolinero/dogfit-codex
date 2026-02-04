package com.astralimit.dogfit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "Dispositivo encendido, iniciando DogFit Service")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                // Iniciar el servicio BLE
                val serviceIntent = Intent(context, DogFitBleService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}