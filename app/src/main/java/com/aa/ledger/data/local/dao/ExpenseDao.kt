package com.aa.ledger.data.local.dao

import androidx.room.*
import com.aa.ledger.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT COUNT(*) FROM expenses")
    fun getAllExpensesFlow(): Flow<Int>

    @Query("SELECT * FROM expenses WHERE ledgerId = :ledgerId ORDER BY createdAt DESC")
    fun getExpensesByLedger(ledgerId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE ledgerId = :ledgerId ORDER BY createdAt DESC")
    suspend fun getExpensesByLedgerSync(ledgerId: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpensesSync(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE ledgerId = :ledgerId AND createdAt BETWEEN :startTime AND :endTime ORDER BY createdAt DESC")
    suspend fun getExpensesByDateRange(ledgerId: Long, startTime: Long, endTime: Long): List<ExpenseEntity>

    @Query("""
        SELECT SUM(totalAmountCny) FROM expenses
        WHERE ledgerId = :ledgerId AND paidByMemberId = :memberId
    """)
    suspend fun getTotalPaidByMember(ledgerId: Long, memberId: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE ledgerId = :ledgerId")
    suspend fun deleteAllByLedger(ledgerId: Long)
}
