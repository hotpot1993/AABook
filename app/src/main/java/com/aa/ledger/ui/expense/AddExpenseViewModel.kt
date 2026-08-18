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
    val error: String? = null,
    val savedSuccessfully: Boolean = false,
    val showOcrReview: Boolean = false,
    val ocrReviewAmount: String = "",
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
            try {
                val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                val image = InputImage.fromFilePath(appContext, uri)
                val result = recognizer.process(image).await()
                val text = result.text
                val amount = extractTotalAmount(text)
                // 弹出 OCR 复核对话框，让用户确认/修改金额
                _uiState.update { it.copy(
                    isOcrProcessing = false,
                    showOcrReview = true,
                    ocrReviewAmount = amount ?: "",
                    ocrReviewUri = uri
                ) }
            } catch (_: Exception) {
                _uiState.update { it.copy(
                    isOcrProcessing = false,
                    showOcrReview = true,
                    ocrReviewAmount = "",
                    ocrReviewUri = uri
                ) }
            }
        }
    }

    fun confirmOcrAmount(amount: String) {
        val cleaned = amount.trim().replace(Regex("""[¥元\s]"""), "")
        if (cleaned.toDoubleOrNull() != null && cleaned.toDoubleOrNull()!! > 0) {
            _uiState.update { it.copy(amount = cleaned, showOcrReview = false, ocrReviewAmount = "", ocrReviewUri = null) }
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
        _uiState.update { it.copy(showOcrReview = false, ocrReviewAmount = "", ocrReviewUri = null) }
    }

    /**
     * 从 OCR 文本中提取最终付款总金额。
     * 策略：
     *   1. 优先匹配「实付/实收/应付/合计/总计/付款金额」后面的金额
     *   2. 排除 单价/小计/折扣/优惠/税率/服务费/找零 等干扰行
     *   3. 清洗数字：去符号、统一小数点
     *   4. 校验：0.01 ~ 99999.99，最多 2 位小数
     *   5. 返回格式化字符串，失败返回 null
     */
    private fun extractTotalAmount(text: String): String? {
        val cleaned = cleanText(text)

        // ── Step 1：按行拆分，找包含总金额关键词的行 ──
        val totalKeywords = listOf("实付", "实收", "应付", "应付金额", "付款金额", "支付金额",
            "合计", "总计", "总额", "总金额", "应收", "应收金额", "消费金额", "账单金额")
        val excludeKeywords = listOf("单价", "小计", "折扣", "优惠", "税率", "服务费",
            "找零", "数量", "件数", "会员", "积分", "抹零", "抵扣", "代金券", "优惠券")

        val lines = cleaned.split('\n', '\r').map { it.trim() }.filter { it.isNotBlank() }

        // 从后往前扫描（总金额通常在末尾）
        var candidateLine: String? = null
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            // 排除干扰行
            if (excludeKeywords.any { line.contains(it) }) continue
            // 匹配目标行
            if (totalKeywords.any { line.contains(it) }) {
                candidateLine = line
                break
            }
        }

        // Step 2：如果没有关键词行，尝试从倒数几行中提取
        if (candidateLine == null) {
            for (i in lines.indices.reversed().take(5)) {
                val line = lines[i]
                if (excludeKeywords.any { line.contains(it) }) continue
                val n = extractNumber(line)
                if (n != null) { candidateLine = line; break }
            }
        }

        // Step 3：如果还是没有，扫描全部行
        if (candidateLine == null) {
            for (line in lines) {
                if (excludeKeywords.any { line.contains(it) }) continue
                val n = extractNumber(line)
                if (n != null) { candidateLine = line; break }
            }
        }

        candidateLine ?: return null
        return extractNumber(candidateLine)
    }

    /** 清洗 OCR 文本：去不可见字符、统一全角半角、修复常见 OCR 错误 */
    private fun cleanText(text: String): String {
        return text
            .replace('：', ':')       // 全角冒号 → 半角
            .replace('，', ',')
            .replace('¥', '¥')
            .replace(" ", "")          // 去空格
            .replace(Regex("""[Oo]"""), "0")  // 常见 OCR 字母→数字混淆
            .replace(Regex("""[lI]"""), "1")
            .replace(Regex("""[sS]"""), "5")
            .replace(",", ".")          // 逗号→小数点 (如 12,50 → 12.50)
            .replace(Regex("""\.{2,}"""), ".") // 多小数点→单小数点
            .replace(Regex("""[`‘']"""), ".")  // 非标准引号→点
    }

    /** 从一行文本中提取有效金额并校验，返回格式化字符串 */
    private fun extractNumber(line: String): String? {
        // 匹配所有可能的金额表达式
        val amountPatterns = listOf(
            Regex("""¥\s*(\d+\.?\d{0,2})"""),        // ¥123.45
            Regex("""(\d+\.\d{1,2})\s*元"""),         // 123.45元
            Regex(""":\s*(\d+\.?\d{0,2})"""),         // :123.45
            Regex("""(\d+\.\d{1,2})""")               // 123.45 末尾匹配
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

        // 处理无小数点的情况：如 "12340" 可能是 "123.40"
        val valueStr = if (!digits.contains('.') && digits.length > 2) {
            // 尝试理解：如果是整数，不做假设
            digits
        } else digits

        // 校验
        val value = valueStr.toDoubleOrNull() ?: return null

        // 合理区间：0.01 ~ 99999.99
        if (value < 0.01 || value > 99999.99) return null

        // 格式化：最多保留 2 位小数
        val formatted = "%.2f".format(value)
        return formatted
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
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "请输入消费名称") }
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

                val expense = Expense(
                    id = if (state.isEditing) state.editingExpenseId ?: 0 else 0,
                    ledgerId = state.selectedLedgerId!!,
                    title = state.title,
                    totalAmountCny = state.convertedAmountCny,
                    originalCurrency = state.selectedCurrency,
                    originalAmount = amount,
                    exchangeRate = state.exchangeRate,
                    category = if (state.selectedCategory == "__custom__") state.customCategory else state.selectedCategory,
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
