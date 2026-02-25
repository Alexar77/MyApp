package com.example.habittracker.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.habittracker.R
import com.example.habittracker.repository.HabitRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val uniqueKey = intent.getStringExtra(EXTRA_UNIQUE_KEY) ?: return
        val timeValue = intent.getStringExtra(EXTRA_TIME).orEmpty().ifBlank { "00:00" }
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Reminder" }
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty().ifBlank { "Time to check MyApp" }
        val skipReschedule = intent.getBooleanExtra(EXTRA_SKIP_RESCHEDULE, false)

        val reminder = HabitRepository.ReminderScheduleItem(
            uniqueKey = uniqueKey,
            title = title,
            message = message,
            timeValue = timeValue
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                if (!skipReschedule) scheduler.schedule(reminder)
                return
            }
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Daily reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setColor(Color.RED)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(uniqueKey.hashCode(), notification)

        if (!skipReschedule) scheduler.schedule(reminder)
    }

    companion object {
        const val EXTRA_UNIQUE_KEY = "extra_unique_key"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_SKIP_RESCHEDULE = "extra_skip_reschedule"
        const val CHANNEL_ID = "daily_reminders"
    }
}
