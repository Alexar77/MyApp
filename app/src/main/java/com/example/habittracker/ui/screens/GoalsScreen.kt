package com.example.habittracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.ui.viewmodel.GoalUiItem
import com.example.habittracker.ui.viewmodel.GoalsViewModel
import com.example.habittracker.ui.viewmodel.SubGoalUiItem
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: GoalsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var expandedGoalIds by remember { mutableStateOf(setOf<Long>()) }

    var isGoalDialogVisible by rememberSaveable { mutableStateOf(false) }
    var goalInput by rememberSaveable { mutableStateOf("") }

    var isSubGoalDialogVisible by rememberSaveable { mutableStateOf(false) }
    var selectedGoalIdForSubGoal by rememberSaveable { mutableLongStateOf(0L) }
    var subGoalInput by rememberSaveable { mutableStateOf("") }

    var goalPendingDelete by remember { mutableStateOf<GoalUiItem?>(null) }
    var subGoalPendingDelete by remember { mutableStateOf<SubGoalUiItem?>(null) }

    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { isRefreshing = true }
    )
    if (isRefreshing) {
        LaunchedEffect(Unit) {
            delay(650)
            isRefreshing = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Goals") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { isGoalDialogVisible = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add goal")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pullRefresh(pullRefreshState)
        ) {
            if (state.goals.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No goals yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.goals, key = { it.id }) { goal ->
                        val expanded = expandedGoalIds.contains(goal.id)
                        GoalCard(
                            goal = goal,
                            expanded = expanded,
                            onToggleGoalDone = { viewModel.toggleGoal(goal) },
                            onToggleExpanded = {
                                expandedGoalIds = if (expanded) expandedGoalIds - goal.id else expandedGoalIds + goal.id
                            },
                            onAddSubGoal = {
                                selectedGoalIdForSubGoal = goal.id
                                subGoalInput = ""
                                isSubGoalDialogVisible = true
                            },
                            onDeleteGoal = { goalPendingDelete = goal },
                            onToggleSubGoal = viewModel::toggleSubGoal,
                            onDeleteSubGoal = { subGoalPendingDelete = it }
                        )
                    }
                }
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

    if (isGoalDialogVisible) {
        AlertDialog(
            onDismissRequest = { isGoalDialogVisible = false },
            title = { Text("Add goal") },
            text = {
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { goalInput = it },
                    label = { Text("Goal") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (goalInput.isNotBlank()) {
                            viewModel.addGoal(goalInput)
                            goalInput = ""
                            isGoalDialogVisible = false
                        }
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { isGoalDialogVisible = false }) { Text("Cancel") }
            }
        )
    }

    if (isSubGoalDialogVisible) {
        AlertDialog(
            onDismissRequest = { isSubGoalDialogVisible = false },
            title = { Text("Add subgoal") },
            text = {
                OutlinedTextField(
                    value = subGoalInput,
                    onValueChange = { subGoalInput = it },
                    label = { Text("Subgoal") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (subGoalInput.isNotBlank()) {
                            viewModel.addSubGoal(selectedGoalIdForSubGoal, subGoalInput)
                            isSubGoalDialogVisible = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { isSubGoalDialogVisible = false }) { Text("Cancel") }
            }
        )
    }

    if (goalPendingDelete != null) {
        AlertDialog(
            onDismissRequest = { goalPendingDelete = null },
            title = { Text("Delete goal") },
            text = { Text("Do you want to delete \"${goalPendingDelete?.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        goalPendingDelete?.let(viewModel::deleteGoal)
                        goalPendingDelete = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { goalPendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (subGoalPendingDelete != null) {
        AlertDialog(
            onDismissRequest = { subGoalPendingDelete = null },
            title = { Text("Delete subgoal") },
            text = { Text("Do you want to delete \"${subGoalPendingDelete?.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        subGoalPendingDelete?.let(viewModel::deleteSubGoal)
                        subGoalPendingDelete = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { subGoalPendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun GoalCard(
    goal: GoalUiItem,
    expanded: Boolean,
    onToggleGoalDone: () -> Unit,
    onToggleExpanded: () -> Unit,
    onAddSubGoal: () -> Unit,
    onDeleteGoal: () -> Unit,
    onToggleSubGoal: (SubGoalUiItem) -> Unit,
    onDeleteSubGoal: (SubGoalUiItem) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleGoalDone() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (goal.isDone) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = if (goal.isDone) "Mark goal as not done" else "Mark goal as done"
                    )
                    Text(
                        modifier = Modifier.padding(start = 10.dp),
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (goal.isDone) TextDecoration.LineThrough else TextDecoration.None
                    )
                }

                IconButton(onClick = onAddSubGoal) {
                    Icon(Icons.Default.Add, contentDescription = "Add subgoal")
                }
                IconButton(onClick = onDeleteGoal) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete goal")
                }
                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse goal" else "Expand goal"
                    )
                }
            }

            if (expanded) {
                if (goal.subGoals.isEmpty()) {
                    Text(
                        text = "No subgoals yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    goal.subGoals.forEach { subGoal ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onToggleSubGoal(subGoal) }) {
                                if (subGoal.isDone) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Mark as not done")
                                } else {
                                    Icon(
                                        Icons.Outlined.RadioButtonUnchecked,
                                        contentDescription = "Mark as done"
                                    )
                                }
                            }
                            Text(
                                modifier = Modifier.weight(1f),
                                text = subGoal.title,
                                textDecoration = if (subGoal.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (subGoal.isDone) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            IconButton(onClick = { onDeleteSubGoal(subGoal) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete subgoal")
                            }
                        }
                    }
                }
            }
        }
    }
}
