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
    @SerializedName("user_id", alternate = ["id", "staff_id", "uid"])
    val userId: Int = 0,

    @SerializedName("username", alternate = ["user_name", "login"])
    val username: String = "",

    @SerializedName("full_name", alternate = ["name", "staff_name", "employee_name"])
    val fullName: String = "",

    @SerializedName("email", alternate = ["email_address", "work_email"])
    val email: String? = null,

    @SerializedName("phone", alternate = ["mobile", "contact", "contact_number", "phone_number"])
    val phone: String? = null,

    @SerializedName("role", alternate = ["user_role", "type"])
    val role: String = "Seller",

    @SerializedName("designation", alternate = ["position", "job_title", "title"])
    val designation: String? = "Staff",

    @SerializedName("department", alternate = ["dept", "division"])
    val department: String? = "General",

    @SerializedName("profile_picture", alternate = ["avatar", "image", "photo"])
    val profilePicture: String? = null,

    @SerializedName("profile_picture_url", alternate = ["avatar_url", "image_url", "photo_url"])
    val profilePictureUrl: String? = null,

    @SerializedName("blood_group", alternate = ["blood", "blood_grp"])
    val bloodGroup: String? = null,

    @SerializedName("address", alternate = ["location", "present_address", "base_station", "office_address", "city"])
    val address: String? = null,

    @SerializedName("permanent_address", alternate = ["perm_address", "home_address"])
    val permanentAddress: String? = null,

    @SerializedName("emergency_contact", alternate = ["emergency_phone", "emergency_number", "alt_phone"])
    val emergencyContact: String? = null,

    @SerializedName("nid_number", alternate = ["nid", "national_id"])
    val nidNumber: String? = null,

    @SerializedName("salary", alternate = ["basic_salary", "monthly_salary"])
    val salary: Double? = null,

    @SerializedName("joining_date", alternate = ["join_date", "hire_date", "created_at"])
    val joiningDate: String? = null
)

/**
 * Login Response returned by /api/v1/auth-login
 */
data class LoginResponse(
    @SerializedName("status")
    val status: String? = "success",

    @SerializedName("code")
    val code: Int = 200,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("token", alternate = ["access_token", "auth_token"])
    val token: String? = null,

    @SerializedName("user", alternate = ["profile", "staff", "current_user"])
    val user: UserProfile? = null
)

/**
 * Sold Vehicle Item in Seller's Fleet
 */
data class SoldVehicleItem(
    @SerializedName("id", alternate = ["vehicle_id"])
    val id: Int = 0,

    @SerializedName("vehicle_plate_number", alternate = ["vehicle_no", "plate_number", "registration_number", "reg_no", "plate_no", "vehicle_number"])
    val vehiclePlateNumber: String = "",

    @SerializedName("vehicle_name", alternate = ["name", "model", "vehicle_model", "vehicle_type"])
    val vehicleName: String? = null,

    @SerializedName("device_type", alternate = ["device", "tracker_type"])
    val deviceType: String? = "Voice",

    @SerializedName("device_price", alternate = ["price", "amount", "unit_price"])
    val devicePrice: Double? = 0.0,

    @SerializedName("monthly_bill", alternate = ["bill", "monthly_service_charge", "service_charge", "monthly_charge", "monthly_fee"])
    val monthlyBill: Double? = 0.0,

    @SerializedName("referral_commission", alternate = ["commission"])
    val referralCommission: Double? = 0.0,

    @SerializedName("installation_date", alternate = ["install_date", "date", "created_at"])
    val installationDate: String? = null,

    @SerializedName("status", alternate = ["install_status", "device_status"])
    val status: String? = "Active",

    @SerializedName("customer_name", alternate = ["client_name", "customer", "client"])
    val customerName: String? = null,

    @SerializedName("customer_phone", alternate = ["client_phone", "phone", "contact_number", "contact"])
    val customerPhone: String? = null,

    @SerializedName("due_amount", alternate = ["due", "balance", "remaining_amount"])
    val dueAmount: Double = 0.0,

    @SerializedName("due_invoice_no", alternate = ["invoice_no", "invoice_number"])
    val dueInvoiceNo: String? = null,

    @SerializedName("seller_id", alternate = ["user_sel_id", "sales_person_id"])
    val sellerId: Int? = null,

    @SerializedName("seller_name", alternate = ["seller", "sales_person", "sales_executive", "agent", "sold_by", "ref_name"])
    val sellerName: String? = null,

    @SerializedName("installer_id", alternate = ["technician_id", "user_inst_id"])
    val installerId: Int? = null,

    @SerializedName("installer_name", alternate = ["installer", "technician", "technician_name", "installed_by", "fitter"])
    val installerName: String? = null,

    @SerializedName("is_due", alternate = ["has_due"])
    val isDue: Boolean = false
)

/**
 * Client Due Record with Contact Information
 */
