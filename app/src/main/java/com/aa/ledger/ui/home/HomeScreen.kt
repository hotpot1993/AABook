package com.aa.ledger.ui.home

import android.app.Activity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.aa.ledger.domain.model.Ledger
import com.aa.ledger.ui.common.DeleteConfirmDialog
import com.aa.ledger.ui.common.SwipeActionRow
import com.aa.ledger.ui.common.bounceClick
import com.aa.ledger.ui.common.staggerEnter
import com.aa.ledger.ui.home.HomeFilterMode
import com.aa.ledger.ui.theme.*
import com.aa.ledger.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onLedgerClick: (Long, Ledger) -> Unit, onCreateLedger: () -> Unit, onSettingsClick: () -> Unit, onDefaultLedgerChanged: (Long) -> Unit = {}, viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var deleteTarget by remember { mutableStateOf<Ledger?>(null) }
    val totalMembersAll = uiState.ledgers.sumOf { it.memberCount }
    val ledgerCount = uiState.ledgers.size

    // Sync default ledger to parent (NavGraph) for quick-add navigation
    LaunchedEffect(uiState.defaultLedgerId) {
        onDefaultLedgerChanged(uiState.defaultLedgerId)
    }

    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val prevColor = window.statusBarColor
        window.statusBarColor = MontraBackground.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        onDispose { window.statusBarColor = prevColor }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "我的账本",
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = MontraTextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MontraBackground)
            )
        },
        containerColor = MontraBackground
    ) { padding ->
        if (uiState.isLoading && uiState.ledgers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MontraPrimary)
            }
        } else if (uiState.ledgers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).background(MontraPrimary.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.MenuBook, null, Modifier.size(36.dp), tint = MontraPrimary)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("还没有账本", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MontraTextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text("点击下方 + 创建", fontSize = 13.sp, color = MontraTextSecondary)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onCreateLedger) {
                        Text("创建账本", color = MontraPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // ── Green Gradient Summary Card ──
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(
                                    Brush.linearGradient(listOf(MontraPrimary, MontraPrimaryLight)),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text(
                                    "总待结算",
                                    color = GreenLight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    CurrencyFormatter.formatCny(uiState.pendingTotal),
                                    color = Color.White,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-1).sp
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "涉及 $ledgerCount 个账本 · $totalMembersAll 位成员",
                                    color = GreenLight,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // ── Section Header: "全部账本" + "新建账本" ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "全部账本",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MontraTextPrimary
                        )
                        // Pill-shaped "新建账本" button
                        Surface(
                            modifier = Modifier.bounceClick(onClick = onCreateLedger),
                            shape = RoundedCornerShape(100.dp),
                            color = MontraPrimary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp).height(36.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Add, "新建", tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("新建账本", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Filter chips
                    Row(
                        Modifier.padding(start = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HomeFilterMode.entries.forEach { mode ->
                            val label = when (mode) {
                                HomeFilterMode.ALL -> "全部"
                                HomeFilterMode.ACTIVE -> "进行中"
                                HomeFilterMode.SETTLED -> "已结算"
                            }
                            val isSel = uiState.filterMode == mode
                            Surface(
                                modifier = Modifier.bounceClick(onClick = { viewModel.setFilterMode(mode) }),
                                shape = RoundedCornerShape(100.dp),
                                color = if (isSel) MontraPrimary else MontraFill
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSel) Color.White else MontraTextSecondary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ── Ledger Cards ──
                val displayLedgers = viewModel.getFilteredLedgers(uiState)
                items(displayLedgers.size) { i ->
                    val ledger = displayLedgers[i]
                    val isSettled = uiState.ledgerSettledMap[ledger.id] ?: false
                    val isArchived = ledger.isArchived
                    Box(modifier = Modifier.padding(horizontal = 24.dp).staggerEnter(i)) {
                        val isDefault = ledger.id == uiState.defaultLedgerId
                        SwipeActionRow(
                            onEdit = null,
                            onDelete = { deleteTarget = ledger },
                            onClick = {
                                viewModel.onLedgerClicked(ledger)
                                onLedgerClick(ledger.id, ledger)
                            },
                            cornerRadius = 20
                        ) {
                            NewLedgerCard(ledger = ledger, isSettled = isSettled, isDefault = isDefault)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        // ── Delete Confirmation ──
        deleteTarget?.let { l ->
            DeleteConfirmDialog(
                title = "删除账本",
                message = "确认删除「${l.name}」？此操作将同时删除该账本下的所有消费记录。",
                onConfirm = { viewModel.deleteLedger(l.id); deleteTarget = null },
                onDismiss = { deleteTarget = null }
            )
        }
    }
}

// ── New Ledger Card (design-fidelity) ──
@Composable
private fun NewLedgerCard(ledger: Ledger, isSettled: Boolean = false, isDefault: Boolean = false) {
    val coverIcon = mapOf("green" to "✈️", "blue" to "🏠", "orange" to "🍽️", "gold" to "❤️")
    val iconBg = when (ledger.coverType) {
        "blue" -> InfoBlueBg
        "orange" -> WarningOrangeBg
        else -> GreenBg
    }
    val (statusText, statusBg, statusColor) = if (isSettled) {
        Triple("已结算", GreenLight, MontraPrimary)
    } else if (isDefault) {
        Triple("进行中", GreenLight, MontraPrimary)
    } else {
        Triple("进行中", WarningOrangeBg, WarningOrange)
    }

    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MontraSurface,
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(coverIcon[ledger.coverType] ?: "📒", fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(ledger.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MontraTextPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${ledger.memberCount} 人 · ${
                            SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(ledger.createdAt))
                        }",
                        fontSize = 12.sp, color = MontraTextSecondary
                    )
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(100.dp)).background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(statusText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MontraDivider)
            Spacer(Modifier.height(14.dp))

            Column {
                Text("总支出", fontSize = 11.sp, color = MontraTextSecondary)
                Spacer(Modifier.height(2.dp))
                Text(
                    CurrencyFormatter.formatCny(ledger.totalExpense),
                    fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    color = if (isSettled) MontraTextTertiary else MontraTextPrimary
                )
            }
        }
    }
}
