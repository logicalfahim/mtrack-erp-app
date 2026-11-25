package com.mtrack.installer.data.api

import com.mtrack.installer.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface MTrackApiService {

    /**
     * User Authentication & Bearer Token Generation
     */
    @POST("api/v1/auth-login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    /**
     * Universal Role-Based ERP Departmental Dashboard
     */
    @GET("api/v1/erp-dashboard")
    suspend fun getErpDashboard(
        @Header("Authorization") bearerToken: String,
        @Query("month") month: String? = null
    ): Response<DashboardResponse>

    /**
     * Submit single/multi-vehicle installation onboarding request
     */
    @POST("api/v1/install-requests")
    suspend fun submitInstallationRequest(
        @Body payload: InstallationRequestPayload,
        @Header("X-API-KEY") apiKey: String? = null
    ): Response<ApiResponse<InstallSubmissionResult>>

    /**
     * Synchronize live staff GPS coordinates & device battery telematics
     */
    @POST("api/v1/update-location")
    suspend fun updateStaffLocation(
        @Header("Authorization") bearerToken: String,
        @Body request: LocationUpdateRequest
    ): Response<LocationUpdateResponse>

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