data class ClientDueItem(
    @SerializedName("invoice_id", alternate = ["id"])
    val invoiceId: Int = 0,

    @SerializedName("invoice_no", alternate = ["invoice_number", "bill_no"])
    val invoiceNo: String = "",

    @SerializedName("grand_total", alternate = ["total", "total_amount", "bill_amount"])
    val grandTotal: Double = 0.0,

    @SerializedName("paid_amount", alternate = ["paid", "received_amount"])
    val paidAmount: Double = 0.0,

    @SerializedName("due_amount", alternate = ["due", "balance", "remaining_due"])
    val dueAmount: Double = 0.0,

    @SerializedName("payment_status", alternate = ["status"])
    val paymentStatus: String? = null,

    @SerializedName("invoice_date", alternate = ["date", "created_at"])
    val invoiceDate: String? = null,

    @SerializedName("vehicle_plate_number", alternate = ["plate_number", "registration_number", "reg_no", "plate_no"])
    val vehiclePlateNumber: String? = null,

    @SerializedName("vehicle_name", alternate = ["name", "vehicle_model"])
    val vehicleName: String? = null,

    @SerializedName("customer_name", alternate = ["client_name", "customer"])
    val customerName: String? = null,

    @SerializedName("customer_phone", alternate = ["client_phone", "phone", "contact_number"])
    val customerPhone: String? = null,

    @SerializedName("due_invoice_no", alternate = ["invoice_no"])
    val dueInvoiceNo: String? = null,

    @SerializedName("is_due")
    val isDue: Boolean = true
)

/**
 * Seller / Sales Department Performance Metrics
 */
data class SellerDashboardMetrics(
    @SerializedName("month", alternate = ["current_month", "period", "date"])
    val month: String = "",

    @SerializedName("month_name", alternate = ["month_label", "name"])
    val monthName: String = "",

    @SerializedName("target_qty", alternate = ["target", "sales_target", "monthly_target"])
    val targetQty: Int = 0,

    @SerializedName("achieved_qty", alternate = ["achieved", "sales_achieved", "sold_count", "total_sold", "sold_vehicles_count"])
    val achievedQty: Int = 0,

    @SerializedName("last_month_achieved", alternate = ["last_month_achieved_qty", "prev_month_achieved"])
    val lastMonthAchieved: Int = 0,

    @SerializedName("growth_rate", alternate = ["growth", "growth_percentage"])
    val growthRate: Float = 0f,

    @SerializedName("progress_percentage", alternate = ["progress", "achievement_rate", "rate", "completion_rate"])
    val progressPercentage: Float = 0f,

    @SerializedName("total_due_amount", alternate = ["due_amount", "total_dues", "dues", "outstanding_amount", "total_due"])
    val totalDueAmount: Double = 0.0,

    @SerializedName("earned_commission", alternate = ["commission", "incentive", "bonus"])
    val earnedCommission: Double = 0.0,

    @SerializedName("lifetime_installed", alternate = ["lifetime_vehicles", "total_installed", "executive_lifetime"])
    val lifetimeInstalled: Int = 0,

    @SerializedName("hardware_value", alternate = ["total_hardware_value", "device_value"])
    val hardwareValue: Double = 0.0,

    @SerializedName("mrr", alternate = ["monthly_recurring_revenue", "monthly_bill_total"])
    val mrr: Double = 0.0,

    @SerializedName("company_total_sales", alternate = ["company_sales", "total_sales", "overall_sales"])
    val companyTotalSales: Int = 0,

    @SerializedName("sold_vehicles_count", alternate = ["vehicles_count", "sold_count", "total_vehicles"])
    val soldVehiclesCount: Int = 0,

    @SerializedName("sold_vehicles_list", alternate = ["sold_vehicles", "vehicles", "sales", "fleet", "sold_list"])
    val soldVehiclesList: List<SoldVehicleItem> = emptyList(),

    @SerializedName("client_dues_count", alternate = ["dues_count", "due_count", "total_client_dues"])
    val clientDuesCount: Int = 0,

    @SerializedName("client_dues_list", alternate = ["client_dues", "dues", "due_invoices", "invoices"])
    val clientDuesList: List<ClientDueItem> = emptyList()
)

/**
 * Technical / Installation Operations Metrics
 */
data class TechnicalMetrics(
    @SerializedName("pending_installations", alternate = ["pending", "pending_jobs"])
    val pendingInstallations: Int = 0,

    @SerializedName("completed_installations", alternate = ["completed", "completed_jobs"])
    val completedInstallations: Int = 0
)

/**
 * Overall Company 360 Telematics Overview
 */
data class CompanyOverview(
    @SerializedName("total_fleet_size", alternate = ["fleet_size", "total_fleet"])
    val totalFleetSize: Int = 0,

    @SerializedName("this_month_sales", alternate = ["monthly_sales", "sales_this_month"])
    val thisMonthSales: Int = 0,

    @SerializedName("active_vehicles", alternate = ["active_fleet", "online_vehicles"])
    val activeVehicles: Int = 0,

    @SerializedName("total_clients", alternate = ["clients", "total_customers", "customers"])
    val totalClients: Int = 0
)

