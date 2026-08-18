package com.aa.ledger.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val currencyCode: String,  // USD, JPY, EUR, etc.
    val rateToCny: Double,
    val currencyName: String = "",         // 中文名称
    val updatedAt: Long = System.currentTimeMillis()
)
