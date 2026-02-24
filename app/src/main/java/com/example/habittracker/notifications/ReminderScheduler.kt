package com.example.habittracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.habittracker.repository.HabitRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun rescheduleAll(items: List<HabitRepository.ReminderScheduleItem>) {
        val previousRequestCodes = prefs.getStringSet(KEY_REQUEST_CODES, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            .orEmpty()

        previousRequestCodes.forEach { cancelByRequestCode(it) }

        val nextRequestCodes = mutableSetOf<String>()
        items.forEach { item ->
            val requestCode = schedule(item)
            nextRequestCodes += requestCode.toString()
        }

        prefs.edit().putStringSet(KEY_REQUEST_CODES, nextRequestCodes).apply()
    }

    fun schedule(item: HabitRepository.ReminderScheduleItem): Int {
        val parts = item.timeValue.split(":")
        if (parts.size != 2) return requestCodeFor(item.uniqueKey)

        val hour = parts[0].toIntOrNull() ?: return requestCodeFor(item.uniqueKey)
        val minute = parts[1].toIntOrNull() ?: return requestCodeFor(item.uniqueKey)

        val now = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)

        val requestCode = requestCodeFor(item.uniqueKey)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_UNIQUE_KEY, item.uniqueKey)
            putExtra(ReminderReceiver.EXTRA_TIME, item.timeValue)
            putExtra(ReminderReceiver.EXTRA_TITLE, item.title)
            putExtra(ReminderReceiver.EXTRA_MESSAGE, item.message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

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
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }

        return requestCode
    }

    private fun cancelByRequestCode(requestCode: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun requestCodeFor(uniqueKey: String): Int = uniqueKey.hashCode()

    companion object {
        private const val PREFS_NAME = "reminder_scheduler"
        private const val KEY_REQUEST_CODES = "scheduled_request_codes"
    }
}
