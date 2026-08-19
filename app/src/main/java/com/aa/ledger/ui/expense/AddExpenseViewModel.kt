package com.aa.ledger.ui.expense

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.exifinterface.media.ExifInterface
import com.aa.ledger.data.local.entity.MemberEntity
import com.aa.ledger.data.repository.CurrencyGroup
import com.aa.ledger.data.repository.ExchangeRateRepository
import com.aa.ledger.data.repository.ExpenseRepository
import com.aa.ledger.data.repository.GlmRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import com.aa.ledger.data.repository.LedgerRepository
import com.aa.ledger.data.local.dao.MemberDao
import com.aa.ledger.domain.calculator.SplitCalculator
import com.aa.ledger.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AddExpenseUiState(
    // 表单字段
    val title: String = "",
    val amount: String = "",
    val selectedCurrency: String = "CNY",
    val convertedAmountCny: Double = 0.0,
    val exchangeRate: Double = 1.0,
    val selectedCategory: String = "餐饮",
    val customCategory: String = "",
    val note: String = "",
    val receiptUri: Uri? = null,

    // 付款人与参与人
    val paidByMemberId: Long? = null,
    val paidForAll: Boolean = true,
    val selectedMemberIds: Set<Long> = emptySet(),

    // 分摊
    val shareType: ShareType = ShareType.EQUAL,
    val shareValues: Map<Long, Double> = emptyMap(), // memberId -> share/custom value

    // 数据
    val members: List<MemberEntity> = emptyList(),
    val allLedgers: List<com.aa.ledger.domain.model.Ledger> = emptyList(),
    val selectedLedgerId: Long? = null,
    val availableCurrencies: List<CurrencyGroup> = emptyList(),

    // 状态
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isOcrProcessing: Boolean = false,
    val isAiOcrProcessing: Boolean = false,
    val error: String? = null,
    val savedSuccessfully: Boolean = false,
    val showOcrReview: Boolean = false,
    val ocrReviewAmount: String = "",
    val ocrReviewCurrency: String? = null,
    val ocrReviewUri: Uri? = null,

    // 编辑模式
    val isEditing: Boolean = false,
    val editingExpenseId: Long? = null
)

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val expenseRepository: ExpenseRepository,
    private val ledgerRepository: LedgerRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val memberDao: MemberDao,
    private val glmRepository: GlmRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val ledgerIdArg = savedStateHandle.get<Long>("ledgerId") ?: -1L
    private val expenseIdArg = savedStateHandle.get<Long>("expenseId") ?: -1L

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    val categories = listOf("餐饮", "住宿", "交通", "购物", "娱乐", "医疗", "人情", "教育", "保险", "__custom__")

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        // 直接使用精选 20 种货币（按区域分组），不依赖 IO
        _uiState.update { it.copy(availableCurrencies = ExchangeRateRepository.CURATED_CURRENCIES) }

        // 后台刷新汇率（不影响币种列表）
        viewModelScope.launch {
            exchangeRateRepository.refreshRates()
        }

        // 加载所有账本
        viewModelScope.launch {
            ledgerRepository.getAllLedgers().first().let { ledgers ->
                val targetLedgerId = if (ledgerIdArg > 0) ledgerIdArg
                else if (expenseIdArg > 0) null // 编辑模式从 expense 获取
                else null

                _uiState.update { it.copy(
                    allLedgers = ledgers,
                    selectedLedgerId = targetLedgerId
                ) }

                // 加载选中账本的成员
                targetLedgerId?.let { loadMembers(it) }

                // 编辑模式：加载已有消费记录
                if (expenseIdArg > 0) {
                    loadExpenseForEditing(expenseIdArg)
                }
            }
        }
    }

    private fun loadMembers(ledgerId: Long) {
        viewModelScope.launch {
            val members = memberDao.getMembersByLedgerSync(ledgerId)
            _uiState.update {
                it.copy(
                    members = members,
                    selectedMemberIds = members.map { m -> m.id }.toSet()
                )
            }
        }
    }

    private suspend fun loadExpenseForEditing(expenseId: Long) {
        val expense = expenseRepository.getExpenseById(expenseId) ?: return
        _uiState.update {
            it.copy(
                isEditing = true,
                editingExpenseId = expenseId,
                title = expense.title,
                amount = expense.originalAmount.toString(),
                selectedCurrency = expense.originalCurrency,
                convertedAmountCny = expense.totalAmountCny,
                exchangeRate = expense.exchangeRate,
                selectedCategory = if (expense.category in categories) expense.category else "__custom__",
                customCategory = if (expense.category in categories) "" else expense.category,
                note = expense.note,
                paidByMemberId = expense.paidByMemberId,
                paidForAll = expense.paidForAll,
                selectedMemberIds = expense.splits.map { s -> s.memberId }.toSet(),
                shareType = expense.splits.firstOrNull()?.shareType ?: ShareType.EQUAL,
                shareValues = expense.splits.associate { it.memberId to it.shareValue },
                selectedLedgerId = expense.ledgerId,
                receiptUri = expense.receiptUri?.let { android.net.Uri.parse(it) }
            )
        }
        loadMembers(expense.ledgerId)
    }

    // --- 表单更新方法 ---

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateAmount(amount: String) {
        // Only allow valid numeric input: digits, one decimal point, max 2 decimal places
        val filtered = amount.filter { it.isDigit() || it == '.' }
            .let { if (it.count { c -> c == '.' } > 1) it.substringBeforeLast('.') + "." + it.substringAfterLast('.') else it }
            .let { val dotIdx = it.indexOf('.'); if (dotIdx >= 0 && it.length - dotIdx > 3) it.substring(0, dotIdx + 3) else it }
            .take(12) // max 12 chars (e.g. 999999999.99)
        _uiState.update { it.copy(amount = filtered) }
        recalculateConversion()
    }

    fun updateCurrency(currency: String) {
        _uiState.update { it.copy(selectedCurrency = currency) }
        viewModelScope.launch {
            val rate = exchangeRateRepository.getRate(currency)
            _uiState.update { it.copy(exchangeRate = rate) }
            recalculateConversion()
        }
    }

    private fun recalculateConversion() {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull() ?: 0.0
        val converted = amount * state.exchangeRate
        _uiState.update { it.copy(convertedAmountCny = converted) }
    }

    fun updateCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun updateCustomCategory(category: String) {
        _uiState.update { it.copy(customCategory = category) }
    }

    fun updateNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun updateReceiptUri(uri: Uri?) {
        if (uri == null) {
            _uiState.update { it.copy(receiptUri = null) }
            return
        }
        viewModelScope.launch {
            // 压缩图片（缩放 + 重编码）并保存到持久目录
            val compressed = withContext(Dispatchers.IO) {
                try { compressAndSave(uri) } catch (_: Exception) { null }
            }
            _uiState.update { it.copy(receiptUri = compressed ?: uri) }
            // OCR 用原图识别，更准确
            runOcr(uri)
        }
    }

    /** 压缩图片：解码 → 纠正 EXIF 旋转 → 缩放到最长边 1280px → 重编码 JPEG 并逐步降质控制在 ~200KB → 保存到 filesDir/receipts/ */
    private fun compressAndSave(uri: Uri): Uri {
        val maxDim = 1280

        // 1. 读取原图尺寸
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        appContext.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        // 2. 读取 EXIF 旋转角
        val orientation = appContext.contentResolver.openInputStream(uri)?.use { input ->
            try { ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            catch (_: Exception) { ExifInterface.ORIENTATION_NORMAL }
        } ?: ExifInterface.ORIENTATION_NORMAL
        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        // 3. 按旋转后的有效尺寸计算采样率，避免大图 OOM
        val swapped = rotationDegrees == 90f || rotationDegrees == 270f
        val effW = if (swapped) bounds.outHeight else bounds.outWidth
        val effH = if (swapped) bounds.outWidth else bounds.outHeight
        var sampleSize = 1
        while (effW / sampleSize > maxDim || effH / sampleSize > maxDim) sampleSize *= 2

        // 4. 解码
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val src = appContext.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw Exception("无法解码图片")

        // 5. 纠正旋转
        val rotated = if (rotationDegrees != 0f) {
            val m = Matrix().apply { postRotate(rotationDegrees) }
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true).also { src.recycle() }
        } else src

        // 6. 若仍超限则进一步缩放
        val bitmap = if (rotated.width > maxDim || rotated.height > maxDim) {
            val ratio = minOf(maxDim.toFloat() / rotated.width, maxDim.toFloat() / rotated.height)
            Bitmap.createScaledBitmap(rotated, (rotated.width * ratio).toInt().coerceAtLeast(1), (rotated.height * ratio).toInt().coerceAtLeast(1), true)
                .also { if (it !== rotated) rotated.recycle() }
        } else rotated

        // 7. 保存为 JPEG，逐步降低质量，控制在 ~200KB 左右
        val dir = java.io.File(appContext.filesDir, "receipts").apply { mkdirs() }
        val file = java.io.File(dir, "receipt_${System.currentTimeMillis()}.jpg")
        val targetBytes = 200 * 1024
        var quality = 80
        var out: ByteArray
        do {
            val baos = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            out = baos.toByteArray()
            quality -= 15
        } while (out.size > targetBytes && quality >= 35)
        file.writeBytes(out)
        bitmap.recycle()

        return android.net.Uri.parse(file.toURI().toString())
    }

    private fun runOcr(uri: Uri) {
        _uiState.update { it.copy(isOcrProcessing = true) }
        viewModelScope.launch {
            // 本地 ML Kit OCR 优先（离线可用）
            var amount: String? = null
            var currency: String? = null
            try {
                val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                val image = InputImage.fromFilePath(appContext, uri)
                val result = recognizer.process(image).await()
                val text = result.text
                android.util.Log.d("OCR", "本地原始识别文本: $text")
                amount = extractTotalAmountLayout(result) ?: extractTotalAmount(text)
                currency = extractCurrency(text)
                android.util.Log.d("OCR", "本地识别金额: $amount, 货币: $currency")
            } catch (_: Exception) { }
            // 弹出复核对话框，让用户确认/修改；不满意可点「AI识别」走 GLM 大模型
            _uiState.update { it.copy(
                isOcrProcessing = false,
                showOcrReview = true,
                ocrReviewAmount = amount ?: "",
                ocrReviewCurrency = currency,
                ocrReviewUri = uri
            ) }
        }
    }

    /** 复核弹窗里的「AI识别」：用 GLM 视觉大模型做更精确的结构化识别 */
    fun runAiOcr() {
        val uri = _uiState.value.ocrReviewUri ?: return
        _uiState.update { it.copy(isAiOcrProcessing = true) }
        viewModelScope.launch {
            val glmReceipt = try {
                val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                val mime = appContext.contentResolver.getType(uri) ?: "image/jpeg"
                if (bytes != null) glmRepository.recognizeReceipt(bytes, mime) else null
            } catch (e: Exception) { null }
            if (glmReceipt != null && glmReceipt.total != null) {
                val total = glmReceipt.total
                val amountStr = if (total == total.toLong().toDouble()) total.toLong().toString() else total.toString()
                android.util.Log.d("OCR", "AI识别金额: $amountStr, 货币: ${glmReceipt.currency}")
                _uiState.update { it.copy(
                    isAiOcrProcessing = false,
                    ocrReviewAmount = amountStr,
                    ocrReviewCurrency = glmReceipt.currency
                ) }
            } else {
                _uiState.update { it.copy(isAiOcrProcessing = false) }
            }
        }
    }

    fun confirmOcrAmount(amount: String) {
        val cleaned = amount.trim().replace(Regex("""[¥￥元$€£₩\s]"""), "")
        if (cleaned.toDoubleOrNull() != null && cleaned.toDoubleOrNull()!! > 0) {
            // OCR 识别出了货币则自动切换
            val currency = _uiState.value.ocrReviewCurrency
            if (currency != null && currency != _uiState.value.selectedCurrency &&
                currency in ExchangeRateRepository.CURATED_CURRENCY_CODES) {
                updateCurrency(currency)
            }
            _uiState.update { it.copy(amount = cleaned, showOcrReview = false, ocrReviewAmount = "", ocrReviewCurrency = null, ocrReviewUri = null) }
            recalculateConversion()
        } else {
            _uiState.update { it.copy(error = "请输入有效金额") }
        }
    }

    // Public wrapper for re-running OCR on existing receipt
    fun runOcrOnExisting(uri: Uri) {
        runOcr(uri)
    }

    fun dismissOcrReview() {
        _uiState.update { it.copy(showOcrReview = false, ocrReviewAmount = "", ocrReviewCurrency = null, ocrReviewUri = null) }
    }

    /**
     * 从 OCR 文本中提取最终付款总金额。
     * 策略（按优先级）：
     *   1. 带 ¥/￥ 货币符号的行（支付宝/微信账单金额必带符号，且金额常出现在截图顶部）
     *   2. 带总金额关键词的行（实付/应付/合计/总计…）
     *   3. 带「元」后缀的行
     *   4. 兜底：带小数点的独立金额（排除日期/时间/长单号）
     * 校验：0.01 ~ 99999.99，最多 2 位小数；失败返回 null
     */
    private fun extractTotalAmount(text: String): String? {
        val cleaned = cleanText(text)

        val totalKeywords = listOf("实付", "实收", "应付", "应付金额", "付款金额", "支付金额",
            "合计", "合計", "总计", "总额", "总金额", "应收", "应收金额", "消费金额", "账单金额",
            "TOTAL", "Total", "Amount Due", "Total Due")
        val excludeKeywords = listOf("单价", "小计", "小計", "折扣", "优惠", "税率", "服务费",
            "找零", "数量", "件数", "会员", "积分", "抹零", "抵扣", "代金券", "优惠券",
            "消費税", "外税", "内税", "税額")

        val lines = cleaned.split('\n', '\r').map { it.trim() }.filter { it.isNotBlank() }

        // Step 1：总金额关键词行（最高优先级，小票的合计/实付/Total 等）
        for (line in lines) {
            if (excludeKeywords.any { line.contains(it) }) continue
            if (totalKeywords.any { line.contains(it, ignoreCase = true) }) {
                val n = extractNumber(line)
                if (n != null) return n
            }
        }

        // Step 2：带 ¥/￥/$ 的行，取金额最大的（合计通常是金额最大的，避免取到商品单价）
        var maxValue: Double? = null
        for (line in lines) {
            if (excludeKeywords.any { line.contains(it) }) continue
            if (line.contains('¥') || line.contains('￥') || line.contains('$')) {
                val n = extractNumber(line)
                if (n != null) {
                    val v = n.toDouble()
                    if (maxValue == null || v > maxValue) maxValue = v
                }
            }
        }
        if (maxValue != null) return "%.2f".format(maxValue)

        // Step 3：带「元」后缀的行
        for (line in lines) {
            if (excludeKeywords.any { line.contains(it) }) continue
            if (line.contains('元')) {
                val n = extractNumber(line)
                if (n != null) return n
            }
        }

        // Step 4：兜底——从后往前找带小数的独立金额
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            if (excludeKeywords.any { line.contains(it) }) continue
            val n = extractNumber(line)
            if (n != null) return n
        }

        return null
    }

    /** 清洗 OCR 文本：去不可见字符、统一全角半角、修复常见 OCR 错误 */
    private fun cleanText(text: String): String {
        return text
            .replace('：', ':')       // 全角冒号 → 半角
            .replace('，', ',')
            .replace('￥', '¥')       // 全角人民币符号 → 半角
            .replace(" ", "")          // 去空格
            .replace(Regex("""[Oo]"""), "0")  // 常见 OCR 字母→数字混淆
            .replace(Regex("""[lI]"""), "1")
            .replace(Regex("""[sS]"""), "5")
            .let { normalizeComma(it) }  // 区分千位分隔符与小数逗号
            .replace(Regex("""\.{2,}"""), ".") // 多小数点→单小数点
            .replace(Regex("""[`‘']"""), ".")  // 非标准引号→点
    }

    /** 正确处理逗号：区分千位分隔符（$1,211.00 → $1211.00）和小数逗号（12,50 → 12.50） */
    private fun normalizeComma(s: String): String {
        return s
            // 千位分隔符：逗号后恰好 3 位数字 → 删除逗号
            .replace(Regex("""(\d),(\d{3})(?=\d|\.|$)"""), "$1$2")
            // 小数逗号：逗号后恰好 2 位数字且其后无数字 → 转小数点
            .replace(Regex("""(\d),(\d{2})(?!\d)"""), "$1.$2")
    }

    /** 从一行文本中提取有效金额并校验，返回格式化字符串 */
    private fun extractNumber(line: String): String? {
        // 匹配可能的金额表达式（不再匹配冒号，避免把时间 12:34:56 误当成金额）
        val amountPatterns = listOf(
            Regex("""[¥￥${'$'}]\s*(\d+(?:\.\d{1,2})?)"""),      // ¥123 或 $123.45
            Regex("""(\d+(?:\.\d{1,2})?)\s*元"""),                // 123 或 123.45元
            Regex("""(?<![\d.])(\d{1,6}\.\d{1,2})(?![\d.])""")    // 独立小数金额（排除日期/时间/长单号）
        )

        var rawValue: String? = null
        for (p in amountPatterns) {
            val matches = p.findAll(line).toList()
            // 取最后一个匹配（通常是总金额而非商品单价）
            if (matches.isNotEmpty()) {
                rawValue = matches.last().groupValues[1]
                break
            }
        }

        rawValue ?: return null

        // 清洗数字：去掉非数字字符（保留小数点）
        val digits = rawValue.replace(Regex("""[^\d.]"""), "")

        // 校验
        val value = digits.toDoubleOrNull() ?: return null

        // 合理区间：0.01 ~ 99999.99
        if (value < 0.01 || value > 99999.99) return null

        // 格式化：最多保留 2 位小数
        return "%.2f".format(value)
    }

    /** 布局感知提取金额：利用 bounding box（Y 坐标 + 字号）定位「合计」金额 */
    private fun extractTotalAmountLayout(result: com.google.mlkit.vision.text.Text): String? {
        data class LineInfo(val text: String, val top: Int, val height: Int)

        val lines = mutableListOf<LineInfo>()
        for (block in result.textBlocks) {
            for (line in block.lines) {
                val t = line.text ?: ""
                if (t.isBlank()) continue
                val box = line.boundingBox ?: continue
                lines.add(LineInfo(t, box.top, box.bottom - box.top))
            }
        }
        if (lines.isEmpty()) return null

        val totalKeywords = listOf("合计", "合計", "总计", "总额", "总金额", "实付", "应付",
            "付款金额", "支付金额", "TOTAL", "Total", "Amount Due", "Total Due")
        val excludeKeywords = listOf("单价", "小计", "小計", "折扣", "优惠", "税率", "服务费",
            "找零", "数量", "件数", "会员", "积分", "抹零", "抵扣", "代金券", "优惠券",
            "消費税", "消費稅", "外税", "外稅", "内税", "内稅", "税額", "稅額", "税", "稅", "%")

        data class Cand(val info: LineInfo, val value: Double, val hasKeyword: Boolean)
        val candidates = mutableListOf<Cand>()
        for (li in lines) {
            if (excludeKeywords.any { li.text.contains(it) }) continue
            val v = extractAmountRelaxed(li.text) ?: continue
            candidates.add(Cand(li, v, totalKeywords.any { li.text.contains(it, ignoreCase = true) }))
        }
        if (candidates.isEmpty()) return null

        // 1) 金额与「合计」关键词同行的，取字号最大的
        candidates.filter { it.hasKeyword }
            .maxWithOrNull(compareBy({ it.info.height }, { it.value }))
            ?.let { return "%.2f".format(it.value) }

        // 2) 有「合计」关键词但金额在别的行：取关键词下方、字号最大的
        val keywordTop = lines.filter { li -> totalKeywords.any { li.text.contains(it, ignoreCase = true) } }
            .maxOfOrNull { it.top }
        if (keywordTop != null) {
            candidates.filter { it.info.top >= keywordTop - 20 }
                .maxWithOrNull(compareBy({ it.info.height }, { it.value }))
                ?.let { return "%.2f".format(it.value) }
        }

        // 3) 无关键词：取字号最大的（支付宝/微信的大字金额）
        candidates.maxWithOrNull(compareBy({ it.info.height }, { it.value }))
            ?.let { return "%.2f".format(it.value) }

        return null
    }

    /** 宽松提取金额：先严格（¥/元/小数），再识别千位分隔整数（如 "1,000"，用于无货币符号的合计） */
    private fun extractAmountRelaxed(rawLine: String): Double? {
        extractNumber(cleanText(rawLine))?.toDouble()?.let { return it }
        val normalized = rawLine.replace('O', '0').replace('o', '0')
        val m = Regex("""(\d{1,3})\s*,\s*(\d{3})(?![,\d])""").find(normalized)
        if (m != null) {
            val num = (m.groupValues[1] + m.groupValues[2]).toDoubleOrNull()
            if (num != null && num >= 100.0 && num <= 99999.99) return num
        }
        return null
    }

    /** 从 OCR 文本识别货币代码（USD/JPY/EUR 等），无法识别返回 null */
    private fun extractCurrency(text: String): String? {
        // 0. 强日文语境（假名/円）→ 日元（最优先，避免 ¥ 被 OCR 误读成 $ 而判为美元）
        if (isJapaneseContext(text)) return "JPY"

        // 1. 明确的中文/英文货币名（优先级最高，最可靠）
        val nameMap = listOf(
            "美元" to "USD", "美金" to "USD", "USD" to "USD",
            "欧元" to "EUR", "EUR" to "EUR",
            "英镑" to "GBP", "GBP" to "GBP",
            "港币" to "HKD", "港元" to "HKD", "HKD" to "HKD",
            "澳门元" to "MOP", "澳门币" to "MOP",
            "日元" to "JPY", "日圆" to "JPY", "JPY" to "JPY",
            "韩元" to "KRW", "韩币" to "KRW",
            "新加坡元" to "SGD", "新币" to "SGD",
            "泰铢" to "THB",
            "林吉特" to "MYR",
            "印尼盾" to "IDR", "印尼卢比" to "IDR",
            "印度卢比" to "INR", "卢比" to "INR",
            "越南盾" to "VND",
            "迪拉姆" to "AED",
            "瑞士法郎" to "CHF", "瑞郎" to "CHF",
            "瑞典克朗" to "SEK",
            "卢布" to "RUB",
            "里拉" to "TRY",
            "加元" to "CAD", "加拿大元" to "CAD",
            "澳元" to "AUD", "澳大利亚元" to "AUD",
            "兰特" to "ZAR",
            "新台币" to "TWD", "台币" to "TWD",
            "人民币" to "CNY", "RMB" to "CNY", "CNY" to "CNY",
        )
        for ((name, code) in nameMap) {
            if (text.contains(name, ignoreCase = true)) return code
        }

        // 2. 货币符号（带前缀的 $ 优先，避免 $ 误判为美元）
        val symbolMap = listOf(
            "HK$" to "HKD", "MOP$" to "MOP", "NT$" to "TWD",
            "US$" to "USD", "S$" to "SGD", "A$" to "AUD", "C$" to "CAD",
            "€" to "EUR", "£" to "GBP", "₩" to "KRW", "₺" to "TRY",
            "₽" to "RUB", "₹" to "INR", "₫" to "VND", "฿" to "THB",
            "Rp" to "IDR", "RM" to "MYR",
        )
        for ((symbol, code) in symbolMap) {
            if (text.contains(symbol)) return code
        }

        // 3. 单独的 $（美元）
        if (text.contains('$')) return "USD"

        // 4. ¥ / ￥：默认人民币，日文语境则为日元
        if (text.contains('¥') || text.contains('￥')) {
            return if (isJapaneseContext(text)) "JPY" else "CNY"
        }

        // 5. 元（人民币）
        if (text.contains('元')) return "CNY"

        return null
    }

    /** 判断 OCR 文本是否为日文语境（用于区分 ¥ 是人民币还是日元） */
    private fun isJapaneseContext(text: String): Boolean {
        // 平假名/片假名（U+3040–U+30FF）
        val hasKana = text.any { it.code in 0x3040..0x30FF }
        // 日文「円」字符（区别于中文「元」）
        return hasKana || text.contains('円')
    }

    fun updatePaidByMember(memberId: Long) {
        _uiState.update { it.copy(paidByMemberId = memberId) }
    }

    fun updatePaidForAll(paidForAll: Boolean) {
        _uiState.update { state ->
            val newMemberIds = if (paidForAll) {
                // "付款人也参与消费": include all members
                state.members.map { it.id }.toSet()
            } else {
                // "付款人代付": exclude payer from participants
                state.selectedMemberIds - (state.paidByMemberId ?: -1L)
            }
            state.copy(paidForAll = paidForAll, selectedMemberIds = newMemberIds)
        }
    }

    fun selectLedger(ledgerId: Long) {
        _uiState.update { it.copy(selectedLedgerId = ledgerId) }
        loadMembers(ledgerId)
    }

    fun toggleMember(memberId: Long) {
        _uiState.update { state ->
            val newSet = if (memberId in state.selectedMemberIds) {
                state.selectedMemberIds - memberId
            } else {
                state.selectedMemberIds + memberId
            }
            state.copy(selectedMemberIds = newSet)
        }
    }

    fun selectAllMembers() {
        _uiState.update { state ->
            state.copy(selectedMemberIds = state.members.map { it.id }.toSet())
        }
    }

    fun invertMemberSelection() {
        _uiState.update { state ->
            state.copy(
                selectedMemberIds = state.members.map { it.id }.toSet() - state.selectedMemberIds
            )
        }
    }

    fun updateShareType(type: ShareType) {
        _uiState.update { it.copy(shareType = type, shareValues = emptyMap()) }
    }

    fun updateShareValue(memberId: Long, value: Double) {
        _uiState.update { state ->
            state.copy(shareValues = state.shareValues + (memberId to value))
        }
    }

    // --- 保存 ---

    fun saveExpense() {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "请输入有效金额") }
            return
        }
        if (state.paidByMemberId == null) {
            _uiState.update { it.copy(error = "请选择付款人") }
            return
        }
        if (state.selectedMemberIds.isEmpty()) {
            _uiState.update { it.copy(error = "请选择至少一位参与人") }
            return
        }
        if (state.selectedLedgerId == null) {
            _uiState.update { it.copy(error = "请选择账本") }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            try {
                val memberIds = state.selectedMemberIds.toList()
                val shareValues = when (state.shareType) {
                    ShareType.EQUAL -> memberIds.map { 1.0 }
                    ShareType.SHARES -> memberIds.map { state.shareValues[it] ?: 1.0 }
                    ShareType.CUSTOM -> memberIds.map { state.shareValues[it] ?: 0.0 }
                    ShareType.PERCENTAGE -> memberIds.map { state.shareValues[it] ?: 0.0 }
                }

                val splits = SplitCalculator.calculate(
                    totalAmountCny = state.convertedAmountCny,
                    memberIds = memberIds,
                    shareType = state.shareType,
                    shareValues = shareValues
                )

                // 消费名称未填写时，默认使用消费分类名
                val category = if (state.selectedCategory == "__custom__") state.customCategory else state.selectedCategory
                val expense = Expense(
                    id = if (state.isEditing) state.editingExpenseId ?: 0 else 0,
                    ledgerId = state.selectedLedgerId!!,
                    title = state.title.ifBlank { category },
                    totalAmountCny = state.convertedAmountCny,
                    originalCurrency = state.selectedCurrency,
                    originalAmount = amount,
                    exchangeRate = state.exchangeRate,
                    category = category,
                    paidByMemberId = state.paidByMemberId!!,
                    paidForAll = state.paidForAll,
                    note = state.note,
                    receiptUri = state.receiptUri?.toString(),
                    splits = splits
                )

                expenseRepository.saveExpense(expense)
                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "保存失败：${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
