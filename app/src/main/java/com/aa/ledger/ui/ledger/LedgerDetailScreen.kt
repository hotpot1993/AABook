package com.aa.ledger.ui.ledger

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aa.ledger.domain.model.Expense
import com.aa.ledger.ui.common.DeleteConfirmDialog
import com.aa.ledger.ui.theme.*
import com.aa.ledger.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LedgerDetailScreen(
    ledgerId: Long,
    onAddExpense: () -> Unit,
    onEditExpense: (Long) -> Unit,
    onSettlement: () -> Unit,
    onManageMembers: () -> Unit,
    onStats: () -> Unit,
    onBack: () -> Unit,
    viewModel: LedgerDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var deleteTarget by remember { mutableStateOf<Expense?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("消费流水", "成员账单")
    val ctx = LocalContext.current

    val ledger = uiState.ledger
    val totalExpense = uiState.totalExpense
    val memberCount = uiState.memberCount
    val expenseCount = uiState.expenses.size
    val perPerson = if (memberCount > 0) totalExpense / memberCount else 0.0

    Scaffold(
        containerColor = MontraBackground,
        floatingActionButton = {
            Box(modifier = Modifier.padding(bottom = 76.dp)) {
                FloatingActionButton(
                    onClick = onAddExpense,
                    shape = CircleShape,
                    containerColor = MontraPrimary,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp)
                ) { Icon(Icons.Filled.Add, "记一笔") }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // ── Header: Back + Title + Add Member ──
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = MontraSurface,
                    shadowElevation = 1.dp
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "返回",
                        modifier = Modifier.size(40.dp).padding(11.dp),
                        tint = MontraTextPrimary
                    )
                }
                Spacer(Modifier.width(8.dp))
                // Title
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        ledger?.name ?: "账本详情",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MontraTextPrimary
                    )
                    Text(
                        "$memberCount 人 · ${if (uiState.isSettled) "已结算" else "进行中"}",
                        fontSize = 12.sp,
                        color = if (uiState.isSettled) MontraPrimary else MontraTextSecondary
                    )
                }
                // Add member button
                Surface(
                    onClick = onManageMembers,
                    shape = CircleShape,
                    color = MontraSurface,
                    shadowElevation = 1.dp
                ) {
                    Icon(
                        Icons.Outlined.PersonAdd, "添加成员",
                        modifier = Modifier.size(40.dp).padding(11.dp),
                        tint = MontraTextPrimary
                    )
                }
            }

            // ── Summary Card ──
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                color = MontraSurface,
                shadowElevation = 1.dp
            ) {
                Column(Modifier.padding(20.dp)) {
                    // 3 stats row
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DetailStat(CurrencyFormatter.formatCny(totalExpense), "总支出", MontraTextPrimary)
                        Box(Modifier.width(1.dp).height(40.dp).background(MontraDivider))
                        DetailStat(CurrencyFormatter.formatCny(perPerson), "人均", MontraPrimary)
                        Box(Modifier.width(1.dp).height(40.dp).background(MontraDivider))
                        DetailStat("$expenseCount", "消费笔数", MontraTextPrimary)
                    }

                    // Budget progress (if set)
                    val budget = uiState.ledger?.budgetAmount ?: 0.0
                    if (budget > 0) {
                        Spacer(Modifier.height(14.dp))
                        val budgetPct = if (budget > 0) (totalExpense / budget).coerceIn(0.0, 1.0).toFloat() else 0f
                        val budgetColor = when {
                            budgetPct > 0.9f -> MontraRed
                            budgetPct > 0.7f -> WarningOrange
                            else -> MontraPrimary
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("预算", fontSize = 12.sp, color = MontraTextSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${CurrencyFormatter.formatCny(totalExpense)} / ${CurrencyFormatter.formatCny(budget)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = budgetColor
                                )
                                if (budgetPct >= 1f) {
                                    Spacer(Modifier.width(4.dp))
                                    Text("超预算!", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MontraRed)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(100.dp)).background(MontraFill)
                        ) {
                            Box(
                                Modifier.fillMaxWidth(budgetPct).height(6.dp)
                                    .background(budgetColor, RoundedCornerShape(100.dp))
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // "我的余额" / Settlement bar
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(WarningOrangeBg)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "待结算 · ${uiState.pendingSettlementCount} 笔",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WarningOrange
                        )
                        Surface(
                            onClick = onSettlement,
                            shape = RoundedCornerShape(100.dp),
                            color = MontraPrimary
                        ) {
                            Text(
                                "去结算",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Tab Switcher ──
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MontraFill)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tabs.forEachIndexed { index, label ->
                    val isActive = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (isActive) Modifier.background(Color.White)
                                else Modifier.clickable { selectedTab = index }
                            )
                            .then(
                                if (!isActive) Modifier.background(Color.Transparent)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isActive) MontraTextPrimary else MontraTextSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Search & Filter Bar (only in expense tab) ──
            if (selectedTab == 0 && uiState.expenses.isNotEmpty()) {
                var searchQuery by remember { mutableStateOf("") }

                Column(Modifier.padding(horizontal = 24.dp)) {
                    // Search field
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MontraFill
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Search, "搜索",
                                Modifier.size(18.dp), tint = MontraTextSecondary
                            )
                            Spacer(Modifier.width(8.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 14.sp, color = MontraTextPrimary),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) Text(
                                        "搜索消费名称或备注…",
                                        fontSize = 14.sp,
                                        color = MontraTextSecondary
                                    )
                                    inner()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Outlined.Close, "清除", Modifier.size(16.dp), tint = MontraTextSecondary)
                                }
                            }
                        }
                    }
                }

                // Apply search to the expense content
                val filteredExpenses = uiState.expenses.filter { exp ->
                    searchQuery.isEmpty() ||
                        exp.title.contains(searchQuery, ignoreCase = true) ||
                        exp.note.contains(searchQuery, ignoreCase = true)
                }

                Spacer(Modifier.height(12.dp))

                // ── Tab Content (using filtered expenses for tab 0) ──
                when {
                    uiState.isLoading && uiState.expenses.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MontraPrimary)
                        }
                    }
                    else -> {
                        when (selectedTab) {
                            0 -> ExpenseTabContent(
                                expenses = filteredExpenses,
                                onEditExpense = onEditExpense,
                                onDeleteExpense = { deleteTarget = it },
                                onClickExpense = onEditExpense,
                                emptyMessage = if (searchQuery.isNotEmpty()) "没有匹配的消费记录" else "还没有消费记录",
                                emptyAction = { onAddExpense() },
                                emptyActionLabel = "记一笔",
                                emptyIcon = { Icon(Icons.AutoMirrored.Outlined.ReceiptLong, null, Modifier.size(36.dp), tint = MontraPrimary) }
                            )
                            1 -> MemberBillTabContent(
                                memberBills = uiState.memberBills,
                                onManageMembers = onManageMembers
                            )
                        }
                    }
                }
            } else {
                // ── Tab Content (no search when in member tab or empty) ──
            when {
                uiState.isLoading && uiState.expenses.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MontraPrimary)
                    }
                }
                else -> {
                    when (selectedTab) {
                        0 -> ExpenseTabContent(
                            expenses = uiState.expenses,
                            onEditExpense = onEditExpense,
                            onDeleteExpense = { deleteTarget = it },
                            onClickExpense = onEditExpense,
                            emptyMessage = "还没有消费记录",
                            emptyAction = { onAddExpense() },
                            emptyActionLabel = "记一笔",
                            emptyIcon = { Icon(Icons.AutoMirrored.Outlined.ReceiptLong, null, Modifier.size(36.dp), tint = MontraPrimary) }
                        )
                        1 -> MemberBillTabContent(
                            memberBills = uiState.memberBills,
                            onManageMembers = onManageMembers
                        )
                    }
                }
            }
            }
        }

        // ── Delete Confirmation ──
        deleteTarget?.let { e ->
            DeleteConfirmDialog(
                title = "确认删除",
                message = "删除「${e.title}」？此操作不可恢复。",
                onConfirm = { viewModel.deleteExpense(e.id); deleteTarget = null },
                onDismiss = { deleteTarget = null }
            )
        }
    }
}

