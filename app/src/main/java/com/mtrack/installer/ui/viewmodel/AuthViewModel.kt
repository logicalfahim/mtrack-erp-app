package com.mtrack.installer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mtrack.installer.data.local.SessionManager
import com.mtrack.installer.data.model.UserProfile
import com.mtrack.installer.data.repository.ErpRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val usernameInput: String = "",
    val passwordInput: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val currentUser: UserProfile? = null
) {
    val isAuthenticated: Boolean get() = isLoggedIn
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val repository = ErpRepository()

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isLoggedIn = sessionManager.isLoggedIn(),
            currentUser = sessionManager.getUser()
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onUsernameChange(input: String) {
        _uiState.update { it.copy(usernameInput = input, errorMessage = null) }
    }

    fun onPasswordChange(input: String) {
        _uiState.update { it.copy(passwordInput = input, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun submitLogin() = login()

    fun login() {
        val username = _uiState.value.usernameInput.trim()
        val password = _uiState.value.passwordInput.trim()

        if (username.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter both Username/Phone and Password.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.login(username, password)
            result.fold(
                onSuccess = { res ->
                    val token = res.token ?: ""
                    val user = res.user
                    if (user != null && token.isNotEmpty()) {
                        sessionManager.saveAuth(token, user)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                currentUser = user,
                                passwordInput = "",
                                errorMessage = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = res.message ?: "Authentication failed."
                            )
                        }
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.localizedMessage ?: "Network error. Please check server."
                        )
                    }
                }
            )
        }
    }

    fun logout() {
        sessionManager.logout()
        _uiState.update {
            AuthUiState(
                isLoggedIn = false,
                currentUser = null
            )
        }
    }
}
