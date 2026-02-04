# DogFit ESP32-C3 Firmware

Firmware para el collar inteligente DogFit que monitorea la actividad física de tu mascota.

## Hardware Requerido

- **ESP32-C3 Super Mini** - Microcontrolador principal
- **MPU6050** - Acelerómetro/Giroscopio para detección de pasos
- **NEO-6MV2** - Módulo GPS para coordenadas y hora
- **Módulo MicroSD** - Almacenamiento cuando no hay conexión BLE
- **TP4056** - Cargador de batería Li-Ion
- **Mini560** - Convertidor Buck 3.3V
- **Batería 18650** - Fuente de alimentación

## Conexiones

### MPU6050 (I2C)
| MPU6050 | ESP32-C3 |
|---------|----------|
| VCC     | 3.3V     |
| GND     | GND      |
| SDA     | GPIO8    |
| SCL     | GPIO9    |

### GPS NEO-6MV2 (UART)
| GPS     | ESP32-C3 |
|---------|----------|
| VCC     | 3.3V     |
| GND     | GND      |
| TX      | GPIO20   |
| RX      | GPIO21   |

### Módulo MicroSD (SPI)
| SD Card | ESP32-C3 |
|---------|----------|
| VCC     | 3.3V     |
| GND     | GND      |
| MOSI    | GPIO6    |
| MISO    | GPIO5    |
| SCK     | GPIO4    |
| CS      | GPIO7    |

### Monitoreo de Batería
| Divisor Voltaje | ESP32-C3 |
|-----------------|----------|
| Punto Medio     | GPIO3    |

## Características

1. **Detección de Pasos**: Usa el acelerómetro MPU6050 para contar pasos
2. **GPS**: Registra coordenadas y obtiene fecha/hora del satélite
3. **Bluetooth Low Energy**: Envía datos en tiempo real a la app
4. **Almacenamiento SD**: Guarda datos cuando no hay conexión BLE
5. **Monitoreo de Batería**: Reporta nivel de batería

## UUIDs Bluetooth

- **Service UUID**: `4fafc201-1fb5-459e-8fcc-c5c9c331914b`
- **Activity Characteristic**: `beb5483e-36e1-4688-b7f5-ea07361b26a8`
- **GPS Characteristic**: `beb5483e-36e1-4688-b7f5-ea07361b26a9`
- **Battery Characteristic**: `beb5483e-36e1-4688-b7f5-ea07361b26aa`
- **Sync Characteristic**: `beb5483e-36e1-4688-b7f5-ea07361b26ab`

## Compilación

### Usando PlatformIO (Recomendado)
```bash
pio run
pio run --target upload
```

### Usando Arduino IDE
1. Instalar soporte para ESP32 en Board Manager:
   - Abrir Preferencias → URLs adicionales de gestor de tarjetas
   - Agregar: `https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json`
2. Seleccionar "ESP32C3 Dev Module" en Herramientas → Placa
3. Instalar librerías desde el Library Manager:
   - **TinyGPSPlus-ESP32** por Tinyu-Zhao (buscar "TinyGPSPlus ESP32")
   - Alternativamente, descargar manualmente de: https://github.com/mikalhart/TinyGPSPlus
4. Configurar en Herramientas:
   - USB CDC On Boot: Enabled
   - Upload Speed: 921600
5. Compilar y subir

**Nota**: En Arduino IDE la librería se llama "TinyGPSPlus-ESP32". 
En PlatformIO se instala automáticamente con `mikalhart/TinyGPSPlus`.

## Formato de Datos

### Actividad (JSON)
```json
{
  "steps": 1234,
  "activeMin": 45,
  "timestamp": "2024-01-15 14:30:00"
}
```

### GPS (JSON)
```json
{
  "lat": -33.456789,
  "lng": -70.654321,
  "date": "2024-01-15",
  "time": "14:30:00"
}
```

### Batería (JSON)
```json
{
  "level": 85,
  "voltage": 3.92
}
```

## Circuito de Alimentación

```
Batería 18650 → TP4056 → Mini560 (3.3V) → ESP32-C3 y sensores
                  ↓
             USB-C carga
```

## Consumo Estimado

- Modo activo con BLE: ~80mA
- Modo activo sin BLE: ~50mA
- Con batería 18650 (2600mAh): ~32-52 horas de uso
