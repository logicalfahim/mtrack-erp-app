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

    suspend fun login(userInput: String, password: String): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val trimmedInput = userInput.trim()
                val isEmail = trimmedInput.contains("@")
                val isPhone = trimmedInput.all { it.isDigit() || it == '+' }

                val request = LoginRequest(
                    username = trimmedInput,
                    password = password,
                    phone = if (isPhone) trimmedInput else null,
                    email = if (isEmail) trimmedInput else null
                )
                val response = api.login(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.status == "success" && !body.token.isNullOrBlank()) {
                        Result.success(body)
                    } else {
                        Result.failure(Exception(body.message ?: "Authentication failed."))
                    }
                } else {
                    val rawErr = response.errorBody()?.string()
                    val cleanMsg = parseErrorMessage(rawErr, response.code())
                    Result.failure(Exception(cleanMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseErrorMessage(rawErr: String?, statusCode: Int): String {
        if (rawErr.isNullOrBlank()) return "Authentication failed (Code: $statusCode)"
        return try {
            val json = com.google.gson.JsonParser.parseString(rawErr).asJsonObject
            when {
                json.has("message") && !json.get("message").isJsonNull -> json.get("message").asString
                json.has("error") && !json.get("error").isJsonNull -> json.get("error").asString
                else -> rawErr
            }
        } catch (e: Exception) {
            rawErr
        }
    }

    suspend fun getDashboard(token: String, month: String? = null): Result<DashboardResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = api.getErpDashboard(authHeader, month)
                if (response.isSuccessful && response.body() != null) {
                    var dashResponse = response.body()!!
                    var currentData = dashResponse.data ?: com.mtrack.installer.data.model.ErpDashboardData()

                    // Fallback for Staff Directory if empty in main response
                    if (currentData.staffDirectory.isEmpty()) {
                        try {
                            val staffResp = api.getStaffDirectory(authHeader)
                            if (staffResp.isSuccessful && !staffResp.body()?.data.isNullOrEmpty()) {
                                currentData = currentData.copy(staffDirectory = staffResp.body()!!.data!!)
                            } else {
                                val altStaff = api.getStaffs(authHeader)
                                if (altStaff.isSuccessful && !altStaff.body()?.data.isNullOrEmpty()) {
                                    currentData = currentData.copy(staffDirectory = altStaff.body()!!.data!!)
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    // Fallback for Sales Department if missing in main response
                    if (currentData.salesDepartment == null) {
                        try {
                            val salesResp = api.getSales(authHeader, month)
                            if (salesResp.isSuccessful && salesResp.body()?.data != null) {
                                currentData = currentData.copy(salesDepartment = salesResp.body()!!.data)
                            }
                        } catch (_: Exception) {}
                    }

                    dashResponse = dashResponse.copy(data = currentData)
                    Result.success(dashResponse)
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
