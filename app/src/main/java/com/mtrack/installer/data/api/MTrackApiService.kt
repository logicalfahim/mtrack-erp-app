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
    ): Response<com.google.gson.JsonElement>

    @GET("api/v1/dashboard")
    suspend fun getDashboardGeneral(
        @Header("Authorization") bearerToken: String,
        @Query("month") month: String? = null
    ): Response<com.google.gson.JsonElement>

    @GET("api/v1/seller-dashboard")
    suspend fun getSellerDashboard(
        @Header("Authorization") bearerToken: String,
        @Query("month") month: String? = null
    ): Response<com.google.gson.JsonElement>

    @GET("api/v1/admin-dashboard")
    suspend fun getAdminDashboard(
        @Header("Authorization") bearerToken: String,
        @Query("month") month: String? = null
    ): Response<com.google.gson.JsonElement>

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
     * Staff Directory fallback endpoints with raw JsonElement parsing
     */
    @GET("api/v1/staffs")
    suspend fun getStaffs(
        @Header("Authorization") bearerToken: String
    ): Response<com.google.gson.JsonElement>

    @GET("api/v1/staff-directory")
    suspend fun getStaffDirectory(
        @Header("Authorization") bearerToken: String
    ): Response<com.google.gson.JsonElement>

    @GET("api/v1/staff")
    suspend fun getStaffSingle(
        @Header("Authorization") bearerToken: String
    ): Response<com.google.gson.JsonElement>

    @GET("api/v1/users")
    suspend fun getUsers(
        @Header("Authorization") bearerToken: String
    ): Response<com.google.gson.JsonElement>

    @GET("api/v1/employees")
    suspend fun getEmployees(
        @Header("Authorization") bearerToken: String
    ): Response<com.google.gson.JsonElement>

    /**
     * Sales & Installation Requests endpoints
     */
    @GET("api/v1/install-requests")
    suspend fun getInstallRequests(
        @Header("Authorization") bearerToken: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("ref_name") refName: String? = null,
        @Query("seller") seller: String? = null
    ): Response<com.google.gson.JsonElement>

    @GET("api/v1/sales")
    suspend fun getSales(
        @Header("Authorization") bearerToken: String,
        @Query("month") month: String? = null,
        @Query("seller") seller: String? = null,
        @Query("ref_name") refName: String? = null
    ): Response<com.google.gson.JsonElement>

    @GET("api/v1/vehicles")
    suspend fun getVehicles(
        @Header("Authorization") bearerToken: String,
        @Query("seller_id") sellerId: Int? = null,
        @Query("seller") seller: String? = null,
        @Query("ref_name") refName: String? = null,
        @Query("month") month: String? = null,
        @Query("limit") limit: Int = 100
    ): Response<com.google.gson.JsonElement>

    /**
     * Real-time notifications endpoints per ERP specification
     */
    @GET("api/notifications_check.php")
    suspend fun getNotificationsCheck(
        @Header("Authorization") bearerToken: String
    ): Response<com.google.gson.JsonElement>

    @POST("api/notifications_read_all.php")
    suspend fun markAllNotificationsRead(
        @Header("Authorization") bearerToken: String,
        @Body body: Map<String, Int>
    ): Response<com.google.gson.JsonElement>

    @GET("api/v1/notifications")
    suspend fun getNotifications(
        @Header("Authorization") bearerToken: String
    ): Response<com.google.gson.JsonElement>

    @GET("api/v1/staff-notifications")
    suspend fun getStaffNotifications(
        @Header("Authorization") bearerToken: String
    ): Response<com.google.gson.JsonElement>

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
