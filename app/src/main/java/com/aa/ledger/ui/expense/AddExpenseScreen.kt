package com.aa.ledger.ui.expense

import android.Manifest; import android.net.Uri; import androidx.activity.compose.rememberLauncherForActivityResult; import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*; import androidx.compose.foundation.layout.*; import androidx.compose.foundation.lazy.LazyRow; import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape; import androidx.compose.foundation.shape.RoundedCornerShape; import androidx.compose.foundation.text.BasicTextField; import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons; import androidx.compose.material.icons.automirrored.filled.ArrowBack; import androidx.compose.material.icons.filled.*; import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Alignment; import androidx.compose.ui.Modifier; import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color; import androidx.compose.ui.layout.ContentScale; import androidx.compose.ui.platform.LocalContext; import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily; import androidx.compose.ui.text.font.FontWeight; import androidx.compose.ui.text.input.KeyboardType; import androidx.compose.ui.unit.dp; import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat; import androidx.core.content.FileProvider; import androidx.hilt.navigation.compose.hiltViewModel
import com.aa.ledger.data.repository.CurrencyGroup; import com.aa.ledger.domain.model.ShareType; import com.aa.ledger.ui.common.bounceClick; import com.aa.ledger.ui.theme.*; import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseScreen(ledgerId: Long? = null, expenseId: Long? = null, onBack: () -> Unit, onSaved: () -> Unit, viewModel: AddExpenseViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState(); val ctx = LocalContext.current; var pUri by remember { mutableStateOf<Uri?>(null) }
    val cam = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok && pUri != null) viewModel.updateReceiptUri(pUri) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok && pUri != null) cam.launch(pUri!!) }
    val gal = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { viewModel.updateReceiptUri(it) } }
    LaunchedEffect(uiState.savedSuccessfully) { if (uiState.savedSuccessfully) onSaved() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "编辑消费" else "添加账单", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    Box(modifier = Modifier.padding(end = 16.dp)) {
                        Surface(
                        modifier = Modifier.bounceClick(enabled = !uiState.isSaving, onClick = { viewModel.saveExpense() }),
                        shape = RoundedCornerShape(100.dp),
                        color = MontraPrimary
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp).height(36.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.isSaving) CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text("保存", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MontraBackground)
            )
        },
        containerColor = MontraBackground
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            uiState.error?.let { err -> Surface(shape = RoundedCornerShape(12.dp), color = MontraRed.copy(alpha = 0.1f)) { Text(err, Modifier.padding(12.dp), color = MontraRed) } }
            if (ledgerId == null) LedgerSelector(ledgers = uiState.allLedgers, selectedId = uiState.selectedLedgerId, onSelect = { viewModel.selectLedger(it) })

            // ═══ 卡片 1：外币换算 ═══
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MontraSurface), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(Modifier.padding(16.dp)) { Text("金额", style = MaterialTheme.typography.labelMedium, color = MontraTextSecondary); Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { BasicTextField(value = uiState.amount, onValueChange = { viewModel.updateAmount(it) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = MontraTextPrimary), decorationBox = { inner -> Box { if (uiState.amount.isEmpty()) Text("0.00", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = MontraTextSecondary); inner() } }) }
                        Spacer(Modifier.width(8.dp)); var ce by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = ce, onExpandedChange = { ce = it }) {
                            OutlinedTextField(value = uiState.selectedCurrency, onValueChange = {}, readOnly = true, singleLine = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(ce) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).width(110.dp), textStyle = MaterialTheme.typography.bodySmall, shape = RoundedCornerShape(12.dp))
                            ExposedDropdownMenu(expanded = ce, onDismissRequest = { ce = false }, modifier = Modifier.width(240.dp).heightIn(max = 320.dp)) { uiState.availableCurrencies.forEach { g: CurrencyGroup -> HorizontalDivider(color = MontraDivider); Text(g.region, Modifier.padding(horizontal = 16.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = MontraPrimary, fontWeight = FontWeight.Bold); g.currencies.forEach { p -> DropdownMenuItem(text = { Row { Text(p.first, fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp)); Text(p.second, color = MontraTextSecondary) } }, onClick = { viewModel.updateCurrency(p.first); ce = false }) } } }
                        }
                    }
                    if (uiState.selectedCurrency != "CNY") Text("≈ ¥${"%.2f".format(uiState.convertedAmountCny)} · 汇率 ${"%.4f".format(uiState.exchangeRate)}", fontSize = 12.sp, color = MontraTextSecondary)
                }
            }

            // ═══ 卡片 2：基础信息 ═══
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MontraSurface), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column { Text("消费名称", style = MaterialTheme.typography.labelMedium, color = MontraTextSecondary); Spacer(Modifier.height(4.dp)); Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MontraFill).padding(horizontal = 12.dp, vertical = 14.dp)) { if (uiState.title.isEmpty()) Text("输入消费名称", color = MontraTextSecondary, fontSize = 16.sp); BasicTextField(value = uiState.title, onValueChange = { viewModel.updateTitle(it) }, singleLine = true, textStyle = TextStyle(fontSize = 16.sp, color = MontraTextPrimary), modifier = Modifier.fillMaxWidth()) } }
                    Column { Text("备注（选填）", style = MaterialTheme.typography.labelMedium, color = MontraTextSecondary); Spacer(Modifier.height(4.dp)); Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MontraFill).padding(horizontal = 12.dp, vertical = 14.dp)) { if (uiState.note.isEmpty()) Text("备注信息", color = MontraTextSecondary, fontSize = 16.sp); BasicTextField(value = uiState.note, onValueChange = { viewModel.updateNote(it) }, singleLine = true, textStyle = TextStyle(fontSize = 16.sp, color = MontraTextPrimary), modifier = Modifier.fillMaxWidth()) } }
                }
            }

            // ═══ 卡片 3：小票照片 ═══
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MontraSurface), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("小票照片", style = MaterialTheme.typography.labelMedium, color = MontraTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { val f = java.io.File(ctx.cacheDir, "receipt_tmp_${System.currentTimeMillis()}.jpg"); pUri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f); if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) cam.launch(pUri!!) else perm.launch(Manifest.permission.CAMERA) }, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MontraPrimary)) { Text("拍照", color = MontraPrimary) }; OutlinedButton(onClick = { gal.launch("image/*") }, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MontraPrimary)) { Text("相册", color = MontraPrimary) } }
                    uiState.receiptUri?.let { uri ->
                        var showPreview by remember { mutableStateOf(false) }
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MontraFill).padding(8.dp).bounceClick { showPreview = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            coil.compose.AsyncImage(model = uri, contentDescription = "小票", modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("✅ 已选择小票", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MontraPrimary)
                                Text("点击查看大图", fontSize = 11.sp, color = MontraTextSecondary)
                                if (uiState.isEditing) { TextButton(onClick = { viewModel.runOcrOnExisting(uri) }, contentPadding = PaddingValues(0.dp)) { Text("重新识别金额", fontSize = 12.sp, color = InfoBlue) } }
                            }
                            IconButton(onClick = { viewModel.updateReceiptUri(null) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.Close, "移除照片", Modifier.size(18.dp), tint = MontraRed) }
                        }
                        // Full-screen preview dialog
                        if (showPreview) {
                            AlertDialog(
                                onDismissRequest = { showPreview = false },
                                shape = RoundedCornerShape(16.dp),
                                confirmButton = {},
                                dismissButton = { TextButton(onClick = { showPreview = false }) { Text("关闭") } },
                                text = {
                                    coil.compose.AsyncImage(
                                        model = uri,
                                        contentDescription = "小票大图",
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // ═══ 卡片 4：分类 ═══
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MontraSurface), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("消费分类", style = MaterialTheme.typography.labelMedium, color = MontraTextSecondary)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { viewModel.categories.forEach { cat -> val lbl = if (cat == "__custom__") "自定义" else cat; val sel = uiState.selectedCategory == cat; ChipTag(lbl, sel) { viewModel.updateCategory(cat) } } }
                    if (uiState.selectedCategory == "__custom__") Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MontraFill).padding(horizontal = 12.dp, vertical = 14.dp)) { if (uiState.customCategory.isEmpty()) Text("自定义分类名称", color = MontraTextSecondary); BasicTextField(value = uiState.customCategory, onValueChange = { viewModel.updateCustomCategory(it) }, singleLine = true, textStyle = TextStyle(fontSize = 16.sp, color = MontraTextPrimary), modifier = Modifier.fillMaxWidth()) }
                }
            }

            // ═══ 卡片 4：人员+分摊 ═══
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MontraSurface), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("付款人", style = MaterialTheme.typography.labelMedium, color = MontraTextSecondary)
                    if (uiState.members.isEmpty()) Text("请先选择账本", fontSize = 13.sp, color = MontraTextSecondary)
                    else { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(uiState.members) { m -> IosMemberChip(name = m.nickname.ifEmpty { m.name }, selected = uiState.paidByMemberId == m.id, onClick = { viewModel.updatePaidByMember(m.id) }) } }; Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = uiState.paidForAll, onCheckedChange = { viewModel.updatePaidForAll(it) }, colors = CheckboxDefaults.colors(checkedColor = MontraPrimary)); Text(if (uiState.paidForAll) "付款人也参与消费" else "付款人代付", fontSize = 15.sp, color = MontraTextSecondary) } }
                    HorizontalDivider(color = MontraDivider)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("参与人", style = MaterialTheme.typography.labelMedium, color = MontraTextSecondary); Row { TextButton(onClick = { viewModel.selectAllMembers() }) { Text("全选", color = MontraPrimary) }; TextButton(onClick = { viewModel.invertMemberSelection() }) { Text("反选", color = MontraPrimary) } } }
                    if (uiState.members.isEmpty()) Text("请先选择账本", fontSize = 13.sp, color = MontraTextSecondary)
                    else LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(uiState.members) { m -> IosMemberChip(name = m.nickname.ifEmpty { m.name }, selected = m.id in uiState.selectedMemberIds, onClick = { viewModel.toggleMember(m.id) }) } }
                    HorizontalDivider(color = MontraDivider)
                    Text("分摊方式", style = MaterialTheme.typography.labelMedium, color = MontraTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ShareType.entries.forEach { t -> ChipTag(t.label, uiState.shareType == t) { viewModel.updateShareType(t) } } }
                    when (uiState.shareType) { ShareType.SHARES -> uiState.selectedMemberIds.toList().forEach { mid -> val m = uiState.members.find { it.id == mid } ?: return@forEach; val v = uiState.shareValues[mid] ?: 1.0; ShareRow(name = m.nickname.ifEmpty { m.name }, value = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString(), placeholder = "份数", onChange = { viewModel.updateShareValue(mid, it.toDoubleOrNull() ?: 0.0) }) }; ShareType.CUSTOM -> uiState.selectedMemberIds.toList().forEach { mid -> val m = uiState.members.find { it.id == mid } ?: return@forEach; val v = uiState.shareValues[mid] ?: 0.0; ShareRow(name = m.nickname.ifEmpty { m.name }, value = if (v == 0.0) "" else "%.2f".format(v), placeholder = "金额(¥)", onChange = { viewModel.updateShareValue(mid, it.toDoubleOrNull() ?: 0.0) }) }; ShareType.PERCENTAGE -> uiState.selectedMemberIds.toList().forEach { mid -> val m = uiState.members.find { it.id == mid } ?: return@forEach; val v = uiState.shareValues[mid] ?: 0.0; ShareRow(name = m.nickname.ifEmpty { m.name }, value = if (v == 0.0) "" else "%.1f".format(v), placeholder = "%", onChange = { viewModel.updateShareValue(mid, it.toDoubleOrNull() ?: 0.0) }) }; else -> {} }
                }
            }
            Spacer(Modifier.height(32.dp))
        }

        // ── OCR 复核对话框 ──
        if (uiState.showOcrReview) {
            var editAmount by remember(uiState.ocrReviewAmount) { mutableStateOf(uiState.ocrReviewAmount) }
            AlertDialog(
                onDismissRequest = { viewModel.dismissOcrReview() },
                shape = RoundedCornerShape(20.dp),
                title = { Text("确认金额", fontWeight = FontWeight.SemiBold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("小票已拍摄，请确认或修改下方金额：", color = MontraTextSecondary, fontSize = 14.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editAmount,
                                onValueChange = { editAmount = it },
                                label = { Text("实际金额") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            )
                            Button(
                                onClick = { viewModel.confirmOcrAmount(editAmount) },
                                modifier = Modifier.padding(top = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MontraPrimary)
                            ) { Text("确认", fontWeight = FontWeight.SemiBold) }
                        }
                        if (uiState.ocrReviewAmount.isEmpty()) {
                            Text("未自动识别到金额，请手动输入", color = MontraRed, fontSize = 12.sp)
                        } else {
                            Text(
                                "已识别：¥${uiState.ocrReviewAmount}" + (uiState.ocrReviewCurrency?.let { "（$it）" } ?: ""),
                                color = MontraPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    Button(
                        onClick = { viewModel.dismissOcrReview() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MontraFill, contentColor = MontraTextSecondary)
                    ) { Text("放弃", fontWeight = FontWeight.Medium) }
                }
            )
        }
    }
}

@Composable fun IosMemberChip(name: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MontraPrimary else MontraSurface
    val border = if (selected) MontraPrimary else MontraDivider
    val shape = RoundedCornerShape(20.dp)
    Box(Modifier.clip(shape).background(bg).border(1.dp, border, shape).bounceClick(onClick = onClick).padding(start = 6.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(if (selected) Color.White.copy(alpha = 0.3f) else MontraFill), contentAlignment = Alignment.Center) {
                if (selected) Icon(Icons.Filled.Check, null, Modifier.size(14.dp), tint = Color.White)
                else Text(name.take(1), fontSize = 11.sp, color = MontraTextSecondary)
            }
            Spacer(Modifier.width(6.dp))
            Text(name, color = if (selected) Color.White else MontraTextPrimary, style = MaterialTheme.typography.labelLarge)
        }
    }
}
@Composable fun ShareRow(name: String, value: String, placeholder: String, onChange: (String) -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(name, Modifier.width(80.dp), color = MontraTextPrimary); Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MontraFill).padding(horizontal = 12.dp, vertical = 14.dp)) { if (value.isEmpty()) Text(placeholder, color = MontraTextSecondary); BasicTextField(value = value, onValueChange = onChange, singleLine = true, textStyle = TextStyle(fontSize = 16.sp, color = MontraTextPrimary)) } } }

