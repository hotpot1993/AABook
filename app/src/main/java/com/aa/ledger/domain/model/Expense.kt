package com.aa.ledger.domain.model

import com.aa.ledger.data.local.entity.ExpenseEntity
import com.aa.ledger.data.local.entity.ExpenseSplitEntity

data class Expense(
    val id: Long = 0,
    val ledgerId: Long,
    val title: String,
    val totalAmountCny: Double,
    val originalCurrency: String = "CNY",
    val originalAmount: Double,
    val exchangeRate: Double = 1.0,
    val category: String = "其他",
    val paidByMemberId: Long,
    val paidForAll: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val receiptUri: String? = null,
    val splits: List<ExpenseSplit> = emptyList()
)

data class ExpenseSplit(
    val id: Long = 0,
    val expenseId: Long = 0,
    val memberId: Long,
    val shareType: ShareType,
    val shareValue: Double = 1.0,
    val amountCny: Double
)

enum class ShareType(val label: String) {
    EQUAL("均分"),
    SHARES("按份分"),
    CUSTOM("自定义"),
    PERCENTAGE("按百分比")
}

fun Expense.toEntity() = ExpenseEntity(
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
    receiptUri = receiptUri
)

fun ExpenseSplit.toEntity() = ExpenseSplitEntity(
    id = id,
    expenseId = expenseId,
    memberId = memberId,
    shareType = shareType.name,
    shareValue = shareValue,
    amountCny = amountCny
)

fun ExpenseSplitEntity.toSplit() = ExpenseSplit(
    id = id,
    expenseId = expenseId,
    memberId = memberId,
    shareType = ShareType.valueOf(shareType),
    shareValue = shareValue,
    amountCny = amountCny
)
