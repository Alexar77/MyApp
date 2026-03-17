package com.example.habittracker.ui.screens

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import com.example.habittracker.ui.icons.AppIcons
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.ui.components.FabScrollClearance
import com.example.habittracker.ui.viewmodel.GoalUiItem
import com.example.habittracker.ui.viewmodel.GoalsViewModel
import com.example.habittracker.ui.viewmodel.SubGoalUiItem
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onOpenMenu: () -> Unit = {},
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val goalsToAchieve by remember(state.goals) {
        derivedStateOf { state.goals.filter { !it.isDone } }
    }
    val goalsAchieved by remember(state.goals) {
        derivedStateOf { state.goals.filter { it.isDone } }
    }
    var expandedGoalIds by remember { mutableStateOf(setOf<Long>()) }

    var isGoalDialogVisible by rememberSaveable { mutableStateOf(false) }
    var goalInput by rememberSaveable { mutableStateOf("") }

    var isSubGoalDialogVisible by rememberSaveable { mutableStateOf(false) }
    var selectedGoalIdForSubGoal by rememberSaveable { mutableLongStateOf(0L) }
    var subGoalInput by rememberSaveable { mutableStateOf("") }

    var goalPendingDelete by remember { mutableStateOf<GoalUiItem?>(null) }
    var subGoalPendingDelete by remember { mutableStateOf<SubGoalUiItem?>(null) }
    var goalPendingRename by remember { mutableStateOf<GoalUiItem?>(null) }
    var subGoalPendingRename by remember { mutableStateOf<SubGoalUiItem?>(null) }
    var renameInput by rememberSaveable { mutableStateOf("") }
    var draggingGoalId by remember { mutableStateOf<Long?>(null) }
    var isToAchieveExpanded by rememberSaveable { mutableStateOf(true) }
    var isAchievedExpanded by rememberSaveable { mutableStateOf(false) }

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
        topBar = {
            TopAppBar(
                title = { Text("Goals") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Open navigation menu")
                    }
                }
            )
        },
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
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = FabScrollClearance),
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = FabScrollClearance)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { isToAchieveExpanded = !isToAchieveExpanded },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Goals to achieve",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = if (isToAchieveExpanded) {
                                        AppIcons.ExpandLess
                                    } else {
                                        AppIcons.ExpandMore
                                    },
                                    contentDescription = if (isToAchieveExpanded) "Collapse" else "Expand"
                                )
                            }
                        }
                    }

                    if (isToAchieveExpanded) {
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
                                    onRenameGoal = {
                                        goalPendingRename = goal
                                        renameInput = goal.title
                                    },
                                    onToggleSubGoal = viewModel::toggleSubGoal,
                                    onMoveSubGoal = { subGoalId, direction ->
                                        viewModel.moveSubGoal(goal.id, subGoalId, direction)
                                    },
                                    onDeleteSubGoal = { subGoalPendingDelete = it },
                                    onRenameSubGoal = {
                                        subGoalPendingRename = it
                                        renameInput = it.title
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { isAchievedExpanded = !isAchievedExpanded },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Goals achieved",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = if (isAchievedExpanded) {
                                        AppIcons.ExpandLess
                                    } else {
                                        AppIcons.ExpandMore
                                    },
                                    contentDescription = if (isAchievedExpanded) "Collapse" else "Expand"
                                )
                            }
                        }
                    }

                    if (isAchievedExpanded) {
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
                                    onRenameGoal = {
                                        goalPendingRename = goal
                                        renameInput = goal.title
                                    },
                                    onToggleSubGoal = viewModel::toggleSubGoal,
                                    onMoveSubGoal = { subGoalId, direction ->
                                        viewModel.moveSubGoal(goal.id, subGoalId, direction)
                                    },
                                    onDeleteSubGoal = { subGoalPendingDelete = it },
                                    onRenameSubGoal = {
                                        subGoalPendingRename = it
                                        renameInput = it.title
                                    }
                                )
                            }
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

    if (goalPendingRename != null) {
        AlertDialog(
            onDismissRequest = { goalPendingRename = null },
            title = { Text("Rename goal") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Goal title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val goal = goalPendingRename
                        if (goal != null && renameInput.isNotBlank()) {
                            viewModel.renameGoal(goal, renameInput)
                            goalPendingRename = null
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { goalPendingRename = null }) { Text("Cancel") }
            }
        )
    }

    if (subGoalPendingRename != null) {
        AlertDialog(
            onDismissRequest = { subGoalPendingRename = null },
            title = { Text("Rename subgoal") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Subgoal title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val subGoal = subGoalPendingRename
                        if (subGoal != null && renameInput.isNotBlank()) {
                            viewModel.renameSubGoal(subGoal, renameInput)
                            subGoalPendingRename = null
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { subGoalPendingRename = null }) { Text("Cancel") }
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
    onRenameGoal: () -> Unit,
    onToggleSubGoal: (SubGoalUiItem) -> Unit,
    onMoveSubGoal: (Long, Int) -> Unit,
    onDeleteSubGoal: (SubGoalUiItem) -> Unit,
    onRenameSubGoal: (SubGoalUiItem) -> Unit
) {
    var dragOffsetY by remember(goal.id) { mutableFloatStateOf(0f) }
    var cardHeightPx by remember(goal.id) { mutableFloatStateOf(0f) }
    var isGoalActionsMenuExpanded by rememberSaveable(goal.id) { mutableStateOf(false) }
    val dragContainerColor = if (isDragging) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = dragContainerColor),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp)
            )
            .graphicsLayer { translationY = dragOffsetY }
            .zIndex(if (isDragging) 1f else 0f)
            .onSizeChanged { size ->
                cardHeightPx = size.height.toFloat().coerceAtLeast(1f)
            }
            .pointerInput(goal.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        dragOffsetY = 0f
                        onDragStart()
                    },
                    onDragEnd = {
                        dragOffsetY = 0f
                        onDragEnd()
                    },
                    onDragCancel = {
                        dragOffsetY = 0f
                        onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y
                        val reorderStepPx = cardHeightPx

                        while (dragOffsetY > reorderStepPx) {
                            onMove(1)
                            dragOffsetY -= reorderStepPx
                        }
                        while (dragOffsetY < -reorderStepPx) {
                            onMove(-1)
                            dragOffsetY += reorderStepPx
                        }
                    }
                )
            },
        shape = RoundedCornerShape(16.dp)
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
                        imageVector = if (goal.isDone) Icons.Default.CheckCircle else AppIcons.RadioButtonUnchecked,
                        contentDescription = if (goal.isDone) "Mark goal as not done" else "Mark goal as done"
                    )
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (goal.isDone) TextDecoration.LineThrough else TextDecoration.None
                        )
                        if (goal.isDone && goal.completedAt != null) {
                            val completedDateText = remember(goal.completedAt) {
                                val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
                                java.time.Instant.ofEpochMilli(goal.completedAt)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                    .format(formatter)
                            }
                            Text(
                                text = "Completed on $completedDateText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { isGoalActionsMenuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Open goal actions")
                    }
                    DropdownMenu(
                        expanded = isGoalActionsMenuExpanded,
                        onDismissRequest = { isGoalActionsMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add subgoal") },
                            onClick = {
                                isGoalActionsMenuExpanded = false
                                onAddSubGoal()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename goal") },
                            onClick = {
                                isGoalActionsMenuExpanded = false
                                onRenameGoal()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete goal") },
                            onClick = {
                                isGoalActionsMenuExpanded = false
                                onDeleteGoal()
                            }
                        )
                    }
                }
                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (expanded) AppIcons.ExpandLess else AppIcons.ExpandMore,
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
                        key(subGoal.id) {
                            SubGoalRow(
                                subGoal = subGoal,
                                onToggleSubGoal = { onToggleSubGoal(subGoal) },
                                onMove = { direction -> onMoveSubGoal(subGoal.id, direction) },
                                onDelete = { onDeleteSubGoal(subGoal) },
                                onRename = { onRenameSubGoal(subGoal) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubGoalRow(
    subGoal: SubGoalUiItem,
    onToggleSubGoal: () -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    var dragOffsetY by remember(subGoal.id) { mutableFloatStateOf(0f) }
    var rowHeightPx by remember(subGoal.id) { mutableFloatStateOf(0f) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = dragOffsetY }
            .zIndex(if (dragOffsetY != 0f) 1f else 0f)
            .onSizeChanged { rowHeightPx = it.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(subGoal.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragOffsetY = 0f },
                    onDragEnd = { dragOffsetY = 0f },
                    onDragCancel = { dragOffsetY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y
                        val reorderStepPx = rowHeightPx
                        while (dragOffsetY > reorderStepPx) {
                            onMove(1)
                            dragOffsetY -= reorderStepPx
                        }
                        while (dragOffsetY < -reorderStepPx) {
                            onMove(-1)
                            dragOffsetY += reorderStepPx
                        }
                    }
                )
            }
    ) {
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
                    .clickable { onToggleSubGoal() }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (subGoal.isDone) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Mark as not done")
                } else {
                    Icon(AppIcons.RadioButtonUnchecked, contentDescription = "Mark as done")
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                ) {
                    Text(
                        text = subGoal.title,
                        textDecoration = if (subGoal.isDone) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (subGoal.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    if (subGoal.isDone && subGoal.completedAt != null) {
                        val completedDateText = remember(subGoal.completedAt) {
                            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
                            java.time.Instant.ofEpochMilli(subGoal.completedAt)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .format(formatter)
                        }
                        Text(
                            text = "Completed on $completedDateText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete subgoal")
            }
            IconButton(onClick = onRename) {
                Icon(Icons.Default.Edit, contentDescription = "Rename subgoal")
            }
        }
    }
}
