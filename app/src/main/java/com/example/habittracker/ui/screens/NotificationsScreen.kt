package com.example.habittracker.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.notifications.ReminderReceiver
import com.example.habittracker.ui.components.EmptyStateCard
import com.example.habittracker.ui.components.PillLabel
import com.example.habittracker.ui.components.PremiumCard
import com.example.habittracker.ui.components.ScreenBackground
import com.example.habittracker.ui.components.SectionHeader
import com.example.habittracker.ui.components.SurfaceCard
import com.example.habittracker.ui.theme.MyAppTheme
import com.example.habittracker.ui.viewmodel.NotificationPreviewItem
import com.example.habittracker.ui.viewmodel.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    fun sendRandomTestNotification() {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_UNIQUE_KEY, "test:${System.currentTimeMillis()}")
            putExtra(ReminderReceiver.EXTRA_TIME, "09:00")
            putExtra(ReminderReceiver.EXTRA_TITLE, "MyApp check")
            putExtra(ReminderReceiver.EXTRA_MESSAGE, "Premium notification preview from the redesigned screen.")
            putExtra(ReminderReceiver.EXTRA_SKIP_RESCHEDULE, true)
        }
        context.sendBroadcast(intent)
    }

    val todayItems = state.upcoming.filter { item ->
        viewModel.formatTrigger(item.triggerAtMillis).startsWith(
            java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))
        )
    }
    val weekLimit = System.currentTimeMillis() + 6L * 24 * 60 * 60 * 1000
    val weekItems = state.upcoming.filter { it.triggerAtMillis <= weekLimit && it !in todayItems }
    val laterItems = state.upcoming - todayItems.toSet() - weekItems.toSet()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        ScreenBackground {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
            ) {
                item {
                    PremiumCard(accent = MyAppTheme.extraColors.warning) {
                        SectionHeader(
                            title = if (notificationsGranted) "Notifications are ready" else "Permission needed",
                            subtitle = if (notificationsGranted) {
                                "Your reminders can surface on time."
                            } else {
                                "Grant notification access so habits, tasks, and birthdays can reach you."
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PillLabel(
                                text = if (notificationsGranted) "Enabled" else "Disabled",
                                color = if (notificationsGranted) MyAppTheme.extraColors.success else MyAppTheme.extraColors.accent,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(
                                onClick = {
                                    if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        sendRandomTestNotification()
                                    }
                                }
                            ) {
                                Text(if (notificationsGranted) "Send test" else "Grant access")
                            }
                        }
                    }
                }

                if (state.upcoming.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No reminders queued",
                            message = "Add a habit reminder, task reminder, or birthday reminder to see your schedule here.",
                            actionLabel = "Send test",
                            onAction = ::sendRandomTestNotification
                        )
                    }
                } else {
                    notificationSection("Today", todayItems, viewModel)
                    notificationSection("This week", weekItems, viewModel)
                    notificationSection("Later", laterItems, viewModel)
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.notificationSection(
    title: String,
    items: List<NotificationPreviewItem>,
    viewModel: NotificationsViewModel
) {
    if (items.isEmpty()) return
    item {
        SectionHeader(title = title, subtitle = "${items.size} scheduled")
    }
    items(items, key = { it.uniqueKey }) { item ->
        SurfaceCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            item.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                PillLabel(
                    text = item.sourceLabel,
                    color = when (item.sourceLabel) {
                        "Habit" -> MaterialTheme.colorScheme.primary
                        "Task" -> MyAppTheme.extraColors.success
                        "Birthday" -> MyAppTheme.extraColors.accent
                        else -> MyAppTheme.extraColors.warning
                    },
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = viewModel.formatTrigger(item.triggerAtMillis),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