/** 完全填充的胶囊标签 — 选中时实心蓝色+白字，未选中白底灰边 */
@Composable fun ChipTag(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val bg = if (selected) MontraPrimary else MontraSurface
    val bd = if (selected) MontraPrimary else MontraDivider
    Box(Modifier.clip(shape).background(bg).border(1.dp, bd, shape).bounceClick(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)) {
        Text(label, color = if (selected) Color.White else MontraTextPrimary, style = MaterialTheme.typography.labelLarge)
    }
}
@OptIn(ExperimentalMaterial3Api::class) @Composable fun LedgerSelector(ledgers: List<com.aa.ledger.domain.model.Ledger>, selectedId: Long?, onSelect: (Long) -> Unit) { var e by remember { mutableStateOf(false) }; val sel = ledgers.find { it.id == selectedId }; ExposedDropdownMenuBox(expanded = e, onExpandedChange = { e = it }) { OutlinedTextField(value = sel?.name ?: "选择账本", onValueChange = {}, readOnly = true, label = { Text("所属账本") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(e) }, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable), shape = RoundedCornerShape(12.dp)); ExposedDropdownMenu(expanded = e, onDismissRequest = { e = false }) { ledgers.forEach { l -> DropdownMenuItem(text = { Text(l.name) }, onClick = { onSelect(l.id); e = false }) } } } }
