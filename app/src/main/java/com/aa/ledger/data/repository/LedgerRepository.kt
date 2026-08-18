package com.aa.ledger.data.repository

import com.aa.ledger.data.local.dao.*
import com.aa.ledger.data.local.entity.LedgerEntity
import com.aa.ledger.data.local.entity.MemberEntity
import com.aa.ledger.domain.model.Ledger
import com.aa.ledger.domain.model.toLedger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LedgerRepository @Inject constructor(
    private val ledgerDao: LedgerDao,
    private val memberDao: MemberDao,
    private val expenseDao: ExpenseDao,
    private val settlementDao: SettlementDao
) {
    fun getAllLedgers(): Flow<List<Ledger>> = flow {
        // 同时监听账本表、消费表、成员表、结算表变化
        combine(
            ledgerDao.getAllLedgers(),
            expenseDao.getAllExpensesFlow(),
            memberDao.getAllMembersFlow(),
            settlementDao.getAllSettlementsFlow()
        ) { entities, _, _, _ ->
            entities.map { entity ->
                val memberCount = memberDao.getMemberCount(entity.id)
                val totalExpense = expenseDao.getExpensesByLedgerSync(entity.id).sumOf { it.totalAmountCny }
                entity.toLedger(memberCount = memberCount, totalExpense = totalExpense)
            }
        }.collect { emit(it) }
    }

    suspend fun getLedgerById(id: Long): Ledger? {
        return ledgerDao.getLedgerById(id)?.toLedger()
    }

    suspend fun createLedger(
        name: String,
        description: String = "",
        coverType: String = "green",
        baseCurrency: String = "CNY",
        budgetAmount: Double = 0.0,
        initialMembers: List<String>
    ): Long {
        val ledgerId = ledgerDao.insertLedger(
            LedgerEntity(
                name = name,
                description = description,
                coverType = coverType,
                baseCurrency = baseCurrency,
                budgetAmount = budgetAmount
            )
        )
        // 添加初始成员
        initialMembers.forEach { memberName ->
            memberDao.insertMember(
                MemberEntity(
                    ledgerId = ledgerId,
                    name = memberName
                )
            )
        }
        return ledgerId
    }

    suspend fun updateLedger(ledger: Ledger) {
        val entity = ledgerDao.getLedgerById(ledger.id) ?: return
        ledgerDao.updateLedger(
            entity.copy(
                name = ledger.name,
                description = ledger.description,
                coverType = ledger.coverType,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteLedger(id: Long) {
        val entity = ledgerDao.getLedgerById(id) ?: return
        ledgerDao.deleteLedger(entity)
    }

    suspend fun archiveLedger(id: Long) {
        ledgerDao.archiveLedger(id)
    }

    suspend fun restoreLedger(id: Long) {
        ledgerDao.restoreLedger(id)
    }

    suspend fun getLedgerMemberCount(ledgerId: Long): Int {
        return memberDao.getMemberCount(ledgerId)
    }

    suspend fun getLedgerTotalExpense(ledgerId: Long): Double {
        val expenses = expenseDao.getExpensesByLedgerSync(ledgerId)
        return expenses.sumOf { it.totalAmountCny }
    }
}
