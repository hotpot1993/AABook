package com.aa.ledger.ui.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aa.ledger.BuildConfig
import com.aa.ledger.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: (() -> Unit)? = null, onCloudLogin: () -> Unit = {}, onAdmin: () -> Unit = {}, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val ctx = LocalContext.current

    // Refresh cloud login status every time screen is shown
    LaunchedEffect(Unit) { viewModel.refreshCloudStatus() }

    // Auto-dismiss snackbar message after 4 seconds
    LaunchedEffect(uiState.message) {
        if (uiState.message != null) {
            kotlinx.coroutines.delay(4000)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("设置", fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = (-0.5).sp)
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MontraBackground)
            )
        },
        snackbarHost = {
            uiState.message?.let { m -> Snackbar(Modifier.padding(16.dp)) { Text(m) } }
        },
        containerColor = MontraBackground
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── 汇率设置 Section ──
            Text(
                "汇率设置",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MontraTextSecondary
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MontraSurface,
                shadowElevation = 1.dp
            ) {
                Column(
                    Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Refresh button
                    Surface(
                        onClick = { viewModel.refreshExchangeRates() },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1A7FDB)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                "刷新",
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "刷新汇率",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                    // Status text
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (statusText, statusColor) = when {
                            uiState.rateAgeInDays == 0 -> "汇率今日已刷新" to MontraPrimary
                            uiState.rateAgeInDays == Int.MAX_VALUE -> "尚未缓存汇率" to MontraTextSecondary
                            else -> "汇率更新于 ${uiState.rateAgeInDays} 天前" to MontraTextSecondary
                        }
                        if (uiState.rateAgeInDays == 0) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                null,
                                modifier = Modifier.size(13.dp),
                                tint = MontraPrimary
                            )
                            Spacer(Modifier.width(5.dp))
                        }
                        Text(statusText, fontSize = 12.sp, color = statusColor)
                    }
                }
            }

            // ── 云端同步 Section ──
            Text(
                "云端同步",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MontraTextSecondary
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MontraSurface,
                shadowElevation = 1.dp
            ) {
                Column {
                    // Login status
                    SettingsRow(
                        icon = Icons.Outlined.Cloud,
                        iconBg = if (uiState.isLoggedIn) GreenBg else InfoBlueBg,
                        iconTint = if (uiState.isLoggedIn) MontraPrimary else InfoBlue,
                        title = if (uiState.isLoggedIn) "已登录：${uiState.cloudNickname}" else "云端登录",
                        subtitle = if (uiState.isLoggedIn) "" else "登录后可多设备同步数据",
                        onClick = onCloudLogin
                    )
                    // Sync actions (only when logged in)
                    if (uiState.isLoggedIn) {
                        HorizontalDivider(color = MontraDivider)
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Upload button
                            Surface(
                                onClick = { viewModel.syncToCloud() },
                                shape = RoundedCornerShape(10.dp),
                                color = MontraPrimary,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (uiState.isUploading) CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                    else Icon(Icons.Outlined.CloudUpload, null, Modifier.size(16.dp), tint = Color.White)
                                    Spacer(Modifier.width(6.dp))
                                    Text("上传", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            // Download button
                            Surface(
                                onClick = { viewModel.pullFromCloud() },
                                shape = RoundedCornerShape(10.dp),
                                color = InfoBlue,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (uiState.isDownloading) CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                    else Icon(Icons.Outlined.CloudDownload, null, Modifier.size(16.dp), tint = Color.White)
                                    Spacer(Modifier.width(6.dp))
                                    Text("下载", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        // Progress text
                        if (uiState.syncProgress.isNotEmpty()) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (uiState.isSyncing) {
                                    CircularProgressIndicator(Modifier.size(14.dp), color = MontraPrimary, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    uiState.syncProgress,
                                    fontSize = 12.sp,
                                    color = if (uiState.isSyncing) MontraTextSecondary else MontraPrimary,
                                    fontWeight = if (uiState.isSyncing) FontWeight.Normal else FontWeight.Medium
                                )
                            }
                        }
                        // Admin (only visible for admin account)
                        if (uiState.isAdmin) {
                            SettingsRow(
                                icon = Icons.Outlined.AdminPanelSettings,
                                iconBg = MontraRed.copy(alpha = 0.1f),
                                iconTint = MontraRed,
                                title = "账号管理",
                                subtitle = "管理所有注册用户",
                                onClick = onAdmin
                            )
                        }
                        // Logout
                        SettingsRow(
                            icon = Icons.Outlined.Logout,
                            iconBg = MontraRed.copy(alpha = 0.1f),
                            iconTint = MontraRed,
                            title = "退出登录",
                            subtitle = "",
                            onClick = { viewModel.logout() }
                        )
                    }
                }
            }

            // ── 关于 Section ──
            Text(
                "关于",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MontraTextSecondary
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MontraSurface,
                shadowElevation = 1.dp
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Outlined.Info,
                        iconBg = GreenBg,
                        iconTint = MontraPrimary,
                        title = "AA 记账",
                        subtitle = "v${BuildConfig.VERSION_NAME} · 离线优先的多人记账工具",
                        showChevron = false
                    )
                    HorizontalDivider(color = MontraBackground)
                    SettingsRow(
                        icon = Icons.Outlined.BugReport,
                        iconBg = InfoBlueBg,
                        iconTint = InfoBlue,
                        title = "导出日志",
                        subtitle = "导出应用日志，用于排查问题",
                        onClick = {
                            val uri = viewModel.exportLogs()
                            if (uri != null) {
                                val i = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                ctx.startActivity(Intent.createChooser(i, "导出日志"))
                            }
                        }
                    )
                    HorizontalDivider(color = MontraBackground)
                    SettingsRow(
                        icon = Icons.Outlined.WarningAmber,
                        iconBg = WarningOrangeLight,
                        iconTint = MontraRed,
                        title = "提示",
                        subtitle = "所有数据仅存储在本机，删除应用将导致数据丢失。建议登录云端同步，定时上传备份。",
                        showChevron = false
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = true
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = iconTint)
        }
        Spacer(Modifier.width(12.dp))
        // Text
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = MontraTextPrimary)
            Text(subtitle, fontSize = 12.sp, color = MontraTextSecondary)
        }
        // Chevron
        if (showChevron) {
            Icon(
                Icons.Outlined.ChevronRight,
                null,
                modifier = Modifier.size(16.dp),
                tint = MontraTextTertiary
            )
        }
    }
}
