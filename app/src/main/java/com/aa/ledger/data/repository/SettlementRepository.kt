package com.aa.ledger.data.repository

import com.aa.ledger.data.local.dao.*
import com.aa.ledger.data.local.entity.SettlementEntity
import com.aa.ledger.domain.calculator.DebtCalculator
import com.aa.ledger.domain.model.SettlementResult
import com.aa.ledger.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

    /** 账本待结算状态：是否已结清、以及尚未结清的金额。 */
    data class PendingStatus(val isSettled: Boolean, val pendingAmount: Double)

    /**
     * 计算账本待结算金额，逻辑与结算清单（SettlementViewModel）完全一致：
     * 实时用债务算法重算转账，再按 (from, to, amount) 匹配已保存的结算记录，
     * 扣掉已结清的转账，剩余之和即待结算金额（而非直接累加已保存的过期金额）。
     */
    suspend fun getPendingStatus(ledgerId: Long): PendingStatus {
        return try {
            val result = calculateSettlement(ledgerId)
            val entities = getSettlements(ledgerId).first()
            val unpaid = result.transactions.sumOf { tx ->
                val match = entities.find {
                    it.fromMemberId == tx.fromMemberId &&
                        it.toMemberId == tx.toMemberId &&
                        kotlin.math.abs(it.amountCny - tx.amountCny) < 0.01
                }
                if (match?.isPaid == true) 0.0 else tx.amountCny
            }
            PendingStatus(result.transactions.isNotEmpty() && unpaid <= 0.0, unpaid)
        } catch (_: Exception) {
            PendingStatus(false, 0.0)
        }
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
