package com.example.habittracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.ui.components.AppModalSheet
import com.example.habittracker.ui.components.EmptyStateCard
import com.example.habittracker.ui.components.PillLabel
import com.example.habittracker.ui.components.PremiumCard
import com.example.habittracker.ui.components.ScreenBackground
import com.example.habittracker.ui.components.SectionHeader
import com.example.habittracker.ui.components.SummaryChip
import com.example.habittracker.ui.components.SurfaceCard
import com.example.habittracker.ui.icons.AppIcons
import com.example.habittracker.ui.theme.MyAppTheme
import com.example.habittracker.ui.viewmodel.GoalUiItem
import com.example.habittracker.ui.viewmodel.GoalsViewModel
import com.example.habittracker.ui.viewmodel.SubGoalUiItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: GoalsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activeGoals = remember(state.goals) { state.goals.filterNot { it.isDone } }
    val completedGoals = remember(state.goals) { state.goals.filter { it.isDone } }
    val totalSubGoals = remember(state.goals) { state.goals.sumOf { it.subGoals.size } }
    val completedSubGoals = remember(state.goals) { state.goals.sumOf { goal -> goal.subGoals.count { it.isDone } } }

    var showCompleted by rememberSaveable { mutableStateOf(false) }
    var isAddGoalVisible by rememberSaveable { mutableStateOf(false) }
    var goalInput by rememberSaveable { mutableStateOf("") }
    var selectedGoal by remember { mutableStateOf<GoalUiItem?>(null) }
    var subGoalInput by rememberSaveable { mutableStateOf("") }
    var renameGoalTarget by remember { mutableStateOf<GoalUiItem?>(null) }
    var renameSubGoalTarget by remember { mutableStateOf<SubGoalUiItem?>(null) }
    var renameInput by rememberSaveable { mutableStateOf("") }
    var pendingDeleteGoal by remember { mutableStateOf<GoalUiItem?>(null) }
    var pendingDeleteSubGoal by remember { mutableStateOf<SubGoalUiItem?>(null) }

    val visibleGoals = if (showCompleted) completedGoals else activeGoals

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Goals") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { isAddGoalVisible = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add goal")
            }
        }
    ) { innerPadding ->
        ScreenBackground {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    PremiumCard(accent = MyAppTheme.extraColors.success) {
                        SectionHeader(
                            title = "Goal system",
                            subtitle = "Track progress at the goal and subgoal level without clutter."
                        )
                        Text(
                            text = "${activeGoals.size} active / ${completedGoals.size} completed",
                            style = MaterialTheme.typography.displayMedium
                        )
                        LinearProgressIndicator(
                            progress = {
                                if (totalSubGoals == 0) 0f else completedSubGoals.toFloat() / totalSubGoals.toFloat()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = MyAppTheme.extraColors.success,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                        Text(
                            text = "$completedSubGoals of $totalSubGoals subgoals complete",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryChip(text = "Active", selected = !showCompleted, onClick = { showCompleted = false })
                        SummaryChip(text = "Completed", selected = showCompleted, onClick = { showCompleted = true })
                    }
                }

                if (visibleGoals.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = if (showCompleted) "No completed goals" else "No active goals",
                            message = if (showCompleted) {
                                "Finished goals will appear here once every subgoal is done."
                            } else {
                                "Create a goal and break it into smaller wins."
                            },
                            actionLabel = if (showCompleted) null else "Add goal",
                            onAction = if (showCompleted) null else ({ isAddGoalVisible = true })
                        )
                    }
                } else {
                    items(visibleGoals, key = { it.id }) { goal ->
                        GoalOverviewCard(
                            goal = goal,
                            onOpen = { selectedGoal = goal },
                            onToggleDone = { viewModel.toggleGoal(goal) }
                        )
                    }
                }
            }
        }
    }

    if (isAddGoalVisible) {
        AppModalSheet(onDismissRequest = { isAddGoalVisible = false }) {
            SectionHeader(title = "Add goal", subtitle = "Name the outcome, then break it into subgoals.")
            OutlinedTextField(
                value = goalInput,
                onValueChange = { goalInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Goal title") },
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { isAddGoalVisible = false }) { Text("Cancel") }
                TextButton(
                    onClick = {
                        if (goalInput.isBlank()) return@TextButton
                        viewModel.addGoal(goalInput.trim())
                        goalInput = ""
                        isAddGoalVisible = false
                    }
                ) { Text("Save") }
            }
        }
    }

    if (selectedGoal != null) {
        val goal = selectedGoal!!
        AppModalSheet(onDismissRequest = { selectedGoal = null }) {
            SectionHeader(
                title = goal.title,
                subtitle = if (goal.subGoals.isEmpty()) "No subgoals yet" else "${goal.subGoals.count { it.isDone }} of ${goal.subGoals.size} complete"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    renameGoalTarget = goal
                    renameInput = goal.title
                }) { Text("Rename") }
                TextButton(onClick = { pendingDeleteGoal = goal }) { Text("Delete") }
                TextButton(onClick = { viewModel.toggleGoal(goal) }) {
                    Text(if (goal.isDone) "Mark active" else "Mark complete")
                }
            }
            LinearProgressIndicator(
                progress = {
                    if (goal.subGoals.isEmpty()) 0f else goal.subGoals.count { it.isDone }.toFloat() / goal.subGoals.size.toFloat()
                },
                modifier = Modifier.fillMaxWidth(),
                color = MyAppTheme.extraColors.success,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            OutlinedTextField(
                value = subGoalInput,
                onValueChange = { subGoalInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Add subgoal") },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (subGoalInput.isBlank()) return@IconButton
                            viewModel.addSubGoal(goal.id, subGoalInput.trim())
                            subGoalInput = ""
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add subgoal")
                    }
                }
            )
            goal.subGoals.forEach { subGoal ->
                SurfaceCard(contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = subGoal.title,
                                style = MaterialTheme.typography.titleMedium,
                                textDecoration = if (subGoal.isDone) TextDecoration.LineThrough else TextDecoration.None
                            )
                            if (subGoal.completedAt != null) {
                                Text(
                                    text = "Completed ${Instant.ofEpochMilli(subGoal.completedAt).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = { viewModel.toggleSubGoal(subGoal) }) {
                                Icon(
                                    imageVector = if (subGoal.isDone) Icons.Default.CheckCircle else AppIcons.RadioButtonUnchecked,
                                    contentDescription = "Toggle subgoal"
                                )
                            }
                            IconButton(onClick = {
                                renameSubGoalTarget = subGoal
                                renameInput = subGoal.title
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename subgoal")
                            }
                            IconButton(onClick = { pendingDeleteSubGoal = subGoal }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete subgoal")
                            }
                        }
                    }
                }
            }
        }
    }

    if (renameGoalTarget != null || renameSubGoalTarget != null) {
        AppModalSheet(onDismissRequest = {
            renameGoalTarget = null
            renameSubGoalTarget = null
        }) {
            SectionHeader(title = "Rename")
            OutlinedTextField(
                value = renameInput,
                onValueChange = { renameInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    renameGoalTarget = null
                    renameSubGoalTarget = null
                }) { Text("Cancel") }
                TextButton(
                    onClick = {
                        if (renameInput.isBlank()) return@TextButton
                        renameGoalTarget?.let { viewModel.renameGoal(it, renameInput.trim()) }
                        renameSubGoalTarget?.let { viewModel.renameSubGoal(it, renameInput.trim()) }
                        renameGoalTarget = null
                        renameSubGoalTarget = null
                    }
                ) { Text("Save") }
            }
        }
    }

    if (pendingDeleteGoal != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteGoal = null },
            title = { Text("Delete goal") },
            text = { Text("Delete ${pendingDeleteGoal?.title} and its subgoals?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteGoal?.let(viewModel::deleteGoal)
                        pendingDeleteGoal = null
                        selectedGoal = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteGoal = null }) { Text("Cancel") }
            }
        )
    }

    if (pendingDeleteSubGoal != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteSubGoal = null },
            title = { Text("Delete subgoal") },
            text = { Text("Delete ${pendingDeleteSubGoal?.title}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteSubGoal?.let(viewModel::deleteSubGoal)
                        pendingDeleteSubGoal = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSubGoal = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun GoalOverviewCard(
    goal: GoalUiItem,
    onOpen: () -> Unit,
    onToggleDone: () -> Unit
) {
    val completedCount = goal.subGoals.count { it.isDone }
    val progress = if (goal.subGoals.isEmpty()) 0f else completedCount.toFloat() / goal.subGoals.size.toFloat()

    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.titleLarge,
                    textDecoration = if (goal.isDone) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = if (goal.subGoals.isEmpty()) "No subgoals yet" else "$completedCount of ${goal.subGoals.size} subgoals complete",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MyAppTheme.extraColors.success,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                if (goal.subGoals.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        goal.subGoals.take(2).forEach { subGoal ->
                            PillLabel(
                                text = subGoal.title.take(20),
                                color = if (subGoal.isDone) MyAppTheme.extraColors.success else MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onToggleDone) {
                    Icon(
                        imageVector = if (goal.isDone) Icons.Default.CheckCircle else AppIcons.RadioButtonUnchecked,
                        contentDescription = "Toggle goal"
                    )
                }
                TextButton(onClick = onOpen) {
                    Text("Open")
                }
            }
        }
    }
}