/**
 * Staff Directory Member Item with Live Active Status, Blood Group, Address and Contact
 */
data class StaffMemberItem(
    @SerializedName("user_id", alternate = ["id", "staff_id", "uid"])
    val userId: Int = 0,

    @SerializedName("username", alternate = ["user_name", "login"])
    val username: String = "",

    @SerializedName("full_name", alternate = ["name", "staff_name", "employee_name"])
    val fullName: String = "",

    @SerializedName("phone", alternate = ["mobile", "contact", "contact_number", "phone_number"])
    val phone: String? = null,

    @SerializedName("email", alternate = ["email_address", "work_email"])
    val email: String? = null,

    @SerializedName("blood_group", alternate = ["blood", "blood_grp"])
    val bloodGroup: String? = null,

    @SerializedName("address", alternate = ["location", "present_address", "base_station", "city"])
    val address: String? = null,

    @SerializedName("role", alternate = ["user_role", "type"])
    val role: String? = "Staff",

    @SerializedName("designation", alternate = ["position", "job_title", "title"])
    val designation: String? = "Staff",

    @SerializedName("department", alternate = ["dept", "division"])
    val department: String? = "General",

    @SerializedName("profile_picture_url", alternate = ["avatar", "image", "photo", "profile_picture", "picture"])
    val profilePictureUrl: String? = null,

    @SerializedName("last_active_at", alternate = ["last_seen", "last_location_at", "updated_at"])
    val lastActiveAt: String? = null,

    @SerializedName("active_recency_text", alternate = ["recency", "status_text"])
    val activeRecencyText: String? = "Offline",

    @SerializedName("is_online", alternate = ["online", "active"])
    val isOnline: Boolean = false
)

/**
 * Consolidated ERP Dashboard Data Container
 */
data class ErpDashboardData(
    @SerializedName("sales_department", alternate = ["sales", "sales_data", "seller_dashboard", "admin_dashboard", "sales_overview", "department_sales", "seller"])
    val salesDepartment: SellerDashboardMetrics? = null,

    @SerializedName("technical_department", alternate = ["technical", "tech_department", "installations"])
    val technicalDepartment: TechnicalMetrics? = null,

    @SerializedName("company_overview", alternate = ["company", "overview", "business_overview"])
    val companyOverview: CompanyOverview? = null,

    @SerializedName("staff_directory", alternate = ["staff_list", "staffs", "employees", "staff", "users", "colleagues", "staff_members"])
    val staffDirectory: List<StaffMemberItem> = emptyList()
)

/**
 * Full Dashboard Response from /api/v1/erp-dashboard
 */
data class DashboardResponse(
    @SerializedName("status")
    val status: String? = "success",

    @SerializedName("code")
    val code: Int = 200,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("user", alternate = ["profile", "staff", "current_user"])
    val user: UserProfile? = null,

    @SerializedName("data", alternate = ["dashboard", "result", "payload"])
    val data: ErpDashboardData? = null,

    @SerializedName("notifications", alternate = ["notification_list", "alerts"])
    val notifications: List<StaffNotificationItem> = emptyList()
)

/**
 * Staff GPS Location Update Payload
 */
data class LocationUpdateRequest(
    @SerializedName("user_id")
    val userId: Int? = null,

    @SerializedName("staff_id")
    val staffId: Int? = userId,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("lat")
    val lat: Double = latitude,

    @SerializedName("lng")
    val lng: Double = longitude,

    @SerializedName("accuracy")
    val accuracy: Float = 0.0f,

    @SerializedName("speed")
    val speed: Float = 0.0f,

    @SerializedName("altitude")
    val altitude: Double = 0.0,

    @SerializedName("battery_level")
    val batteryLevel: Int = 0,

    @SerializedName("battery")
    val battery: Int = batteryLevel,

    @SerializedName("is_charging")
    val isCharging: Boolean = false,

    @SerializedName("is_mocked")
    val isMocked: Boolean = false,

    @SerializedName("provider")
    val provider: String = "fused"
)

data class LocationUpdateResponse(
    @SerializedName("status")
    val status: String? = "success",

    @SerializedName("code")
    val code: Int = 200,

    @SerializedName("message")
    val message: String? = null
)

/**
 * Enterprise Notification Item for Half-Drawer Sheet
 */
data class StaffNotificationItem(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("title")
    val title: String = "",

    @SerializedName("message")
    val message: String = "",

    @SerializedName("category", alternate = ["cat", "dept"])
    val category: String = "Alert",

    @SerializedName("time_ago", alternate = ["time", "created_at", "timeAgo"])
    val timeAgo: String = "Recently",

    @SerializedName("is_read", alternate = ["read", "status", "isRead"])
    val isRead: Boolean = false,

    @SerializedName("type", alternate = ["alert_type", "level"])
    val type: String = "info"
)
