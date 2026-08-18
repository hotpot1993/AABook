package com.aa.ledger.ui.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aa.ledger.data.local.dao.MemberDao
import com.aa.ledger.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsUiState(
    val categorySummary: Map<String, Double> = emptyMap(),
    val memberPaidAmounts: Map<Long, Double> = emptyMap(),
    val memberOwedAmounts: Map<Long, Double> = emptyMap(),
    val memberNames: Map<Long, String> = emptyMap(),
    val monthlyTimeline: Map<String, List<Double>> = emptyMap(), // month -> expenses
    val totalExpense: Double = 0.0,
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val expenseRepository: ExpenseRepository,
    private val memberDao: MemberDao
) : ViewModel() {

    private val ledgerId: Long = savedStateHandle.get<Long>("ledgerId") ?: 0L

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        // Reactively observe expense changes
        viewModelScope.launch {
            expenseRepository.getExpensesByLedger(ledgerId).collect { expenses ->
                computeStats()
            }
        }
    }

    private suspend fun computeStats() {
        val categorySummary = expenseRepository.getCategorySummary(ledgerId)
        val paidAmounts = expenseRepository.getMembersPaidAmounts(ledgerId)
        val owedAmounts = expenseRepository.getMembersOwedAmounts(ledgerId)
        val members = memberDao.getMembersByLedgerSync(ledgerId)
        val memberNames = members.associate { it.id to (it.nickname.ifEmpty { it.name }) }
        val totalExpense = paidAmounts.values.sum()

        // Monthly timeline (last 12 months)
        val now = System.currentTimeMillis()
        val twelveMonthsAgo = now - 365L * 24 * 3600 * 1000
        val expensesByMonth = expenseRepository
            .getExpensesByDateRange(ledgerId, twelveMonthsAgo, now)
            .groupBy { expense ->
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = expense.createdAt
                "${cal.get(java.util.Calendar.YEAR)}-${(cal.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')}"
            }
            .mapValues { (_, expenses) -> expenses.map { it.totalAmountCny } }

        _uiState.update {
            it.copy(
                categorySummary = categorySummary,
                memberPaidAmounts = paidAmounts,
                memberOwedAmounts = owedAmounts,
                memberNames = memberNames,
                monthlyTimeline = expensesByMonth,
                totalExpense = totalExpense,
                isLoading = false
            )
        }
    }
}
