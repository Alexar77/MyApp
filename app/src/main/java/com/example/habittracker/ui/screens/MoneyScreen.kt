package com.example.habittracker.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.ui.components.FabScrollClearance
import com.example.habittracker.ui.components.ChartTimeRange
import com.example.habittracker.ui.components.DatePickerField
import com.example.habittracker.ui.components.FilterChipsRow
import com.example.habittracker.ui.components.TrendChart
import com.example.habittracker.ui.components.TrendPoint
import com.example.habittracker.ui.icons.AppIcons
import com.example.habittracker.ui.viewmodel.MoneyExpenseUiItem
import com.example.habittracker.ui.viewmodel.MoneyViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.IsoFields

private enum class MoneyChartRate(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

private enum class MoneyEntryType(val label: String, val isIncome: Boolean) {
    EXPENSE("Expense", false),
    INCOME("Income", true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    onOpenMenu: () -> Unit = {},
    viewModel: MoneyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isBudgetDialogVisible by rememberSaveable { mutableStateOf(false) }
    var isExpenseDialogVisible by rememberSaveable { mutableStateOf(false) }
    var budgetInput by rememberSaveable { mutableStateOf("") }
    var hourlyWageInput by rememberSaveable { mutableStateOf("") }
    var expenseTitleInput by rememberSaveable { mutableStateOf("") }
    var expenseAmountInput by rememberSaveable { mutableStateOf("") }
    var expenseDateInput by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var expenseTypeInput by rememberSaveable { mutableStateOf(MoneyEntryType.EXPENSE) }
    var hiddenAmounts by rememberSaveable { mutableStateOf(false) }
    var expensePendingDelete by remember { mutableStateOf<MoneyExpenseUiItem?>(null) }
    var selectedRate by rememberSaveable { mutableStateOf(MoneyChartRate.DAILY) }
    var selectedRange by rememberSaveable { mutableStateOf(ChartTimeRange.MONTHLY) }
    var customStartDateInput by rememberSaveable { mutableStateOf(LocalDate.now().minusMonths(1).toString()) }
    var customEndDateInput by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }

    fun displayAmount(value: Double): String =
        if (hiddenAmounts) "••••••" else viewModel.formatCurrency(value)

    fun openDatePicker(currentValue: String, onSelected: (String) -> Unit) {
        val current = runCatching { LocalDate.parse(currentValue) }.getOrNull() ?: LocalDate.now()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onSelected(LocalDate.of(year, month + 1, dayOfMonth).toString())
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        ).show()
    }

    val chartPoints = remember(
        state.expenses,
        selectedRate,
        selectedRange,
        customStartDateInput,
        customEndDateInput
    ) {
        buildMoneyTrendPoints(
            expenses = state.expenses,
            selectedRate = selectedRate,
            selectedRange = selectedRange,
            customStart = customStartDateInput,
            customEnd = customEndDateInput
        )
    }
    val budgetLinePoints = remember(chartPoints, state.budgetAmount) {
        chartPoints.map { it.copy(value = state.budgetAmount) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Money") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Open navigation menu")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            budgetInput = state.budgetAmount.takeIf { it > 0 }?.toString().orEmpty()
                            hourlyWageInput = state.hourlyWage.takeIf { it > 0 }?.toString().orEmpty()
                            isBudgetDialogVisible = true
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit budget settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                expenseTitleInput = ""
                expenseAmountInput = ""
                expenseDateInput = LocalDate.now().toString()
                expenseTypeInput = MoneyEntryType.EXPENSE
                isExpenseDialogVisible = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add payment")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 16.dp,
                bottom = FabScrollClearance
            )
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Budget: ${displayAmount(state.budgetAmount)}")
                            IconButton(onClick = { hiddenAmounts = !hiddenAmounts }) {
                                Icon(
                                    imageVector = if (hiddenAmounts) AppIcons.Visibility else AppIcons.VisibilityOff,
                                    contentDescription = if (hiddenAmounts) "Show amounts" else "Hide amounts"
                                )
                            }
                        }
                        Text("Spent: ${displayAmount(state.totalSpent)}")
                        Text(
                            text = "Remaining: ${displayAmount(state.remainingBudget)}",
                            color = if (state.remainingBudget < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (hiddenAmounts) "Hourly wage: ••••" else "Hourly wage: ${viewModel.formatCurrency(state.hourlyWage)} / hour",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Income: ${displayAmount(state.totalIncome)}",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Money graph", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        FilterChipsRow(
                            labels = MoneyChartRate.entries.map { it.label },
                            selected = selectedRate.label,
                            onSelect = { label -> selectedRate = MoneyChartRate.entries.first { it.label == label } }
                        )
                        FilterChipsRow(
                            labels = ChartTimeRange.entries.map { it.label },
                            selected = selectedRange.label,
                            onSelect = { label -> selectedRange = ChartTimeRange.entries.first { it.label == label } }
                        )
                        if (selectedRange == ChartTimeRange.CUSTOM) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                DatePickerField(
                                    value = customStartDateInput,
                                    label = "Start date",
                                    onClick = { openDatePicker(customStartDateInput) { customStartDateInput = it } }
                                )
                                DatePickerField(
                                    value = customEndDateInput,
                                    label = "End date",
                                    onClick = { openDatePicker(customEndDateInput) { customEndDateInput = it } }
                                )
                            }
                        }
                        if (chartPoints.isEmpty()) {
                            Text("No spending data for the selected range", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            TrendChart(
                                points = chartPoints,
                                secondaryPoints = budgetLinePoints
                            )
                            Text(
                                text = "Primary line: net cash flow  •  Secondary line: budget",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text("Payments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (state.expenses.isEmpty()) {
                item {
                    Text("No payments recorded yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(state.expenses, key = { it.id }) { expense ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(expense.title, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Paid ${viewModel.formatDateTime(expense.paidAt)}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = if (expense.isIncome) "Time value: ${viewModel.formatHours(expense.hoursCost)}"
                                    else "Time cost: ${viewModel.formatHours(expense.hoursCost)}",
                                    color = if (expense.isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (expense.isIncome) "+${displayAmount(expense.amount)}" else "-${displayAmount(expense.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (expense.isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(onClick = { expensePendingDelete = expense }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete payment")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isBudgetDialogVisible) {
        AlertDialog(
            onDismissRequest = { isBudgetDialogVisible = false },
            title = { Text("Budget settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Budget amount") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = hourlyWageInput,
                        onValueChange = { hourlyWageInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Hourly wage") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveBudgetSettings(
                        budgetAmount = budgetInput.toDoubleOrNull() ?: 0.0,
                        hourlyWage = hourlyWageInput.toDoubleOrNull() ?: 0.0
                    )
                    isBudgetDialogVisible = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { isBudgetDialogVisible = false }) { Text("Cancel") }
            }
        )
    }

    if (isExpenseDialogVisible) {
        AlertDialog(
            onDismissRequest = { isExpenseDialogVisible = false },
            title = { Text("Add payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChipsRow(
                        labels = MoneyEntryType.entries.map { it.label },
                        selected = expenseTypeInput.label,
                        onSelect = { label -> expenseTypeInput = MoneyEntryType.entries.first { it.label == label } }
                    )
                    OutlinedTextField(
                        value = expenseTitleInput,
                        onValueChange = { expenseTitleInput = it },
                        label = { Text(if (expenseTypeInput.isIncome) "Income source" else "Item") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = expenseAmountInput,
                        onValueChange = { expenseAmountInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Amount") },
                        singleLine = true
                    )
                    DatePickerField(
                        value = expenseDateInput,
                        label = "Payment date",
                        onClick = { openDatePicker(expenseDateInput) { expenseDateInput = it } }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = expenseAmountInput.toDoubleOrNull() ?: 0.0
                    val paidDate = runCatching { LocalDate.parse(expenseDateInput) }.getOrNull()
                    if (paidDate != null) {
                        viewModel.addExpense(
                            title = expenseTitleInput,
                            amount = amount,
                            isIncome = expenseTypeInput.isIncome,
                            paidAt = paidDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        )
                        isExpenseDialogVisible = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { isExpenseDialogVisible = false }) { Text("Cancel") }
            }
        )
    }

    if (expensePendingDelete != null) {
        AlertDialog(
            onDismissRequest = { expensePendingDelete = null },
            title = { Text("Delete payment") },
            text = { Text("Delete \"${expensePendingDelete?.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    expensePendingDelete?.let { viewModel.deleteExpense(it.id) }
                    expensePendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { expensePendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

private fun buildMoneyTrendPoints(
    expenses: List<MoneyExpenseUiItem>,
    selectedRate: MoneyChartRate,
    selectedRange: ChartTimeRange,
    customStart: String,
    customEnd: String
): List<TrendPoint> {
    val today = LocalDate.now()
    val rawStartDate = when (selectedRange) {
        ChartTimeRange.WEEKLY -> today.minusWeeks(1)
        ChartTimeRange.MONTHLY -> today.minusMonths(1)
        ChartTimeRange.YEARLY -> today.minusYears(1)
        ChartTimeRange.CUSTOM -> runCatching { LocalDate.parse(customStart) }.getOrElse { today.minusMonths(1) }
    }
    val rawEndDate = when (selectedRange) {
        ChartTimeRange.CUSTOM -> runCatching { LocalDate.parse(customEnd) }.getOrElse { today }
        else -> today
    }
    val startDate = minOf(rawStartDate, rawEndDate)
    val endDate = maxOf(rawStartDate, rawEndDate)

    return expenses
        .mapNotNull { expense ->
            val date = runCatching {
                java.time.Instant.ofEpochMilli(expense.paidAt).atZone(ZoneId.systemDefault()).toLocalDate()
            }.getOrNull() ?: return@mapNotNull null
            if (date.isBefore(startDate) || date.isAfter(endDate)) return@mapNotNull null
            date to if (expense.isIncome) expense.amount else -expense.amount
        }
        .groupBy(
            keySelector = { (date, _) ->
                when (selectedRate) {
                    MoneyChartRate.DAILY -> date.toString()
                    MoneyChartRate.WEEKLY -> "${date.get(IsoFields.WEEK_BASED_YEAR)}-W${date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"
                    MoneyChartRate.MONTHLY -> YearMonth.from(date).toString()
                    MoneyChartRate.YEARLY -> date.year.toString()
                }
            },
            valueTransform = { it.second }
        )
        .toSortedMap()
        .map { (label, values) -> TrendPoint(label = label, value = values.sum()) }
}
