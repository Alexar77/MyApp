package com.example.habittracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun rescheduleAll(times: List<String>, habitsEnabled: Boolean, tasksEnabled: Boolean) {
        cancelAll()
        if (!habitsEnabled && !tasksEnabled) return

        times.forEach { timeValue ->
            scheduleAtTime(timeValue, habitsEnabled, tasksEnabled)
        }
    }

    fun scheduleAtTime(timeValue: String, habitsEnabled: Boolean, tasksEnabled: Boolean) {
        val parts = timeValue.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val now = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TIME, timeValue)
            putExtra(ReminderReceiver.EXTRA_HABITS, habitsEnabled)
            putExtra(ReminderReceiver.EXTRA_TASKS, tasksEnabled)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeForTime(hour, minute),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = target.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            val canUseExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            if (canUseExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                // Graceful fallback on Android 12+ when exact-alarm permission is not granted.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Final fallback if OEM/OS throws despite capability checks.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelAll() {
        for (minuteOfDay in 0 until 24 * 60) {
            val hour = minuteOfDay / 60
            val minute = minuteOfDay % 60
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCodeForTime(hour, minute),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    private fun requestCodeForTime(hour: Int, minute: Int): Int = hour * 100 + minute
}
