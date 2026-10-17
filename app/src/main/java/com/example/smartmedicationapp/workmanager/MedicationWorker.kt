package com.example.smartmedicationapp.workmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class MedicationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val medId = inputData.getInt("MED_ID", 0)
        val medName = inputData.getString("MED_NAME") ?: "Medication"
        val dosage = inputData.getString("MED_DOSAGE") ?: ""

        showNotification(medId, medName, dosage)

        return Result.success()
    }

    private fun showNotification(id: Int, name: String, dosage: String) {
        val channelId = "med_reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled medication times"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MarkAsTakenReceiver::class.java).apply {
            putExtra("MED_ID", id)
            action = "com.example.smartmedicationapp.ACTION_MARK_TAKEN_$id"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(name)
            .setContentText("$dosage - Time to take your meds!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Mark as Taken",
                pendingIntent
            )
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }
}