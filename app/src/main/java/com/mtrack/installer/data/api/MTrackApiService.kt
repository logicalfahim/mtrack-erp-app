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
    @POST("api/v1/staff-tracking")
    suspend fun recordStaffTracking(
        @Header("Authorization") bearerToken: String,
        @Body request: LocationUpdateRequest
    ): Response<LocationUpdateResponse>

    @POST("api/v1/staff_tracking")
    suspend fun recordStaffTrackingUnderscore(
        @Header("Authorization") bearerToken: String,
        @Body request: LocationUpdateRequest
    ): Response<LocationUpdateResponse>

    @POST("api/v1/update-location")
    suspend fun updateStaffLocation(
        @Header("Authorization") bearerToken: String,
        @Body request: LocationUpdateRequest
    ): Response<LocationUpdateResponse>

    @POST("api/v1/staff-location")
    suspend fun recordStaffLocation(
        @Header("Authorization") bearerToken: String,
        @Body request: LocationUpdateRequest
    ): Response<LocationUpdateResponse>

    /**
     * Staff Directory fallback endpoint
     */
    @GET("api/v1/staffs")
    suspend fun getStaffs(
        @Header("Authorization") bearerToken: String
    ): Response<ApiResponse<List<StaffMemberItem>>>

    @GET("api/v1/staff-directory")
    suspend fun getStaffDirectory(
        @Header("Authorization") bearerToken: String
    ): Response<ApiResponse<List<StaffMemberItem>>>

    /**
     * Sales metrics fallback endpoint
     */
    @GET("api/v1/sales")
    suspend fun getSales(
        @Header("Authorization") bearerToken: String,
        @Query("month") month: String? = null
    ): Response<ApiResponse<SellerDashboardMetrics>>

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
