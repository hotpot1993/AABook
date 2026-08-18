package com.aa.ledger.ui.settlement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aa.ledger.data.local.entity.SettlementEntity
import com.aa.ledger.data.repository.SettlementRepository
import com.aa.ledger.domain.model.SettlementResult
import com.aa.ledger.domain.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettlementTransfer(
    val transaction: Transaction,
    val settlementId: Long?,
    val isPaid: Boolean = false
)

data class SettlementUiState(
    val result: SettlementResult? = null,
    val transfers: List<SettlementTransfer> = emptyList(),
    val paidCount: Int = 0,
    val unpaidTotal: Double = 0.0,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val shareText: String = "",
    val error: String? = null
)

@HiltViewModel
class SettlementViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settlementRepository: SettlementRepository
) : ViewModel() {

    private val ledgerId: Long = savedStateHandle.get<Long>("ledgerId") ?: 0L

    private val _uiState = MutableStateFlow(SettlementUiState())
    val uiState: StateFlow<SettlementUiState> = _uiState.asStateFlow()

    init {
        loadSettlement()
    }

    private fun loadSettlement() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val result = settlementRepository.calculateSettlement(ledgerId)
                val entities = settlementRepository.getSettlements(ledgerId).first()
                val transfers = buildTransfers(result.transactions, entities)
                refreshState(result, transfers)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "计算失败：${e.message}") }
            }
        }
    }

    private fun refreshState(result: SettlementResult, transfers: List<SettlementTransfer>) {
        val paidCount = transfers.count { it.isPaid }
        val unpaidTotal = transfers.filter { !it.isPaid }.sumOf { it.transaction.amountCny }
        _uiState.update {
            it.copy(
                result = result,
                transfers = transfers,
                paidCount = paidCount,
                unpaidTotal = unpaidTotal,
                isLoading = false,
                shareText = com.aa.ledger.util.ShareUtil.generateSettlementText(result)
            )
        }
    }

    fun calculateSettlement() = loadSettlement()

    private fun buildTransfers(
        transactions: List<Transaction>,
        entities: List<SettlementEntity>
    ): List<SettlementTransfer> {
        return transactions.map { tx ->
            val match = entities.find {
                it.fromMemberId == tx.fromMemberId &&
                it.toMemberId == tx.toMemberId &&
                kotlin.math.abs(it.amountCny - tx.amountCny) < 0.01
            }
            SettlementTransfer(
                transaction = tx,
                settlementId = match?.id,
                isPaid = match?.isPaid ?: false
            )
        }
    }

    fun markAsPaid(index: Int) {
        viewModelScope.launch {
            val current = _uiState.value
            val transfers = current.transfers.toMutableList()
            val st = transfers.getOrNull(index) ?: return@launch

            // If plan not saved yet, save it first
            if (st.settlementId == null) {
                settlementRepository.saveSettlementPlan(ledgerId, current.result!!.transactions)
                // Reload to get IDs
                val entities = settlementRepository.getSettlements(ledgerId).first()
                val newTransfers = buildTransfers(current.result!!.transactions, entities)
                val newSt = newTransfers.getOrNull(index) ?: return@launch
                if (newSt.settlementId == null) return@launch
                // Mark this specific one as paid
                settlementRepository.markAsPaid(newSt.settlementId)
            } else {
                settlementRepository.markAsPaid(st.settlementId)
            }

            // Reload entities to refresh state
            val entities = settlementRepository.getSettlements(ledgerId).first()
            val refreshed = buildTransfers(current.result!!.transactions, entities)
            refreshState(current.result!!, refreshed)
        }
    }

    fun saveSettlementPlan() {
        val transactions = _uiState.value.result?.transactions ?: return
        viewModelScope.launch {
            settlementRepository.saveSettlementPlan(ledgerId, transactions)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
