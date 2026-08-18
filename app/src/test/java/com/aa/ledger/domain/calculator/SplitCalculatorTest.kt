package com.aa.ledger.domain.calculator

import com.aa.ledger.domain.model.ShareType
import org.junit.Assert.*
import org.junit.Test

class SplitCalculatorTest {

    @Test
    fun `equal split - 100 CNY among 4 people gives 25 each`() {
        val result = SplitCalculator.calculate(
            totalAmountCny = 100.0,
            memberIds = listOf(1, 2, 3, 4),
            shareType = ShareType.EQUAL,
            shareValues = listOf(1.0, 1.0, 1.0, 1.0)
        )
        assertEquals(4, result.size)
        result.forEach { assertEquals(25.0, it.amountCny, 0.01) }
    }

    @Test
    fun `equal split - rounding to cents, remainder goes to last person`() {
        val result = SplitCalculator.calculate(
            totalAmountCny = 100.0,
            memberIds = listOf(1, 2, 3),
            shareType = ShareType.EQUAL,
            shareValues = listOf(1.0, 1.0, 1.0)
        )
        val sum = result.sumOf { it.amountCny }
        assertEquals(100.0, sum, 0.001)
        // First two should be 33.33, last gets 33.34
        assertEquals(33.33, result[0].amountCny, 0.01)
        assertEquals(33.33, result[1].amountCny, 0.01)
        assertEquals(33.34, result[2].amountCny, 0.01)
    }

    @Test
    fun `shares split - weighted by share values`() {
        val result = SplitCalculator.calculate(
            totalAmountCny = 300.0,
            memberIds = listOf(1, 2, 3),
            shareType = ShareType.SHARES,
            shareValues = listOf(1.0, 2.0, 3.0)
        )
        assertEquals(3, result.size)
        assertEquals(50.0, result[0].amountCny, 0.01)   // 1 share
        assertEquals(100.0, result[1].amountCny, 0.01)   // 2 shares
        assertEquals(150.0, result[2].amountCny, 0.01)   // 3 shares
    }

    @Test
    fun `shares split - zero total shares falls back to equal`() {
        val result = SplitCalculator.calculate(
            totalAmountCny = 90.0,
            memberIds = listOf(1, 2, 3),
            shareType = ShareType.SHARES,
            shareValues = listOf(0.0, 0.0, 0.0)
        )
        assertEquals(3, result.size)
        result.forEach { assertEquals(30.0, it.amountCny, 0.01) }
    }

    @Test
    fun `custom split - proportional adjustment`() {
        val result = SplitCalculator.calculate(
            totalAmountCny = 100.0,
            memberIds = listOf(1, 2),
            shareType = ShareType.CUSTOM,
            shareValues = listOf(30.0, 70.0)
        )
        val sum = result.sumOf { it.amountCny }
        assertEquals(100.0, sum, 0.001)
        assertEquals(30.0, result[0].amountCny, 0.01)
        assertEquals(70.0, result[1].amountCny, 0.01)
    }

    @Test
    fun `percentage split - standard 100 percent`() {
        val result = SplitCalculator.calculate(
            totalAmountCny = 200.0,
            memberIds = listOf(1, 2),
            shareType = ShareType.PERCENTAGE,
            shareValues = listOf(60.0, 40.0)
        )
        assertEquals(2, result.size)
        assertEquals(120.0, result[0].amountCny, 0.01)
        assertEquals(80.0, result[1].amountCny, 0.01)
    }

    @Test
    fun `validate custom amounts - within tolerance`() {
        assertTrue(SplitCalculator.validateCustomAmounts(100.0, listOf(50.0, 50.01)))
        assertFalse(SplitCalculator.validateCustomAmounts(100.0, listOf(50.0, 60.0)))
    }

    @Test
    fun `validate percentages - within tolerance`() {
        assertTrue(SplitCalculator.validatePercentages(listOf(50.0, 50.05)))
        assertFalse(SplitCalculator.validatePercentages(listOf(50.0, 30.0)))
    }

    @Test
    fun `empty member list returns empty`() {
        val result = SplitCalculator.calculate(
            totalAmountCny = 100.0,
            memberIds = emptyList(),
            shareType = ShareType.EQUAL,
            shareValues = emptyList()
        )
        assertTrue(result.isEmpty())
    }
}
