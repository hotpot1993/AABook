package com.aa.ledger.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aa.ledger.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLedgerScreen(
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var memberInput by remember { mutableStateOf("") }
    var members by remember { mutableStateOf(listOf<String>()) }
    var selectedType by remember { mutableStateOf("green") }
    var selectedCurrency by remember { mutableStateOf("CNY") }
    var budgetAmount by remember { mutableStateOf("") }

    val ledgerTypes = listOf(
        Triple("green", "旅行", "✈️"),
        Triple("orange", "聚餐", "🍜"),
        Triple("blue", "合租", "🏠")
    )

    val commonCurrencies = listOf(
        "CNY" to "¥ 人民币",
        "USD" to "$ 美元",
        "EUR" to "€ 欧元",
        "JPY" to "¥ 日元",
        "KRW" to "₩ 韩元",
        "TWD" to "NT$ 新台币",
        "HKD" to "HK$ 港币",
        "GBP" to "£ 英镑"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建新账本", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MontraBackground)
            )
        },
        containerColor = MontraBackground
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Name field
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("账本名称", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MontraTextSecondary)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text("如：三亚旅行 2024", color = MontraTextTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MontraPrimary,
                        unfocusedBorderColor = MontraFill,
                        focusedContainerColor = MontraFill,
                        unfocusedContainerColor = MontraFill
                    )
                )
            }

            // Type selector
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("账本类型", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MontraTextSecondary)
                Row(
                    Modifier.fillMaxWidth().height(64.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ledgerTypes.forEach { (type, label, icon) ->
                        val isSelected = selectedType == type
                        Surface(
                            onClick = { selectedType = type },
                            shape = RoundedCornerShape(14.dp),
                            color = when {
                                isSelected -> MontraPrimary
                                type == "orange" -> WarningOrangeBg
                                type == "blue" -> InfoBlueBg
                                else -> GreenBg
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Column(
                                Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(icon, fontSize = 20.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = when {
                                        isSelected -> Color.White
                                        type == "orange" -> WarningOrange
                                        type == "blue" -> InfoBlue
                                        else -> MontraPrimary
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Budget field
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("预算金额（选填）", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MontraTextSecondary)
                OutlinedTextField(
                    value = budgetAmount,
                    onValueChange = { v -> budgetAmount = v.filter { it.isDigit() || it == '.' } },
                    singleLine = true,
                    placeholder = { Text("不设预算", color = MontraTextTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MontraPrimary,
                        unfocusedBorderColor = MontraFill,
                        focusedContainerColor = MontraFill,
                        unfocusedContainerColor = MontraFill
                    )
                )
            }

            // Member input
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("添加成员", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MontraTextSecondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = memberInput,
                        onValueChange = { memberInput = it },
                        singleLine = true,
                        placeholder = { Text("输入姓名", color = MontraTextTertiary) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MontraPrimary,
                            unfocusedBorderColor = MontraFill,
                            focusedContainerColor = MontraFill,
                            unfocusedContainerColor = MontraFill
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = {
                            if (memberInput.isNotBlank()) {
                                members = members + memberInput.trim()
                                memberInput = ""
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = MontraPrimary
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            "添加成员",
                            tint = Color.White,
                            modifier = Modifier.size(52.dp).padding(14.dp)
                        )
                    }
                }
                // Member tags
                if (members.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        members.forEachIndexed { idx, m ->
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(MontraFill.copy(alpha = 0.6f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("👤", fontSize = 12.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(m, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MontraPrimary)
                                Spacer(Modifier.weight(1f))
                                IconButton(
                                    onClick = { members = members.filterIndexed { i, _ -> i != idx } },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Outlined.Close, "移除", modifier = Modifier.size(14.dp), tint = MontraTextSecondary)
                                }
                            }
                        }
                    }
                }
                if (members.isEmpty()) {
                    Text("请至少添加一位成员", fontSize = 13.sp, color = MontraRed)
                }
            }

            // Currency selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("记账币种", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MontraTextSecondary)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    commonCurrencies.take(4).forEach { (code, label) ->
                        val isSel = selectedCurrency == code
                        Surface(
                            onClick = { selectedCurrency = code },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) MontraPrimary else MontraFill
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSel) Color.White else MontraTextSecondary
                            )
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    commonCurrencies.drop(4).forEach { (code, label) ->
                        val isSel = selectedCurrency == code
                        Surface(
                            onClick = { selectedCurrency = code },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) MontraPrimary else MontraFill
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSel) Color.White else MontraTextSecondary
                            )
                        }
                    }
                }
            }

            // Create button
            Button(
                onClick = {
                    viewModel.createLedger(
                        name, "", selectedType, selectedCurrency,
                        budgetAmount.toDoubleOrNull() ?: 0.0, members
                    )
                    onBack()
                },
                enabled = name.isNotBlank() && members.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MontraPrimary, contentColor = Color.White)
            ) {
                Text("创建账本", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
