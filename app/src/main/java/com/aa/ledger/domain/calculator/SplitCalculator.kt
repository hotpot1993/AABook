package com.aa.ledger.domain.calculator

import com.aa.ledger.domain.model.ExpenseSplit
import com.aa.ledger.domain.model.ShareType
import kotlin.math.abs

/**
 * 分摊计算器
 * 支持四种分摊方式：均分、按份分、自定义金额、按百分比
 */
object SplitCalculator {

    /**
     * 根据分摊方式计算每个参与人应付金额
     *
     * @param totalAmountCny 总金额（人民币）
     * @param memberIds 参与人 ID 列表
     * @param shareType 分摊方式
     * @param shareValues 每人份额（对于按份分=份数，自定义=金额，百分比=百分比值）
     * @return 分摊结果列表
     */
    fun calculate(
        totalAmountCny: Double,
        memberIds: List<Long>,
        shareType: ShareType,
        shareValues: List<Double>
    ): List<ExpenseSplit> {
        val rawSplits = when (shareType) {
            ShareType.EQUAL -> calculateEqual(totalAmountCny, memberIds)
            ShareType.SHARES -> calculateByShares(totalAmountCny, memberIds, shareValues)
            ShareType.CUSTOM -> calculateCustom(totalAmountCny, memberIds, shareValues)
            ShareType.PERCENTAGE -> calculateByPercentage(totalAmountCny, memberIds, shareValues)
        }
        // Round to cents and distribute remainder to last person
        return roundToCents(rawSplits, totalAmountCny)
    }

    /**
     * Round all split amounts to 2 decimal places (cents),
     * then adjust the last split to make the sum exact.
     */
    private fun roundToCents(
        splits: List<ExpenseSplit>,
        targetTotal: Double
    ): List<ExpenseSplit> {
        if (splits.isEmpty()) return splits
        val rounded = splits.map { split ->
            split.copy(amountCny = Math.round(split.amountCny * 100.0) / 100.0)
        }.toMutableList()
        // Fix rounding diff on last element
        val roundedSum = rounded.sumOf { it.amountCny }
        val diff = Math.round((targetTotal - roundedSum) * 100.0) / 100.0
        if (diff != 0.0 && rounded.isNotEmpty()) {
            val last = rounded.last()
            rounded[rounded.lastIndex] = last.copy(amountCny = last.amountCny + diff)
        }
        return rounded
    }

    private fun calculateEqual(
        totalAmountCny: Double,
        memberIds: List<Long>
    ): List<ExpenseSplit> {
        if (memberIds.isEmpty()) return emptyList()
        val eachAmount = totalAmountCny / memberIds.size
        return memberIds.map { memberId ->
            ExpenseSplit(
                memberId = memberId,
                shareType = ShareType.EQUAL,
                shareValue = 1.0,
                amountCny = eachAmount
            )
        }
    }

    private fun calculateByShares(
        totalAmountCny: Double,
        memberIds: List<Long>,
        shareValues: List<Double>
    ): List<ExpenseSplit> {
        val totalShares = shareValues.sum()
        if (totalShares == 0.0) return calculateEqual(totalAmountCny, memberIds)
        return memberIds.zip(shareValues).map { (memberId, shares) ->
            ExpenseSplit(
                memberId = memberId,
                shareType = ShareType.SHARES,
                shareValue = shares,
                amountCny = totalAmountCny * shares / totalShares
            )
        }
    }

    private fun calculateCustom(
        totalAmountCny: Double,
        memberIds: List<Long>,
        customAmounts: List<Double>
    ): List<ExpenseSplit> {
        // 自定义金额需精确等于总金额，有误差时按比例调整
        val sum = customAmounts.sum()
        return memberIds.zip(customAmounts).map { (memberId, amount) ->
            val adjusted = if (sum == 0.0) totalAmountCny / memberIds.size
            else amount * totalAmountCny / sum
            ExpenseSplit(
                memberId = memberId,
                shareType = ShareType.CUSTOM,
                shareValue = amount,
                amountCny = adjusted
            )
        }
    }

    private fun calculateByPercentage(
        totalAmountCny: Double,
        memberIds: List<Long>,
        percentages: List<Double>
    ): List<ExpenseSplit> {
        val totalPercent = percentages.sum()
        return memberIds.zip(percentages).map { (memberId, pct) ->
            ExpenseSplit(
                memberId = memberId,
                shareType = ShareType.PERCENTAGE,
                shareValue = pct,
                amountCny = if (totalPercent == 0.0) totalAmountCny / memberIds.size
                else totalAmountCny * pct / totalPercent
            )
        }
    }

    /**
     * 验证自定义金额是否合理（误差在 1 分以内）
     */
    fun validateCustomAmounts(totalAmountCny: Double, customAmounts: List<Double>): Boolean {
        val sum = customAmounts.sum()
        return abs(sum - totalAmountCny) < 0.02
    }

    /**
     * 验证百分比总和
     */
    fun validatePercentages(percentages: List<Double>): Boolean {
        val sum = percentages.sum()
        return abs(sum - 100.0) < 0.1
    }
}
