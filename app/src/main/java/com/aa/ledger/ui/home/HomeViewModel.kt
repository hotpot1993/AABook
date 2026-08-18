package com.aa.ledger.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aa.ledger.data.repository.LedgerRepository
import com.aa.ledger.data.repository.SettlementRepository
import com.aa.ledger.domain.model.Ledger
import com.aa.ledger.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val ledgers: List<Ledger> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val pendingTotal: Double = 0.0,
    val ledgerSettledMap: Map<Long, Boolean> = emptyMap(),
    val filterMode: HomeFilterMode = HomeFilterMode.ACTIVE,
    val defaultLedgerId: Long = -1L
)

enum class HomeFilterMode { ALL, ACTIVE, SETTLED }

private data class SettlementStatus(
    val isSettled: Boolean,
    val unpaidAmount: Double
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ledgerRepository: LedgerRepository,
    private val settlementRepository: SettlementRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ledgerRepository.getAllLedgers().collect { ledgers ->
                val nonArchived = ledgers.filter { !it.isArchived }
                val settledMap = mutableMapOf<Long, Boolean>()
                var pendingTotal = 0.0
                for (ledger in nonArchived) {
                    val status = checkLedgerSettled(ledger.id, ledger.totalExpense)
                    settledMap[ledger.id] = status.isSettled
                    if (!status.isSettled) {
                        pendingTotal += status.unpaidAmount
                    }
                }

                // Default ledger logic
                var defaultId = prefs.defaultLedgerId
                // If saved default no longer exists or is archived, reset and auto-pick first
                if (nonArchived.none { it.id == defaultId }) {
                    defaultId = if (nonArchived.isNotEmpty()) nonArchived.first().id else -1L
                    prefs.defaultLedgerId = defaultId  // persist the auto-selected default
                }
                // If never set (-1) but have ledgers, use first
                if (defaultId == -1L && nonArchived.isNotEmpty()) {
                    defaultId = nonArchived.first().id
                    prefs.defaultLedgerId = defaultId
                }

                // Sort: default ledger first
                val sorted = ledgers.sortedByDescending { it.id == defaultId }

                _uiState.update {
                    it.copy(
                        ledgers = sorted,
                        isLoading = false,
                        pendingTotal = pendingTotal,
                        ledgerSettledMap = settledMap,
                        defaultLedgerId = defaultId
                    )
                }
            }
        }
    }

    /** Called when user clicks a ledger card. Sets it as default unless settled. */
    fun onLedgerClicked(ledger: Ledger) {
        val isSettled = _uiState.value.ledgerSettledMap[ledger.id] == true
        if (!isSettled) {
            prefs.defaultLedgerId = ledger.id
            _uiState.update { it.copy(defaultLedgerId = ledger.id) }
        }
    }

    private suspend fun checkLedgerSettled(ledgerId: Long, totalExpense: Double): SettlementStatus {
        return try {
            val entities = settlementRepository.getSettlements(ledgerId).first()
            if (entities.isEmpty()) return SettlementStatus(false, totalExpense)
            val allPaid = entities.all { it.isPaid }
            val unpaidAmount = entities.filter { !it.isPaid }.sumOf { it.amountCny }
            SettlementStatus(allPaid, unpaidAmount)
        } catch (_: Exception) {
            SettlementStatus(false, totalExpense)
        }
    }

    fun refreshSettlementStatus() {
        viewModelScope.launch {
            val ledgers = _uiState.value.ledgers
            val settledMap = mutableMapOf<Long, Boolean>()
            var pendingTotal = 0.0
            for (ledger in ledgers) {
                val status = checkLedgerSettled(ledger.id, ledger.totalExpense)
                settledMap[ledger.id] = status.isSettled
                if (!status.isSettled) {
                    pendingTotal += status.unpaidAmount
                }
            }
            _uiState.update {
                it.copy(
                    pendingTotal = pendingTotal,
                    ledgerSettledMap = settledMap
                )
            }
        }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun dismissCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun createLedger(name: String, description: String = "", coverType: String = "green", baseCurrency: String = "CNY", budgetAmount: Double = 0.0, initialMembers: List<String>) {
        viewModelScope.launch {
            ledgerRepository.createLedger(name, description, coverType = coverType, baseCurrency = baseCurrency, budgetAmount = budgetAmount, initialMembers = initialMembers)
            _uiState.update { it.copy(showCreateDialog = false) }
        }
    }

    fun archiveLedger(id: Long) {
        viewModelScope.launch {
            ledgerRepository.archiveLedger(id)
        }
    }

    fun restoreLedger(id: Long) {
        viewModelScope.launch {
            ledgerRepository.restoreLedger(id)
        }
    }

    fun setFilterMode(mode: HomeFilterMode) {
        _uiState.update { it.copy(filterMode = mode) }
    }

    fun deleteLedger(id: Long) {
        viewModelScope.launch {
            ledgerRepository.deleteLedger(id)
        }
    }

    fun getFilteredLedgers(state: HomeUiState): List<Ledger> {
        val nonArchived = state.ledgers.filter { !it.isArchived }
        // Default ledger first
        val sorted = nonArchived.sortedByDescending { it.id == state.defaultLedgerId }
        return when (state.filterMode) {
            HomeFilterMode.ALL -> sorted
            HomeFilterMode.ACTIVE -> sorted.filter { state.ledgerSettledMap[it.id] != true }
            HomeFilterMode.SETTLED -> sorted.filter { state.ledgerSettledMap[it.id] == true }
        }
    }
}
