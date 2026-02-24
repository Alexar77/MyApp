package com.example.habittracker.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.material3.CardDefaults
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
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
    var draggingGoalId by remember { mutableStateOf<Long?>(null) }

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
            val goalsToAchieve = state.goals.filter { !it.isDone }
            val goalsAchieved = state.goals.filter { it.isDone }

            if (state.goals.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
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
                    item {
                        Text(
                            text = "Goals to achieve",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (goalsToAchieve.isEmpty()) {
                        item {
                            Text(
                                text = "No goals to achieve right now",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(goalsToAchieve, key = { it.id }) { goal ->
                            val expanded = expandedGoalIds.contains(goal.id)
                            GoalCard(
                                modifier = Modifier,
                                goal = goal,
                                expanded = expanded,
                                isDragging = draggingGoalId == goal.id,
                                onToggleGoalDone = { viewModel.toggleGoal(goal) },
                                onToggleExpanded = {
                                    expandedGoalIds = if (expanded) expandedGoalIds - goal.id else expandedGoalIds + goal.id
                                },
                                onMove = { direction -> viewModel.moveGoal(goal.id, goal.isDone, direction) },
                                onDragStart = { draggingGoalId = goal.id },
                                onDragEnd = { draggingGoalId = null },
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

                    item {
                        Text(
                            text = "Goals achieved",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    item {
                        Text(
                            text = "Look what you have achieved untill now keep going you got this",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (goalsAchieved.isEmpty()) {
                        item {
                            Text(
                                text = "No achieved goals yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(goalsAchieved, key = { it.id }) { goal ->
                            val expanded = expandedGoalIds.contains(goal.id)
                            GoalCard(
                                modifier = Modifier,
                                goal = goal,
                                expanded = expanded,
                                isDragging = draggingGoalId == goal.id,
                                onToggleGoalDone = { viewModel.toggleGoal(goal) },
                                onToggleExpanded = {
                                    expandedGoalIds = if (expanded) expandedGoalIds - goal.id else expandedGoalIds + goal.id
                                },
                                onMove = { direction -> viewModel.moveGoal(goal.id, goal.isDone, direction) },
                                onDragStart = { draggingGoalId = goal.id },
                                onDragEnd = { draggingGoalId = null },
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
    modifier: Modifier = Modifier,
    goal: GoalUiItem,
    expanded: Boolean,
    isDragging: Boolean,
    onToggleGoalDone: () -> Unit,
    onToggleExpanded: () -> Unit,
    onMove: (Int) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onAddSubGoal: () -> Unit,
    onDeleteGoal: () -> Unit,
    onToggleSubGoal: (SubGoalUiItem) -> Unit,
    onDeleteSubGoal: (SubGoalUiItem) -> Unit
) {
    var dragY = 0f
    val animatedScale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        animationSpec = spring()
    )
    val dragContainerColor by animateColorAsState(
        targetValue = if (isDragging) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = dragContainerColor),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .pointerInput(goal.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDragEnd = {
                        dragY = 0f
                        onDragEnd()
                    },
                    onDragCancel = {
                        dragY = 0f
                        onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragY += dragAmount.y
                        when {
                            dragY > 28f -> {
                                onMove(1)
                                dragY = 0f
                            }
                            dragY < -28f -> {
                                onMove(-1)
                                dragY = 0f
                            }
                        }
                    }
                )
            }
    ) {
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
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleGoalDone() }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
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
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onToggleSubGoal(subGoal) }
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (subGoal.isDone) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Mark as not done")
                                } else {
                                    Icon(
                                        Icons.Outlined.RadioButtonUnchecked,
                                        contentDescription = "Mark as done"
                                    )
                                }
                                Text(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 10.dp),
                                    text = subGoal.title,
                                    textDecoration = if (subGoal.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (subGoal.isDone) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
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
