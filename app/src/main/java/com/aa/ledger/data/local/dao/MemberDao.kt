package com.aa.ledger.data.local.dao

import androidx.room.*
import com.aa.ledger.data.local.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT COUNT(*) FROM members")
    fun getAllMembersFlow(): Flow<Int>

    @Query("SELECT * FROM members WHERE ledgerId = :ledgerId ORDER BY joinedAt ASC")
    fun getMembersByLedger(ledgerId: Long): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE ledgerId = :ledgerId ORDER BY joinedAt ASC")
    suspend fun getMembersByLedgerSync(ledgerId: Long): List<MemberEntity>

    @Query("SELECT * FROM members")
    suspend fun getAllMembersSync(): List<MemberEntity>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberById(id: Long): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity): Long

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Delete
    suspend fun deleteMember(member: MemberEntity)

    @Query("SELECT COUNT(*) FROM members WHERE ledgerId = :ledgerId")
    suspend fun getMemberCount(ledgerId: Long): Int
}
