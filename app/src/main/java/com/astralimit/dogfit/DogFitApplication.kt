package com.astralimit.dogfit

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

class DogFitApplication : Application() {

    companion object {
        const val CHANNEL_ID = "dogfit_service_channel"
        const val CHANNEL_NAME = "DogFit Service"
        const val CHANNEL_DESCRIPTION = "Servicio para monitoreo BLE de DogFit"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        setupExceptionHandler()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Canal para el servicio BLE
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // Canal para alertas
            val alertsChannel = NotificationChannel(
                "dogfit_alerts_channel",
                "DogFit Alertas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas y notificaciones importantes"
                setShowBadge(true)
                enableLights(true)
                lightColor = ContextCompat.getColor(this@DogFitApplication, android.R.color.holo_red_dark)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }

            // Canal para actualizaciones
            val updatesChannel = NotificationChannel(
                "dogfit_updates_channel",
                "DogFit Actualizaciones",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Actualizaciones de actividad y estado"
                setShowBadge(true)
            }

            // Canal para recordatorios médicos (vacunas/desparasitación)
            val medicalChannel = NotificationChannel(
                "dogfit_medical_reminders",
                "Recordatorios Médicos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios de vacunas y desparasitaciones"
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alertsChannel)
            notificationManager.createNotificationChannel(updatesChannel)
            notificationManager.createNotificationChannel(medicalChannel)
        }
    }

    private fun setupExceptionHandler() {
        // Manejo global de excepciones
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("DogFitApp", "Excepción no capturada en thread: ${thread.name}", throwable)
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(1)
        }
    }
}