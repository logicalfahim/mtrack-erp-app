package com.mtrack.installer.data.repository

import com.mtrack.installer.data.api.MTrackApiService
import com.mtrack.installer.data.api.RetrofitClient
import com.mtrack.installer.data.model.DashboardResponse
import com.mtrack.installer.data.model.LoginRequest
import com.mtrack.installer.data.model.LoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ErpRepository(
    private val api: MTrackApiService = RetrofitClient.apiService
) {

    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.login(LoginRequest(username = username, password = password))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.status == "success" && !body.token.isNullOrBlank()) {
                        Result.success(body)
                    } else {
                        Result.failure(Exception(body.message ?: "Authentication failed."))
                    }
                } else {
                    val errText = response.errorBody()?.string() ?: "Invalid credentials (Code: ${response.code()})"
                    Result.failure(Exception(errText))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getDashboard(token: String, month: String? = null): Result<DashboardResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = api.getErpDashboard(authHeader, month)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errText = response.errorBody()?.string() ?: "Failed to fetch dashboard (Code: ${response.code()})"
                    Result.failure(Exception(errText))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
