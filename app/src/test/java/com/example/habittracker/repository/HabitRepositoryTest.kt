package com.example.habittracker.repository

import android.content.SharedPreferences
import com.example.habittracker.data.dao.BirthdayDao
import com.example.habittracker.data.dao.GoalDao
import com.example.habittracker.data.dao.HomeMonthSnapshotDao
import com.example.habittracker.data.dao.HabitCompletionDao
import com.example.habittracker.data.dao.HabitDao
import com.example.habittracker.data.dao.HabitDayNoteDao
import com.example.habittracker.data.dao.SubGoalDao
import com.example.habittracker.data.dao.TaskCategoryDao
import com.example.habittracker.data.dao.TaskDao
import com.example.habittracker.data.dao.WhoAmINoteDao
import com.example.habittracker.data.entity.Birthday
import com.example.habittracker.data.entity.Habit
import com.example.habittracker.data.entity.HabitCompletion
import com.example.habittracker.data.entity.HabitDayNote
import com.example.habittracker.data.entity.TaskItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.slot
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HabitRepositoryTest {
    private val habitDao = mockk<HabitDao>(relaxed = true)
    private val completionDao = mockk<HabitCompletionDao>(relaxed = true)
    private val noteDao = mockk<HabitDayNoteDao>(relaxed = true)
    private val whoAmINoteDao = mockk<WhoAmINoteDao>(relaxed = true)
    private val taskDao = mockk<TaskDao>(relaxed = true)
    private val taskCategoryDao = mockk<TaskCategoryDao>(relaxed = true)
    private val goalDao = mockk<GoalDao>(relaxed = true)
    private val subGoalDao = mockk<SubGoalDao>(relaxed = true)
    private val birthdayDao = mockk<BirthdayDao>(relaxed = true)
    private val homeMonthSnapshotDao = mockk<HomeMonthSnapshotDao>(relaxed = true)
    private val appPreferences = mockk<SharedPreferences>(relaxed = true)
    private val appPreferencesEditor = mockk<SharedPreferences.Editor>(relaxed = true)

    private lateinit var repository: HabitRepository

    @Before
    fun setUp() {
        every { habitDao.observeHabits() } returns emptyFlow()
        every { completionDao.observeAllCompletions() } returns emptyFlow()
        every { completionDao.observeCompletionsForHabit(any()) } returns emptyFlow()
        every { noteDao.observeNotesForHabit(any()) } returns emptyFlow()
        every { noteDao.observeAllNotes() } returns emptyFlow()
        every { whoAmINoteDao.observeAll() } returns emptyFlow()
        every { taskDao.observeAll() } returns emptyFlow()
        every { taskCategoryDao.observeAll() } returns emptyFlow()
        every { goalDao.observeAll() } returns emptyFlow()
        every { subGoalDao.observeAll() } returns emptyFlow()
        every { birthdayDao.observeAll() } returns emptyFlow()
        every { appPreferences.edit() } returns appPreferencesEditor
        every { appPreferencesEditor.putString(any(), any()) } returns appPreferencesEditor

        repository = HabitRepository(
            habitDao = habitDao,
            completionDao = completionDao,
            habitDayNoteDao = noteDao,
            whoAmINoteDao = whoAmINoteDao,
            taskDao = taskDao,
            taskCategoryDao = taskCategoryDao,
            goalDao = goalDao,
            subGoalDao = subGoalDao,
            birthdayDao = birthdayDao,
            homeMonthSnapshotDao = homeMonthSnapshotDao,
            appPreferences = appPreferences
        )
    }

    @Test
    fun `createHabit normalizes weekly input before insert`() = runTest {
        coEvery { habitDao.getMaxSortOrder() } returns 2

        repository.createHabit(
            name = "  Read  ",
            frequencyTypeValue = "WEEKLY",
            frequencyIntervalDays = 99,
            frequencyWeekdays = "7,2,2,9",
            reminderEnabled = true,
            reminderTime = "09:00, 09:00, 18:30",
            reminderMessage = "  Keep going  "
        )

        val insertedHabit = slot<Habit>()
        coVerify { habitDao.insertHabit(capture(insertedHabit)) }
        assertEquals("Read", insertedHabit.captured.name)
        assertEquals(3, insertedHabit.captured.sortOrder)
        assertEquals("WEEKLY", insertedHabit.captured.frequencyType)
        assertNull(insertedHabit.captured.frequencyIntervalDays)
        assertEquals("2,7", insertedHabit.captured.frequencyWeekdays)
        assertTrue(insertedHabit.captured.reminderEnabled)
        assertEquals("09:00,18:30", insertedHabit.captured.reminderTime)
        assertEquals("Keep going", insertedHabit.captured.reminderMessage)
    }

    @Test
    fun `createHabit ignores invalid reminder csv`() = runTest {
        repository.createHabit(
            name = "Hydrate",
            reminderEnabled = true,
            reminderTime = "bad-value"
        )

        coVerify(exactly = 0) { habitDao.insertHabit(any()) }
    }

    @Test
    fun `toggleCompletion flips existing completion state`() = runTest {
        coEvery { completionDao.getCompletion(5L, "2026-03-07") } returns
            HabitCompletion(id = 10L, habitId = 5L, date = "2026-03-07", completed = true)

        repository.toggleCompletion(5L, "2026-03-07")

        val completion = slot<HabitCompletion>()
        coVerify { completionDao.upsertCompletion(capture(completion)) }
        assertEquals(10L, completion.captured.id)
        assertFalse(completion.captured.completed)
    }

    @Test
    fun `saveHabitDayNote deletes existing note when content is blank`() = runTest {
        coEvery { noteDao.getNoteForDay(3L, "2026-03-01") } returns
            HabitDayNote(id = 8L, habitId = 3L, date = "2026-03-01", note = "Old")

        repository.saveHabitDayNote(3L, "2026-03-01", "   ")

        coVerify { noteDao.deleteById(8L) }
        coVerify(exactly = 0) { noteDao.upsert(any()) }
    }

    @Test
    fun `addTask requires a valid fallback time when one-time reminders are absent`() = runTest {
        repository.addTask(
            title = "Pay rent",
            reminderEnabled = true,
            reminderTime = "not-a-time",
            reminderDateTimesCsv = null
        )

        coVerify(exactly = 0) { taskDao.insert(any()) }
    }

    @Test
    fun `getReminderScheduleItems builds habit task and birthday reminders`() = runTest {
        val future = System.currentTimeMillis() + 60_000
        coEvery { habitDao.getAllHabits() } returns listOf(
            Habit(id = 1L, name = "Run", createdAt = 0L, reminderEnabled = true, reminderTime = "08:00,19:00")
        )
        coEvery { taskDao.getAll() } returns listOf(
            TaskItem(id = 2L, title = "Submit", isDone = false, createdAt = 0L, reminderEnabled = true, reminderDateTimesCsv = "$future"),
            TaskItem(id = 3L, title = "Backup", isDone = false, createdAt = 0L, reminderEnabled = true, reminderTime = "21:30")
        )
        coEvery { birthdayDao.getAll() } returns listOf(
            Birthday(id = 4L, name = "Alex", year = 1990, month = 3, day = 7, reminderDateTimesCsv = "$future", createdAt = 0L)
        )

        val items = repository.getReminderScheduleItems()

        assertEquals(5, items.size)
        assertTrue(items.any { it.uniqueKey == "habit:1:08:00" && it.message == "Habit: Run" })
        assertTrue(items.any { it.uniqueKey == "habit:1:19:00" })
        assertTrue(items.any { it.uniqueKey == "task:2:one:0" && it.triggerAtMillis == future })
        assertTrue(items.any { it.uniqueKey == "task:3" && it.timeValue == "21:30" })
        assertTrue(items.any { it.uniqueKey == "birthday:4:0" && it.message == "Birthday: Alex" })
    }

    @Test
    fun `scheduled dates respect every n days cadence`() {
        val dates = repository.calculateScheduledDates(
            createdDate = LocalDate.of(2026, 3, 1),
            untilDate = LocalDate.of(2026, 3, 7),
            frequencyTypeValue = "EVERY_N_DAYS",
            frequencyIntervalDays = 3,
            frequencyWeekdays = null
        )

        assertEquals(setOf("2026-03-01", "2026-03-04", "2026-03-07"), dates)
    }

    @Test
    fun `birthday occurrence clamps invalid leap day for non leap year`() {
        assertEquals(
            LocalDate.of(2025, 2, 28),
            repository.birthdayOccurrenceInYear(month = 2, day = 29, year = 2025)
        )
    }

    @Test
    fun `currentBusinessDate uses five hour rollover`() {
        assertEquals(
            LocalDate.of(2026, 3, 6),
            repository.currentBusinessDate(LocalDateTime.of(2026, 3, 7, 4, 59))
        )
        assertEquals(
            LocalDate.of(2026, 3, 7),
            repository.currentBusinessDate(LocalDateTime.of(2026, 3, 7, 5, 0))
        )
    }
}
