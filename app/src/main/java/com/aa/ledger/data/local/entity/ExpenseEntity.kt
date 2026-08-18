package com.aa.ledger.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = LedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledgerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["paidByMemberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ledgerId"), Index("paidByMemberId")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ledgerId: Long,
    val title: String,
    val totalAmountCny: Double,          // 换算为人民币后的金额
    val originalCurrency: String = "CNY", // 原始币种
    val originalAmount: Double,           // 原始金额
    val exchangeRate: Double = 1.0,       // 使用汇率
    val category: String = "其他",         // 餐饮、交通、住宿、购物、娱乐、其他
    val paidByMemberId: Long,             // 付款人 ID
    val paidForAll: Boolean = true,       // 付款人是否也参与消费？
    val createdAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val receiptUri: String? = null        // 小票照片 URI
)
