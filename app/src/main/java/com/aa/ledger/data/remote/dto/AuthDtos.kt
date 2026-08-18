package com.aa.ledger.data.remote.dto

data class LoginRequest(val nickname: String, val password: String)

data class LoginResponse(val token: String, val user: UserInfo)

data class UserInfo(val id: Int, val nickname: String)

data class LedgerListResponse(val ledgers: List<CloudLedger>)

data class CloudLedger(
    val id: Int, val name: String, val description: String = "",
    val coverType: String = "green", val baseCurrency: String = "CNY",
    val budgetAmount: Double = 0.0, val ownerId: Int,
    val inviteCode: String, val memberCount: Int,
    val members: List<CloudMember>, val expenseCount: Int,
    val totalExpense: Double = 0.0, val createdAt: String, val updatedAt: String
)

data class CloudMember(val userId: Int, val nickname: String)

data class LedgerDetailResponse(val ledger: CloudLedgerDetail)

data class CloudLedgerDetail(
    val id: Int, val name: String, val inviteCode: String = "",
    val members: List<CloudMember>, val expenses: List<CloudExpense>,
    val settlements: List<CloudSettlement>
)

data class CloudExpense(
    val id: Int, val title: String, val totalAmountCny: Double,
    val originalCurrency: String = "CNY", val originalAmount: Double = 0.0,
    val exchangeRate: Double = 1.0, val category: String = "其他",
    val paidByMemberId: Int, val note: String = "", val createdAt: String,
    val splits: List<CloudSplit>
)

data class CloudSplit(
    val id: Int, val memberId: Int, val shareType: String,
    val shareValue: Double, val amountCny: Double
)

data class CloudSettlement(
    val id: Int, val fromMemberId: Int, val toMemberId: Int,
    val amountCny: Double, val isPaid: Boolean = false
)

data class CreateLedgerRequest(
    val name: String, val description: String = "", val coverType: String = "green",
    val baseCurrency: String = "CNY", val budgetAmount: Double = 0.0
)

data class JoinLedgerRequest(val inviteCode: String)

data class JoinLedgerResponse(val ledgerId: Int, val ledgerName: String, val message: String)

data class ApiError(val error: String)

data class CreateLedgerResponse(val ledger: CloudLedgerDetail)

data class CreateLedgerMapResponse(val id: Int, val name: String, val inviteCode: String)
