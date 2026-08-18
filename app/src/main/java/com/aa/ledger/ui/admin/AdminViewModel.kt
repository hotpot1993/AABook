package com.aa.ledger.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aa.ledger.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUser(
    val id: Int,
    val nickname: String,
    val createdAt: String,
    val isAdmin: Boolean = false
)

data class AdminUiState(
    val users: List<AdminUser> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.getAdminUsers().fold(
                onSuccess = { raw ->
                    val users = raw.map { u ->
                        AdminUser(
                            id = (u["id"] as? Double)?.toInt() ?: 0,
                            nickname = u["nickname"] as? String ?: "?",
                            createdAt = (u["createdAt"] as? String)?.take(10) ?: "",
                            isAdmin = u["nickname"] == "admin"
                        )
                    }
                    _uiState.update { it.copy(users = users, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun deleteUser(id: Int) {
        viewModelScope.launch {
            authRepository.deleteAdminUser(id).fold(
                onSuccess = { loadUsers() },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }
}
