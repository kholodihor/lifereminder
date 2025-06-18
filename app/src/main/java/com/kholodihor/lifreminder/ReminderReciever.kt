package com.kholodihor.lifreminder

import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class ReminderReceiver : BroadcastReceiver() {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("REMINDER_MESSAGE") ?: "Time to exercise!"
        val reminderId = intent.getIntExtra("REMINDER_ID", -1)

        val channelId = "exercise_reminder_channel"
        val notificationId = reminderId.takeIf { it != -1 } ?: 1

        // Створюємо канал сповіщень для Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Exercise Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // Показуємо сповіщення
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.kholodihor.lifreminder.R.drawable.ic_notification)
            .setContentTitle("Exercise Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)

        // Запускаємо ReminderWorker для видалення з бази
        if (reminderId != -1) {
            val inputData = Data.Builder()
                .putInt("reminder_id", reminderId)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}


