package com.aa.ledger.data.local.dao

import androidx.room.*
import com.aa.ledger.data.local.entity.SettlementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementDao {
    @Query("SELECT COUNT(*) FROM settlements")
    fun getAllSettlementsFlow(): Flow<Int>

    @Query("SELECT * FROM settlements WHERE ledgerId = :ledgerId ORDER BY settledAt DESC")
    fun getSettlementsByLedger(ledgerId: Long): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE ledgerId = :ledgerId ORDER BY settledAt DESC")
    suspend fun getSettlementsByLedgerSync(ledgerId: Long): List<SettlementEntity>

    @Query("SELECT * FROM settlements WHERE ledgerId = :ledgerId AND isPaid = 0 ORDER BY settledAt DESC")
    suspend fun getUnpaidSettlements(ledgerId: Long): List<SettlementEntity>

    @Query("SELECT * FROM settlements")
    suspend fun getAllSettlementsSync(): List<SettlementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlements(settlements: List<SettlementEntity>)

    @Update
    suspend fun updateSettlement(settlement: SettlementEntity)

    @Query("UPDATE settlements SET isPaid = 1, paidAt = :paidAt WHERE id = :id")
    suspend fun markAsPaid(id: Long, paidAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM settlements WHERE ledgerId = :ledgerId")
    suspend fun deleteByLedger(ledgerId: Long)
}
