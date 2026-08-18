package com.aa.ledger.domain.calculator

import com.aa.ledger.domain.model.MemberBalance
import com.aa.ledger.domain.model.Transaction
import kotlin.math.abs

/**
 * 债务合并计算器
 *
 * 核心算法：将多人之间复杂的债务关系简化为最少转账次数。
 *
 * 步骤：
 * 1. 汇集每笔消费的分摊结果，计算每个人支付了多少、应承担多少
 * 2. 计算净额：净额 = 已付 - 应承担
 *    - 正数 = 多付了，应收回钱（债权人）
 *    - 负数 = 少付了，应付出钱（债务人）
 * 3. 贪心匹配法：最大债权人 与 最大债务人 配对
 * 4. 重复直到所有债务清零
 *
 * 复杂度：O(n² log n)，n 为参与人数
 */
object DebtCalculator {

    /**
     * 计算结算方案
     *
     * @param memberNames Map<memberId, memberName> 成员名称映射
     * @param paidAmounts Map<memberId, paidAmount> 每个成员支付的总额
     * @param owedAmounts Map<memberId, owedAmount> 每个成员应承担的总额
     * @param totalExpense 账本总支出
     * @return SettlementResult 包含余额和最少转账方案
     */
    fun calculate(
        memberNames: Map<Long, String>,
        paidAmounts: Map<Long, Double>,
        owedAmounts: Map<Long, Double>,
        totalExpense: Double
    ): Pair<List<MemberBalance>, List<Transaction>> {
        // 收集所有成员 ID
        val allMemberIds = (memberNames.keys + paidAmounts.keys + owedAmounts.keys).toSet()

        // 计算每个成员的净额
        val netBalances = mutableListOf<MemberBalance>()
        for (memberId in allMemberIds) {
            val paid = paidAmounts[memberId] ?: 0.0
            val owed = owedAmounts[memberId] ?: 0.0
            netBalances.add(
                MemberBalance(
                    memberId = memberId,
                    memberName = memberNames[memberId] ?: "未知",
                    totalPaid = paid,
                    totalOwed = owed,
                    netBalance = paid - owed  // 正=应收，负=应付
                )
            )
        }

        // 分离债权人和债务人
        // 使用 Double，所以用阈值判断
        val creditors = netBalances
            .filter { it.netBalance > 0.01 }
            .sortedByDescending { it.netBalance }
            .toMutableList()
        val debtors = netBalances
            .filter { it.netBalance < -0.01 }
            .sortedBy { it.netBalance } // 最负的排最前
            .toMutableList()

        // 贪心匹配
        val transactions = mutableListOf<Transaction>()
        var ci = 0
        var di = 0

        while (ci < creditors.size && di < debtors.size) {
            val creditor = creditors[ci]
            val debtor = debtors[di]
            val creditAmount = creditor.netBalance
            val debitAmount = abs(debtor.netBalance)

            val transferAmount = minOf(creditAmount, debitAmount)

            if (transferAmount >= 0.01) {
                transactions.add(
                    Transaction(
                        fromMemberId = debtor.memberId,
                        fromMemberName = debtor.memberName,
                        toMemberId = creditor.memberId,
                        toMemberName = creditor.memberName,
                        amountCny = transferAmount
                    )
                )
            }

            // 更新余额
            creditors[ci] = creditor.copy(netBalance = creditAmount - transferAmount)
            debtors[di] = debtor.copy(netBalance = -(debitAmount - transferAmount))

            if (abs(creditors[ci].netBalance) < 0.01) ci++
            if (abs(debtors[di].netBalance) < 0.01) di++
        }

        // 修正显示用的余额（取反，正面含义）
        val balances = netBalances.map { b ->
            b.copy(
                totalPaid = kotlin.math.round(b.totalPaid * 100) / 100,
                totalOwed = kotlin.math.round(b.totalOwed * 100) / 100,
                netBalance = kotlin.math.round(b.netBalance * 100) / 100
            )
        }.sortedByDescending { it.netBalance }

        return Pair(balances, transactions)
    }
}
