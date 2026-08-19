package com.aa.ledger.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aa.ledger.domain.model.Ledger
import com.aa.ledger.ui.home.HomeViewModel
import com.aa.ledger.ui.common.bounceClick
import com.aa.ledger.ui.common.staggerEnter
import com.aa.ledger.ui.theme.*
import com.aa.ledger.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsOverviewScreen(
    onLedgerStatsClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val ledgers = uiState.ledgers

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("消费统计", fontWeight = FontWeight.Bold, fontSize = 26.sp, color = MontraTextPrimary, letterSpacing = (-0.5).sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MontraBackground)
            )
        },
        containerColor = MontraBackground
    ) { padding ->
        if (ledgers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("还没有账本", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MontraTextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text("创建账本后可查看消费统计", fontSize = 13.sp, color = MontraTextSecondary)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "选择账本查看统计",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MontraTextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(ledgers.size) { index ->
                    val ledger = ledgers[index]
                    StatsLedgerCard(
                        ledger = ledger,
                        onClick = { onLedgerStatsClick(ledger.id) },
                        modifier = Modifier.staggerEnter(index)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsLedgerCard(
    ledger: Ledger,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MontraSurface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ledgerCoverColor(ledger.coverType).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (ledger.coverType) {
                        "green" -> "✈️"
                        "orange" -> "🍜"
                        "blue" -> "🏠"
                        else -> "📌"
                    },
                    fontSize = 22.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(ledger.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MontraTextPrimary)
                Text(
                    "${ledger.memberCount} 人",
                    fontSize = 12.sp,
                    color = MontraTextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    CurrencyFormatter.formatCny(ledger.totalExpense),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MontraPrimary
                )
                Text("总支出", fontSize = 11.sp, color = MontraTextSecondary)
            }
        }
    }
}
