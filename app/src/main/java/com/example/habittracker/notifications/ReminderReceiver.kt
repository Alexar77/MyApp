package com.example.habittracker.notifications

import android.Manifest
import android.app.PendingIntent
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val uniqueKey = intent.getStringExtra(EXTRA_UNIQUE_KEY) ?: return
        val timeValue = intent.getStringExtra(EXTRA_TIME).orEmpty().ifBlank { "00:00" }
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Reminder" }
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty().ifBlank { "Time to check MyApp" }
        val skipReschedule = intent.getBooleanExtra(EXTRA_SKIP_RESCHEDULE, false)
        val notificationId = uniqueKey.hashCode()

        if (action == ACTION_SNOOZE_1_MINUTE || action == ACTION_SNOOZE_1_HOUR || action == ACTION_SNOOZE_1_DAY) {
            val durationMillis = when (action) {
                ACTION_SNOOZE_1_MINUTE -> TimeUnit.MINUTES.toMillis(1)
                ACTION_SNOOZE_1_HOUR -> TimeUnit.HOURS.toMillis(1)
                else -> TimeUnit.DAYS.toMillis(1)
            }
            val targetMillis = System.currentTimeMillis() + durationMillis
            val snoozeTimeValue = Instant.ofEpochMilli(targetMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .format(DateTimeFormatter.ofPattern("HH:mm"))
            scheduler.schedule(
                HabitRepository.ReminderScheduleItem(
                    uniqueKey = "$uniqueKey:snooze:$targetMillis",
                    title = title,
                    message = message,
                    timeValue = snoozeTimeValue,
                    triggerAtMillis = targetMillis
                )
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(notificationId)
            return
        }

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

        val snooze1MinuteIntent = Intent(context, ReminderReceiver::class.java).apply {
            setAction(ACTION_SNOOZE_1_MINUTE)
            putExtra(EXTRA_UNIQUE_KEY, uniqueKey)
            putExtra(EXTRA_TIME, timeValue)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_SKIP_RESCHEDULE, true)
        }
        val snooze1HourIntent = Intent(context, ReminderReceiver::class.java).apply {
            setAction(ACTION_SNOOZE_1_HOUR)
            putExtra(EXTRA_UNIQUE_KEY, uniqueKey)
            putExtra(EXTRA_TIME, timeValue)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_SKIP_RESCHEDULE, true)
        }
        val snooze1DayIntent = Intent(context, ReminderReceiver::class.java).apply {
            setAction(ACTION_SNOOZE_1_DAY)
            putExtra(EXTRA_UNIQUE_KEY, uniqueKey)
            putExtra(EXTRA_TIME, timeValue)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_SKIP_RESCHEDULE, true)
        }
        val snooze1MinutePendingIntent = PendingIntent.getBroadcast(
            context,
            "$uniqueKey:snooze1m".hashCode(),
            snooze1MinuteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snooze1HourPendingIntent = PendingIntent.getBroadcast(
            context,
            "$uniqueKey:snooze1h".hashCode(),
            snooze1HourIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snooze1DayPendingIntent = PendingIntent.getBroadcast(
            context,
            "$uniqueKey:snooze1d".hashCode(),
            snooze1DayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setColor(Color.RED)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, "Snooze 1m", snooze1MinutePendingIntent)
            .addAction(0, "Snooze 1h", snooze1HourPendingIntent)
            .addAction(0, "Snooze 1d", snooze1DayPendingIntent)
            .build()

        manager.notify(notificationId, notification)

        if (!skipReschedule) scheduler.schedule(reminder)
    }

    companion object {
        const val EXTRA_UNIQUE_KEY = "extra_unique_key"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_SKIP_RESCHEDULE = "extra_skip_reschedule"
        const val CHANNEL_ID = "daily_reminders"
        const val ACTION_SNOOZE_1_MINUTE = "com.example.habittracker.notifications.SNOOZE_1_MINUTE"
        const val ACTION_SNOOZE_1_HOUR = "com.example.habittracker.notifications.SNOOZE_1_HOUR"
        const val ACTION_SNOOZE_1_DAY = "com.example.habittracker.notifications.SNOOZE_1_DAY"
    }
}
