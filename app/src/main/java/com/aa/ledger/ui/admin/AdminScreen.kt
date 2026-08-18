package com.aa.ledger.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aa.ledger.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadUsers() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账号管理", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
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
                Text(uiState.error!!, color = MontraRed)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("共 ${uiState.users.size} 个用户", fontSize = 13.sp, color = MontraTextSecondary)
                    Spacer(Modifier.height(8.dp))
                }
                items(uiState.users) { user ->
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MontraSurface
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    user.nickname,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = if (user.isAdmin) MontraRed else MontraTextPrimary
                                )
                                Text(
                                    "ID: ${user.id} · ${user.createdAt}",
                                    fontSize = 12.sp,
                                    color = MontraTextSecondary
                                )
                            }
                            if (!user.isAdmin) {
                                Surface(
                                    onClick = { viewModel.deleteUser(user.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MontraRed.copy(alpha = 0.1f)
                                ) {
                                    Icon(Icons.Outlined.Delete, "删除", Modifier.size(32.dp).padding(7.dp), tint = MontraRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
