package com.example.habittracker.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.habittracker.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val timeValue = intent.getStringExtra(EXTRA_TIME) ?: return
        val habitsEnabled = intent.getBooleanExtra(EXTRA_HABITS, true)
        val tasksEnabled = intent.getBooleanExtra(EXTRA_TASKS, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                scheduler.scheduleAtTime(timeValue, habitsEnabled, tasksEnabled)
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

        val message = when {
            habitsEnabled && tasksEnabled -> "Time to complete your habits and tasks"
            habitsEnabled -> "Time to complete your habits"
            tasksEnabled -> "Time to complete your tasks"
            else -> "Reminder"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("MyApp Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(timeValue.hashCode(), notification)

        scheduler.scheduleAtTime(timeValue, habitsEnabled, tasksEnabled)
    }

    companion object {
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_HABITS = "extra_habits"
        const val EXTRA_TASKS = "extra_tasks"
        const val CHANNEL_ID = "daily_reminders"
    }
}
