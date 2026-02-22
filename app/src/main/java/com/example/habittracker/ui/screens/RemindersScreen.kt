package com.example.habittracker.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.ui.viewmodel.RemindersViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun RemindersScreen(viewModel: RemindersViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    var hasNotificationPermission by rememberSaveable {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasRequestedNotificationPermission by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        hasRequestedNotificationPermission = true
        if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val shouldShowRationale =
                activity?.let {
                    androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                        it,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                } ?: false
            if (!shouldShowRationale) {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                context.startActivity(intent)
            }
        }
    }

    var isAddTimeDialogVisible by rememberSaveable { mutableStateOf(false) }
    var timeInput by rememberSaveable { mutableStateOf("09:00") }
    var timeError by rememberSaveable { mutableStateOf(false) }
    var timePendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var timePendingDeleteLabel by rememberSaveable { mutableStateOf("") }
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { isRefreshing = true }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    if (isRefreshing) {
        LaunchedEffect(Unit) {
            delay(650)
            isRefreshing = false
        }
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Reminders") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { isAddTimeDialogVisible = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add reminder time")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Daily notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notifications are disabled",
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@TextButton
                                val shouldShowRationale =
                                    activity?.let {
                                        androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                                            it,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    } ?: false

                                if (!hasRequestedNotificationPermission || shouldShowRationale) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        ) {
                            Text("Grant permission")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notify for habits")
                    Switch(checked = state.habitsEnabled, onCheckedChange = viewModel::setHabitsEnabled)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notify for tasks")
                    Switch(checked = state.tasksEnabled, onCheckedChange = viewModel::setTasksEnabled)
                }

                Text("Times", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (state.times.isEmpty()) {
                    Text("No reminder times", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(state.times, key = { it.id }) { reminder ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(reminder.timeValue)
                                IconButton(
                                    onClick = {
                                        timePendingDeleteId = reminder.id
                                        timePendingDeleteLabel = reminder.timeValue
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete time")
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Business day changes at 05:00 (4:00AM Tuesday is still Monday).",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (isAddTimeDialogVisible) {
        AlertDialog(
            onDismissRequest = { isAddTimeDialogVisible = false },
            title = { Text("Add reminder time") },
            text = {
                OutlinedTextField(
                    value = timeInput,
                    onValueChange = {
                        timeInput = it
                        timeError = false
                    },
                    singleLine = true,
                    isError = timeError,
                    label = { Text("Time (HH:MM)") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = viewModel.addTime(timeInput)
                        if (ok) {
                            isAddTimeDialogVisible = false
                        } else {
                            timeError = true
                        }
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { isAddTimeDialogVisible = false }) { Text("Cancel") }
            }
        )
    }

    if (timePendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = {
                timePendingDeleteId = null
                timePendingDeleteLabel = ""
            },
            title = { Text("Delete time") },
            text = { Text("Do you want to delete $timePendingDeleteLabel?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        timePendingDeleteId?.let(viewModel::deleteTime)
                        timePendingDeleteId = null
                        timePendingDeleteLabel = ""
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        timePendingDeleteId = null
                        timePendingDeleteLabel = ""
                    }
                ) { Text("Cancel") }
            }
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
