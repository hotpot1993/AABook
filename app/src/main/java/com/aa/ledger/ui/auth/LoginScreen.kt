package com.aa.ledger.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aa.ledger.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPw by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.loggedIn) {
        if (uiState.loggedIn) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("云端登录", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Text("同步到云端", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MontraTextPrimary)
            Text("登录后可在多设备间同步数据，与朋友协作记账", fontSize = 14.sp, color = MontraTextSecondary)

            Spacer(Modifier.height(12.dp))

            // Error
            uiState.error?.let { err ->
                Surface(shape = RoundedCornerShape(12.dp), color = MontraRed.copy(alpha = 0.1f)) {
                    Text(err, Modifier.padding(12.dp), color = MontraRed, fontSize = 13.sp)
                }
            }

            // Nickname
            OutlinedTextField(
                value = uiState.nickname,
                onValueChange = { viewModel.updateNickname(it) },
                label = { Text("昵称") },
                leadingIcon = { Icon(Icons.Outlined.Person, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MontraPrimary,
                    unfocusedBorderColor = MontraFill,
                    focusedContainerColor = MontraFill,
                    unfocusedContainerColor = MontraFill
                )
            )

            // Password
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.updatePassword(it) },
                label = { Text("密码") },
                leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { showPw = !showPw }) {
                        Icon(if (showPw) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
                    }
                },
                singleLine = true,
                visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MontraPrimary,
                    unfocusedBorderColor = MontraFill,
                    focusedContainerColor = MontraFill,
                    unfocusedContainerColor = MontraFill
                )
            )

            // Login button
            Button(
                onClick = { viewModel.login() },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MontraPrimary, contentColor = Color.White)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("登录 / 注册", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Text("首次输入昵称将自动注册账号", fontSize = 12.sp, color = MontraTextTertiary)
        }
    }
}
