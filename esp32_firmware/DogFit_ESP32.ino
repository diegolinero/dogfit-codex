/*
 * DogFit ESP32-C3 Smart Collar Firmware
 * 
 * Hardware:
 * - ESP32-C3 Super Mini
 * - MPU6050 (Acelerómetro/Giroscopio)
 * - GPS NEO-6MV2
 * - Módulo MicroSD
 * - Batería 18650 + TP4056 + Mini560
 */

#include <Wire.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <TinyGPSPlus.h>
#include <HardwareSerial.h>
#include <SD.h>
#include <SPI.h>

#define I2C_SDA 8
#define I2C_SCL 9

#define GPS_TX 20
#define GPS_RX 21
#define GPS_BAUD 9600

#define SD_CS 7
#define SD_MOSI 6
#define SD_MISO 5
#define SD_SCK 4

#define BATTERY_PIN 3

#define MPU6050_ADDR 0x68

#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define ACTIVITY_CHAR_UUID  "beb5483e-36e1-4688-b7f5-ea07361b26a8"
#define GPS_CHAR_UUID       "beb5483e-36e1-4688-b7f5-ea07361b26a9"
#define BATTERY_CHAR_UUID   "beb5483e-36e1-4688-b7f5-ea07361b26aa"
#define SYNC_CHAR_UUID      "beb5483e-36e1-4688-b7f5-ea07361b26ab"

TinyGPSPlus gps;
HardwareSerial gpsSerial(1);

BLEServer* pServer = NULL;
BLECharacteristic* pActivityCharacteristic = NULL;
BLECharacteristic* pGpsCharacteristic = NULL;
BLECharacteristic* pBatteryCharacteristic = NULL;
BLECharacteristic* pSyncCharacteristic = NULL;

bool deviceConnected = false;
bool sdCardAvailable = false;

unsigned long stepCount = 0;
unsigned long activeMinutes = 0;
unsigned long lastStepTime = 0;
float lastAccMagnitude = 0;
bool stepDetected = false;

const float STEP_THRESHOLD = 1.3;
const unsigned long STEP_DEBOUNCE = 250;
const unsigned long ACTIVE_THRESHOLD = 500;

float currentLat = 0;
float currentLng = 0;
String currentDate = "";
String currentTime = "";
bool gpsValid = false;

unsigned long lastSaveTime = 0;
const unsigned long SAVE_INTERVAL = 60000;

unsigned long lastActivityUpdate = 0;
const unsigned long ACTIVITY_UPDATE_INTERVAL = 1000;

// Prototipos de funciones para evitar errores de declaración
void syncStoredData();
void clearStoredData();
int readBatteryLevel();
void initMPU6050();
void initSDCard();
void initBLE();
void readMPU6050();
void readGPS();
void updateActivity();
void detectStep(float magnitude);
void saveDataToSD();

class ServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
        deviceConnected = true;
        Serial.println("Dispositivo conectado");
    };

    void onDisconnect(BLEServer* pServer) {
        deviceConnected = false;
        Serial.println("Dispositivo desconectado");
        BLEDevice::startAdvertising();
    }
};

class SyncCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* pCharacteristic) {
        String value = pCharacteristic->getValue().c_str();
        if (value == "SYNC") {
            syncStoredData();
        } else if (value == "CLEAR") {
            clearStoredData();
        }
    }
};

void setup() {
    Serial.begin(115200);
    Serial.println("DogFit Collar Iniciando...");
    
    Wire.begin(I2C_SDA, I2C_SCL);
    
    initMPU6050();
    
    gpsSerial.begin(GPS_BAUD, SERIAL_8N1, GPS_TX, GPS_RX);
    Serial.println("GPS inicializado");
    
    initSDCard();
    
    initBLE();
    
    Serial.println("DogFit Collar listo!");
}

