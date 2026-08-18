package com.aa.ledger.ui.member

import android.net.Uri; import androidx.activity.compose.rememberLauncherForActivityResult; import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background; import androidx.compose.foundation.layout.*; import androidx.compose.foundation.lazy.LazyColumn; import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape; import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons; import androidx.compose.material.icons.filled.*; import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Alignment; import androidx.compose.ui.Modifier; import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color; import androidx.compose.ui.layout.ContentScale; import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight; import androidx.compose.ui.unit.dp; import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider; import androidx.hilt.navigation.compose.hiltViewModel; import coil.compose.AsyncImage; import com.aa.ledger.ui.common.DeleteConfirmDialog
import com.aa.ledger.ui.theme.*; import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberManageScreen(ledgerId: Long, onBack: () -> Unit, viewModel: MemberManageViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState(); val ctx = LocalContext.current; var photoUri by remember { mutableStateOf<Uri?>(null) }; var tid by remember { mutableStateOf<Long?>(null) }
    var deleteTarget by remember { mutableStateOf<com.aa.ledger.data.local.entity.MemberEntity?>(null) }
    val cam = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok && photoUri != null && tid != null) viewModel.updateMemberAvatar(tid!!, photoUri!!) }
    Scaffold(topBar = { TopAppBar(title = { Text("成员管理", fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = (-0.5).sp) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } }, actions = { Surface(onClick = { viewModel.showAddDialog() }, shape = RoundedCornerShape(100.dp), color = MontraPrimary) { Row(Modifier.padding(horizontal = 16.dp, vertical = 0.dp).height(36.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Add, "添加", tint = Color.White, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(6.dp)); Text("添加成员", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MontraBackground)) }, containerColor = MontraBackground) { padding ->
        if (uiState.isLoading) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MontraPrimary) }
        else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("成员列表".uppercase(), Modifier.padding(horizontal = 4.dp), fontSize = 13.sp, color = MontraTextSecondary, fontWeight = FontWeight.Medium); Spacer(Modifier.height(8.dp)) }
            item { Surface(shape = RoundedCornerShape(20.dp), color = MontraSurface, shadowElevation = 1.dp) { Column {
                uiState.members.forEachIndexed { i, m -> Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp)) { if (m.avatarUri != null) AsyncImage(model = m.avatarUri, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop) else Surface(Modifier.size(40.dp), shape = CircleShape, color = MontraPrimary.copy(alpha = 0.12f)) { Box(contentAlignment = Alignment.Center) { Text(m.name.take(1), color = MontraPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) } }
                        IconButton(onClick = { tid = m.id; val f = File(ctx.cacheDir, "avatar_${System.currentTimeMillis()}.jpg"); photoUri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f); cam.launch(photoUri!!) }, modifier = Modifier.align(Alignment.BottomEnd).size(20.dp)) { Icon(Icons.Filled.CameraAlt, "拍照", Modifier.size(12.dp), tint = MontraPrimary) } }
                    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(m.name, fontWeight = FontWeight.Medium, color = MontraTextPrimary); if (m.nickname.isNotBlank()) Text(m.nickname, fontSize = 13.sp, color = MontraTextSecondary) }
                    // Edit button
                    Surface(
                        onClick = { viewModel.showEditDialog(m) },
                        shape = RoundedCornerShape(10.dp),
                        color = MontraFill
                    ) {
                        Icon(Icons.Outlined.Edit, "编辑", Modifier.size(34.dp).padding(8.dp), tint = MontraTextSecondary)
                    }
                    Spacer(Modifier.width(6.dp))
                    // Delete button
                    Surface(
                        onClick = { viewModel.requestDeleteMember(m) },
                        shape = RoundedCornerShape(10.dp),
                        color = MontraRed.copy(alpha = 0.1f)
                    ) {
                        Icon(Icons.Outlined.Delete, "删除", Modifier.size(34.dp).padding(8.dp), tint = MontraRed)
                    }
                }; if (i < uiState.members.lastIndex) HorizontalDivider(Modifier.padding(start = 56.dp), color = MontraDivider) }
            } } }
        }

        // Add/Edit dialog
        if (uiState.showAddDialog) {
            val isEditing = uiState.editingMember != null
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                shape = RoundedCornerShape(20.dp),
                title = { Text(if (isEditing) "编辑成员" else "添加成员", fontWeight = FontWeight.SemiBold) },
                text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(uiState.newMemberName, { viewModel.updateNewMemberName(it) }, label = { Text("姓名") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(uiState.newMemberNickname, { viewModel.updateNewMemberNickname(it) }, label = { Text("昵称（选填）") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    if (!isEditing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = uiState.sharePrevious,
                                onCheckedChange = { viewModel.updateSharePrevious(it) },
                                colors = CheckboxDefaults.colors(checkedColor = MontraPrimary)
                            )
                            Text("分担之前的消费", fontSize = 14.sp, color = MontraTextSecondary)
                        }
                    }
                } },
                confirmButton = { Button(onClick = { viewModel.addOrUpdateMember() }, enabled = uiState.newMemberName.isNotBlank(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MontraPrimary, contentColor = Color.White)) { Text(if (isEditing) "保存" else "添加", fontWeight = FontWeight.SemiBold) } },
                dismissButton = { Button(onClick = { viewModel.dismissDialog() }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MontraFill, contentColor = MontraTextSecondary)) { Text("取消") } }
            )
        }

        // Delete warning dialog (has related records)
        if (uiState.showDeleteWarning && uiState.pendingDeleteMember != null) {
            val m = uiState.pendingDeleteMember!!
            DeleteConfirmDialog(
                title = "删除成员",
                message = "确认删除「${m.nickname.ifEmpty { m.name }}」？${uiState.pendingDeleteInfo}",
                onConfirm = { viewModel.confirmDelete() },
                onDismiss = { viewModel.dismissDeleteWarning() }
            )
        }
    }
}