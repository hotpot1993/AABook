package com.aa.ledger.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.aa.ledger.domain.model.SettlementResult
import com.aa.ledger.domain.model.Transaction
import java.io.File
import java.io.FileOutputStream

object ShareUtil {

    /**
     * Generate a PDF settlement report and return the share intent.
     */
    fun generateAndSharePdf(context: Context, result: SettlementResult, expenses: List<com.aa.ledger.domain.model.Expense> = emptyList()): Intent {
        val file = java.io.File(context.cacheDir, "settlement_report_${System.currentTimeMillis()}.pdf")

        // Generate PDF using Android's built-in PdfDocument API
        val pdf = android.graphics.pdf.PdfDocument()
        val pageWidth = 595  // A4 width in points
        val pageHeight = 842 // A4 height in points

        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f; color = android.graphics.Color.parseColor("#386A20")
            typeface = Typeface.DEFAULT_BOLD
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 16f; color = android.graphics.Color.DKGRAY
            typeface = Typeface.DEFAULT_BOLD
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f; color = android.graphics.Color.DKGRAY
        }
        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f; color = android.graphics.Color.parseColor("#FF6B35")
            typeface = Typeface.DEFAULT_BOLD
        }
        val dividerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E0E0E0"); strokeWidth = 1f
        }

        var y = 50f
        val margin = 40f

        // Title
        canvas.drawText("AA 记账 — 结算报告", margin, y, titlePaint)
        y += 40f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 30f

        // Summary
        canvas.drawText("总消费：${CurrencyFormatter.formatCny(result.totalExpense)}", margin, y, headerPaint)
        y += 30f
        canvas.drawText("转账笔数：${result.transactions.size}", margin, y, textPaint)
        y += 22f
        val unpaidTotal = result.transactions.sumOf { it.amountCny }
        canvas.drawText("需转账总额：${CurrencyFormatter.formatCny(unpaidTotal)}", margin, y, textPaint)
        y += 35f

        // Transfer details
        if (result.transactions.isNotEmpty()) {
            canvas.drawText("转账明细", margin, y, headerPaint)
            y += 28f
            result.transactions.forEachIndexed { i, t ->
                canvas.drawText("${i + 1}. ${t.fromMemberName} → ${t.toMemberName}   ${CurrencyFormatter.formatCny(t.amountCny)}", margin, y, amountPaint)
                y += 22f
            }
            y += 15f
        }

        // Member balances
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 25f
        canvas.drawText("个人明细", margin, y, headerPaint)
        y += 28f
        result.memberBalances.forEach { balance ->
            val status = when {
                balance.netBalance > 0.01 -> "应收 ${CurrencyFormatter.formatCny(balance.netBalance)}"
                balance.netBalance < -0.01 -> "应付 ${CurrencyFormatter.formatCny(-balance.netBalance)}"
                else -> "已结清"
            }
            canvas.drawText("  ${balance.memberName}：$status", margin, y, textPaint)
            y += 22f
        }

        // Expense details (if provided)
        if (expenses.isNotEmpty()) {
            // Check if we need a new page
            if (y > pageHeight - 100) {
                pdf.finishPage(page)
                val page2Info = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
                val page2 = pdf.startPage(page2Info)
                val canvas2 = page2.canvas
                y = 50f
                canvas2.drawText("消费明细", margin, y, headerPaint)
                y += 28f
                expenses.forEach { exp ->
                    canvas2.drawText("${exp.title}  ${CurrencyFormatter.formatCny(exp.totalAmountCny)}  [${exp.category}]", margin, y, textPaint)
                    y += 20f
                }
                pdf.finishPage(page2)
            } else {
                y += 20f
                canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
                y += 25f
                canvas.drawText("消费明细", margin, y, headerPaint)
                y += 28f
                expenses.forEach { exp ->
                    canvas.drawText("${exp.title}  ${CurrencyFormatter.formatCny(exp.totalAmountCny)}  [${exp.category}]", margin, y, textPaint)
                    y += 20f
                }
            }
        }

        pdf.finishPage(page)
        pdf.writeTo(java.io.FileOutputStream(file))
        pdf.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * 生成结算清单文字
     */
    fun generateSettlementText(result: SettlementResult): String {
        val sb = StringBuilder()
        sb.appendLine("📋 AA 结算清单")
        sb.appendLine("────────────────────")
        sb.appendLine("总消费：${CurrencyFormatter.formatCny(result.totalExpense)}")
        sb.appendLine()

        if (result.transactions.isEmpty()) {
            sb.appendLine("✅ 已全部结清，无需转账！")
        } else {
            sb.appendLine("📌 建议转账：")
            sb.appendLine()
            result.transactions.forEachIndexed { index, t ->
                sb.appendLine("${index + 1}. ${t.fromMemberName} → ${t.toMemberName}")
                sb.appendLine("   ${CurrencyFormatter.formatCny(t.amountCny)}")
                sb.appendLine()
            }
        }

        sb.appendLine("────────────────────")
        sb.appendLine("个人明细：")
        result.memberBalances.forEach { balance ->
            val status = when {
                balance.netBalance > 0.01 -> "应收 ${CurrencyFormatter.formatCny(balance.netBalance)}"
                balance.netBalance < -0.01 -> "应付 ${CurrencyFormatter.formatCny(-balance.netBalance)}"
                else -> "已结清"
            }
            sb.appendLine("  ${balance.memberName}：$status")
        }

        return sb.toString()
    }

    /**
     * 复制到剪贴板
     */
    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("settlement", text))
    }

    /**
     * 生成结算清单图片
     */
    fun generateSettlementImage(result: SettlementResult, width: Int = 1080): Bitmap {
        val padding = 60f
        val textSize = 36f
        val titleSize = 48f
        val lineHeight = 52f

        val bgColor = android.graphics.Color.WHITE
        val textColor = android.graphics.Color.DKGRAY
        val titleColor = android.graphics.Color.parseColor("#386A20")
        val accentColor = android.graphics.Color.parseColor("#FF6B35")
        val dividerColor = android.graphics.Color.parseColor("#E0E0E0")

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = titleSize
            this.color = titleColor
            this.typeface = Typeface.DEFAULT_BOLD
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            this.color = textColor
        }

        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            this.color = accentColor
            this.typeface = Typeface.DEFAULT_BOLD
        }

        val dividerPaint = Paint().apply {
            this.color = dividerColor
            this.strokeWidth = 2f
        }

        // 计算高度
        var y = padding
        val itemCount = 4 + result.transactions.size * 3 + result.memberBalances.size + 2
        val height = (padding * 2 + itemCount * lineHeight + titleSize * 2).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bgColor)

        // 标题
        canvas.drawText("📋 AA 结算清单", padding, y + titleSize, titlePaint)
        y += titleSize + lineHeight
        canvas.drawLine(padding, y, width - padding, y, dividerPaint)
        y += lineHeight

        // 总消费
        canvas.drawText("总消费：${CurrencyFormatter.formatCny(result.totalExpense)}", padding, y, textPaint)
        y += lineHeight * 1.5f

        // 转账建议
        if (result.transactions.isEmpty()) {
            canvas.drawText("✅ 已全部结清，无需转账！", padding, y, textPaint)
            y += lineHeight
        } else {
            canvas.drawText("📌 建议转账：", padding, y, textPaint)
            y += lineHeight
            result.transactions.forEach { t ->
                val line = "${t.fromMemberName} → ${t.toMemberName}"
                canvas.drawText(line, padding, y, textPaint)
                y += lineHeight * 0.8f
                canvas.drawText(CurrencyFormatter.formatCny(t.amountCny), padding, y, amountPaint)
                y += lineHeight * 1.2f
            }
        }

        y += lineHeight * 0.5f
        canvas.drawLine(padding, y, width - padding, y, dividerPaint)
        y += lineHeight

        // 个人明细
        canvas.drawText("个人明细：", padding, y, textPaint)
        y += lineHeight
        result.memberBalances.forEach { balance ->
            val status = when {
                balance.netBalance > 0.01 -> "应收 ${CurrencyFormatter.formatCny(balance.netBalance)}"
                balance.netBalance < -0.01 -> "应付 ${CurrencyFormatter.formatCny(-balance.netBalance)}"
                else -> "已结清"
            }
            canvas.drawText("  ${balance.memberName}：$status", padding, y, textPaint)
            y += lineHeight
        }

        return bitmap
    }

    /**
     * 保存图片并返回分享 Intent
     */
    fun shareImage(context: Context, bitmap: Bitmap): Intent {
        val file = File(context.cacheDir, "settlement_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
