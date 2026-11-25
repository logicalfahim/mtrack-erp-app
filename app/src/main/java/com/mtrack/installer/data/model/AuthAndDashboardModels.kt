package com.mtrack.installer.data.model

import com.google.gson.annotations.SerializedName

/**
 * Authentication Credentials Payload
 */
data class LoginRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("email")
    val email: String? = null
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
    val profilePicture: String? = null,

    @SerializedName("profile_picture_url")
    val profilePictureUrl: String? = null,

    @SerializedName("blood_group")
    val bloodGroup: String? = null,

    @SerializedName("address")
    val address: String? = null
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
    val customerPhone: String? = null,

    @SerializedName("due_amount")
    val dueAmount: Double = 0.0,

    @SerializedName("due_invoice_no")
    val dueInvoiceNo: String? = null,

    @SerializedName("is_due")
    val isDue: Boolean = false
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
    val customerPhone: String? = null,

    @SerializedName("due_invoice_no")
    val dueInvoiceNo: String? = null,

    @SerializedName("is_due")
    val isDue: Boolean = false
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
 * Staff Directory Member Item with Live Active Status, Blood Group, Address and Contact
 */
data class StaffMemberItem(
    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("username")
    val username: String,

    @SerializedName("full_name")
    val fullName: String,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("blood_group")
    val bloodGroup: String? = null,

    @SerializedName("address")
    val address: String? = null,

    @SerializedName("role")
    val role: String? = "Staff",

    @SerializedName("designation")
    val designation: String? = "Staff",

    @SerializedName("department")
    val department: String? = "General",

    @SerializedName("profile_picture_url")
    val profilePictureUrl: String? = null,

    @SerializedName("last_active_at")
    val lastActiveAt: String? = null,

    @SerializedName("active_recency_text")
    val activeRecencyText: String? = "Offline",

    @SerializedName("is_online")
    val isOnline: Boolean = false
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
    val companyOverview: CompanyOverview? = null,

    @SerializedName("staff_directory")
    val staffDirectory: List<StaffMemberItem> = emptyList()
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

/**
 * Staff GPS Location Update Payload
 */
data class LocationUpdateRequest(
    @SerializedName("user_id")
    val userId: Int? = null,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("accuracy")
    val accuracy: Float = 0.0f,

    @SerializedName("speed")
    val speed: Float = 0.0f,

    @SerializedName("altitude")
    val altitude: Double = 0.0,

    @SerializedName("battery_level")
    val batteryLevel: Int = 0,

    @SerializedName("is_charging")
    val isCharging: Boolean = false,

    @SerializedName("is_mocked")
    val isMocked: Boolean = false,

    @SerializedName("provider")
    val provider: String = "fused"
)

data class LocationUpdateResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("code")
    val code: Int,

    @SerializedName("message")
    val message: String? = null
)

/**
 * Enterprise Notification Item for Half-Drawer Sheet
 */
data class StaffNotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val category: String,
    val timeAgo: String,
    val isRead: Boolean = false,
    val type: String = "info"
)
