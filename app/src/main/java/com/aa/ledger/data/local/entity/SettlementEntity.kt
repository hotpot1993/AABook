package com.aa.ledger.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "settlements",
    foreignKeys = [
        ForeignKey(
            entity = LedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledgerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ledgerId")]
)
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ledgerId: Long,
    val fromMemberId: Long,
    val toMemberId: Long,
    val amountCny: Double,
    val isPaid: Boolean = false,           // 是否已还款
    val paidAt: Long? = null,
    val settledAt: Long = System.currentTimeMillis()
)
