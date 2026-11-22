package com.mtrack.installer.data.repository

import com.mtrack.installer.data.api.MTrackApiService
import com.mtrack.installer.data.api.RetrofitClient
import com.mtrack.installer.data.model.ApiResponse
import com.mtrack.installer.data.model.InstallSubmissionResult
import com.mtrack.installer.data.model.InstallationRequestPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstallRepository(
    private val api: MTrackApiService = RetrofitClient.apiService
) {

    suspend fun submitRequest(payload: InstallationRequestPayload): Result<ApiResponse<InstallSubmissionResult>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.submitInstallationRequest(payload)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.status == "success" || response.code() in 200..201) {
                        Result.success(body)
                    } else {
                        Result.failure(Exception(body.message ?: "Server reported an error."))
                    }
                } else {
                    val errString = response.errorBody()?.string() ?: "Network error (Code: ${response.code()})"
                    Result.failure(Exception(errString))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun trackStatus(phone: String): Result<ApiResponse<List<Map<String, Any>>>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getRequestsByPhone(phone)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to fetch records."))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
