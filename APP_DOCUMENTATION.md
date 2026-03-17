# MyApp Application Documentation

## App Summary

`MyApp` is a native Android habit-tracking and personal organization app built with Kotlin, Jetpack Compose, Room, and Hilt. The app combines:

- habit tracking on a monthly calendar
- daily notes attached to habit dates
- a personal "Who am I?" notes area
- categorized tasks with reminders
- goals and subgoals
- birthday tracking with one-time reminder timestamps
- a notifications preview/test screen

The primary live navigation is a bottom navigation bar with five tabs:

- Home
- Who am I?
- Motivation
- Tasks
- Goals

Two additional screens are reachable from the Home top bar:

- Birthdays
- Notifications

Two screens/components exist in code but are not part of the current live flow:

- `StatsScreen`
- `BlankScreen`

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Entry Points and Manifest](#entry-points-and-manifest)
- [Navigation Map](#navigation-map)
- [Screen-by-Screen Documentation](#screen-by-screen-documentation)
- [Button and Action Map](#button-and-action-map)
- [Components Overview](#components-overview)
- [ViewModels Overview](#viewmodels-overview)
- [Data Model and Persistence](#data-model-and-persistence)
- [Notifications and Background Behavior](#notifications-and-background-behavior)
- [File and Folder Map](#file-and-folder-map)
- [Key User Flows](#key-user-flows)
- [Uncertain Behavior and Non-Live Code](#uncertain-behavior-and-non-live-code)
- [Quick Onboarding Notes](#quick-onboarding-notes)

## Architecture Overview

The app follows a straightforward Compose + ViewModel + Repository + Room architecture.

- UI layer:
  - `app/src/main/java/com/example/habittracker/ui/AppNavGraph.kt`
  - `app/src/main/java/com/example/habittracker/ui/screens/*`
  - `app/src/main/java/com/example/habittracker/ui/components/*`
- State/business orchestration:
  - `app/src/main/java/com/example/habittracker/ui/viewmodel/*`
- Data/business logic:
  - `app/src/main/java/com/example/habittracker/repository/HabitRepository.kt`
- Persistence:
  - `app/src/main/java/com/example/habittracker/data/entity/*`
  - `app/src/main/java/com/example/habittracker/data/dao/*`
  - `app/src/main/java/com/example/habittracker/data/database/AppDatabase.kt`
- Dependency injection:
  - `app/src/main/java/com/example/habittracker/di/AppModule.kt`
- Reminder scheduling and broadcast handling:
  - `app/src/main/java/com/example/habittracker/notifications/*`

Typical flow:

1. Compose screen collects state from a Hilt-injected ViewModel.
2. ViewModel reads/writes through `HabitRepository`.
3. Repository talks to Room DAOs and computes derived data such as schedules, streaks, calendar snapshots, and reminder items.
4. For reminders, ViewModels call `ReminderScheduler.rescheduleAll(...)` after changes that affect notifications.

## Entry Points and Manifest

### Application startup

- `app/src/main/java/com/example/habittracker/MyAppApplication.kt`
  - `@HiltAndroidApp` application class.
- `app/src/main/java/com/example/habittracker/MainActivity.kt`
  - main launcher activity
  - wraps the app in `MyAppTheme`
  - renders `AppNavGraph()`

### Theme

- `app/src/main/java/com/example/habittracker/ui/theme/Theme.kt`
  - uses Material 3 `darkColorScheme()` only
- `app/src/main/java/com/example/habittracker/ui/theme/Type.kt`
  - uses default Material 3 typography

### Manifest-declared behavior

- `app/src/main/AndroidManifest.xml`
  - launcher activity: `.MainActivity`
  - application class: `.MyAppApplication`
  - permissions:
    - `POST_NOTIFICATIONS`
    - `RECEIVE_BOOT_COMPLETED`
  - receivers:
    - `.notifications.ReminderReceiver`
    - `.notifications.BootCompletedReceiver`

## Navigation Map

Navigation is defined in `app/src/main/java/com/example/habittracker/ui/AppNavGraph.kt`.

### Bottom navigation tabs

| Route | Label | Destination |
| --- | --- | --- |
| `home` | Home | `MainScreen` |
| `who_am_i` | Who am I? | `WhoAmIScreen` |
| `motivational` | Motivation | `MotivationScreen` |
| `tasks` | Tasks | `TasksScreen` |
| `goals` | Goals | `GoalsScreen` |

### Non-tab routes

| Route | Entry point | Destination |
| --- | --- | --- |
| `birthdays` | Home top bar cake icon | `BirthdaysScreen` |
| `notifications` | Home top bar bell icon | `NotificationsScreen` |

### Navigation notes

- Bottom bar is rendered by the root `Scaffold` in `AppNavGraph`, so it remains visible across all current destinations, including Birthdays and Notifications.
- Birthdays and Notifications are not bottom-bar items.
- There is special logic when the user is on `birthdays` and taps the Home bottom tab:
  - the nav controller tries to pop back to the existing Home destination instead of pushing a duplicate.
- There is no route for `StatsScreen`.
- There is no route for `BlankScreen`.

## Screen-by-Screen Documentation

### Screen: Home

- File: `app/src/main/java/com/example/habittracker/ui/screens/MainScreen.kt`
- ViewModel: `app/src/main/java/com/example/habittracker/ui/viewmodel/MainViewModel.kt`
- Purpose:
  - main habit dashboard
  - shows a global monthly overview
  - shows a selected habit's month calendar
  - allows creating, editing, deleting, selecting, and completing habits
  - allows attaching notes to individual habit dates
  - links to Birthdays and Notifications
- Entry path:
  - bottom nav `Home`
- Main UI sections:
  - top app bar with notification, birthdays, seed, edit, delete actions
  - floating action button for creating a habit
  - global "Month overview" card for all habits
  - selected-habit monthly card with month navigation and habit picker
- Data shown:
  - list of habits and their current streaks
  - selected month
  - selected habit completed dates
  - selected habit scheduled dates
  - global completed/scheduled/birthday/note dates
  - day notes for the selected habit
- User actions:
  - open Notifications screen
  - open Birthdays screen
  - seed large demo data set
  - add habit
  - edit selected habit
  - delete selected habit
  - switch month
  - choose habit from dropdown
  - tap day on selected habit calendar to toggle completion
  - long-press day on selected habit calendar to open day-note editor
  - tap day on global calendar to open aggregated detail dialog
- State/logic:
  - `MainViewModel` maintains selected month and selected habit
  - home screen caches and restores a monthly snapshot through both Room and `SharedPreferences`
  - calendar scheduling is computed by repository using habit frequency:
    - daily
    - selected weekdays
    - every N days
  - day toggles are blocked unless the date is:
    - on/after habit created date
    - on/before business today
    - actually scheduled for that habit
  - business today is `LocalDateTime.now().minusHours(5).toLocalDate()`
- Dependencies:
  - `MonthCalendar`
  - `HabitRepository`
  - `ReminderScheduler`
- Notes:
  - when there are no habits and no cached snapshot content, the screen shows an empty state
  - if cached snapshot content exists, the UI can temporarily show "Saved snapshot" before live data fully arrives
  - creating/editing a habit with reminders can trigger Android 13+ notification permission request
  - the selected habit dropdown label includes the current streak using a fire emoji

### Home dialogs and forms

- Create habit dialog:
  - basic field: habit name
  - optional advanced section toggled by `More options`
  - optional created date picker
  - frequency chips
  - weekly weekday chips when weekly is selected
  - numeric interval input when `EVERY_N_DAYS` is selected
  - reminder switch
  - reminder time picker plus multi-time chip list
  - optional reminder message
- Edit habit dialog:
  - same frequency/reminder controls, except no created-date editing
- Delete habit dialog:
  - deletes the currently selected habit
- Day note dialog:
  - saves or clears note for selected habit/date
  - empty note deletes the stored note row
- Global day details dialog:
  - shows birthdays on that date
  - shows each scheduled habit and whether it was done
  - shows day note text if present
- Seed test data dialog:
  - inserts demo habits, completions, notes, tasks, goals, birthdays, and reminders

### Screen: Who am I?

- File: `app/src/main/java/com/example/habittracker/ui/screens/WhoAmIScreen.kt`
- ViewModel: `app/src/main/java/com/example/habittracker/ui/viewmodel/WhoAmIViewModel.kt`
- Purpose:
  - stores free-form self-reflection or identity notes
- Entry path:
  - bottom nav `Who am I?`
- Main UI sections:
  - top bar with `Copy all notes`
  - floating action button for new note
  - lazy list of note cards
- Data shown:
  - note title
  - note content when expanded
- User actions:
  - create note
  - expand/collapse note
  - edit note content
  - save note content
  - rename note
  - delete note
  - copy single note content
  - copy all notes combined
  - reorder notes by long-press drag
- State/logic:
  - only one note is expanded at a time through `selectedNoteId`
  - text field changes update local UI immediately
  - persistence happens only when `Save` is tapped
  - a temporary saving spinner is shown via `savingNoteId`
  - reordering is optimistic and DB persistence is debounced by 300 ms
- Dependencies:
  - `HabitRepository.observeWhoAmINotes()`
- Notes:
  - pull-to-refresh is cosmetic only; it shows a spinner and waits ~650 ms but does not trigger a repository refresh

### Screen: Tasks

- File: `app/src/main/java/com/example/habittracker/ui/screens/TasksScreen.kt`
- ViewModel: `app/src/main/java/com/example/habittracker/ui/viewmodel/TasksViewModel.kt`
- Purpose:
  - task management with category filtering, reordering, completion tracking, and reminders
- Entry path:
  - bottom nav `Tasks`
- Main UI sections:
  - top bar with add-category and delete-category actions
  - category filter chips
  - pending tasks section
  - done tasks section
  - floating action button for adding task
- Data shown:
  - categories
  - selected category filter
  - pending tasks
  - completed tasks
  - completed date for done tasks
  - reminder metadata is not shown in rows, only in dialogs
- User actions:
  - select category filter
  - reorder category chips by long-press drag
  - add category
  - delete current category
  - add task
  - toggle task done/not done
  - open row overflow menu
  - edit task
  - transfer task to another category
  - delete task
  - reorder tasks within pending or done list by long-press drag
- State/logic:
  - category filter `All` is synthetic and not stored in DB
  - task categories are stored separately in `task_categories`
  - toggling a task updates `completedAt`
  - moving a task between pending/done also updates its sort order in the target done-state group
  - deleting a category deletes all tasks in that category first, then deletes the category record
  - transfering a task to a new category will create the target category if needed
  - reminders currently use one-time datetime timestamps in the UI
- Dependencies:
  - `HabitRepository`
  - `ReminderScheduler`
- Notes:
  - pull-to-refresh is cosmetic only
  - add-task dialog chooses category from existing categories only
  - edit-task dialog allows free-text category entry; this can create a task category value that is not present in `task_categories` unless separately added

### Task dialogs

- Add task dialog:
  - task title
  - existing category picker
  - reminder switch
  - optional list of one-time reminder date/times
  - optional reminder message
  - if reminders are enabled, at least one reminder datetime is required by the ViewModel/UI path
- Edit task dialog:
  - task title
  - free-text category
  - reminder switch
  - reminder date/time chips
  - reminder message
- Delete task dialog:
  - permanently deletes selected task
- Add category dialog:
  - creates category and immediately selects it
- Delete category dialog:
  - warns that all tasks inside will be permanently deleted
- Transfer task dialog:
  - lets user type or chip-select target category

### Screen: Goals

- File: `app/src/main/java/com/example/habittracker/ui/screens/GoalsScreen.kt`
- ViewModel: `app/src/main/java/com/example/habittracker/ui/viewmodel/GoalsViewModel.kt`
- Purpose:
  - manage goals and nested subgoals
- Entry path:
  - bottom nav `Goals`
- Main UI sections:
  - top bar title
  - floating action button for new goal
  - collapsible `Goals to achieve` section
  - collapsible `Goals achieved` section
  - expandable goal cards with subgoal rows
- Data shown:
  - goal title
  - goal completion state/date
  - subgoal titles
  - subgoal completion state/date
- User actions:
  - add goal
  - rename goal
  - delete goal
  - toggle goal done/not done
  - add subgoal
  - rename subgoal
  - delete subgoal
  - toggle subgoal done/not done
  - reorder goals within pending or achieved section by long-press drag
  - expand/collapse goal card
  - expand/collapse section groups
- State/logic:
  - a goal can only be marked done if all its subgoals are already done
  - toggling a goal done updates `completedAt`
  - subgoal completion is independent and stored directly
  - goal reorder persistence writes sort order only within the selected done-state group
- Dependencies:
  - `HabitRepository.observeGoalsWithSubGoals()`
- Notes:
  - pull-to-refresh is cosmetic only

### Screen: Birthdays

- File: `app/src/main/java/com/example/habittracker/ui/screens/BirthdaysScreen.kt`
- ViewModel: `app/src/main/java/com/example/habittracker/ui/viewmodel/BirthdaysViewModel.kt`
- Purpose:
  - manage birthdays and birthday reminder timestamps
- Entry path:
  - Home top bar cake icon
- Main UI sections:
  - top bar with back button
  - floating action button for new birthday
  - birthday cards list
- Data shown:
  - name
  - original birthday date including year
  - computed next occurrence
  - count of reminder timestamps
- User actions:
  - add birthday
  - edit birthday
  - delete birthday
  - open date picker for birthday date
  - add reminder date/time timestamps
  - remove reminder timestamps by tapping their chips
- State/logic:
  - birthdays are sorted by next upcoming occurrence, then name
  - birthday reminders are stored as one-time timestamps in `reminderDateTimesCsv`
  - creating a birthday is a two-step repository operation:
    - insert birthday
    - update the birthday with reminder timestamps if creation succeeded
  - changing birthdays refreshes the home month snapshot and reschedules reminders
- Dependencies:
  - `HabitRepository`
  - `ReminderScheduler`

### Screen: Notifications

- File: `app/src/main/java/com/example/habittracker/ui/screens/NotificationsScreen.kt`
- ViewModel: `app/src/main/java/com/example/habittracker/ui/viewmodel/NotificationsViewModel.kt`
- Purpose:
  - preview upcoming reminder schedule and manually send a test notification
- Entry path:
  - Home top bar bell icon
- Main UI sections:
  - top bar with back and send-test icon
  - secondary `Send test` text button
  - list of future scheduled reminders
- Data shown:
  - reminder title
  - reminder message
  - next trigger datetime
- User actions:
  - go back
  - send test notification
- State/logic:
  - preview list is derived from repository reminder schedule items
  - recurring habit reminders are converted to the next future daily trigger in the ViewModel
  - one-time task/birthday reminders use their stored timestamp directly
  - sending a test notification sends a broadcast to `ReminderReceiver` with randomized title/message
- Dependencies:
  - `HabitRepository.observeReminderScheduleItems()`
  - `ReminderReceiver`
- Notes:
  - on Android 13+, sending a test notification requests notification permission if needed
  - there is no UI to cancel individual scheduled reminders from this screen

### Screen: Motivation

- File: `app/src/main/java/com/example/habittracker/ui/screens/MotivationalScreen.kt`
- Purpose:
  - placeholder motivation page
- Entry path:
  - bottom nav `Motivation`
- Main UI sections:
  - top bar
  - centered title and helper text
- Notes:
  - currently static placeholder content only

### Screen: Stats

- File: `app/src/main/java/com/example/habittracker/ui/screens/StatsScreen.kt`
- ViewModel: `app/src/main/java/com/example/habittracker/ui/viewmodel/StatsViewModel.kt`
- Purpose:
  - display current streak, longest streak, and total completions for one habit
- Entry path:
  - no current navigation route
- Data shown:
  - habit name
  - current streak
  - longest streak
  - total completions
- Notes:
  - ViewModel requires `habitId` from `SavedStateHandle`
  - no route currently supplies that argument
  - this screen is implemented but unreachable in the current app flow

### Screen: Blank

- File: `app/src/main/java/com/example/habittracker/ui/screens/BlankScreen.kt`
- Purpose:
  - empty full-screen `Surface`
- Entry path:
  - none
- Notes:
  - unused placeholder

## Button and Action Map

| Screen | Element | Type | Action | Outcome | Navigation | Logic location |
| --- | --- | --- | --- | --- | --- | --- |
| Global | Home tab | Bottom nav item | Navigates to Home | Shows `MainScreen` | `home` | `ui/AppNavGraph.kt` |
| Global | Who am I? tab | Bottom nav item | Navigates to notes screen | Shows `WhoAmIScreen` | `who_am_i` | `ui/AppNavGraph.kt` |
| Global | Motivation tab | Bottom nav item | Navigates to motivation page | Shows placeholder page | `motivational` | `ui/AppNavGraph.kt` |
| Global | Tasks tab | Bottom nav item | Navigates to tasks | Shows `TasksScreen` | `tasks` | `ui/AppNavGraph.kt` |
| Global | Goals tab | Bottom nav item | Navigates to goals | Shows `GoalsScreen` | `goals` | `ui/AppNavGraph.kt` |
| Home | Bell icon | Icon button | Opens notifications | Shows notifications screen | `notifications` | `ui/screens/MainScreen.kt` |
| Home | Cake icon | Icon button | Opens birthdays | Shows birthdays screen | `birthdays` | `ui/screens/MainScreen.kt` |
| Home | `Seed` | Text button | Opens confirmation dialog | Can insert demo data set | None | `ui/screens/MainScreen.kt`, `ui/viewmodel/MainViewModel.kt`, `repository/HabitRepository.kt` |
| Home | Edit icon | Icon button | Opens edit dialog for selected habit | Loads selected habit values into form | None | `ui/screens/MainScreen.kt` |
| Home | Delete icon | Icon button | Opens delete-habit confirmation | Can delete selected habit | None | `ui/screens/MainScreen.kt`, `ui/viewmodel/MainViewModel.kt` |
| Home | FAB `+` | Floating action button | Opens create-habit dialog | Starts new habit flow | None | `ui/screens/MainScreen.kt` |
| Home | Global month day tap | Calendar tap | Opens day details | Shows birthdays and habit statuses for that day | None | `ui/components/MonthCalendar.kt`, `ui/viewmodel/MainViewModel.kt` |
| Home | Previous month | Icon button | Moves visible month back | Reloads selected/global month data | None | `ui/screens/MainScreen.kt`, `ui/viewmodel/MainViewModel.kt` |
| Home | Next month | Icon button | Moves visible month forward | Reloads selected/global month data | None | `ui/screens/MainScreen.kt`, `ui/viewmodel/MainViewModel.kt` |
| Home | Habit dropdown button | Outlined button | Opens habit menu | Lets user change selected habit | None | `ui/screens/MainScreen.kt` |
| Home | Habit dropdown item | Dropdown item | Selects habit | Refreshes selected-habit snapshot | None | `ui/screens/MainScreen.kt`, `ui/viewmodel/MainViewModel.kt` |
| Home | Selected calendar day tap | Calendar tap | Toggles completion | Adds/replaces completion record for selected habit/date | None | `ui/components/MonthCalendar.kt`, `ui/viewmodel/MainViewModel.kt`, `repository/HabitRepository.kt` |
| Home | Selected calendar day long press | Calendar long press | Opens day note dialog | Lets user save or clear day note | None | `ui/components/MonthCalendar.kt`, `ui/screens/MainScreen.kt` |
| Home | Create habit `More options` | Text button | Expands/collapses advanced form | Reveals date/frequency/reminder controls | None | `ui/screens/MainScreen.kt` |
| Home | Create habit created-date field | Disabled field + click target | Opens date picker | Sets habit created date | None | `ui/screens/MainScreen.kt` |
| Home | Frequency chips | Assist chips | Sets frequency mode | Changes weekly/interval controls | None | `ui/screens/MainScreen.kt` |
| Home | Weekly weekday chips | Assist chips | Toggles included weekdays | Changes weekly schedule | None | `ui/screens/MainScreen.kt` |
| Home | Reminder switch | Switch | Enables/disables reminders | Shows or hides reminder controls | None | `ui/screens/MainScreen.kt` |
| Home | Reminder time field | Disabled field + click target | Opens time picker | Sets current reminder time draft | None | `ui/screens/MainScreen.kt` |
| Home | `Add reminder time` | Text button | Adds current time to chip list | Multi-time habit reminders stored as CSV | None | `ui/screens/MainScreen.kt` |
| Home | Reminder time chip | Assist chip | Removes that time | Updates reminder CSV draft | None | `ui/screens/MainScreen.kt` |
| Home | Create | Text button | Creates habit | Inserts habit, refreshes snapshot, reschedules reminders | None | `ui/viewmodel/MainViewModel.kt`, `repository/HabitRepository.kt` |
| Home | Save habit edits | Text button | Updates selected habit | Updates habit row, refreshes snapshot, reschedules reminders | None | `ui/viewmodel/MainViewModel.kt`, `repository/HabitRepository.kt` |
| Home | Delete habit confirm | Text button | Deletes selected habit | Deletes habit and cascaded child data | None | `ui/viewmodel/MainViewModel.kt`, `repository/HabitRepository.kt` |
| Home | Save day note | Text button | Saves note | Upserts or deletes day note row | None | `ui/viewmodel/MainViewModel.kt`, `repository/HabitRepository.kt` |
| Home | Add data | Text button | Seeds demo content | Inserts demo habits, notes, tasks, goals, birthdays | None | `ui/viewmodel/MainViewModel.kt`, `repository/HabitRepository.kt` |
| Who am I? | Copy all icon | Icon button | Copies all note titles and content | Puts combined text on clipboard | None | `ui/screens/WhoAmIScreen.kt` |
| Who am I? | FAB `+` | Floating action button | Opens add-note dialog | Starts note creation | None | `ui/screens/WhoAmIScreen.kt` |
| Who am I? | Note card header tap | Clickable row | Expands/collapses note | Sets `selectedNoteId` | None | `ui/screens/WhoAmIScreen.kt`, `ui/viewmodel/WhoAmIViewModel.kt` |
| Who am I? | Delete note icon | Icon button | Opens delete dialog | Can remove note | None | `ui/screens/WhoAmIScreen.kt` |
| Who am I? | Rename note icon | Icon button | Opens rename dialog | Can update title | None | `ui/screens/WhoAmIScreen.kt` |
| Who am I? | Copy note icon | Icon button | Copies note content | Clipboard updated | None | `ui/screens/WhoAmIScreen.kt` |
| Who am I? | Save note | Text button | Persists note content | Updates note content in DB | None | `ui/viewmodel/WhoAmIViewModel.kt`, `repository/HabitRepository.kt` |
| Who am I? | Long-press drag on note card | Drag gesture | Reorders notes | Updates sort order after debounce | None | `ui/screens/WhoAmIScreen.kt`, `ui/viewmodel/WhoAmIViewModel.kt` |
| Tasks | Add category icon | Icon button | Opens add-category dialog | Can create category | None | `ui/screens/TasksScreen.kt` |
| Tasks | Delete category icon | Icon button | Opens delete-category dialog | Can delete selected category and its tasks | None | `ui/screens/TasksScreen.kt`, `ui/viewmodel/TasksViewModel.kt` |
| Tasks | `All` chip | Assist chip | Selects all-category view | Shows all tasks | None | `ui/screens/TasksScreen.kt`, `ui/viewmodel/TasksViewModel.kt` |
| Tasks | Category chip tap | Assist chip | Selects category | Filters pending/done lists | None | `ui/screens/TasksScreen.kt`, `ui/viewmodel/TasksViewModel.kt` |
| Tasks | Category chip long-press drag | Drag gesture | Reorders categories | Persists category sort order after debounce | None | `ui/screens/TasksScreen.kt`, `ui/viewmodel/TasksViewModel.kt` |
| Tasks | FAB `+` | Floating action button | Opens add-task dialog | Starts task creation | None | `ui/screens/TasksScreen.kt` |
| Tasks | Add-task category field | Click target | Opens category dropdown | Selects existing category | None | `ui/screens/TasksScreen.kt` |
| Tasks | Add reminder date/time | Text button | Opens date then time picker | Adds one-time reminder timestamp | None | `ui/screens/TasksScreen.kt` |
| Tasks | Reminder chip | Assist chip | Removes timestamp | Updates reminder draft | None | `ui/screens/TasksScreen.kt` |
| Tasks | Add task confirm | Text button | Creates task | Inserts task, reschedules reminders | None | `ui/viewmodel/TasksViewModel.kt`, `repository/HabitRepository.kt` |
| Tasks | Task toggle circle | Icon button | Toggles done/not done | Updates `isDone`, `completedAt`, sort order, reminders | None | `ui/screens/TasksScreen.kt`, `ui/viewmodel/TasksViewModel.kt`, `repository/HabitRepository.kt` |
| Tasks | Task row long-press drag | Drag gesture | Reorders within current section | Updates sort order after debounce | None | `ui/screens/TasksScreen.kt`, `ui/viewmodel/TasksViewModel.kt` |
| Tasks | Row overflow `Edit` | Dropdown item | Opens edit dialog | Can change title/category/reminders | None | `ui/screens/TasksScreen.kt` |
| Tasks | Row overflow `Transfer` | Dropdown item | Opens transfer dialog | Can move task to another category | None | `ui/screens/TasksScreen.kt` |
| Tasks | Row overflow `Delete` | Dropdown item | Opens delete dialog | Can remove task | None | `ui/screens/TasksScreen.kt` |
| Tasks | Edit task save | Text button | Updates task | Updates row, reschedules reminders | None | `ui/viewmodel/TasksViewModel.kt`, `repository/HabitRepository.kt` |
| Tasks | Add category confirm | Text button | Creates category | Inserts category and selects it | None | `ui/viewmodel/TasksViewModel.kt`, `repository/HabitRepository.kt` |
| Tasks | Delete category confirm | Text button | Deletes category | Deletes all tasks in category, deletes category, reschedules reminders | None | `ui/viewmodel/TasksViewModel.kt`, `repository/HabitRepository.kt` |
| Tasks | Transfer confirm | Text button | Transfers task | Creates target category if needed and updates task category | None | `ui/viewmodel/TasksViewModel.kt`, `repository/HabitRepository.kt` |
| Goals | FAB `+` | Floating action button | Opens add-goal dialog | Starts goal creation | None | `ui/screens/GoalsScreen.kt` |
| Goals | `Goals to achieve` header | Clickable card | Expands/collapses section | Shows or hides pending goals | None | `ui/screens/GoalsScreen.kt` |
| Goals | `Goals achieved` header | Clickable card | Expands/collapses section | Shows or hides completed goals | None | `ui/screens/GoalsScreen.kt` |
| Goals | Goal main row tap | Clickable row | Toggles goal done/not done | Updates goal completion if allowed | None | `ui/screens/GoalsScreen.kt`, `ui/viewmodel/GoalsViewModel.kt` |
| Goals | Add subgoal icon | Icon button | Opens add-subgoal dialog | Creates subgoal under target goal | None | `ui/screens/GoalsScreen.kt` |
| Goals | Delete goal icon | Icon button | Opens delete dialog | Can remove goal | None | `ui/screens/GoalsScreen.kt` |
| Goals | Rename goal icon | Icon button | Opens rename dialog | Can update goal title | None | `ui/screens/GoalsScreen.kt` |
| Goals | Expand goal icon | Icon button | Expands/collapses goal card | Shows or hides subgoals | None | `ui/screens/GoalsScreen.kt` |
| Goals | Goal card long-press drag | Drag gesture | Reorders goals within section | Updates sort order in DB | None | `ui/screens/GoalsScreen.kt`, `ui/viewmodel/GoalsViewModel.kt` |
| Goals | Subgoal row tap | Clickable row | Toggles subgoal done/not done | Updates subgoal completion | None | `ui/screens/GoalsScreen.kt`, `ui/viewmodel/GoalsViewModel.kt` |
| Goals | Delete subgoal icon | Icon button | Opens delete dialog | Can remove subgoal | None | `ui/screens/GoalsScreen.kt` |
| Goals | Rename subgoal icon | Icon button | Opens rename dialog | Can update subgoal title | None | `ui/screens/GoalsScreen.kt` |
| Birthdays | Back | Icon button | Pops current destination | Returns to previous screen | Back stack pop | `ui/screens/BirthdaysScreen.kt` |
| Birthdays | FAB `+` | Floating action button | Opens add-birthday dialog | Starts birthday creation | None | `ui/screens/BirthdaysScreen.kt` |
| Birthdays | Edit birthday | Icon button | Opens edit dialog | Loads existing values into form | None | `ui/screens/BirthdaysScreen.kt` |
| Birthdays | Delete birthday | Icon button | Opens delete dialog | Can remove birthday | None | `ui/screens/BirthdaysScreen.kt` |
| Birthdays | Birthday date field | Disabled field + click target | Opens date picker | Sets birthday date | None | `ui/screens/BirthdaysScreen.kt` |
| Birthdays | Add reminder date/time | Text button | Opens date then time picker | Adds reminder timestamp | None | `ui/screens/BirthdaysScreen.kt` |
| Birthdays | Reminder chip | Assist chip | Removes timestamp | Updates reminder draft | None | `ui/screens/BirthdaysScreen.kt` |
| Birthdays | Add birthday confirm | Text button | Creates birthday | Inserts birthday, updates reminder timestamps, reschedules reminders | None | `ui/viewmodel/BirthdaysViewModel.kt`, `repository/HabitRepository.kt` |
| Birthdays | Save birthday edits | Text button | Updates birthday | Updates row and reschedules reminders | None | `ui/viewmodel/BirthdaysViewModel.kt`, `repository/HabitRepository.kt` |
| Birthdays | Delete birthday confirm | Text button | Deletes birthday | Removes birthday and reschedules reminders | None | `ui/viewmodel/BirthdaysViewModel.kt`, `repository/HabitRepository.kt` |
| Notifications | Back | Icon button | Pops current destination | Returns to previous screen | Back stack pop | `ui/screens/NotificationsScreen.kt` |
| Notifications | Bell icon | Icon button | Sends test notification | Broadcasts directly to `ReminderReceiver` | None | `ui/screens/NotificationsScreen.kt` |
| Notifications | `Send test` | Text button | Sends test notification | Same as bell action | None | `ui/screens/NotificationsScreen.kt` |

## Components Overview

### `MonthCalendar`

- File: `app/src/main/java/com/example/habittracker/ui/components/MonthCalendar.kt`
- Used by:
  - Home global month overview
  - Home selected habit month calendar
- What it renders:
  - custom drawn calendar grid using `Canvas`
  - weekday labels
  - date cells with state coloring and indicators
- Important inputs:
  - `month`
  - `completedDates`
  - `scheduledDates`
  - `birthdayDates`
  - `noteDates`
  - `todayDate`
  - `onToggleDate`
  - `onOpenDayNote`
  - `interactive`
- Interaction behavior:
  - tap resolves tapped cell and calls `onToggleDate(dateKey)`
  - long press calls `onOpenDayNote(dateKey)` only for scheduled cells
- Visual meaning:
  - completed cells use dark green
  - missed scheduled days use error container
  - scheduled upcoming/current days use `surfaceVariant`
  - birthday indicator is a stroked circle
  - note indicator is a filled dot

### `HabitWeekRow`

- File: `app/src/main/java/com/example/habittracker/ui/components/HabitWeekRow.kt`
- Status:
  - present in code but not used by current screen flow
- Purpose:
  - compact row of dates with completion state

### `AppIcons`

- File: `app/src/main/java/com/example/habittracker/ui/icons/AppIcons.kt`
- Purpose:
  - local vector implementations for a subset of Material icons
  - used to avoid adding `material-icons-extended`

## ViewModels Overview

### `MainViewModel`

- File: `app/src/main/java/com/example/habittracker/ui/viewmodel/MainViewModel.kt`
- Used by:
  - `MainScreen`
- Responsibilities:
  - maintain selected month and selected habit
  - merge live DB flows into `MainUiState`
  - restore cached monthly snapshot
  - create/update/delete habits
  - toggle completions
  - save day notes
  - compute global day details dialog content
  - seed demo data
  - reschedule reminders after habit-affecting changes

### `WhoAmIViewModel`

- File: `app/src/main/java/com/example/habittracker/ui/viewmodel/WhoAmIViewModel.kt`
- Used by:
  - `WhoAmIScreen`
- Responsibilities:
  - load notes
  - track expanded note
  - create, rename, delete, reorder notes
  - manage transient saving state for note content

### `TasksViewModel`

- File: `app/src/main/java/com/example/habittracker/ui/viewmodel/TasksViewModel.kt`
- Used by:
  - `TasksScreen`
- Responsibilities:
  - combine tasks and task categories
  - build filtered pending/done UI state
  - create/update/delete/toggle tasks
  - reorder tasks and categories
  - transfer tasks across categories
  - reschedule reminders after task-affecting changes

### `GoalsViewModel`

- File: `app/src/main/java/com/example/habittracker/ui/viewmodel/GoalsViewModel.kt`
- Used by:
  - `GoalsScreen`
- Responsibilities:
  - load goals with subgoals
  - create/rename/delete goals and subgoals
  - toggle goal/subgoal completion
  - enforce "all subgoals must be done before goal can be marked done"
  - reorder goals

### `BirthdaysViewModel`

- File: `app/src/main/java/com/example/habittracker/ui/viewmodel/BirthdaysViewModel.kt`
- Used by:
  - `BirthdaysScreen`
- Responsibilities:
  - load birthdays
  - compute next occurrence per birthday
  - create/update/delete birthdays
  - reschedule reminders after birthday changes

### `NotificationsViewModel`

- File: `app/src/main/java/com/example/habittracker/ui/viewmodel/NotificationsViewModel.kt`
- Used by:
  - `NotificationsScreen`
- Responsibilities:
  - observe reminder schedule items
  - convert them into future preview rows
  - format display timestamps

### `StatsViewModel`

- File: `app/src/main/java/com/example/habittracker/ui/viewmodel/StatsViewModel.kt`
- Used by:
  - `StatsScreen`
- Responsibilities:
  - load one habit and its streak/completion stats
- Note:
  - not currently used by navigation

## Data Model and Persistence

### Database

- File: `app/src/main/java/com/example/habittracker/data/database/AppDatabase.kt`
- Database name: `habit_tracker.db`
- Current version: `17`

### Entities

| Entity | Purpose | Key fields |
| --- | --- | --- |
| `Habit` | Habit definition | `name`, `createdAt`, `frequencyType`, `frequencyIntervalDays`, `frequencyWeekdays`, `reminderEnabled`, `reminderTime`, `reminderMessage`, `sortOrder` |
| `HabitCompletion` | Per-habit per-date completion state | `habitId`, `date`, `completed` |
| `HabitDayNote` | Free-text note for one habit/date | `habitId`, `date`, `note` |
| `WhoAmINote` | Personal note | `title`, `content`, `sortOrder`, `createdAt` |
| `TaskItem` | Task row | `title`, `category`, `isDone`, `completedAt`, `sortOrder`, `reminderEnabled`, `reminderTime`, `reminderDateTimesCsv`, `reminderMessage` |
| `TaskCategory` | Stored task category and chip order | `name`, `sortOrder` |
| `Goal` | Goal row | `title`, `isDone`, `completedAt`, `sortOrder`, `createdAt` |
| `SubGoal` | Subgoal under a goal | `goalId`, `title`, `isDone`, `completedAt`, `createdAt` |
| `Birthday` | Birthday row | `name`, `year`, `month`, `day`, `reminderDateTimesCsv`, `createdAt` |
| `HomeMonthSnapshot` | Cached home-month state | month key, selected/global sets, serialized note map, updated timestamp |
| `ReminderSettings` | Settings table | present in DB, not currently used by live UI |
| `ReminderTime` | Standalone reminder time table | present in DB, not currently used by live UI |

### DAO responsibilities

- `HabitDao`
  - observe, insert, reorder, update, delete habits
- `HabitCompletionDao`
  - observe per-date completion rows globally or by habit/range
- `HabitDayNoteDao`
  - observe or upsert notes tied to habit/date
- `WhoAmINoteDao`
  - CRUD and sort order for personal notes
- `TaskDao`
  - CRUD, done-state updates, sort order, category transfer/delete
- `TaskCategoryDao`
  - CRUD and sort order for categories
- `GoalDao`
  - CRUD, done-state updates, sort order for goals
- `SubGoalDao`
  - CRUD and done-state updates for subgoals
- `BirthdayDao`
  - CRUD for birthdays
- `HomeMonthSnapshotDao`
  - persist and restore cached home-month state
- `ReminderSettingsDao`, `ReminderTimeDao`
  - defined but not part of current UI flows

### Repository behavior

`app/src/main/java/com/example/habittracker/repository/HabitRepository.kt` is the core business-logic file.

It is responsible for:

- building habit options for Home dropdown
- computing current streaks
- computing selected-habit monthly schedule
- computing global monthly completion/birthday/note overlays
- validating and normalizing reminder CSV data
- computing recurring schedule dates for daily/weekly/every-N-days habits
- creating demo data
- rescheduling-dependent schedule item generation
- caching/restoring Home snapshots through Room and `SharedPreferences`

### Important business rules

- Habit scheduling:
  - `DAILY` means every day from created date through business today.
  - `WEEKLY` uses stored weekday integers `1..7`.
  - `EVERY_N_DAYS` schedules by interval anchored to created date.
- Business day:
  - the app defines "today" as current local time minus 5 hours.
  - this affects streaks and scheduled dates.
- Goal completion:
  - a goal cannot be marked done if any subgoal is not done.
- Task reminders:
  - UI currently uses one-time reminder timestamps.
  - repository still supports fallback daily reminders via `reminderTime`.
- Birthday reminders:
  - only one-time future timestamps are used.

## Notifications and Background Behavior

### Scheduling

- File: `app/src/main/java/com/example/habittracker/notifications/ReminderScheduler.kt`
- Uses `AlarmManager` and broadcast `PendingIntent`s.
- `rescheduleAll(...)`:
  - cancels previously tracked request codes from shared prefs
  - schedules every current reminder item again
  - stores the new request codes

### Reminder sources

Repository builds reminder schedule items from:

- habits with `reminderEnabled = true`
  - one item per time in `reminderTime` CSV
  - title: `Habit reminder`
- tasks with `reminderEnabled = true` and `isDone = false`
  - one-time future timestamps from `reminderDateTimesCsv`
  - if no one-time timestamps exist and `reminderTime` is valid, a recurring daily fallback item is produced
  - title: `Task reminder`
- birthdays
  - one-time future timestamps from `reminderDateTimesCsv`
  - title: `Birthday reminder`

### Broadcast receiver behavior

- File: `app/src/main/java/com/example/habittracker/notifications/ReminderReceiver.kt`
- On receive:
  - reads title/message/time/unique key from extras
  - checks Android 13+ notification permission
  - creates notification channel `daily_reminders`
  - posts notification
  - reschedules recurring items unless `EXTRA_SKIP_RESCHEDULE` is true

### Boot recovery

- File: `app/src/main/java/com/example/habittracker/notifications/BootCompletedReceiver.kt`
- On device boot completed:
  - loads all reminder schedule items from repository
  - reschedules everything

## File and Folder Map

### Top-level project folders

- `app/`
  - Android application module
- `gradle/`
  - Gradle wrapper config
- `build/`, `app/build/`
  - generated outputs, not source of truth

### Important source folders under `app/src/main/java/com/example/habittracker`

- `ui/`
  - navigation graph
  - screens
  - theme
  - components
  - icons
- `ui/viewmodel/`
  - screen state and user-action orchestration
- `repository/`
  - central business/data coordination
- `data/entity/`
  - Room entities
- `data/dao/`
  - Room DAO interfaces
- `data/database/`
  - Room database and migrations
- `notifications/`
  - alarm scheduling and broadcast receivers
- `di/`
  - Hilt providers

### High-value files for new developers

- `app/src/main/java/com/example/habittracker/MainActivity.kt`
  - app entry point
- `app/src/main/java/com/example/habittracker/ui/AppNavGraph.kt`
  - live navigation structure
- `app/src/main/java/com/example/habittracker/repository/HabitRepository.kt`
  - most important business logic file
- `app/src/main/java/com/example/habittracker/data/database/AppDatabase.kt`
  - database entity list and version
- `app/src/main/java/com/example/habittracker/di/AppModule.kt`
  - DI wiring

## Key User Flows

### App launch

1. Android launches `MainActivity`.
2. `MyAppTheme` wraps the Compose content.
3. `AppNavGraph` renders the root scaffold and bottom navigation.
4. Home starts as the default destination.
5. `MainViewModel` can hydrate UI from cached Home snapshot before live flows finish.

### Create and track a habit

1. User taps Home FAB.
2. User enters habit name.
3. Optional:
   - created date
   - frequency
   - reminder settings
4. `MainViewModel.addHabit(...)` validates inputs.
5. Repository inserts `Habit`.
6. Repository refreshes Home snapshot.
7. ViewModel reschedules reminders.
8. User can select the habit from dropdown.
9. User taps scheduled days on the selected calendar to mark them complete.
10. Completion rows are stored in `habit_completions`.

### Add a note to a habit day

1. User long-presses a scheduled day in the selected-habit calendar.
2. Day-note dialog opens.
3. User writes note and taps `Save`.
4. Repository upserts `HabitDayNote`.
5. Empty note deletes the note row instead.

### Review global monthly progress

1. User views Home global overview card.
2. User taps any interactive date cell.
3. `MainViewModel.getGlobalDayDetails(...)` returns birthday names and per-habit status.
4. Dialog shows aggregated detail for that date.

### Manage personal reflection notes

1. User opens `Who am I?`.
2. User taps FAB to create a note title.
3. New note is auto-selected/expanded.
4. User edits content.
5. User taps `Save` to persist content.
6. User can reorder notes by long-press drag.

### Manage tasks

1. User opens `Tasks`.
2. User optionally adds categories.
3. User taps FAB to add a task.
4. User optionally enables reminder and adds one or more reminder date/times.
5. Repository inserts task.
6. Reminder scheduler is refreshed.
7. User marks task done later.
8. Repository updates `isDone` and `completedAt`.

### Transfer a task to another category

1. User opens task overflow menu.
2. User taps `Transfer`.
3. User types or taps target category.
4. ViewModel ensures the target category exists.
5. Repository updates task category.

### Manage goals and subgoals

1. User opens `Goals`.
2. User creates a goal.
3. User adds subgoals under that goal.
4. User completes subgoals individually.
5. Once all subgoals are done, user can mark the parent goal done.

### Manage birthdays

1. User opens Birthdays from Home.
2. User taps FAB and enters name/date.
3. User optionally adds one-time reminder timestamps.
4. Birthday is inserted and then updated with reminder timestamps.
5. Reminder schedule is refreshed.

### Review or test notifications

1. User opens Notifications from Home.
2. Screen lists next future reminder triggers.
3. User taps bell icon or `Send test`.
4. Screen sends broadcast directly to `ReminderReceiver`.
5. Receiver posts a notification if permission exists.

## Uncertain Behavior and Non-Live Code

### Confirmed non-live or partially wired code

- `StatsScreen` is implemented but unreachable from current navigation.
- `BlankScreen` is implemented but unused.
- `HabitWeekRow` is implemented but unused.
- `ReminderSettings` and `ReminderTime` tables/DAOs are present in the database but are not connected to current UI flows.

### Behavior that is code-backed but worth double-checking in product review

- Pull-to-refresh in `WhoAmIScreen`, `TasksScreen`, and `GoalsScreen` is visual only; it does not trigger a repository refresh.
- Editing a task category uses free text and does not automatically insert that category into `task_categories`.
- `MainViewModel.moveHabit(...)` exists, but Home does not currently expose any UI control that calls it.
- `StatsViewModel` uses `SavedStateHandle["habitId"]`; if someone manually wires the route without this argument, it will fail fast.

## Quick Onboarding Notes

- Start with `ui/AppNavGraph.kt` to understand what is actually reachable.
- Read `repository/HabitRepository.kt` next; most non-trivial app behavior lives there.
- For UI changes, check whether behavior is inline in the screen file before looking for shared components.
- For reminders, always inspect both:
  - the ViewModel action that calls `rescheduleReminders()`
  - `ReminderScheduler` and `ReminderReceiver`
- When debugging Home behavior, keep in mind:
  - live flows
  - persisted monthly snapshot in Room
  - cached snapshot in `SharedPreferences`
  - the custom business-day shift of minus 5 hours
