package com.aa.ledger.data.backup

import android.content.Context
import android.net.Uri
import com.aa.ledger.data.local.AppDatabase
import com.aa.ledger.data.local.entity.*
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

// ── 备份数据模型 ──────────────────────────────────────────────

/** data.json 的内容：6 张表的完整快照（图片单独放 zip 的 images/） */
data class BackupData(
    val version: String = "1.0",
    val exportedAt: Long = System.currentTimeMillis(),
    val ledgers: List<LedgerEntity> = emptyList(),
    val members: List<MemberEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val expenseSplits: List<ExpenseSplitEntity> = emptyList(),
    val settlements: List<SettlementEntity> = emptyList(),
    val exchangeRates: List<ExchangeRateEntity> = emptyList()
)

/** manifest.json：备份元信息（由服务端在组装下载 zip 时生成） */
data class BackupManifest(
    val version: String = "1.0",
    val exportedAt: Long = 0,
    val checksum: String = "",
    val imageCount: Int = 0,
    val revision: Long = 0
)

/** 单个实体表的差异 */
data class EntityDiff<T>(
    val added: List<T> = emptyList(),
    val updated: List<T> = emptyList(),
    val deletedIds: List<Long> = emptyList()
) {
    val isEmpty: Boolean get() = added.isEmpty() && updated.isEmpty() && deletedIds.isEmpty()
}

/** 汇率表差异（主键是 currencyCode，字符串） */
data class RateDiff(
    val added: List<ExchangeRateEntity> = emptyList(),
    val updated: List<ExchangeRateEntity> = emptyList(),
    val deletedCodes: List<String> = emptyList()
) {
    val isEmpty: Boolean get() = added.isEmpty() && updated.isEmpty() && deletedCodes.isEmpty()
}

/** 图片差异：upsertKeys / deleteKeys 是 zip 内 images/ 下的文件名（含扩展名） */
data class ImageDiff(
    val upsertKeys: List<String> = emptyList(),
    val deleteKeys: List<String> = emptyList()
) {
    val isEmpty: Boolean get() = upsertKeys.isEmpty() && deleteKeys.isEmpty()
}

/** changes.json：增量上传的差异载荷 */
data class BackupChanges(
    val version: String = "1.0",
    val ledgers: EntityDiff<LedgerEntity> = EntityDiff(),
    val members: EntityDiff<MemberEntity> = EntityDiff(),
    val expenses: EntityDiff<ExpenseEntity> = EntityDiff(),
    val expenseSplits: EntityDiff<ExpenseSplitEntity> = EntityDiff(),
    val settlements: EntityDiff<SettlementEntity> = EntityDiff(),
    val exchangeRates: RateDiff = RateDiff(),
    val images: ImageDiff = ImageDiff()
) {
    val hasChanges: Boolean
        get() = !(ledgers.isEmpty && members.isEmpty && expenses.isEmpty &&
                expenseSplits.isEmpty && settlements.isEmpty && exchangeRates.isEmpty && images.isEmpty)
}

/** 本地保存的 baseline：上次成功上传的快照 + 服务端 revision */
data class BackupBaseline(val revision: Long = 0, val data: BackupData = BackupData())

/** 一次上传的产物 */
data class UploadPayload(
    val zipBytes: ByteArray,
    val isFull: Boolean,
    val data: BackupData,
    val baseRevision: Long
)

data class RestoreResult(val ledgerCount: Int, val expenseCount: Int, val imageCount: Int)

