package com.aa.ledger.ui.settlement

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aa.ledger.ui.theme.*
import com.aa.ledger.util.CurrencyFormatter
import com.aa.ledger.util.ShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementScreen(ledgerId: Long, onBack: () -> Unit, viewModel: SettlementViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("结算清单", fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = (-0.5).sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    IconButton(onClick = {
                        uiState.result?.let {
                            val bm = ShareUtil.generateSettlementImage(it)
                            ctx.startActivity(Intent.createChooser(ShareUtil.shareImage(ctx, bm), "分享"))
                        }
                    }) {
                        Icon(Icons.Outlined.Share, "分享", tint = MontraPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MontraBackground)
            )
        },
        containerColor = MontraBackground
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MontraPrimary)
            }
        } else if (uiState.error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.error!!, color = MontraRed)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.calculateSettlement() },
                        colors = ButtonDefaults.buttonColors(containerColor = MontraPrimary)
                    ) { Text("重试") }
                }
            }
        } else {
            val r = uiState.result ?: return@Scaffold
            val transfers = uiState.transfers
            val totalTransfers = transfers.size
            val paidCount = uiState.paidCount
            val unpaidTotal = uiState.unpaidTotal
            val progressPercent = if (totalTransfers > 0) (paidCount * 100 / totalTransfers) else 0

            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── 1. Summary Card (Green Gradient) ──
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(listOf(MontraPrimary, MontraPrimaryLight)),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Text(
                                "最优结算方案",
                                color = GreenLight,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                SummaryStat("$totalTransfers", "笔转账")
                                Box(Modifier.width(1.dp).height(36.dp).background(Color.White.copy(alpha = 0.2f)))
                                SummaryStat(CurrencyFormatter.formatCny(unpaidTotal), "需转账总额")
                                Box(Modifier.width(1.dp).height(36.dp).background(Color.White.copy(alpha = 0.2f)))
                                SummaryStat("$paidCount", "笔已结算")
                            }
                        }
                    }
                }

                // ── 2. Overall Settlement Card ──
                item {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MontraSurface,
                        shadowElevation = 1.dp
                    ) {
                        Column {
                            // Header
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.linearGradient(listOf(MontraPrimary, MontraPrimaryLight)),
                                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                                    )
                                    .padding(horizontal = 20.dp, vertical = 16.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "整体结算建议",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            "最优转账路径 · 已为您优化",
                                            fontSize = 12.sp,
                                            color = GreenLight
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(Color.White.copy(alpha = 0.15f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            "$paidCount/$totalTransfers 已完成",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // Progress
                            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("结算进度", fontSize = 12.sp, color = MontraTextSecondary)
                                    Text(
                                        "$progressPercent%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MontraPrimary
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(MontraFill)
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth(progressPercent / 100f)
                                            .height(6.dp)
                                            .background(
                                                Brush.horizontalGradient(listOf(MontraPrimary, MontraPrimaryLight)),
                                                RoundedCornerShape(100.dp)
                                            )
                                    )
                                }
                            }

                            HorizontalDivider(color = MontraFill)

                            // Transfer list
                            if (transfers.isEmpty()) {
                                Text(
                                    "✅ 已全部结清，无需转账",
                                    Modifier.fillMaxWidth().padding(24.dp),
                                    color = MontraPrimary,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                transfers.forEachIndexed { i, st ->
                                    val tx = st.transaction
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Number badge
                                        val numColor = if (st.isPaid) GreenLight else WarningOrangeLight
                                        val numTextColor = if (st.isPaid) MontraPrimary else WarningOrange
                                        Box(
                                            Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(numColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "${i + 1}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = numTextColor
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        // Names
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                "${tx.fromMemberName} → ${tx.toMemberName}",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = if (st.isPaid) MontraTextTertiary else MontraTextPrimary
                                            )
                                            Text(
                                                "转账",
                                                fontSize = 11.sp,
                                                color = MontraTextSecondary
                                            )
                                        }
                                        // Amount + status
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                CurrencyFormatter.formatCny(tx.amountCny),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (st.isPaid) MontraTextTertiary else WarningOrange
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(100.dp))
                                                    .background(if (st.isPaid) GreenLight else WarningOrangeLight)
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    if (st.isPaid) "已完成" else "待转账",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (st.isPaid) MontraPrimary else WarningOrange
                                                )
                                            }
                                        }
                                    }
                                    if (i < transfers.lastIndex) {
                                        HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MontraBackground)
                                    }
                                }
                            }

                            // Total row
                            if (!transfers.isEmpty()) {
                                HorizontalDivider(color = MontraFill)
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "待完成转账",
                                        fontSize = 13.sp,
                                        color = MontraTextSecondary
                                    )
                                    Text(
                                        CurrencyFormatter.formatCny(unpaidTotal),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = WarningOrange
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 3. Transfer Action Cards ──
                if (transfers.isNotEmpty()) {
                    item {
                        Text(
                            "转账结算",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MontraTextPrimary
                        )
                    }

                    items(transfers.size) { i ->
                        val st = transfers[i]
                        val tx = st.transaction
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
                                    // From avatar
                                    Box(
                                        Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MontraPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            tx.fromMemberName.take(1),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            tx.fromMemberName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = MontraTextPrimary
                                        )
                                        Text(
                                            "转账给 → ${tx.toMemberName}",
                                            fontSize = 12.sp,
                                            color = MontraTextSecondary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            CurrencyFormatter.formatCny(tx.amountCny),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = WarningOrange
                                        )
                                        Text(
                                            if (st.isPaid) "已完成" else "待转账",
                                            fontSize = 11.sp,
                                            color = if (st.isPaid) CoverGreen else MontraTextSecondary
                                        )
                                    }
                                }

                                if (!st.isPaid) {
                                    Spacer(Modifier.height(12.dp))
                                    Surface(
                                        onClick = { viewModel.markAsPaid(i) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MontraFill
                                    ) {
                                        Row(
                                            Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Outlined.Check,
                                                "标记已还",
                                                modifier = Modifier.size(16.dp),
                                                tint = MontraTextSecondary
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "标记已还",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = MontraTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 4. Share Buttons ──
                item {
                    Spacer(Modifier.height(8.dp))
                    // Primary row: Copy + Share Image
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            onClick = { ShareUtil.copyToClipboard(ctx, uiState.shareText) },
                            shape = RoundedCornerShape(16.dp),
                            color = MontraSurface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.ContentCopy, "复制", tint = MontraPrimary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("复制文案", color = MontraPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            }
                        }
                        Surface(
                            onClick = {
                                val bm = ShareUtil.generateSettlementImage(uiState.result!!)
                                ctx.startActivity(Intent.createChooser(ShareUtil.shareImage(ctx, bm), "分享结算报告"))
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = MontraTextPrimary,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Share, "分享", tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("分享图片", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Secondary row: PDF export
                    Surface(
                        onClick = {
                            val intent = ShareUtil.generateAndSharePdf(ctx, uiState.result!!)
                            ctx.startActivity(Intent.createChooser(intent, "导出 PDF 结算报告"))
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MontraPrimary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.PictureAsPdf, "PDF", tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("导出 PDF 报告", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = GreenLight)
    }
}
