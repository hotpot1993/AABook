package com.aa.ledger.data.repository

import com.aa.ledger.data.local.dao.*
import com.aa.ledger.data.local.entity.SettlementEntity
import com.aa.ledger.domain.calculator.DebtCalculator
import com.aa.ledger.domain.model.SettlementResult
import com.aa.ledger.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettlementRepository @Inject constructor(
    private val settlementDao: SettlementDao,
    private val expenseRepository: ExpenseRepository,
    private val memberDao: MemberDao
) {
    fun getSettlements(ledgerId: Long): Flow<List<SettlementEntity>> {
        return settlementDao.getSettlementsByLedger(ledgerId)
    }

    /**
     * 计算当前账本的完整结算方案
     */
    suspend fun calculateSettlement(ledgerId: Long): SettlementResult {
        val members = memberDao.getMembersByLedgerSync(ledgerId)
        val memberNames = members.associate { it.id to (it.nickname.ifEmpty { it.name }) }

        val paidAmounts = expenseRepository.getMembersPaidAmounts(ledgerId)
        val owedAmounts = expenseRepository.getMembersOwedAmounts(ledgerId)
        val totalExpense = paidAmounts.values.sum()

        val (balances, transactions) = DebtCalculator.calculate(
            memberNames = memberNames,
            paidAmounts = paidAmounts,
            owedAmounts = owedAmounts,
            totalExpense = totalExpense
        )

        return SettlementResult(
            ledgerId = ledgerId,
            memberBalances = balances,
            transactions = transactions,
            totalExpense = totalExpense,
            isAllSettled = transactions.isEmpty()
        )
    }

    /**
     * 保存结算方案（标记为建议转账）
     */
    suspend fun saveSettlementPlan(
        ledgerId: Long,
        transactions: List<Transaction>
    ) {
        // Preserve paid status of existing matching settlements
        val existing = settlementDao.getSettlementsByLedgerSync(ledgerId)
        val entities = transactions.map { t ->
            val match = existing.find { e ->
                e.fromMemberId == t.fromMemberId &&
                e.toMemberId == t.toMemberId &&
                kotlin.math.abs(e.amountCny - t.amountCny) < 0.01
            }
            SettlementEntity(
                id = match?.id ?: 0,
                ledgerId = ledgerId,
                fromMemberId = t.fromMemberId,
                toMemberId = t.toMemberId,
                amountCny = t.amountCny,
                isPaid = match?.isPaid ?: false,
                paidAt = match?.paidAt,
                settledAt = match?.settledAt ?: System.currentTimeMillis()
            )
        }
        // Delete old ones that no longer match, then upsert
        settlementDao.deleteByLedger(ledgerId)
        settlementDao.insertSettlements(entities)
    }

    suspend fun markAsPaid(settlementId: Long) {
        settlementDao.markAsPaid(settlementId)
    }
}