void loop() {
    readMPU6050();
    
    readGPS();
    
    if (millis() - lastActivityUpdate >= ACTIVITY_UPDATE_INTERVAL) {
        updateActivity();
        lastActivityUpdate = millis();
    }
    
    if (!deviceConnected && sdCardAvailable) {
        if (millis() - lastSaveTime >= SAVE_INTERVAL) {
            saveDataToSD();
            lastSaveTime = millis();
        }
    }
    
    delay(10);
}

void initMPU6050() {
    Wire.beginTransmission(MPU6050_ADDR);
    Wire.write(0x6B);
    Wire.write(0);
    Wire.endTransmission(true);
    
    Wire.beginTransmission(MPU6050_ADDR);
    Wire.write(0x1C);
    Wire.write(0x00);
    Wire.endTransmission(true);
    
    Serial.println("MPU6050 inicializado");
}

void readMPU6050() {
    Wire.beginTransmission(MPU6050_ADDR);
    Wire.write(0x3B);
    Wire.endTransmission(false);
    Wire.requestFrom(MPU6050_ADDR, 6, true);
    
    if (Wire.available() >= 6) {
        int16_t accX = Wire.read() << 8 | Wire.read();
        int16_t accY = Wire.read() << 8 | Wire.read();
        int16_t accZ = Wire.read() << 8 | Wire.read();
        
        float ax = accX / 16384.0;
        float ay = accY / 16384.0;
        float az = accZ / 16384.0;
        
        float magnitude = sqrt(ax*ax + ay*ay + az*az);
        detectStep(magnitude);
    }
}

void detectStep(float magnitude) {
    unsigned long now = millis();
    
    if (magnitude > STEP_THRESHOLD && lastAccMagnitude <= STEP_THRESHOLD) {
        if (!stepDetected && (now - lastStepTime) > STEP_DEBOUNCE) {
            stepCount++;
            lastStepTime = now;
            stepDetected = true;
        }
    } else if (magnitude < STEP_THRESHOLD) {
        stepDetected = false;
    }
    
    lastAccMagnitude = magnitude;
    
    if (magnitude > 1.1) {
        static unsigned long lastActiveCheck = 0;
        if (now - lastActiveCheck >= 60000) {
            activeMinutes++;
            lastActiveCheck = now;
        }
    }
}

void readGPS() {
    while (gpsSerial.available() > 0) {
        if (gps.encode(gpsSerial.read())) {
            if (gps.location.isValid()) {
                currentLat = gps.location.lat();
                currentLng = gps.location.lng();
                gpsValid = true;
            }
            
            if (gps.date.isValid() && gps.time.isValid()) {
                char dateBuffer[11];
                char timeBuffer[9];
                
                sprintf(dateBuffer, "%04d-%02d-%02d", 
                    gps.date.year(), gps.date.month(), gps.date.day());
                sprintf(timeBuffer, "%02d:%02d:%02d",
                    gps.time.hour(), gps.time.minute(), gps.time.second());
                
                currentDate = String(dateBuffer);
                currentTime = String(timeBuffer);
            }
        }
    }
}

void initSDCard() {
    SPI.begin(SD_SCK, SD_MISO, SD_MOSI, SD_CS);
    
    if (SD.begin(SD_CS)) {
        sdCardAvailable = true;
        Serial.println("Tarjeta SD inicializada");
        
        if (!SD.exists("/dogfit")) {
            SD.mkdir("/dogfit");
        }
    } else {
        sdCardAvailable = false;
        Serial.println("Tarjeta SD no disponible");
    }
}

void saveDataToSD() {
    if (!sdCardAvailable) return;
    
    String filename = "/dogfit/data_" + currentDate + ".txt";
    
    File file = SD.open(filename, FILE_APPEND);
    if (file) {
        String data = currentTime + "," + 
                      String(stepCount) + "," +
                      String(activeMinutes) + "," +
                      String(currentLat, 6) + "," +
                      String(currentLng, 6) + "," +
                      String(readBatteryLevel());
        
        file.println(data);
        file.close();
        Serial.println("Datos guardados en SD");
    }
}