// ── Detail Stat Column ──
@Composable
private fun DetailStat(value: String, label: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = valueColor)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = MontraTextSecondary)
    }
}

// ── Expense Tab ──
@Composable
private fun ExpenseTabContent(
    expenses: List<Expense>,
    onEditExpense: (Long) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onClickExpense: (Long) -> Unit,
    emptyMessage: String,
    emptyAction: () -> Unit,
    emptyActionLabel: String,
    emptyIcon: @Composable () -> Unit
) {
    if (expenses.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(MontraPrimary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) { emptyIcon() }
                Spacer(Modifier.height(16.dp))
                Text(emptyMessage, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MontraTextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("点击右下角 + 记一笔", fontSize = 13.sp, color = MontraTextSecondary)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = emptyAction) {
                    Text(emptyActionLabel, color = MontraPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Group by date
            val grouped = expenses.groupBy {
                val cal = Calendar.getInstance()
                val today = Calendar.getInstance()
                cal.timeInMillis = it.createdAt
                when {
                    cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "今天"
                    cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) - 1 -> "昨天"
                    cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) ->
                        SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(it.createdAt))
                    else -> SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date(it.createdAt))
                }
            }
            grouped.forEach { (label, groupExpenses) ->
                item {
                    Text(
                        "$label · ${groupExpenses.size}笔",
                        Modifier.padding(vertical = 8.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MontraTextSecondary
                    )
                }
                items(groupExpenses.size) { i ->
                    val e = groupExpenses[i]
                    @OptIn(ExperimentalFoundationApi::class)
                    Surface(
                        Modifier.fillMaxWidth()
                            .combinedClickable(
                                onClick = { onEditExpense(e.id) },
                                onLongClick = { onDeleteExpense(e) }
                            ),
                        shape = RoundedCornerShape(20.dp),
                        color = MontraSurface,
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val emoji = mapOf("餐饮" to "🍜", "住宿" to "🏠", "交通" to "🚗", "购物" to "🛍️", "娱乐" to "🎮", "医疗" to "🏥", "人情" to "🎁", "教育" to "📚", "保险" to "🛡️", "其他" to "📌")
                            val catBg = categoryBgColors[e.category] ?: CatOther
                            Box(
                                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(catBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji[e.category] ?: "📌", fontSize = 20.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(e.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MontraTextPrimary)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    "${e.category} · ${e.splits.size}人分摊",
                                    fontSize = 12.sp,
                                    color = MontraTextSecondary
                                )
                            }
                            Text(
                                CurrencyFormatter.formatCny(e.totalAmountCny),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MontraTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Member Bill Tab ──
@Composable
private fun MemberBillTabContent(
    memberBills: List<MemberBillInfo>,
    onManageMembers: () -> Unit
) {
    if (memberBills.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无成员数据", fontSize = 15.sp, color = MontraTextSecondary)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "全部成员 (${memberBills.size}人)",
                    Modifier.padding(vertical = 8.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MontraTextPrimary
                )
            }
            items(memberBills.size) { i ->
                val bill = memberBills[i]
                val avatarColors = listOf(MontraPrimary, WarningOrange, InfoBlue, CoverGold)
                val avatarBg = avatarColors[i % avatarColors.size]

                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MontraSurface,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(avatarBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                bill.member.name.take(1),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        // Info
                        Column(Modifier.weight(1f)) {
                            Text(
                                bill.member.nickname.ifEmpty { bill.member.name },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MontraTextPrimary
                            )
                        }
                        // Paid amount
                        Text(
                            "已付 ${CurrencyFormatter.formatCny(bill.paidAmount)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MontraPrimary
                        )
                    }
                }
            }
        }
    }
}

