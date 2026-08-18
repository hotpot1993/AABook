package com.aa.ledger.ui.member

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aa.ledger.data.local.dao.ExpenseDao
import com.aa.ledger.data.local.dao.ExpenseSplitDao
import com.aa.ledger.data.local.dao.MemberDao
import com.aa.ledger.data.local.entity.ExpenseSplitEntity
import com.aa.ledger.data.local.entity.MemberEntity
import com.aa.ledger.data.repository.ExpenseRepository
import com.aa.ledger.data.repository.SettlementRepository
import com.aa.ledger.domain.calculator.SplitCalculator
import com.aa.ledger.domain.model.ShareType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemberManageUiState(
    val members: List<MemberEntity> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val editingMember: MemberEntity? = null,
    val newMemberName: String = "",
    val newMemberNickname: String = "",
    val sharePrevious: Boolean = true,
    // Delete safety
    val showDeleteWarning: Boolean = false,
    val pendingDeleteMember: MemberEntity? = null,
    val pendingDeleteInfo: String = ""
)

@HiltViewModel
class MemberManageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memberDao: MemberDao,
    private val expenseDao: ExpenseDao,
    private val expenseSplitDao: ExpenseSplitDao,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val ledgerId: Long = savedStateHandle.get<Long>("ledgerId") ?: 0L

    private val _uiState = MutableStateFlow(MemberManageUiState())
    val uiState: StateFlow<MemberManageUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            memberDao.getMembersByLedger(ledgerId).collect { members ->
                _uiState.update { it.copy(members = members, isLoading = false) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update {
            it.copy(showAddDialog = true, editingMember = null, newMemberName = "", newMemberNickname = "")
        }
    }

    fun showEditDialog(member: MemberEntity) {
        _uiState.update {
            it.copy(
                showAddDialog = true,
                editingMember = member,
                newMemberName = member.name,
                newMemberNickname = member.nickname
            )
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showAddDialog = false, editingMember = null) }
    }

    fun updateNewMemberName(name: String) {
        _uiState.update { it.copy(newMemberName = name) }
    }

    fun updateNewMemberNickname(nickname: String) {
        _uiState.update { it.copy(newMemberNickname = nickname) }
    }

    fun updateSharePrevious(v: Boolean) {
        _uiState.update { it.copy(sharePrevious = v) }
    }

    fun addOrUpdateMember() {
        val state = _uiState.value
        if (state.newMemberName.isBlank()) return

        viewModelScope.launch {
            val editing = state.editingMember
            if (editing != null) {
                memberDao.updateMember(
                    editing.copy(
                        name = state.newMemberName.trim(),
                        nickname = state.newMemberNickname.trim()
                    )
                )
            } else {
                val newMember = memberDao.insertMember(
                    MemberEntity(ledgerId = ledgerId, name = state.newMemberName.trim(), nickname = state.newMemberNickname.trim())
                )
                // If sharing previous expenses, add new member to all existing expense splits
                if (state.sharePrevious) {
                    addMemberToExistingSplits(newMember)
                }
            }
            // Recalculate settlement
            recalculateSettlement()
            _uiState.update { it.copy(showAddDialog = false, editingMember = null, sharePrevious = true) }
        }
    }

    private suspend fun addMemberToExistingSplits(newMemberId: Long) {
        val expenses = expenseDao.getExpensesByLedgerSync(ledgerId)
        for (expense in expenses) {
            val existingSplits = expenseSplitDao.getSplitsByExpenseSync(expense.id)
            val memberIds = existingSplits.map { it.memberId }.toMutableList()
            if (newMemberId !in memberIds) {
                memberIds.add(newMemberId)
                val shareTypeStr = existingSplits.firstOrNull()?.shareType ?: "EQUAL"
                val shareType = try { ShareType.valueOf(shareTypeStr) } catch (_: Exception) { ShareType.EQUAL }
                val shareValues = if (shareType == ShareType.EQUAL) memberIds.map { 1.0 }
                else existingSplits.map { it.shareValue } + 1.0
                val newSplits = SplitCalculator.calculate(
                    totalAmountCny = expense.totalAmountCny,
                    memberIds = memberIds,
                    shareType = shareType,
                    shareValues = shareValues
                )
                expenseSplitDao.deleteSplitsByExpense(expense.id)
                expenseSplitDao.insertSplits(newSplits.map { s ->
                    ExpenseSplitEntity(expenseId = expense.id, memberId = s.memberId,
                        shareType = s.shareType.name, shareValue = s.shareValue, amountCny = s.amountCny)
                })
            }
        }
    }

    private suspend fun recalculateSettlement() {
        try {
            val result = settlementRepository.calculateSettlement(ledgerId)
            if (result.transactions.isNotEmpty()) {
                settlementRepository.saveSettlementPlan(ledgerId, result.transactions)
            }
        } catch (_: Exception) {}
    }

    fun requestDeleteMember(member: MemberEntity) {
        viewModelScope.launch {
            // Check for related expenses
            val expenses = expenseDao.getExpensesByLedgerSync(ledgerId)
            val paidCount = expenses.count { it.paidByMemberId == member.id }
            val splitsForMember = expenseSplitDao.getSplitsByLedger(ledgerId)
                .count { it.memberId == member.id }

            if (paidCount > 0 || splitsForMember > 0) {
                val parts = mutableListOf<String>()
                if (paidCount > 0) parts.add("$paidCount 笔付款记录")
                if (splitsForMember > 0) parts.add("$splitsForMember 笔分摊记录")
                _uiState.update {
                    it.copy(
                        showDeleteWarning = true,
                        pendingDeleteMember = member,
                        pendingDeleteInfo = "该成员有 ${parts.joinToString("、")}，删除后相关数据将丢失。"
                    )
                }
            } else {
                // No related data, delete directly
                memberDao.deleteMember(member)
            }
        }
    }

    fun confirmDelete() {
        val member = _uiState.value.pendingDeleteMember ?: return
        viewModelScope.launch {
            memberDao.deleteMember(member)
            _uiState.update { it.copy(showDeleteWarning = false, pendingDeleteMember = null, pendingDeleteInfo = "") }
        }
    }

    fun dismissDeleteWarning() {
        _uiState.update { it.copy(showDeleteWarning = false, pendingDeleteMember = null, pendingDeleteInfo = "") }
    }

    fun removeMember(member: MemberEntity) {
        requestDeleteMember(member)
    }

    fun updateMemberAvatar(memberId: Long, uri: Uri) {
        viewModelScope.launch {
            val member = memberDao.getMemberById(memberId) ?: return@launch
            // 复制到持久目录 filesDir/avatars/，避免头像存 cacheDir 被系统清理
            val persistentUri = try {
                val dir = java.io.File(appContext.filesDir, "avatars")
                dir.mkdirs()
                val file = java.io.File(dir, "avatar_${System.currentTimeMillis()}.jpg")
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                if (file.length() > 100) Uri.parse(file.toURI().toString()) else uri
            } catch (_: Exception) { uri }
            memberDao.updateMember(member.copy(avatarUri = persistentUri.toString()))
        }
    }

}
