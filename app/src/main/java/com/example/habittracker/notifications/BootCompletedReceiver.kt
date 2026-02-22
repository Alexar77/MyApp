package com.example.habittracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.habittracker.repository.HabitRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: HabitRepository

    @Inject
    lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val config = repository.getReminderConfig()
            scheduler.rescheduleAll(
                times = config.times.map { it.timeValue },
                habitsEnabled = config.habitsEnabled,
                tasksEnabled = config.tasksEnabled
            )
        }
    }
}
