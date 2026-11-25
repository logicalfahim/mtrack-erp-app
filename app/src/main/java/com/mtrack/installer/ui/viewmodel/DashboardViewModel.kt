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

    fun loadDashboard(month: String? = null) {
        val token = sessionManager.getToken() ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null, selectedMonth = month ?: "") }

        viewModelScope.launch {
            val result = repository.getDashboard(token, month)
            result.fold(
                onSuccess = { res ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userProfile = res.user ?: it.userProfile,
                            dashboardData = res.data,
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
}
