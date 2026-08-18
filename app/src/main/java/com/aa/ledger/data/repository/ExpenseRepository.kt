package com.aa.ledger.data.repository

import com.aa.ledger.data.local.dao.*
import com.aa.ledger.data.local.entity.ExpenseEntity
import com.aa.ledger.data.local.entity.ExpenseSplitEntity
import com.aa.ledger.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val expenseSplitDao: ExpenseSplitDao,
    private val memberDao: MemberDao
) {
    fun getExpensesByLedger(ledgerId: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesByLedger(ledgerId).map { entities ->
            entities.map { entity ->
                val splits = expenseSplitDao.getSplitsByExpenseSync(entity.id)
                entity.toExpense(splits)
            }
        }
    }

    suspend fun getExpenseById(id: Long): Expense? {
        val entity = expenseDao.getExpenseById(id) ?: return null
        val splits = expenseSplitDao.getSplitsByExpenseSync(id)
        return entity.toExpense(splits)
    }

    suspend fun saveExpense(expense: Expense): Long {
        val entity = expense.toEntity()
        val expenseId = if (expense.id == 0L) {
            expenseDao.insertExpense(entity)
        } else {
            expenseDao.updateExpense(entity)
            expenseSplitDao.deleteSplitsByExpense(expense.id)
            expense.id
        }

        // 保存分摊明细
        val splitEntities = expense.splits.map { it.copy(expenseId = expenseId).toEntity() }
        expenseSplitDao.insertSplits(splitEntities)

        return expenseId
    }

    suspend fun deleteExpense(expenseId: Long) {
        val entity = expenseDao.getExpenseById(expenseId) ?: return
        expenseDao.deleteExpense(entity)
    }

    /**
     * 获取账本中按分类汇总的消费金额
     */
    suspend fun getCategorySummary(ledgerId: Long): Map<String, Double> {
        val expenses = expenseDao.getExpensesByLedgerSync(ledgerId)
        return expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.totalAmountCny } }
    }

    /**
     * 获取指定时间范围内的消费记录
     */
    suspend fun getExpensesByDateRange(
        ledgerId: Long,
        startTime: Long,
        endTime: Long
    ): List<Expense> {
        val entities = expenseDao.getExpensesByDateRange(ledgerId, startTime, endTime)
        return entities.map { entity ->
            val splits = expenseSplitDao.getSplitsByExpenseSync(entity.id)
            entity.toExpense(splits)
        }
    }

    /**
     * 获取每个成员已支付的总额
     */
    suspend fun getMembersPaidAmounts(ledgerId: Long): Map<Long, Double> {
        val expenses = expenseDao.getExpensesByLedgerSync(ledgerId)
        val result = mutableMapOf<Long, Double>()
        for (expense in expenses) {
            result[expense.paidByMemberId] =
                (result[expense.paidByMemberId] ?: 0.0) + expense.totalAmountCny
        }
        return result
    }

    /**
     * 获取每个成员应承担的总额
     */
    suspend fun getMembersOwedAmounts(ledgerId: Long): Map<Long, Double> {
        val splits = expenseSplitDao.getSplitsByLedger(ledgerId)
        val result = mutableMapOf<Long, Double>()
        for (split in splits) {
            result[split.memberId] =
                (result[split.memberId] ?: 0.0) + split.amountCny
        }
        return result
    }

    private fun ExpenseEntity.toExpense(splits: List<ExpenseSplitEntity>): Expense {
        return Expense(
            id = id,
            ledgerId = ledgerId,
            title = title,
            totalAmountCny = totalAmountCny,
            originalCurrency = originalCurrency,
            originalAmount = originalAmount,
            exchangeRate = exchangeRate,
            category = category,
            paidByMemberId = paidByMemberId,
            paidForAll = paidForAll,
            createdAt = createdAt,
            note = note,
            receiptUri = receiptUri,
            splits = splits.map { it.toSplit() }
        )
    }
}
