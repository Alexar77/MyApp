# MyApp - Project Handover and Developer Guide

This document summarizes the current state of the app, architecture, major flows, and what a new developer should know before making changes.

## 1) Project Snapshot

- App name: `MyApp`
- Package: `com.example.habittracker`
- Language/UI: Kotlin + Jetpack Compose (Material 3)
- Architecture: MVVM + Repository + Room + Hilt
- Build:
  - `compileSdk = 35`
  - `targetSdk = 35`
  - `minSdk = 24`
  - Java/Kotlin target: 17
  - Version: `versionCode = 69`, `versionName = "69"`
- Local-only app (no cloud sync/auth)

Key files:
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/strings.xml`

## 2) Current Navigation and Screens

Bottom navigation is currently:
- Home (`MainScreen`)
- Who am I? (`WhoAmIScreen`)
- Tasks (`TasksScreen`)
- Goals (`GoalsScreen`)

Navigation file:
- `app/src/main/java/com/example/habittracker/ui/AppNavGraph.kt`

Notes:
- The old Reminders page was removed from navigation.
- Reminders are now per-habit and per-task options in add/edit dialogs.

## 3) Feature Summary

### Home (Habits + Calendar)

File: `ui/screens/MainScreen.kt`

- Add habit via FAB dialog.
- "More options" in add-habit dialog includes:
  - Created date
  - Reminder toggle + reminder time
- Edit selected habit from top bar edit icon.
- Delete selected habit from top bar delete icon.
- Month calendar supports:
  - Toggle completion on tap
  - Day note on long-press
  - Month navigation arrows
- Calendar behavior is constrained by habit creation date and business day.

Calendar rules:
- Week starts Monday.
- `business day` is defined as `now - 5 hours`.
  - Example: Tuesday 04:00 is treated as Monday.
- Can toggle only from habit `createdDate` to `businessToday`.
- For dates between created day and yesterday:
  - completed => green/check
  - not completed => red X
- Today is not shown as red X when incomplete.

Calendar component:
- `ui/components/MonthCalendar.kt`

### Stats

- Stats ViewModel and screen exist:
  - `ui/screens/StatsScreen.kt`
  - `ui/viewmodel/StatsViewModel.kt`
- Not currently routed from bottom nav.

### Who am I?

File: `ui/screens/WhoAmIScreen.kt`

- Add note via dialog.
- Notes collapse/expand.
- Edit note content in expanded view.
- Copy note content button.
- Delete with confirmation.
- Pull-to-refresh UI behavior present.

### Tasks

File: `ui/screens/TasksScreen.kt`

- Add task via dialog.
- Split list into To Do / Done.
- Toggle done/undone.
- Edit task (title + reminder options).
- Delete with confirmation.
- Pull-to-refresh UI behavior present.

### Goals

File: `ui/screens/GoalsScreen.kt`

- Add goals.
- Add subgoals under each goal.
- Expand/collapse per goal.
- Goal done toggle and subgoal done toggle.
- Delete confirmations for goals/subgoals.
- Subgoals have visible bordered row style.
- Pull-to-refresh UI behavior present.

## 4) Data Layer and Room

### Database

- DB name: `habit_tracker.db`
- Version: `6`
- File: `data/database/AppDatabase.kt`

Entities in DB registration:
- `Habit`
- `HabitCompletion`
- `HabitDayNote`
- `WhoAmINote`
- `TaskItem`
- `Goal`
- `SubGoal`
- `ReminderSettings` (legacy)
- `ReminderTime` (legacy)

### Migrations

File: `data/database/DatabaseMigrations.kt`

Migrations included:
- `1 -> 2`: day notes + who am I notes
- `2 -> 3`: tasks + goals + sub_goals
- `3 -> 4`: `goals.isDone`
- `4 -> 5`: `reminder_settings` + `reminder_times`
- `5 -> 6`: add `reminderEnabled` + `reminderTime` to `habits` and `tasks`

### Important tables

#### Habit
File: `data/entity/Habit.kt`

- `id`
- `name`
- `createdAt`
- `reminderEnabled`
- `reminderTime`

#### HabitCompletion
File: `data/entity/HabitCompletion.kt`

- `id`
- `habitId`
- `date` (`YYYY-MM-DD`)
- `completed`
- Unique index on `(habitId, date)` to prevent duplicates

#### TaskItem
File: `data/entity/TaskItem.kt`

- `id`
- `title`
- `isDone`
- `createdAt`
- `reminderEnabled`
- `reminderTime`

Other entities:
- `Goal`, `SubGoal`, `WhoAmINote`, `HabitDayNote`

## 5) Repository and Business Logic

Primary file:
- `repository/HabitRepository.kt`

It handles:
- Habit CRUD + completion toggling
- Day note save/remove behavior
- WhoAmI notes CRUD
- Task CRUD/toggle
- Goal/subgoal CRUD/toggle
- Stats calculations:
  - current streak
  - longest streak
  - total completions
- Reminder schedule item generation from habits/tasks
- Date helpers:
  - `currentBusinessDate()` (5 AM rollover behavior)
  - date <-> epoch conversions

Reminder generation currently includes:
- Habit reminders when `habit.reminderEnabled == true`
- Task reminders when `task.reminderEnabled == true` AND task is not done

## 6) DI and Module Wiring

Hilt module:
- `di/AppModule.kt`

Provides:
- Room DB singleton
- DAOs
- Repository singleton

App entry points:
- `MyAppApplication.kt` with `@HiltAndroidApp`
- `MainActivity.kt`

## 7) Notifications and Alarm Scheduling

Files:
- `notifications/ReminderScheduler.kt`
- `notifications/ReminderReceiver.kt`
- `notifications/BootCompletedReceiver.kt`

Manifest wiring:
- Permission: `POST_NOTIFICATIONS`
- Permission: `RECEIVE_BOOT_COMPLETED`
- Broadcast receiver for reminder events
- Broadcast receiver for boot complete reschedule

Current behavior:
- Scheduler stores scheduled request codes in SharedPreferences.
- Each reminder is keyed by `habit:<id>` or `task:<id>`.
- Uses exact alarm when allowed; falls back gracefully when not.
- On receive:
  - Checks notification permission
  - Shows notification
  - Reschedules same reminder for next day
- On device reboot:
  - Rebuilds reminder list from repository
  - Reschedules all

## 8) UI/Theme and App Icon

Theme:
- Dark-first custom Material 3 theme files in `ui/theme/`

Launcher icon:
- Foreground: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Active logo source: `app/src/main/res/drawable/myapp_logo.jpg`
- Manifest icon points to `ic_launcher_v2` and `ic_launcher_round_v2`

## 9) Pull-to-Refresh

Implemented in main screens with Compose Material pull refresh:
- Home
- WhoAmI
- Tasks
- Goals

Indicator styling adjusted for dark UI.

## 10) Known Issues / Tech Debt (Important)

1. Legacy reminder tables are still in schema
- `ReminderSettings` and `ReminderTime` entities/DAOs remain in DB and `AppDatabase` but are no longer used in current per-item reminder flow.
- Safe cleanup requires:
  - schema migration(s)
  - removing entities/DAOs from DB registration
  - removing leftover files once migration plan is defined

2. Current syntax issue in `TasksScreen`
- There is a duplicated `dismissButton = {` block in Add Task dialog.
- File: `app/src/main/java/com/example/habittracker/ui/screens/TasksScreen.kt`
- This must be fixed if build fails around that section.

3. Picker UX implementation is evolving
- Date/time picker click handling was changed multiple times.
- If field taps regress again, verify gesture interception around `OutlinedTextField` wrappers.

4. Build environment caveat
- In some shells, Gradle commands fail if `JAVA_HOME` is not set.

## 11) How To Run / Build

From project root:

```bash
./gradlew :app:assembleDebug
```

If using Windows CMD/PowerShell:

```powershell
gradlew.bat :app:assembleDebug
```

If Java error appears, set `JAVA_HOME` to your JDK 17 installation.

## 12) Where to Make Common Changes

- Add a new bottom tab/screen:
  - `ui/AppNavGraph.kt`
- Habit add/edit logic:
  - `ui/screens/MainScreen.kt`
  - `ui/viewmodel/MainViewModel.kt`
  - `repository/HabitRepository.kt`
- Task add/edit logic:
  - `ui/screens/TasksScreen.kt`
  - `ui/viewmodel/TasksViewModel.kt`
  - `repository/HabitRepository.kt`
- Calendar visuals/rules:
  - `ui/components/MonthCalendar.kt`
  - `ui/viewmodel/MainViewModel.kt`
- Notification scheduling behavior:
  - `notifications/ReminderScheduler.kt`
  - `notifications/ReminderReceiver.kt`

## 13) Recommended Next Cleanup Steps

1. Fix `TasksScreen` dialog syntax issue (duplicate `dismissButton` block).
2. Add unit tests for:
- streak calculations
- business-day cutoff behavior (5 AM)
- reminder schedule generation
3. Remove legacy reminder tables/DAOs through a proper migration.
4. Add instrumentation checks for picker dialogs in add/edit flows.

---

If you are taking over this codebase, start by reading in this order:
1. `repository/HabitRepository.kt`
2. `ui/viewmodel/MainViewModel.kt`, `ui/viewmodel/TasksViewModel.kt`
3. `ui/screens/MainScreen.kt`, `ui/screens/TasksScreen.kt`
4. `ui/components/MonthCalendar.kt`
5. `notifications/*`
