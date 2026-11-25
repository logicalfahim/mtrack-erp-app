package com.mtrack.installer.data.model

import com.google.gson.annotations.SerializedName

/**
 * Authentication Credentials Payload
 */
data class LoginRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String
)

/**
 * Logged-in User Profile Information
 */
data class UserProfile(
    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("username")
    val username: String,

    @SerializedName("full_name")
    val fullName: String,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("role")
    val role: String,

    @SerializedName("designation")
    val designation: String? = "Staff",

    @SerializedName("department")
    val department: String? = "General",

    @SerializedName("profile_picture")
    val profilePicture: String? = null
)

/**
 * Login Response returned by /api/v1/auth-login
 */
data class LoginResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("code")
    val code: Int,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("token")
    val token: String? = null,

    @SerializedName("user")
    val user: UserProfile? = null
)

/**
 * Sold Vehicle Item in Seller's Fleet
 */
data class SoldVehicleItem(
    @SerializedName("id")
    val id: Int,

    @SerializedName("vehicle_plate_number")
    val vehiclePlateNumber: String,

    @SerializedName("vehicle_name")
    val vehicleName: String? = null,

    @SerializedName("device_type")
    val deviceType: String? = "Voice",

    @SerializedName("device_price")
    val devicePrice: Double? = 0.0,

    @SerializedName("installation_date")
    val installationDate: String? = null,

    @SerializedName("status")
    val status: String? = "Active",

    @SerializedName("customer_name")
    val customerName: String? = null,

    @SerializedName("customer_phone")
    val customerPhone: String? = null
)

/**
 * Client Due Record with Contact Information
 */
data class ClientDueItem(
    @SerializedName("invoice_id")
    val invoiceId: Int,

    @SerializedName("invoice_no")
    val invoiceNo: String,

    @SerializedName("grand_total")
    val grandTotal: Double,

    @SerializedName("paid_amount")
    val paidAmount: Double,

    @SerializedName("due_amount")
    val dueAmount: Double,

    @SerializedName("payment_status")
    val paymentStatus: String? = null,

    @SerializedName("invoice_date")
    val invoiceDate: String? = null,

    @SerializedName("vehicle_plate_number")
    val vehiclePlateNumber: String? = null,

    @SerializedName("vehicle_name")
    val vehicleName: String? = null,

    @SerializedName("customer_name")
    val customerName: String? = null,

    @SerializedName("customer_phone")
    val customerPhone: String? = null
)

/**
 * Seller / Sales Department Performance Metrics
 */
data class SellerDashboardMetrics(
    @SerializedName("month")
    val month: String,

    @SerializedName("month_name")
    val monthName: String,

    @SerializedName("target_qty")
    val targetQty: Int,

    @SerializedName("achieved_qty")
    val achievedQty: Int,

    @SerializedName("last_month_achieved")
    val lastMonthAchieved: Int = 0,

    @SerializedName("growth_rate")
    val growthRate: Float = 0f,

    @SerializedName("progress_percentage")
    val progressPercentage: Float,

    @SerializedName("total_due_amount")
    val totalDueAmount: Double,

    @SerializedName("earned_commission")
    val earnedCommission: Double,

    @SerializedName("company_total_sales")
    val companyTotalSales: Int,

    @SerializedName("sold_vehicles_count")
    val soldVehiclesCount: Int,

    @SerializedName("sold_vehicles_list")
    val soldVehiclesList: List<SoldVehicleItem> = emptyList(),

    @SerializedName("client_dues_count")
    val clientDuesCount: Int = 0,

    @SerializedName("client_dues_list")
    val clientDuesList: List<ClientDueItem> = emptyList()
)

/**
 * Technical / Installation Operations Metrics
 */
data class TechnicalMetrics(
    @SerializedName("pending_installations")
    val pendingInstallations: Int,

    @SerializedName("completed_installations")
    val completedInstallations: Int
)

/**
 * Overall Company 360 Telematics Overview
 */
data class CompanyOverview(
    @SerializedName("total_fleet_size")
    val totalFleetSize: Int,

    @SerializedName("this_month_sales")
    val thisMonthSales: Int,

    @SerializedName("active_vehicles")
    val activeVehicles: Int,

    @SerializedName("total_clients")
    val totalClients: Int
)

/**
 * Consolidated ERP Dashboard Data Container
 */
data class ErpDashboardData(
    @SerializedName("sales_department")
    val salesDepartment: SellerDashboardMetrics? = null,

    @SerializedName("technical_department")
    val technicalDepartment: TechnicalMetrics? = null,

    @SerializedName("company_overview")
    val companyOverview: CompanyOverview? = null
)

/**
 * Full Dashboard Response from /api/v1/erp-dashboard
 */
data class DashboardResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("code")
    val code: Int,

    @SerializedName("user")
    val user: UserProfile? = null,

    @SerializedName("data")
    val data: ErpDashboardData? = null
)
