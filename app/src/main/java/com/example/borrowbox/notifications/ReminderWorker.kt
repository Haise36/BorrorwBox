package com.example.borrowbox.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.borrowbox.MainActivity
import com.example.borrowbox.R
import com.example.borrowbox.data.local.BorrowBoxDatabase
import com.example.borrowbox.data.local.toDomainModel
import com.example.borrowbox.data.model.BorrowingStatus
import com.example.borrowbox.data.model.calculateCalendarDaysBetween
import com.example.borrowbox.data.model.calculateStatus
import com.example.borrowbox.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settingsRepo = SettingsRepository(applicationContext)
        val notificationsEnabled = settingsRepo.notificationsEnabled.first()
        val reminderDays = settingsRepo.reminderDays.first()
        
        if (!notificationsEnabled) return Result.success()

        createNotificationChannel(applicationContext)

        val database = BorrowBoxDatabase.getInstance(applicationContext)
        val entities = database.borrowingDao().observeActiveBorrowings().first()
        val now = System.currentTimeMillis()

        entities.forEach { entity ->
            val borrowing = entity.toDomainModel()
            val status = borrowing.calculateStatus(now)
            
            val isDueToday = status == BorrowingStatus.DUE_TODAY
            val isOverdue = status == BorrowingStatus.OVERDUE
            
            val daysUntilDue = calculateCalendarDaysBetween(now, borrowing.expectedReturnDate)
            val shouldRemindSoon = daysUntilDue == reminderDays && reminderDays > 0

            if (isDueToday || isOverdue || shouldRemindSoon) {
                sendNotification(borrowing.id, borrowing.itemName, status, daysUntilDue, now)
            }
        }
        return Result.success()
    }

    private fun sendNotification(itemId: Long, itemName: String, status: BorrowingStatus, daysUntilDue: Int, now: Long) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("borrowing_id", itemId)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            itemId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = when (status) {
            BorrowingStatus.DUE_TODAY -> "Your $itemName is due today!"
            BorrowingStatus.OVERDUE -> "Your $itemName is overdue!"
            else -> "Your $itemName is due in $daysUntilDue day${if (daysUntilDue > 1) "s" else ""}."
        }

        val iconRes = if (applicationContext.applicationInfo.icon != 0) {
            applicationContext.applicationInfo.icon
        } else {
            R.drawable.app_logo
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle("BorrowBox Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val todayDateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(now))
        val notificationId = "${itemId}_$todayDateStr".hashCode()
        manager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "borrowbox_reminders"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val existing = manager.getNotificationChannel(CHANNEL_ID)
                if (existing == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "BorrowBox Reminders",
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    manager.createNotificationChannel(channel)
                }
            }
        }

        fun schedule(context: Context) {
            createNotificationChannel(context)
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "borrowbox_daily_check",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