void syncStoredData() {
    if (!sdCardAvailable || !deviceConnected) return;
    
    File root = SD.open("/dogfit");
    if (!root) return;
    
    File file = root.openNextFile();
    while (file) {
        if (!file.isDirectory()) {
            while (file.available()) {
                String line = file.readStringUntil('\n');
                pSyncCharacteristic->setValue(line.c_str());
                pSyncCharacteristic->notify();
                delay(50);
            }
        }
        file = root.openNextFile();
    }
    
    pSyncCharacteristic->setValue("SYNC_COMPLETE");
    pSyncCharacteristic->notify();
}

void clearStoredData() {
    if (!sdCardAvailable) return;
    
    File root = SD.open("/dogfit");
    if (!root) return;
    
    File file = root.openNextFile();
    while (file) {
        if (!file.isDirectory()) {
            String path = "/dogfit/" + String(file.name());
            file.close();
            SD.remove(path.c_str());
        }
        file = root.openNextFile();
    }
    
    Serial.println("Datos de SD borrados");
}

int readBatteryLevel() {
    int rawValue = analogRead(BATTERY_PIN);
    float voltage = (rawValue / 4095.0) * 3.3 * 2;
    
    int percentage = map(voltage * 100, 320, 420, 0, 100);
    percentage = constrain(percentage, 0, 100);
    
    return percentage;
}

void initBLE() {
    BLEDevice::init("DogFit Collar");
    
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new ServerCallbacks());
    
    BLEService *pService = pServer->createService(SERVICE_UUID);
    
    pActivityCharacteristic = pService->createCharacteristic(
        ACTIVITY_CHAR_UUID,
        BLECharacteristic::PROPERTY_READ |
        BLECharacteristic::PROPERTY_NOTIFY
    );
    pActivityCharacteristic->addDescriptor(new BLE2902());
    
    pGpsCharacteristic = pService->createCharacteristic(
        GPS_CHAR_UUID,
        BLECharacteristic::PROPERTY_READ |
        BLECharacteristic::PROPERTY_NOTIFY
    );
    pGpsCharacteristic->addDescriptor(new BLE2902());
    
    pBatteryCharacteristic = pService->createCharacteristic(
        BATTERY_CHAR_UUID,
        BLECharacteristic::PROPERTY_READ |
        BLECharacteristic::PROPERTY_NOTIFY
    );
    pBatteryCharacteristic->addDescriptor(new BLE2902());
    
    pSyncCharacteristic = pService->createCharacteristic(
        SYNC_CHAR_UUID,
        BLECharacteristic::PROPERTY_READ |
        BLECharacteristic::PROPERTY_WRITE |
        BLECharacteristic::PROPERTY_NOTIFY
    );
    pSyncCharacteristic->setCallbacks(new SyncCallbacks());
    pSyncCharacteristic->addDescriptor(new BLE2902());
    
    pService->start();
    
    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(0x06);
    pAdvertising->setMinPreferred(0x12);
    BLEDevice::startAdvertising();
    
    Serial.println("BLE iniciado, esperando conexión...");
}

void updateActivity() {
    if (!deviceConnected) return;
    
    String activityJson = "{\"stp\":" + String(stepCount) + 
                          ",\"act\":" + String(activeMinutes) +
                          ",\"ts\":\"" + currentDate + " " + currentTime + "\"}";
    
    pActivityCharacteristic->setValue(activityJson.c_str());
    pActivityCharacteristic->notify();
    
    if (gpsValid) {
        String gpsJson = "{\"lat\":" + String(currentLat, 6) +
                         ",\"lng\":" + String(currentLng, 6) +
                         ",\"date\":\"" + currentDate + "\"" +
                         ",\"time\":\"" + currentTime + "\"}";
        
        pGpsCharacteristic->setValue(gpsJson.c_str());
        pGpsCharacteristic->notify();
    }
    
    int batteryLevel = readBatteryLevel();
    String batteryJson = "{\"bat\":" + String(batteryLevel) + "}";
    
    pBatteryCharacteristic->setValue(batteryJson.c_str());
    pBatteryCharacteristic->notify();
}
