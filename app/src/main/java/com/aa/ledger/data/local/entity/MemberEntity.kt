package com.aa.ledger.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "members",
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
data class MemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ledgerId: Long,
    val name: String,
    val nickname: String = "",
    val avatarUri: String? = null,
    val joinedAt: Long = System.currentTimeMillis()
)
