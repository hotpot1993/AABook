package com.aa.ledger.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aa.ledger.ui.common.bounceClick
import com.aa.ledger.ui.theme.*

enum class BottomTab(val label: String, val route: String, val icon: ImageVector, val filledIcon: ImageVector) {
    HOME("首页", "home", Icons.Outlined.Home, Icons.Filled.Home),
    ADD_EXPENSE("记账", "add_expense", Icons.Outlined.AddCircle, Icons.Outlined.AddCircle),
    STATS("统计", "stats_overview", Icons.Outlined.BarChart, Icons.Outlined.BarChart),
    SETTINGS("设置", "settings", Icons.Outlined.Person, Icons.Filled.Person)
}

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onTabClick: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MontraBackground)
            .navigationBarsPadding()
            .padding(horizontal = 21.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .shadow(12.dp, RoundedCornerShape(36.dp), ambientColor = Color.Black.copy(alpha = 0.03f), spotColor = Color.Black.copy(alpha = 0.03f))
                .clip(RoundedCornerShape(36.dp))
                .background(Color.White)
                .border(1.dp, MontraDivider, RoundedCornerShape(36.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomTab.entries.forEach { tab ->
                val isActive = isTabActive(currentRoute, tab)
                BottomNavItem(
                    tab = tab,
                    isActive = isActive,
                    onClick = { onTabClick(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun isTabActive(currentRoute: String?, tab: BottomTab): Boolean {
    if (currentRoute == null) return false
    return when (tab) {
        BottomTab.HOME -> currentRoute.startsWith("home") || currentRoute.startsWith("ledger")
        BottomTab.ADD_EXPENSE -> currentRoute.startsWith("add_expense")
        BottomTab.STATS -> currentRoute.startsWith("stats")
        BottomTab.SETTINGS -> currentRoute.startsWith("settings")
    }
}

@Composable
private fun BottomNavItem(
    tab: BottomTab,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(54.dp)
            .background(
                color = if (isActive) MontraPrimary else Color.Transparent,
                shape = RoundedCornerShape(26.dp)
            )
            .bounceClick(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isActive && tab != BottomTab.ADD_EXPENSE) tab.filledIcon else tab.icon,
            contentDescription = tab.label,
            modifier = Modifier.size(20.dp),
            tint = if (isActive) Color.White else MontraTextDisabled
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tab.label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isActive) Color.White else MontraTextDisabled,
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp
        )
    }
}
