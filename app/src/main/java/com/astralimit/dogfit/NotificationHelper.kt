package com.astralimit.dogfit

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

object NotificationHelper {
    
    const val CHANNEL_ID_MEDICAL = "dogfit_medical_reminders"
    const val CHANNEL_ID_ACTIVITY = "dogfit_alerts_channel"
    
    fun showMedicalReminder(
        context: Context,
        title: String,
        message: String,
        notificationId: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "pet_profile")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            notificationId, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MEDICAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "Notification permission not granted", e)
        }
    }
    
    fun scheduleMedicalReminder(
        context: Context,
        reminderType: String,
        itemName: String,
        dueTimeMillis: Long
    ) {
        val oneWeekBefore = dueTimeMillis - (7L * 24 * 60 * 60 * 1000)
        val now = System.currentTimeMillis()
        
        val sanitizedName = itemName.hashCode().toString()
        
        if (oneWeekBefore > now) {
            val delayWeek = oneWeekBefore - now
            scheduleNotificationWork(
                context,
                "week_${reminderType}_$sanitizedName",
                "Recordatorio: $itemName",
                "$reminderType vence en 1 semana",
                delayWeek
            )
        }
        
        if (dueTimeMillis > now) {
            val delayDay = dueTimeMillis - now
            scheduleNotificationWork(
                context,
                "day_${reminderType}_$sanitizedName",
                "¡URGENTE! $itemName",
                "$reminderType vence HOY",
                delayDay
            )
        }
    }
    
    private fun scheduleNotificationWork(
        context: Context,
        uniqueWorkName: String,
        title: String,
        message: String,
        delayMillis: Long
    ) {
        val data = workDataOf(
            "title" to title,
            "message" to message
        )
        
        val notificationRequest = OneTimeWorkRequestBuilder<MedicalReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.REPLACE,
                notificationRequest
            )
    }
}

class MedicalReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    
    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "DogFit Recordatorio"
        val message = inputData.getString("message") ?: "Tienes un recordatorio pendiente"
        
        NotificationHelper.showMedicalReminder(
            applicationContext,
            title,
            message,
            System.currentTimeMillis().toInt()
        )
        
        return Result.success()
    }
}
