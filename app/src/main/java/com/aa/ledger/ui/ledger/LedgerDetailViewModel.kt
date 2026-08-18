package com.aa.ledger.ui.ledger

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aa.ledger.data.local.dao.MemberDao
import com.aa.ledger.data.local.entity.MemberEntity
import com.aa.ledger.data.repository.ExpenseRepository
import com.aa.ledger.data.repository.LedgerRepository
import com.aa.ledger.data.repository.SettlementRepository
import com.aa.ledger.domain.model.Expense
import com.aa.ledger.domain.model.Ledger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemberBillInfo(
    val member: MemberEntity,
    val paidAmount: Double = 0.0,
    val owedAmount: Double = 0.0,
    val netBalance: Double = 0.0
)

data class LedgerDetailUiState(
    val ledger: Ledger? = null,
    val expenses: List<Expense> = emptyList(),
    val memberCount: Int = 0,
    val totalExpense: Double = 0.0,
    val pendingSettlementCount: Int = 0,
    val totalSettlementCount: Int = 0,
    val memberBills: List<MemberBillInfo> = emptyList(),
    val isLoading: Boolean = true
) {
    val isSettled: Boolean get() = totalSettlementCount > 0 && pendingSettlementCount == 0
}

@HiltViewModel
class LedgerDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ledgerRepository: LedgerRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val memberDao: MemberDao
) : ViewModel() {

    private val ledgerId: Long = savedStateHandle.get<Long>("ledgerId") ?: 0L

    private val _uiState = MutableStateFlow(LedgerDetailUiState())
    val uiState: StateFlow<LedgerDetailUiState> = _uiState.asStateFlow()

    init {
        // Ensure settlement plan exists, then load
        viewModelScope.launch {
            try {
                val existing = settlementRepository.getSettlements(ledgerId).first()
                if (existing.isEmpty()) {
                    val result = settlementRepository.calculateSettlement(ledgerId)
                    if (result.transactions.isNotEmpty()) {
                        settlementRepository.saveSettlementPlan(ledgerId, result.transactions)
                    }
                }
            } catch (_: Exception) {}
            // Now start reactive listeners — DB is guaranteed to have data
            startListening()
        }
    }

    private fun startListening() {
        // Ledger info
        viewModelScope.launch {
            val ledger = ledgerRepository.getLedgerById(ledgerId)
            _uiState.update { it.copy(ledger = ledger) }
        }

        // Combined: expenses + settlements → pending count, member bills
        viewModelScope.launch {
            combine(
                expenseRepository.getExpensesByLedger(ledgerId),
                memberDao.getMembersByLedger(ledgerId),
                settlementRepository.getSettlements(ledgerId)
            ) { expenses, members, settlements ->
                val totalExpense = expenses.sumOf { it.totalAmountCny }
                val pendingCount = settlements.count { !it.isPaid }
                val totalCount = settlements.size

                val paidMap = mutableMapOf<Long, Double>()
                val owedMap = mutableMapOf<Long, Double>()
                expenses.forEach { exp ->
                    paidMap[exp.paidByMemberId] = (paidMap[exp.paidByMemberId] ?: 0.0) + exp.totalAmountCny
                    exp.splits.forEach { split ->
                        owedMap[split.memberId] = (owedMap[split.memberId] ?: 0.0) + split.amountCny
                    }
                }
                val bills = members.map { m ->
                    val paid = paidMap[m.id] ?: 0.0
                    val owed = owedMap[m.id] ?: 0.0
                    MemberBillInfo(member = m, paidAmount = paid, owedAmount = owed, netBalance = paid - owed)
                }

                _uiState.update {
                    it.copy(
                        expenses = expenses,
                        totalExpense = totalExpense,
                        memberCount = members.size,
                        pendingSettlementCount = pendingCount,
                        totalSettlementCount = totalCount,
                        memberBills = bills,
                        isLoading = false
                    )
                }
            }.collect { /* state updated inside combine lambda */ }
        }
    }

    fun refreshData() {
        // The combine Flow reacts automatically on DB changes.
        // Force recalculation of settlement plan after expense changes.
        viewModelScope.launch {
            try {
                val result = settlementRepository.calculateSettlement(ledgerId)
                if (result.transactions.isNotEmpty()) {
                    settlementRepository.saveSettlementPlan(ledgerId, result.transactions)
                }
            } catch (_: Exception) {}
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expenseId)
            // Settlement plan will be stale — refresh
            refreshData()
        }
    }
}
