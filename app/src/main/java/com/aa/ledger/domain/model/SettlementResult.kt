package com.aa.ledger.domain.model

/**
 * 结算结果：建议的一笔转账
 */
data class Transaction(
    val fromMemberId: Long,
    val fromMemberName: String,
    val toMemberId: Long,
    val toMemberName: String,
    val amountCny: Double
)

/**
 * 某个成员在账本中的净债务情况
 */
data class MemberBalance(
    val memberId: Long,
    val memberName: String,
    val totalPaid: Double,    // 该成员已支付的总额
    val totalOwed: Double,    // 该成员应承担的总额
    val netBalance: Double    // 净额：正数=应收，负数=应付
)

/**
 * 结算结果汇总
 */
data class SettlementResult(
    val ledgerId: Long,
    val memberBalances: List<MemberBalance>,
    val transactions: List<Transaction>,  // 最少转账方案
    val totalExpense: Double,
    val isAllSettled: Boolean              // 是否全部结清
)
