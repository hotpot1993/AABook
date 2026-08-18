package com.aa.ledger.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    private val cnyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)

    /**
     * 格式化为人民币显示
     */
    fun formatCny(amount: Double): String {
        return "¥${"%.2f".format(amount)}"
    }

    /**
     * 格式化为指定货币显示
     */
    fun format(amount: Double, currencyCode: String): String {
        return when (currencyCode) {
            "CNY" -> "¥${"%.2f".format(amount)}"
            "USD" -> "$${"%.2f".format(amount)}"
            "EUR" -> "€${"%.2f".format(amount)}"
            "JPY" -> "¥${"%.0f".format(amount)}"
            "GBP" -> "£${"%.2f".format(amount)}"
            "KRW" -> "₩${"%.0f".format(amount)}"
            else -> "$currencyCode ${"%.2f".format(amount)}"
        }
    }

    fun currencySymbol(currencyCode: String): String {
        return when (currencyCode) {
            "CNY" -> "¥"
            "USD" -> "$"
            "EUR" -> "€"
            "JPY" -> "¥"
            "GBP" -> "£"
            "KRW" -> "₩"
            else -> currencyCode
        }
    }
}
