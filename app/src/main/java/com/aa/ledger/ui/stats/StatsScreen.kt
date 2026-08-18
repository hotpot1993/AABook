package com.aa.ledger.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aa.ledger.ui.theme.*
import com.aa.ledger.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(ledgerId: Long, onBack: (() -> Unit)? = null, viewModel: StatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var chartMode by remember { mutableIntStateOf(0) }
    val chartModes = listOf("饼图", "柱状图", "时间轴")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("消费统计", fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = (-0.5).sp) },
                navigationIcon = { if (onBack != null) { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MontraBackground)
            )
        },
        containerColor = MontraBackground
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MontraPrimary)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Chart type switcher
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MontraFill)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        chartModes.forEachIndexed { index, label ->
                            val isActive = chartMode == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(
                                        if (isActive) Modifier.background(Color.White)
                                        else Modifier.background(Color.Transparent)
                                    )
                                    .then(
                                        if (!isActive) Modifier.clickable { chartMode = index }
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isActive) MontraTextPrimary else MontraTextTertiary
                                )
                            }
                        }
                    }
                }

                // Total expense hero
                item {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MontraSurface,
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                CurrencyFormatter.formatCny(uiState.totalExpense),
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp,
                                color = MontraPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("总支出", fontSize = 13.sp, color = MontraTextSecondary)

                            // Month-over-month comparison
                            val now = java.util.Calendar.getInstance()
                            val thisMonthKey = "${now.get(java.util.Calendar.YEAR)}-${(now.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')}"
                            val lastMonthCal = now.clone() as java.util.Calendar
                            lastMonthCal.add(java.util.Calendar.MONTH, -1)
                            val lastMonthKey = "${lastMonthCal.get(java.util.Calendar.YEAR)}-${(lastMonthCal.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')}"
                            val thisMonth = uiState.monthlyTimeline[thisMonthKey]?.sum() ?: 0.0
                            val lastMonth = uiState.monthlyTimeline[lastMonthKey]?.sum() ?: 0.0

                            if (lastMonth > 0) {
                                Spacer(Modifier.height(8.dp))
                                val change = ((thisMonth - lastMonth) / lastMonth * 100).toInt()
                                val trendText = if (change >= 0) "较上月 ↑${change}%" else "较上月 ↓${-change}%"
                                val trendColor = if (change >= 0) MontraRed else MontraPrimary
                                Text(trendText, fontSize = 12.sp, color = trendColor, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                when (chartMode) {
                    // ── 饼图: Category spending proportions ──
                    0 -> {
                        if (uiState.categorySummary.isNotEmpty()) {
                            item { SectionHeader("分类支出占比") }
                            item {
                                Surface(
                                    Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    color = MontraSurface,
                                    shadowElevation = 1.dp
                                ) {
                                    Column(Modifier.padding(20.dp)) {
                                        val catData = uiState.categorySummary.entries
                                            .sortedByDescending { it.value }
                                            .map { (cat, amt) -> cat to amt }
                                        val catColors = catData.map { (cat, _) ->
                                            categoryColors[cat] ?: chartColors.first()
                                        }
                                        PieChart(data = catData, colors = catColors)
                                        Spacer(Modifier.height(10.dp))
                                        Legend(items = catData.zip(catColors).map { (catPair, clr) ->
                                            Triple(catPair.first, catPair.second, clr)
                                        })
                                    }
                                }
                            }
                        }
                        // Member owed amounts
                        if (uiState.memberOwedAmounts.isNotEmpty()) {
                            item { SectionHeader("成员应付金额") }
                            item {
                                Surface(
                                    Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    color = MontraSurface,
                                    shadowElevation = 1.dp
                                ) {
                                    Column(Modifier.padding(20.dp)) {
                                        val owedData = uiState.memberOwedAmounts.entries
                                            .sortedByDescending { it.value }
                                            .associate { (mid, amt) ->
                                                (uiState.memberNames[mid] ?: "未知") to amt
                                            }
                                        MemberBarChart(
                                            data = owedData,
                                            total = owedData.values.sum()
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // ── 柱状图: Member spending bars ──
                    1 -> {
                        if (uiState.memberPaidAmounts.isNotEmpty()) {
                            item { SectionHeader("成员支出占比") }
                            item {
                                Surface(
                                    Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    color = MontraSurface,
                                    shadowElevation = 1.dp
                                ) {
                                    Column(Modifier.padding(20.dp)) {
                                        val memberData = uiState.memberPaidAmounts.entries.sortedByDescending { it.value }.associate { (mid, amt) ->
                                            (uiState.memberNames[mid] ?: "未知") to amt
                                        }
                                        MemberBarChart(data = memberData, total = uiState.totalExpense)
                                    }
                                }
                            }
                        }
                    }
                    // ── 时间轴 ──
                    2 -> {
                        if (uiState.monthlyTimeline.isNotEmpty()) {
                            item { SectionHeader("按月支出") }
                            item {
                                Surface(
                                    Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    color = MontraSurface,
                                    shadowElevation = 1.dp
                                ) {
                                    Column(Modifier.padding(20.dp)) {
                                        uiState.monthlyTimeline.entries.sortedBy { it.key }.forEach { (m, amts) ->
                                            Row(
                                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(m, color = MontraTextPrimary)
                                                Text(
                                                    CurrencyFormatter.formatCny(amts.sum()),
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MontraPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        Modifier.padding(horizontal = 4.dp),
        fontSize = 15.sp,
        color = MontraTextPrimary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun CategoryBarChart(data: Map<String, Double>) {
    val mx = data.values.maxOrNull() ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        data.entries.sortedByDescending { it.value }.forEach { (c, a) ->
            val f = (a / mx).toFloat().coerceIn(0f, 1f)
            val clr = categoryColors[c] ?: SystemIndigo
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(100.dp)).background(clr))
                        Spacer(Modifier.width(8.dp))
                        Text(c, fontSize = 13.sp, color = MontraTextSecondary)
                    }
                    Row {
                        Text(
                            CurrencyFormatter.formatCny(a),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MontraTextPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${(a / data.values.sum() * 100).toInt()}%",
                            fontSize = 13.sp,
                            color = MontraTextTertiary
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).background(MontraFill, RoundedCornerShape(4.dp))) {
                    Box(
                        Modifier.fillMaxWidth(f).height(8.dp).background(clr, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun MemberBarChart(data: Map<String, Double>, total: Double) {
    val mx = data.values.maxOrNull() ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        data.entries.forEach { (name, amt) ->
            val f = (amt / mx).toFloat().coerceIn(0f, 1f)
            val pct = if (total > 0) (amt / total * 100).toInt() else 0
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name, fontSize = 13.sp, color = MontraTextPrimary)
                    Row {
                        Text(
                            CurrencyFormatter.formatCny(amt),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MontraTextPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${pct}%", fontSize = 13.sp, color = MontraTextTertiary)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.fillMaxWidth().height(8.dp)
                        .background(MontraFill, RoundedCornerShape(4.dp))
                ) {
                    Box(
                        Modifier.fillMaxWidth(f).height(8.dp)
                            .background(MontraPrimary, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun PieChart(data: List<Pair<String, Double>>, colors: List<Color> = chartColors) {
    val t = data.sumOf { it.second }
    if (t <= 0) return
    var sa = -90f
    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        val r = minOf(size.width, size.height) / 2 * 0.75f
        val c = Offset(size.width / 2, size.height / 2)
        data.forEachIndexed { i, (_, _) ->
            val sw = (data[i].second / t * 360).toFloat()
            drawArc(
                colors[i % colors.size], sa, sw, true,
                Offset(c.x - r, c.y - r), Size(r * 2, r * 2)
            )
            sa += sw
        }
    }
}

@Composable
fun Legend(items: List<Triple<String, Double, Color>>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { (n, a, clr) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(100.dp)).background(clr))
                Spacer(Modifier.width(8.dp))
                Text(n, Modifier.weight(1f), fontSize = 13.sp, color = MontraTextSecondary)
                Text(
                    CurrencyFormatter.formatCny(a),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MontraTextPrimary
                )
            }
        }
    }
}
