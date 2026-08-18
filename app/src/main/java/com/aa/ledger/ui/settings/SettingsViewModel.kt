package com.aa.ledger.ui.settings

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aa.ledger.BuildConfig
import com.aa.ledger.data.backup.BackupManager
import com.aa.ledger.data.remote.CloudSyncManager
import com.aa.ledger.data.repository.AuthRepository
import com.aa.ledger.data.repository.BackupConflictException
import com.aa.ledger.data.repository.ExchangeRateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isUploading: Boolean = false,
    val isDownloading: Boolean = false,
    val syncProgress: String = "",
    val message: String? = null,
    val rateAgeInDays: Int = 0,
    val isLoggedIn: Boolean = false,
    val cloudNickname: String = "",
    val isAdmin: Boolean = false
) {
    val isSyncing: Boolean get() = isUploading || isDownloading
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val authRepository: AuthRepository,
    private val cloudSyncManager: CloudSyncManager,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val days = exchangeRateRepository.getRateAgeInDays()
            val loggedIn = authRepository.isLoggedIn
            val nick = authRepository.nickname ?: ""
            _uiState.update { it.copy(rateAgeInDays = days, isLoggedIn = loggedIn, cloudNickname = nick) }
        }
    }

    fun refreshCloudStatus() {
        viewModelScope.launch {
            val loggedIn = authRepository.isLoggedIn
            val admin = if (loggedIn) authRepository.isAdmin() else false
            _uiState.update {
                it.copy(
                    isLoggedIn = loggedIn,
                    cloudNickname = authRepository.nickname ?: "",
                    isAdmin = admin
                )
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.update { it.copy(isLoggedIn = false, cloudNickname = "") }
    }

    /** Upload: 打包（增量/全量）→ 上传 zip → 保存 baseline */
    fun syncToCloud() {
        if (!authRepository.isLoggedIn) {
            _uiState.update { it.copy(message = "请先登录云端") }
            return
        }
        viewModelScope.launch {
            try {
                android.util.Log.d("SettingsVM", "syncToCloud: 开始上传")
                _uiState.update { it.copy(isUploading = true, syncProgress = "正在打包数据…") }
                var payload = backupManager.buildUpload()
                android.util.Log.d("SettingsVM", "syncToCloud: buildUpload=${if (payload == null) "null(无变化)" else "${payload.zipBytes.size}字节 full=${payload.isFull} rev=${payload.baseRevision}"}")
                if (payload == null) {
                    _uiState.update { it.copy(isUploading = false, syncProgress = "已是最新，无需上传", message = "已是最新，无需上传") }
                    return@launch
                }

                var result = authRepository.uploadBackup(payload.zipBytes, payload.isFull, payload.baseRevision)
                android.util.Log.d("SettingsVM", "syncToCloud: upload结果 isSuccess=${result.isSuccess} err=${result.exceptionOrNull()?.message}")
                // 冲突：另一设备已更新，回退为全量上传
                if (result.isFailure && result.exceptionOrNull() is BackupConflictException) {
                    _uiState.update { it.copy(syncProgress = "检测到其他设备更新，改为全量上传…") }
                    payload = backupManager.buildUpload(forceFull = true)!!
                    result = authRepository.uploadBackup(payload.zipBytes, true, 0)
                }

                if (result.isSuccess) {
                    backupManager.saveBaseline(payload.data, result.getOrThrow())
                    val sizeKb = "%.1f".format(payload.zipBytes.size / 1024.0)
                    val msg = "已上传到云端（${if (payload.isFull) "全量" else "增量"} ${sizeKb}KB）"
                    _uiState.update { it.copy(isUploading = false, syncProgress = msg, message = msg) }
                } else {
                    val msg = "上传失败：${result.exceptionOrNull()?.message}"
                    _uiState.update { it.copy(isUploading = false, syncProgress = msg, message = msg) }
                }
            } catch (e: Exception) {
                val msg = "上传失败：${e.message}"
                android.util.Log.e("SettingsVM", "syncToCloud: 异常", e)
                _uiState.update { it.copy(isUploading = false, syncProgress = msg, message = msg) }
            }
        }
    }

    /** Download: 下载 zip → 恢复本地数据 */
    fun pullFromCloud() {
        if (!authRepository.isLoggedIn) {
            _uiState.update { it.copy(message = "请先登录云端") }
            return
        }
        viewModelScope.launch {
            try {
                android.util.Log.d("SettingsVM", "pullFromCloud: 开始下载")
                _uiState.update { it.copy(isDownloading = true, syncProgress = "正在下载…") }
                val result = authRepository.downloadBackup()
                android.util.Log.d("SettingsVM", "pullFromCloud: 下载返回 isFailure=${result.isFailure}")
                if (result.isFailure) {
                    val msg = result.exceptionOrNull()?.message ?: "未知错误"
                    _uiState.update { it.copy(isDownloading = false, syncProgress = "", message = "下载失败：$msg") }
                    return@launch
                }
                val zip = result.getOrNull() ?: ByteArray(0)
                if (zip.isEmpty()) {
                    _uiState.update { it.copy(isDownloading = false, syncProgress = "", message = "云端无数据，请先上传") }
                    return@launch
                }

                android.util.Log.d("SettingsVM", "pullFromCloud: 开始导入, zip=${zip.size} bytes")
                _uiState.update { it.copy(syncProgress = "正在导入…") }
                val r = backupManager.restoreBackup(zip)
                android.util.Log.d("SettingsVM", "pullFromCloud: 导入完成 ${r.ledgerCount}/${r.expenseCount}/${r.imageCount}")
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        syncProgress = "",
                        message = "已同步 ${r.ledgerCount} 个账本、${r.expenseCount} 笔消费、${r.imageCount} 张图片"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDownloading = false, syncProgress = "", message = "下载失败：${e.message}") }
            }
        }
    }

    fun refreshExchangeRates() {
        viewModelScope.launch {
            val success = exchangeRateRepository.refreshRates()
            val days = exchangeRateRepository.getRateAgeInDays()
            _uiState.update {
                it.copy(
                    rateAgeInDays = days,
                    message = if (success) "汇率已更新" else "汇率更新失败，使用缓存数据"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    /** 导出应用日志到 cacheDir，返回可分享的 FileProvider URI；失败时置入提示消息并返回 null */
    fun exportLogs(): Uri? {
        return try {
            val logs = collectLogcat()
            val header = buildString {
                appendLine("=== AA 记账 日志导出 ===")
                appendLine("版本: ${BuildConfig.VERSION_NAME}")
                appendLine("设备: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} / Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
                appendLine("时间: ${System.currentTimeMillis()}")
                appendLine()
            }
            val file = java.io.File(context.cacheDir, "aa_ledger_log_${System.currentTimeMillis()}.txt")
            file.writeText(header + logs)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            android.util.Log.e("SettingsVM", "exportLogs error", e)
            _uiState.update { it.copy(message = "导出日志失败：${e.message}") }
            null
        }
    }

    /** 读取本进程的 logcat 输出 */
    private fun collectLogcat(): String {
        return try {
            val pid = android.os.Process.myPid().toString()
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time", "--pid", pid))
            val sb = StringBuilder()
            java.io.BufferedReader(java.io.InputStreamReader(process.inputStream)).forEachLine { sb.appendLine(it) }
            sb.toString()
        } catch (e: Exception) {
            "无法读取日志：${e.message}"
        }
    }
}
