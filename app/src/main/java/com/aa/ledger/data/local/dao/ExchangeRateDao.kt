package com.aa.ledger.data.local.dao

import androidx.room.*
import com.aa.ledger.data.local.entity.ExchangeRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates")
    fun getAllRates(): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchange_rates")
    suspend fun getAllRatesSync(): List<ExchangeRateEntity>

    @Query("SELECT * FROM exchange_rates WHERE currencyCode = :code")
    suspend fun getRateByCode(code: String): ExchangeRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<ExchangeRateEntity>)

    @Query("DELETE FROM exchange_rates")
    suspend fun clearAllRates()

    @Query("SELECT MAX(updatedAt) FROM exchange_rates")
    suspend fun getLatestUpdateTime(): Long?
}
