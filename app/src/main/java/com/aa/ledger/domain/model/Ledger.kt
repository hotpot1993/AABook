package com.aa.ledger.domain.model

import com.aa.ledger.data.local.entity.LedgerEntity

data class Ledger(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val coverType: String = "green",
    val baseCurrency: String = "CNY",
    val budgetAmount: Double = 0.0,
    val memberCount: Int = 0,
    val totalExpense: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)

fun LedgerEntity.toLedger(memberCount: Int = 0, totalExpense: Double = 0.0) = Ledger(
    id = id,
    name = name,
    description = description,
    coverType = coverType,
    baseCurrency = baseCurrency,
    budgetAmount = budgetAmount,
    memberCount = memberCount,
    totalExpense = totalExpense,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isArchived = isArchived
)
