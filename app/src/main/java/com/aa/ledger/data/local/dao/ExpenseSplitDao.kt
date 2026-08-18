package com.aa.ledger.data.local.dao

import androidx.room.*
import com.aa.ledger.data.local.entity.ExpenseSplitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseSplitDao {
    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    fun getSplitsByExpense(expenseId: Long): Flow<List<ExpenseSplitEntity>>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitsByExpenseSync(expenseId: Long): List<ExpenseSplitEntity>

    @Query("""
        SELECT * FROM expense_splits
        WHERE expenseId IN (SELECT id FROM expenses WHERE ledgerId = :ledgerId)
    """)
    suspend fun getSplitsByLedger(ledgerId: Long): List<ExpenseSplitEntity>

    @Query("SELECT * FROM expense_splits")
    suspend fun getAllSplitsSync(): List<ExpenseSplitEntity>

    @Query("""
        SELECT COALESCE(SUM(amountCny), 0) FROM expense_splits
        WHERE memberId = :memberId
        AND expenseId IN (SELECT id FROM expenses WHERE ledgerId = :ledgerId)
    """)
    suspend fun getTotalOwedByMember(ledgerId: Long, memberId: Long): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<ExpenseSplitEntity>)

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsByExpense(expenseId: Long)
}
