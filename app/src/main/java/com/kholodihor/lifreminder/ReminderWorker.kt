package com.kholodihor.lifreminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.room.Room

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // Ініціалізація бази даних і DAO
    private val db = Room.databaseBuilder(
        appContext,
        ReminderDatabase::class.java,
        "reminders_db"
    ).build()

    private val dao = db.reminderDao()

    override suspend fun doWork(): Result {
        // Отримуємо id нагадування з вхідних даних
        val reminderId = inputData.getInt("reminder_id", -1)
        if (reminderId == -1) {
            return Result.failure()
        }

        // Показуємо нотифікацію
        showNotification("Нагадування", "Час зробити вправу!")

        // Видаляємо нагадування з бази
        dao.deleteById(reminderId)

        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Reminder Channel"
            val descriptionText = "Канал для нагадувань"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Можна замінити на свою іконку
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        try {
            with(NotificationManagerCompat.from(applicationContext)) {
                notify(1001, builder.build())
            }
        } catch (e: SecurityException) {
            // Логування або ігнорування, якщо немає дозволу
        }
    }
}
