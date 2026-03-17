package com.example.habittracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.notifications.ReminderScheduler
import com.example.habittracker.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationPreviewItem(
    val uniqueKey: String,
    val title: String,
    val message: String,
    val triggerAtMillis: Long
)

data class NotificationsUiState(
    val upcoming: List<NotificationPreviewItem> = emptyList()
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: HabitRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeReminderScheduleItems().collect { scheduleItems ->
                val now = System.currentTimeMillis()
                val nextItems = scheduleItems.mapNotNull { item ->
                    val nextTrigger = item.triggerAtMillis ?: nextDailyTrigger(item.timeValue) ?: return@mapNotNull null
                    if (nextTrigger <= now) return@mapNotNull null
                    NotificationPreviewItem(
                        uniqueKey = item.uniqueKey,
                        title = item.title,
                        message = item.message,
                        triggerAtMillis = nextTrigger
                    )
                }.sortedBy { it.triggerAtMillis }

                mutableUiState.update { current ->
                    current.copy(upcoming = nextItems)
                }
            }
        }
    }

    fun formatTrigger(millis: Long): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(formatter)
    }

    fun snooze(item: NotificationPreviewItem, durationMillis: Long) {
        val targetMillis = System.currentTimeMillis() + durationMillis
        val timeValue = Instant.ofEpochMilli(targetMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(DateTimeFormatter.ofPattern("HH:mm"))
        reminderScheduler.schedule(
            HabitRepository.ReminderScheduleItem(
                uniqueKey = "${item.uniqueKey}:snooze:$targetMillis",
                title = item.title,
                message = item.message,
                timeValue = timeValue,
                triggerAtMillis = targetMillis
            )
        )
    }

    private fun nextDailyTrigger(timeValue: String): Long? {
        val parts = timeValue.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null

        val now = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