// ── 打包 / 解包 / 增量 diff ─────────────────────────────────

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase
) {
    private val gson = Gson()

    /** 构建上传：有差异返回增量 zip，无差异返回 null，首次/无 baseline 或 forceFull 返回全量 zip */
    suspend fun buildUpload(forceFull: Boolean = false): UploadPayload? = withContext(Dispatchers.IO) {
        val data = readAllData()
        val baseline = loadBaseline()

        if (forceFull || baseline == null) {
            return@withContext UploadPayload(buildFullZip(data), isFull = true, data = data, baseRevision = 0)
        }

        val changes = computeChanges(data, baseline.data)
        if (!changes.hasChanges) return@withContext null

        val upserts = collectUpsertImages(data, baseline.data)
        UploadPayload(buildDeltaZip(changes, upserts), isFull = false, data = data, baseRevision = baseline.revision)
    }

    /** 恢复备份：清空 6 表 → 插入 → 还原图片并重写 URI → 更新 baseline */
    suspend fun restoreBackup(zipBytes: ByteArray): RestoreResult = withContext(Dispatchers.IO) {
        val parsed = unzipBackup(zipBytes)
        val data = parsed.data
        val images = parsed.images

        // 校验 checksum（manifest 里的 checksum 是对 data.json 原始字节算的，同一份字节可直接比对）
        val manifest = parsed.manifest
        if (manifest != null && manifest.checksum.isNotBlank()) {
            val actual = sha256(parsed.dataJsonBytes)
            if (actual != manifest.checksum) throw Exception("备份校验失败")
        }

        // 清空（FK 顺序）
        val db = appDatabase.openHelper.writableDatabase
        db.execSQL("DELETE FROM expense_splits")
        db.execSQL("DELETE FROM settlements")
        db.execSQL("DELETE FROM expenses")
        db.execSQL("DELETE FROM members")
        db.execSQL("DELETE FROM ledgers")
        db.execSQL("DELETE FROM exchange_rates")

        val ledgerDao = appDatabase.ledgerDao()
        val memberDao = appDatabase.memberDao()
        val expenseDao = appDatabase.expenseDao()
        val splitDao = appDatabase.expenseSplitDao()
        val settlementDao = appDatabase.settlementDao()
        val rateDao = appDatabase.exchangeRateDao()

        for (l in data.ledgers) ledgerDao.insertLedger(l)

        val avatarDir = File(context.filesDir, "avatars").apply { mkdirs() }
        val members = data.members.map { m ->
            val img = findImage(images, "avatar_${m.id}")
            if (img != null) {
                val f = File(avatarDir, img.first)
                f.writeBytes(img.second)
                m.copy(avatarUri = f.toURI().toString())
            } else m.copy(avatarUri = null)
        }
        for (m in members) memberDao.insertMember(m)

        val receiptDir = File(context.filesDir, "receipts").apply { mkdirs() }
        val expenses = data.expenses.map { e ->
            val img = findImage(images, "receipt_${e.id}")
            if (img != null) {
                val f = File(receiptDir, img.first)
                f.writeBytes(img.second)
                e.copy(receiptUri = f.toURI().toString())
            } else e.copy(receiptUri = null)
        }
        for (e in expenses) expenseDao.insertExpense(e)

        for (s in data.expenseSplits) splitDao.insertSplits(listOf(s))
        for (s in data.settlements) settlementDao.insertSettlements(listOf(s))
        if (data.exchangeRates.isNotEmpty()) rateDao.insertRates(data.exchangeRates)

        // baseline 记录「重写后」的本地 URI，供下次增量对比
        val restored = data.copy(members = members, expenses = expenses)
        writeBaseline(BackupBaseline(revision = manifest?.revision ?: 0, data = restored))

        RestoreResult(data.ledgers.size, data.expenses.size, images.size)
    }

    /** 上传成功后保存 baseline */
    suspend fun saveBaseline(data: BackupData, revision: Long) = withContext(Dispatchers.IO) {
        writeBaseline(BackupBaseline(revision = revision, data = data))
    }

    // ── 数据读取 ──────────────────────────────────────────────

    private suspend fun readAllData(): BackupData {
        return BackupData(
            ledgers = appDatabase.ledgerDao().getAllLedgersSync(),
            members = appDatabase.memberDao().getAllMembersSync(),
            expenses = appDatabase.expenseDao().getAllExpensesSync(),
            expenseSplits = appDatabase.expenseSplitDao().getAllSplitsSync(),
            settlements = appDatabase.settlementDao().getAllSettlementsSync(),
            exchangeRates = appDatabase.exchangeRateDao().getAllRatesSync()
        )
    }

    // ── 差异计算 ──────────────────────────────────────────────

    private fun computeChanges(data: BackupData, baseline: BackupData): BackupChanges {
        return BackupChanges(
            ledgers = diffById(data.ledgers, baseline.ledgers) { it.id },
            members = diffById(data.members, baseline.members) { it.id },
            expenses = diffById(data.expenses, baseline.expenses) { it.id },
            expenseSplits = diffById(data.expenseSplits, baseline.expenseSplits) { it.id },
            settlements = diffById(data.settlements, baseline.settlements) { it.id },
            exchangeRates = diffRates(data.exchangeRates, baseline.exchangeRates),
            images = diffImages(data, baseline)
        )
    }

    private fun <T> diffById(current: List<T>, baseline: List<T>, idOf: (T) -> Long): EntityDiff<T> {
        val cur = current.associateBy(idOf)
        val base = baseline.associateBy(idOf)
        val added = current.filter { idOf(it) !in base }
        val updated = current.filter { idOf(it) in base && base[idOf(it)] != it }
        val deletedIds = baseline.filter { idOf(it) !in cur }.map(idOf)
        return EntityDiff(added, updated, deletedIds)
    }

    private fun diffRates(current: List<ExchangeRateEntity>, baseline: List<ExchangeRateEntity>): RateDiff {
        val cur = current.associateBy { it.currencyCode }
        val base = baseline.associateBy { it.currencyCode }
        val added = current.filter { it.currencyCode !in base }
        val updated = current.filter { it.currencyCode in base && base[it.currencyCode] != it }
        val deletedCodes = baseline.filter { it.currencyCode !in cur }.map { it.currencyCode }
        return RateDiff(added, updated, deletedCodes)
    }

    private fun diffImages(data: BackupData, baseline: BackupData): ImageDiff {
        val upsert = mutableListOf<String>()
        val delete = mutableListOf<String>()

        val curExpenses = data.expenses.associateBy { it.id }
        val baseExpenses = baseline.expenses.associateBy { it.id }
        for (id in (curExpenses.keys + baseExpenses.keys)) {
            val cUri = curExpenses[id]?.receiptUri
            val bUri = baseExpenses[id]?.receiptUri
            if (cUri == bUri) continue
            val cKey = imageKey("receipt", id, cUri)
            val bKey = imageKey("receipt", id, bUri)
            if (bKey != null && bKey != cKey) delete.add(bKey)
            if (cKey != null) upsert.add(cKey)
        }

        val curMembers = data.members.associateBy { it.id }
        val baseMembers = baseline.members.associateBy { it.id }
        for (id in (curMembers.keys + baseMembers.keys)) {
            val cUri = curMembers[id]?.avatarUri
            val bUri = baseMembers[id]?.avatarUri
            if (cUri == bUri) continue
            val cKey = imageKey("avatar", id, cUri)
            val bKey = imageKey("avatar", id, bUri)
            if (bKey != null && bKey != cKey) delete.add(bKey)
            if (cKey != null) upsert.add(cKey)
        }

        return ImageDiff(upsertKeys = upsert.distinct(), deleteKeys = delete.distinct())
    }

    // ── zip 打包 ──────────────────────────────────────────────

    private fun buildFullZip(data: BackupData): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val json = gson.toJson(data)
            zos.putNextEntry(ZipEntry("data.json"))
            zos.write(json.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            for ((key, uriStr) in collectAllImages(data)) {
                val bytes = readImageBytes(uriStr) ?: continue
                zos.putNextEntry(ZipEntry("images/$key"))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    private fun buildDeltaZip(changes: BackupChanges, upserts: List<Pair<String, String>>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val json = gson.toJson(changes)
            zos.putNextEntry(ZipEntry("changes.json"))
            zos.write(json.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            for ((key, uriStr) in upserts) {
                val bytes = readImageBytes(uriStr) ?: continue
                zos.putNextEntry(ZipEntry("images/$key"))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    private fun collectUpsertImages(data: BackupData, baseline: BackupData): List<Pair<String, String>> {
        val diff = diffImages(data, baseline)
        val curExpenses = data.expenses.associateBy { it.id }
        val curMembers = data.members.associateBy { it.id }
        val result = mutableListOf<Pair<String, String>>()
        for (key in diff.upsertKeys) {
            when {
                key.startsWith("receipt_") -> {
                    val id = key.removePrefix("receipt_").substringBefore('.').toLongOrNull()
                    curExpenses[id]?.receiptUri?.let { result.add(key to it) }
                }
                key.startsWith("avatar_") -> {
                    val id = key.removePrefix("avatar_").substringBefore('.').toLongOrNull()
                    curMembers[id]?.avatarUri?.let { result.add(key to it) }
                }
            }
        }
        return result
    }

    private fun collectAllImages(data: BackupData): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        for (e in data.expenses) {
            imageKey("receipt", e.id, e.receiptUri)?.let { result.add(it to e.receiptUri!!) }
        }
        for (m in data.members) {
            imageKey("avatar", m.id, m.avatarUri)?.let { result.add(it to m.avatarUri!!) }
        }
        return result
    }

    // ── zip 解包 ──────────────────────────────────────────────

    private data class ParsedBackup(
        val data: BackupData,
        val dataJsonBytes: ByteArray,
        val manifest: BackupManifest?,
        val images: Map<String, ByteArray>   // key = images/ 下的文件名（含扩展名）
    )

    private fun unzipBackup(zipBytes: ByteArray): ParsedBackup {
        var dataJsonBytes: ByteArray? = null
        var manifestJson: String? = null
        val images = mutableMapOf<String, ByteArray>()

        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                val name = entry.name
                val bytes = zis.readBytes()
                when {
                    name == "data.json" -> dataJsonBytes = bytes
                    name == "manifest.json" -> manifestJson = String(bytes, Charsets.UTF_8)
                    name.startsWith("images/") -> images[name.removePrefix("images/")] = bytes
                }
                zis.closeEntry()
            }
        }

        val json = dataJsonBytes ?: throw Exception("备份缺少 data.json")
        val data = gson.fromJson(String(json, Charsets.UTF_8), BackupData::class.java)
            ?: throw Exception("备份格式不正确")
        val manifest = manifestJson?.let { runCatching { gson.fromJson(it, BackupManifest::class.java) }.getOrNull() }
        return ParsedBackup(data, json, manifest, images)
    }

    private fun findImage(images: Map<String, ByteArray>, baseKey: String): Pair<String, ByteArray>? {
        for ((name, bytes) in images) {
            if (name.startsWith("$baseKey.")) return name to bytes
        }
        return null
    }

    // ── baseline 持久化 ───────────────────────────────────────

    private val baselineFile: File get() = File(context.filesDir, "sync/baseline.json")

    private fun loadBaseline(): BackupBaseline? {
        return try {
            if (!baselineFile.exists()) null
            else gson.fromJson(baselineFile.readText(), BackupBaseline::class.java)
        } catch (_: Exception) { null }
    }

    private fun writeBaseline(baseline: BackupBaseline) {
        baselineFile.parentFile?.mkdirs()
        baselineFile.writeText(gson.toJson(baseline))
    }

    // ── 图片 / 工具 ────────────────────────────────────────────

    private fun readImageBytes(uriStr: String): ByteArray? {
        return try {
            val filename = uriStr.substringAfterLast('/').substringBefore('?')
            val f1 = File(context.filesDir, "receipts/$filename")
            val f2 = File(context.filesDir, "avatars/$filename")
            val f3 = File(context.cacheDir, filename)
            val f4 = File(uriStr.removePrefix("file://").removePrefix("file:"))
            when {
                f1.exists() && f1.length() > 0 -> f1.readBytes()
                f2.exists() && f2.length() > 0 -> f2.readBytes()
                f3.exists() && f3.length() > 0 -> f3.readBytes()
                f4.exists() && f4.length() > 0 -> f4.readBytes()
                else -> context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { it.readBytes() }
            }
        } catch (_: Exception) { null }
    }

    private fun imageKey(prefix: String, id: Long, uriStr: String?): String? =
        uriStr?.takeIf { it.isNotBlank() }?.let { "${prefix}_${id}.${extOf(it)}" }

    private fun extOf(uriStr: String): String {
        val filename = uriStr.substringAfterLast('/').substringBefore('?')
        val dot = filename.lastIndexOf('.')
        return if (dot >= 0 && dot < filename.length - 1) filename.substring(dot + 1).lowercase() else "jpg"
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
