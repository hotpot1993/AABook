package com.aa.ledger.domain.calculator

import org.junit.Assert.*
import org.junit.Test

class DebtCalculatorTest {

    @Test
    fun `simple 2-person settlement - one pays all`() {
        val (balances, transactions) = DebtCalculator.calculate(
            memberNames = mapOf(1L to "Alice", 2L to "Bob"),
            paidAmounts = mapOf(1L to 100.0, 2L to 0.0),
            owedAmounts = mapOf(1L to 50.0, 2L to 50.0),
            totalExpense = 100.0
        )
        // Alice paid 100, owes 50 → net +50 (creditor)
        // Bob paid 0, owes 50 → net -50 (debtor)
        assertEquals(1, transactions.size)
        assertEquals("Bob", transactions[0].fromMemberName)
        assertEquals("Alice", transactions[0].toMemberName)
        assertEquals(50.0, transactions[0].amountCny, 0.01)
    }

    @Test
    fun `3-person circular debt - minimal transfers`() {
        // Alice paid 90, owes 30 → net +60
        // Bob paid 0, owes 30 → net -30
        // Charlie paid 0, owes 30 → net -30
        val (balances, transactions) = DebtCalculator.calculate(
            memberNames = mapOf(1L to "Alice", 2L to "Bob", 3L to "Charlie"),
            paidAmounts = mapOf(1L to 90.0, 2L to 0.0, 3L to 0.0),
            owedAmounts = mapOf(1L to 30.0, 2L to 30.0, 3L to 30.0),
            totalExpense = 90.0
        )
        // 2 transfers: Bob→Alice (30), Charlie→Alice (30)
        assertEquals(2, transactions.size)
        val totalTransferred = transactions.sumOf { it.amountCny }
        assertEquals(60.0, totalTransferred, 0.01)
    }

    @Test
    fun `everyone pays exactly what they owe - no transfers needed`() {
        val (balances, transactions) = DebtCalculator.calculate(
            memberNames = mapOf(1L to "Alice", 2L to "Bob"),
            paidAmounts = mapOf(1L to 50.0, 2L to 50.0),
            owedAmounts = mapOf(1L to 50.0, 2L to 50.0),
            totalExpense = 100.0
        )
        assertTrue(transactions.isEmpty())
    }

    @Test
    fun `balances are rounded to 2 decimal places`() {
        val (balances, _) = DebtCalculator.calculate(
            memberNames = mapOf(1L to "Alice"),
            paidAmounts = mapOf(1L to 100.0 / 3.0),
            owedAmounts = mapOf(1L to 100.0 / 3.0),
            totalExpense = 100.0
        )
        // netBalance should be ~0, rounded
        assertEquals(1, balances.size)
        assertEquals(0.0, balances[0].netBalance, 0.01)
    }

    @Test
    fun `small balances below threshold are ignored`() {
        val (_, transactions) = DebtCalculator.calculate(
            memberNames = mapOf(1L to "Alice", 2L to "Bob"),
            paidAmounts = mapOf(1L to 100.0, 2L to 99.99),
            owedAmounts = mapOf(1L to 100.0, 2L to 99.99),
            totalExpense = 199.99
        )
        // Net differences < 0.01 should produce no transactions
        assertTrue(transactions.isEmpty() || transactions.sumOf { it.amountCny } < 0.02)
    }
}
