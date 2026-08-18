package com.aa.ledger.data.local.dao

import androidx.room.*
import com.aa.ledger.data.local.entity.LedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledgers WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getAllLedgers(): Flow<List<LedgerEntity>>

    @Query("SELECT * FROM ledgers")
    suspend fun getAllLedgersSync(): List<LedgerEntity>

    @Query("SELECT * FROM ledgers WHERE id = :id")
    suspend fun getLedgerById(id: Long): LedgerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedger(ledger: LedgerEntity): Long

    @Update
    suspend fun updateLedger(ledger: LedgerEntity)

    @Query("UPDATE ledgers SET isArchived = 1 WHERE id = :id")
    suspend fun archiveLedger(id: Long)

    @Query("UPDATE ledgers SET isArchived = 0 WHERE id = :id")
    suspend fun restoreLedger(id: Long)

    @Delete
    suspend fun deleteLedger(ledger: LedgerEntity)
}
