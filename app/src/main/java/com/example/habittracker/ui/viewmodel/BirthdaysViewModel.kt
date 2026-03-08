package com.example.habittracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.notifications.ReminderScheduler
import com.example.habittracker.repository.HabitRepository
import com.example.habittracker.util.DebugLog
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BirthdayUiItem(
    val id: Long,
    val name: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val reminderDateTimes: List<Long>,
    val nextOccurrence: LocalDate
)

data class BirthdaysUiState(
    val birthdays: List<BirthdayUiItem> = emptyList()
)

@HiltViewModel
class BirthdaysViewModel @Inject constructor(
    private val repository: HabitRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {
    private val logTag = "BirthdaysVM"

    private val mutableUiState = MutableStateFlow(BirthdaysUiState())
    val uiState: StateFlow<BirthdaysUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeBirthdays().collect { birthdayItems ->
                DebugLog.d(logTag, "flow update birthdays=${birthdayItems.size}")
                val today = repository.currentBusinessDate()
                val sorted = birthdayItems
                    .map { birthday ->
                        BirthdayUiItem(
                            id = birthday.id,
                            name = birthday.name,
                            year = birthday.year,
                            month = birthday.month,
                            day = birthday.day,
                            reminderDateTimes = repository.parseReminderDateTimesCsv(birthday.reminderDateTimesCsv),
                            nextOccurrence = repository.nextBirthdayOccurrence(
                                fromDate = today,
                                month = birthday.month,
                                day = birthday.day
                            )
                        )
                    }
                    .sortedWith(
                        compareBy<BirthdayUiItem> { it.nextOccurrence }
                            .thenBy { it.name.lowercase() }
                    )

                mutableUiState.update { currentState ->
                    currentState.copy(birthdays = sorted)
                }
                DebugLog.d(logTag, "uiState publish birthdays=${sorted.size}")
            }
        }
    }

    fun addBirthday(name: String, date: LocalDate, reminderDateTimes: List<Long>) {
        DebugLog.d(logTag, "addBirthday nameLength=${name.length} date=$date reminders=${reminderDateTimes.size}")
        viewModelScope.launch {
            val id = repository.addBirthday(
                name = name,
                year = date.year,
                month = date.monthValue,
                day = date.dayOfMonth
            )
            if (id > 0L) {
                repository.updateBirthday(
                    birthdayId = id,
                    name = name,
                    year = date.year,
                    month = date.monthValue,
                    day = date.dayOfMonth,
                    reminderDateTimesCsv = reminderDateTimes.joinToString("|")
                )
                rescheduleReminders("addBirthday")
                DebugLog.d(logTag, "addBirthday completed id=$id")
            }
        }
    }

    fun deleteBirthday(id: Long) {
        DebugLog.d(logTag, "deleteBirthday id=$id")
        viewModelScope.launch {
            repository.deleteBirthday(id)
            rescheduleReminders("deleteBirthday")
        }
    }

    fun updateBirthday(
        id: Long,
        name: String,
        date: LocalDate,
        reminderDateTimes: List<Long>
    ) {
        DebugLog.d(logTag, "updateBirthday id=$id date=$date reminders=${reminderDateTimes.size}")
        viewModelScope.launch {
            repository.updateBirthday(
                birthdayId = id,
                name = name,
                year = date.year,
                month = date.monthValue,
                day = date.dayOfMonth,
                reminderDateTimesCsv = reminderDateTimes.joinToString("|")
            )
            rescheduleReminders("updateBirthday")
            DebugLog.d(logTag, "updateBirthday completed id=$id")
        }
    }

    private suspend fun rescheduleReminders(reason: String) {
        val reminders = repository.getReminderScheduleItems()
        DebugLog.d(logTag, "rescheduleReminders reason=$reason count=${reminders.size}")
        reminderScheduler.rescheduleAll(reminders)
    }
}
