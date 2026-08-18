package com.aa.ledger.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aa.ledger.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val nickname: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(loggedIn = authRepository.isLoggedIn))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateNickname(v: String) { _uiState.update { it.copy(nickname = v) } }
    fun updatePassword(v: String) { _uiState.update { it.copy(password = v) } }

    fun login() {
        val state = _uiState.value
        if (state.nickname.length < 2) {
            _uiState.update { it.copy(error = "昵称至少2个字符") }; return
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(error = "密码至少6位") }; return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.login(state.nickname, state.password)
            result.fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, loggedIn = true) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "登录失败") } }
            )
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.update { AuthUiState() }
    }
}
