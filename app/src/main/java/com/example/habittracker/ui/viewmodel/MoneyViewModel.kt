package com.example.habittracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MoneyExpenseUiItem(
    val id: Long,
    val title: String,
    val category: String,
    val amount: Double,
    val isIncome: Boolean,
    val paidAt: Long,
    val hoursCost: Double
)

data class MoneyUiState(
    val budgetAmount: Double = 0.0,
    val hourlyWage: Double = 0.0,
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val expenses: List<MoneyExpenseUiItem> = emptyList()
)

@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val repository: HabitRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MoneyUiState())
    val uiState: StateFlow<MoneyUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeMoneySettings(),
                repository.observeMoneyExpenses()
            ) { settings, expenses ->
                val hourlyWage = settings?.hourlyWage ?: 0.0
                val mappedExpenses = expenses.map { expense ->
                    MoneyExpenseUiItem(
                        id = expense.id,
                        title = expense.title,
                        category = expense.category,
                        amount = expense.amount,
                        isIncome = expense.isIncome,
                        paidAt = expense.paidAt,
                        hoursCost = if (hourlyWage > 0.0) expense.amount / hourlyWage else 0.0
                    )
                }
                val totalSpent = mappedExpenses.filterNot { it.isIncome }.sumOf { it.amount }
                val totalIncome = mappedExpenses.filter { it.isIncome }.sumOf { it.amount }
                MoneyUiState(
                    budgetAmount = settings?.budgetAmount ?: 0.0,
                    hourlyWage = hourlyWage,
                    totalSpent = totalSpent,
                    totalIncome = totalIncome,
                    remainingBudget = (settings?.budgetAmount ?: 0.0) + totalIncome - totalSpent,
                    expenses = mappedExpenses
                )
            }.collect { state ->
                mutableUiState.update { state }
            }
        }
    }

    fun saveBudgetSettings(budgetAmount: Double, hourlyWage: Double) {
        viewModelScope.launch {
            repository.saveMoneySettings(budgetAmount, hourlyWage)
        }
    }

    fun addExpense(title: String, amount: Double, isIncome: Boolean, paidAt: Long, category: String) {
        viewModelScope.launch {
            repository.addMoneyExpense(title, amount, isIncome, paidAt, category)
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            repository.deleteMoneyExpense(expenseId)
        }
    }

    fun deleteCategory(categoryName: String) {
        viewModelScope.launch {
            repository.replaceMoneyCategory(categoryName)
        }
    }

    fun formatCurrency(value: Double): String =
        NumberFormat.getCurrencyInstance(Locale.getDefault()).format(value)

    fun formatHours(hours: Double): String =
        if (hours <= 0.0) "--"
        else String.format(Locale.getDefault(), "%.1f h", hours)

    fun formatDateTime(millis: Long): String =
        Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
}
