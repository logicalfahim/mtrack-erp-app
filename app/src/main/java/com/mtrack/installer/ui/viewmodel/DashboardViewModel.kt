package com.mtrack.installer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mtrack.installer.data.local.SessionManager
import com.mtrack.installer.data.model.ErpDashboardData
import com.mtrack.installer.data.model.UserProfile
import com.mtrack.installer.data.repository.ErpRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userProfile: UserProfile? = null,
    val dashboardData: ErpDashboardData? = null,
    val notifications: List<com.mtrack.installer.data.model.StaffNotificationItem> = emptyList(),
    val selectedMonth: String = ""
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val repository = ErpRepository()

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            userProfile = sessionManager.getUser()
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun onUserLoggedIn(newUser: UserProfile?) {
        val user = newUser ?: sessionManager.getUser()
        _uiState.update {
            it.copy(
                userProfile = user,
                dashboardData = null,
                isLoading = true,
                errorMessage = null
            )
        }
        loadDashboard()
    }

    fun clearSession() {
        _uiState.value = DashboardUiState()
    }

    fun loadDashboard(month: String? = null) {
        val token = sessionManager.getToken() ?: sessionManager.getAuthToken() ?: return
        val currentUser = sessionManager.getUser()
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                userProfile = currentUser ?: it.userProfile,
                selectedMonth = month ?: ""
            )
        }

        viewModelScope.launch {
            val result = repository.getDashboard(token, month)
            result.fold(
                onSuccess = { res ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userProfile = res.user ?: currentUser ?: it.userProfile,
                            dashboardData = res.data,
                            notifications = res.notifications,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.localizedMessage ?: "Failed to load dashboard metrics."
                        )
                    }
                }
            )
        }
    }

    fun markNotificationAsRead(id: String) {
        _uiState.update { state ->
            state.copy(
                notifications = state.notifications.map {
                    if (it.id == id) it.copy(isRead = true) else it
                }
            )
        }
    }

    fun markAllNotificationsAsRead() {
        _uiState.update { state ->
            state.copy(
                notifications = state.notifications.map { it.copy(isRead = true) }
            )
        }
    }
}
