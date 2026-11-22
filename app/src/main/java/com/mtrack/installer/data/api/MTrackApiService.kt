package com.mtrack.installer.data.api

import com.mtrack.installer.data.model.ApiResponse
import com.mtrack.installer.data.model.InstallSubmissionResult
import com.mtrack.installer.data.model.InstallationRequestPayload
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface MTrackApiService {

    /**
     * Submit single/multi-vehicle installation onboarding request
     */
    @POST("api/v1/install-requests")
    suspend fun submitInstallationRequest(
        @Body payload: InstallationRequestPayload,
        @Header("X-API-KEY") apiKey: String? = null
    ): Response<ApiResponse<InstallSubmissionResult>>

    /**
     * Track status of requests by contact number
     */
    @GET("api/v1/install-requests")
    suspend fun getRequestsByPhone(
        @Query("contact_number") phone: String,
        @Query("limit") limit: Int = 10,
        @Header("X-API-KEY") apiKey: String? = null
    ): Response<ApiResponse<List<Map<String, Any>>>>
}
