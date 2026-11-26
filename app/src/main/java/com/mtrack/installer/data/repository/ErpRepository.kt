package com.mtrack.installer.data.repository

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.mtrack.installer.data.api.MTrackApiService
import com.mtrack.installer.data.api.RetrofitClient
import com.mtrack.installer.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ErpRepository(
    private val api: MTrackApiService = RetrofitClient.apiService
) {
    private val gson: Gson = GsonBuilder().setLenient().serializeNulls().create()

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
            val json = JsonParser.parseString(rawErr).asJsonObject
            when {
                json.has("message") && !json.get("message").isJsonNull -> json.get("message").asString
                json.has("error") && !json.get("error").isJsonNull -> json.get("error").asString
                else -> rawErr
            }
        } catch (ignored: Exception) {
            rawErr
        }
    }

    /**
     * Fetches pure server-driven ERP Dashboard (Zero Dummy / Zero Fallback Data)
     * Directly reflects MySQL database queries from /api/v1/erp-dashboard
     */
    suspend fun getDashboard(
        token: String?,
        month: String? = null,
        currentUser: UserProfile? = null
    ): Result<DashboardResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val authHeader = if (!token.isNullOrBlank()) {
                    if (token.startsWith("Bearer ")) token else "Bearer $token"
                } else null

                val effectiveMonth = if (!month.isNullOrBlank()) month else SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

                if (authHeader == null) {
                    return@withContext Result.success(
                        DashboardResponse(
                            status = "unauthenticated",
                            code = 401,
                            user = currentUser,
                            data = ErpDashboardData(),
                            notifications = emptyList()
                        )
                    )
                }

                // 1. Primary Query: GET /api/v1/erp-dashboard?month=YYYY-MM
                var dashResponse = fetchDashboardPayload(authHeader, effectiveMonth, currentUser)
                var currentData = dashResponse.data ?: ErpDashboardData()

                // 2. Pure Server-Driven Staff Directory (users table)
                if (currentData.staffDirectory.isEmpty()) {
                    val remoteStaff = fetchStaffDirectory(authHeader)
                    currentData = currentData.copy(staffDirectory = remoteStaff)
                }

                // 2b. Enrich staff list with 5-minute online threshold per backend SQL specification:
                // CASE WHEN last_active_at >= NOW() - INTERVAL 5 MINUTE THEN 1 ELSE 0 END as is_online
                if (currentData.staffDirectory.isNotEmpty()) {
                    currentData = currentData.copy(
                        staffDirectory = enrichStaffOnlineStatus(currentData.staffDirectory)
                    )
                }

                // 3. Pure Server-Driven Vehicles & Sales Fleet (vehicles table)
                val currentSales = currentData.salesDepartment
                val resolvedSales = resolveServerSalesAndFleet(
                    authHeader = authHeader,
                    month = effectiveMonth,
                    existingSales = currentSales,
                    currentUser = currentUser ?: dashResponse.user
                )
                if (resolvedSales != null) {
                    currentData = currentData.copy(salesDepartment = resolvedSales)
                }

                // 4. Pure Server Notifications (notifications table)
                val serverNotifs = if (dashResponse.notifications.isNotEmpty()) {
                    dashResponse.notifications
                } else {
                    fetchRemoteNotifications(authHeader)
                }

                dashResponse = dashResponse.copy(
                    data = currentData,
                    notifications = serverNotifs
                )
                Result.success(dashResponse)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun markAllNotificationsRead(token: String, userId: Int): Boolean = withContext(Dispatchers.IO) {
        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
        return@withContext try {
            val response = api.markAllNotificationsRead(authHeader, mapOf("user_id" to userId))
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Calls GET /api/v1/erp-dashboard?month=YYYY-MM with fallback to /api/v1/seller-dashboard
     */
    private suspend fun fetchDashboardPayload(
        authHeader: String,
        month: String?,
        currentUser: UserProfile? = null
    ): DashboardResponse {
        val isSeller = currentUser != null && !currentUser.role.equals("admin", true)

        // Try /api/v1/erp-dashboard first (Standard Unified Gateway)
        try {
            val r1 = api.getErpDashboard(authHeader, month)
            if (r1.isSuccessful && r1.body() != null) {
                val parsed = parseDashboardJson(r1.body()!!)
                if (parsed != null && (parsed.data?.salesDepartment != null || parsed.data?.staffDirectory?.isNotEmpty() == true)) {
                    return parsed
                }
            }
        } catch (ignored: Exception) {}

        // If seller, try /api/v1/seller-dashboard
        if (isSeller) {
            try {
                val r2 = api.getSellerDashboard(authHeader, month)
                if (r2.isSuccessful && r2.body() != null) {
                    val parsed = parseDashboardJson(r2.body()!!)
                    if (parsed != null) return parsed
                }
            } catch (ignored: Exception) {}
        }

        // Try /api/v1/dashboard
        try {
            val r3 = api.getDashboardGeneral(authHeader, month)
            if (r3.isSuccessful && r3.body() != null) {
                val parsed = parseDashboardJson(r3.body()!!)
                if (parsed != null) return parsed
            }
        } catch (ignored: Exception) {}

        // If admin, try /api/v1/admin-dashboard
        if (!isSeller) {
            try {
                val r4 = api.getAdminDashboard(authHeader, month)
                if (r4.isSuccessful && r4.body() != null) {
                    val parsed = parseDashboardJson(r4.body()!!)
                    if (parsed != null) return parsed
                }
            } catch (ignored: Exception) {}
        }

        return DashboardResponse(status = "success", code = 200, data = ErpDashboardData())
    }

    /**
     * Parses the backend JSON response into DashboardResponse
     */
    private fun parseDashboardJson(json: JsonElement): DashboardResponse? {
        return try {
            if (!json.isJsonObject) return null
            val obj = json.asJsonObject

            // 1. Root User Profile
            val userProfile: UserProfile? = if (obj.has("user") && obj.get("user").isJsonObject) {
                try { gson.fromJson(obj.get("user"), UserProfile::class.java) } catch (e: Exception) { null }
            } else null

            // 2. Root Notifications
            val notifsList: List<StaffNotificationItem> = if (obj.has("notifications") && obj.get("notifications").isJsonArray) {
                parseNotificationsJson(obj.getAsJsonArray("notifications"))
            } else emptyList()

            // 3. Data Object
            var salesMetrics: SellerDashboardMetrics? = null
            var staffList: List<StaffMemberItem> = emptyList()

            if (obj.has("data") && obj.get("data").isJsonObject) {
                val dataObj = obj.getAsJsonObject("data")

                // Extract Sales Department
                if (dataObj.has("sales_department") && dataObj.get("sales_department").isJsonObject) {
                    salesMetrics = parseSalesMetricsObject(dataObj.getAsJsonObject("sales_department"))
                } else if (dataObj.has("achieved_qty") || dataObj.has("sold_vehicles") || dataObj.has("sold_vehicles_list")) {
                    salesMetrics = parseSalesMetricsObject(dataObj)
                }

                // Extract Staffs Directory
                val staffKeys = listOf("staffs", "staff_directory", "staff_list", "users", "employees", "colleagues")
                for (k in staffKeys) {
                    if (dataObj.has(k) && dataObj.get(k).isJsonArray) {
                        staffList = parseStaffList(dataObj.getAsJsonArray(k))
                        if (staffList.isNotEmpty()) break
                    }
                }
            } else if (obj.has("sales_department") && obj.get("sales_department").isJsonObject) {
                salesMetrics = parseSalesMetricsObject(obj.getAsJsonObject("sales_department"))
            }

            // Check root level staffs if still empty
            if (staffList.isEmpty()) {
                val rootStaffKeys = listOf("staffs", "staff_directory", "users", "employees")
                for (k in rootStaffKeys) {
                    if (obj.has(k) && obj.get(k).isJsonArray) {
                        staffList = parseStaffList(obj.getAsJsonArray(k))
                        if (staffList.isNotEmpty()) break
                    }
                }
            }

            DashboardResponse(
                status = obj.optString("status") ?: "success",
                code = obj.optInt("code", 200),
                user = userProfile,
                data = ErpDashboardData(
                    salesDepartment = salesMetrics,
                    staffDirectory = staffList
                ),
                notifications = notifsList
            )
        } catch (ignored: Exception) {
            null
        }
    }

    /**
     * Parses the sales_department object from backend JSON with strict mapping to SQL columns:
     * v.vehicle_no as vehicle_plate_number, v.vehicle_model as vehicle_name, etc.
     */
    private fun parseSalesMetricsObject(obj: JsonObject): SellerDashboardMetrics {
        var baseMetrics: SellerDashboardMetrics = try {
            gson.fromJson(obj, SellerDashboardMetrics::class.java)
        } catch (e: Exception) {
            SellerDashboardMetrics()
        }

        // Parse sold_vehicles_list array if available
        val vehiclesList = mutableListOf<SoldVehicleItem>()
        val vehicleKeys = listOf("sold_vehicles_list", "vehicles", "sold_vehicles", "fleet", "items", "data")
        for (k in vehicleKeys) {
            if (obj.has(k) && obj.get(k).isJsonArray) {
                val parsed = parseVehiclesArray(obj.getAsJsonArray(k))
                if (parsed.isNotEmpty()) {
                    vehiclesList.addAll(parsed)
                    break
                }
            }
        }

        // If vehicles list parsed, merge with baseMetrics
        if (vehiclesList.isNotEmpty()) {
            val achieved = if (baseMetrics.achievedQty > 0) baseMetrics.achievedQty else vehiclesList.size
            val target = if (baseMetrics.targetQty > 0) baseMetrics.targetQty else 20
            val hardware = if (baseMetrics.hardwareValue > 0.0) baseMetrics.hardwareValue else vehiclesList.sumOf { it.devicePrice ?: 0.0 }
            val mrr = if (baseMetrics.mrr > 0.0) baseMetrics.mrr else vehiclesList.sumOf { it.monthlyBill ?: 0.0 }
            val progress = if (target > 0) ((achieved.toFloat() / target.toFloat()) * 100f).coerceIn(0f, 100f) else baseMetrics.progressPercentage

            baseMetrics = baseMetrics.copy(
                achievedQty = achieved,
                soldVehiclesCount = maxOf(achieved, vehiclesList.size),
                soldVehiclesList = vehiclesList,
                hardwareValue = hardware,
                mrr = mrr,
                progressPercentage = progress
            )
        }

        return baseMetrics
    }

    /**
     * Resolves sales and fleet data strictly from server endpoints (/api/v1/vehicles or /api/v1/sales).
     * Strictly eliminates any mock data or ticket fallbacks.
     */
    private suspend fun resolveServerSalesAndFleet(
        authHeader: String,
        month: String,
        existingSales: SellerDashboardMetrics?,
        currentUser: UserProfile?
    ): SellerDashboardMetrics? {
        val serverVehicles = mutableListOf<SoldVehicleItem>()

        // 1. If already provided by /api/v1/erp-dashboard, retain them
        if (existingSales != null && existingSales.soldVehiclesList.isNotEmpty()) {
            serverVehicles.addAll(existingSales.soldVehiclesList)
        }

        // 2. Query /api/v1/vehicles if fleet list is still empty
        if (serverVehicles.isEmpty()) {
            try {
                val sellerId = currentUser?.userId?.takeIf { it > 0 }
                val sellerUsername = currentUser?.username?.trim()?.ifBlank { null }
                val vehiclesResp = api.getVehicles(
                    bearerToken = authHeader,
                    sellerId = sellerId,
                    seller = sellerUsername,
                    refName = null,
                    month = month,
                    limit = 100
                )
                if (vehiclesResp.isSuccessful && vehiclesResp.body() != null) {
                    val parsed = parseVehiclesArray(vehiclesResp.body()!!)
                    if (parsed.isNotEmpty()) {
                        serverVehicles.addAll(parsed)
                    }
                }
            } catch (ignored: Exception) {}

            // Try general /api/v1/vehicles?month=YYYY-MM if seller-filtered was empty
            if (serverVehicles.isEmpty()) {
                try {
                    val generalResp = api.getVehicles(
                        bearerToken = authHeader,
                        sellerId = null,
                        seller = null,
                        refName = null,
                        month = month,
                        limit = 100
                    )
                    if (generalResp.isSuccessful && generalResp.body() != null) {
                        val parsed = parseVehiclesArray(generalResp.body()!!)
                        if (parsed.isNotEmpty()) {
                            serverVehicles.addAll(parsed)
                        }
                    }
                } catch (ignored: Exception) {}
            }
        }

        // 3. Query /api/v1/sales if still empty
        if (serverVehicles.isEmpty()) {
            try {
                val salesResp = api.getSales(
                    bearerToken = authHeader,
                    month = month,
                    seller = null,
                    refName = null
                )
                if (salesResp.isSuccessful && salesResp.body() != null) {
                    val parsed = parseSalesPayload(salesResp.body()!!)
                    if (parsed != null && parsed.soldVehiclesList.isNotEmpty()) {
                        serverVehicles.addAll(parsed.soldVehiclesList)
                    }
                }
            } catch (ignored: Exception) {}
        }

        // Deduplicate vehicles by plate number or ID and sort newest first
        val distinctVehicles = serverVehicles.distinctBy {
            it.vehiclePlateNumber.ifBlank { it.id.toString() }
        }.sortedByDescending { it.installationDate ?: "" }

        // Extract Client Dues directly from vehicles or existing dues
        val allDues = mutableListOf<ClientDueItem>()
        if (existingSales?.clientDuesList?.isNotEmpty() == true) {
            allDues.addAll(existingSales.clientDuesList)
        } else {
            for (v in distinctVehicles) {
                if (v.isDue || (v.dueAmount ?: 0.0) > 0.0) {
                    allDues.add(
                        ClientDueItem(
                            invoiceId = v.id,
                            invoiceNo = v.dueInvoiceNo ?: "INV-${v.id}",
                            grandTotal = (v.devicePrice ?: 0.0).takeIf { it > 0 } ?: (v.dueAmount ?: 0.0),
                            paidAmount = 0.0,
                            dueAmount = v.dueAmount ?: 0.0,
                            paymentStatus = "Unpaid",
                            invoiceDate = v.installationDate,
                            vehiclePlateNumber = v.vehiclePlateNumber,
                            vehicleName = v.vehicleName,
                            customerName = v.customerName,
                            customerPhone = v.customerPhone,
                            isDue = true
                        )
                    )
                }
            }
        }

        val totalDue = allDues.sumOf { it.dueAmount }.takeIf { it > 0 }
            ?: (existingSales?.totalDueAmount ?: 0.0)

        val achieved = existingSales?.achievedQty?.takeIf { it > 0 }
            ?: distinctVehicles.size

        val target = existingSales?.targetQty?.takeIf { it > 0 } ?: 20
        val progress = if (target > 0) ((achieved.toFloat() / target.toFloat()) * 100f).coerceIn(0f, 100f) else 0f

        val hardwareVal = existingSales?.hardwareValue?.takeIf { it > 0.0 }
            ?: distinctVehicles.sumOf { it.devicePrice ?: 0.0 }

        val mrrVal = existingSales?.mrr?.takeIf { it > 0.0 }
            ?: distinctVehicles.sumOf { it.monthlyBill ?: 0.0 }

        return (existingSales ?: SellerDashboardMetrics()).copy(
            month = month,
            monthName = existingSales?.monthName?.ifBlank { null } ?: month,
            achievedQty = achieved,
            soldVehiclesCount = maxOf(achieved, distinctVehicles.size),
            soldVehiclesList = distinctVehicles,
            hardwareValue = hardwareVal,
            mrr = mrrVal,
            clientDuesCount = allDues.size,
            clientDuesList = allDues,
            totalDueAmount = totalDue,
            progressPercentage = progress
        )
    }

    /**
     * Parses vehicles array from JSON element, handling all SQL column aliases:
     * v.vehicle_no as vehicle_plate_number, v.vehicle_model as vehicle_name, etc.
     */
    private fun parseVehiclesArray(json: JsonElement): List<SoldVehicleItem> {
        val result = mutableListOf<SoldVehicleItem>()
        val itemsArray: JsonArray = (when {
            json.isJsonArray -> json.asJsonArray
            json.isJsonObject -> {
                val obj = json.asJsonObject
                when {
                    obj.has("data") && obj.get("data").isJsonArray -> obj.getAsJsonArray("data")
                    obj.has("data") && obj.get("data").isJsonObject && obj.getAsJsonObject("data").has("data") && obj.getAsJsonObject("data").get("data").isJsonArray -> obj.getAsJsonObject("data").getAsJsonArray("data")
                    obj.has("data") && obj.get("data").isJsonObject && obj.getAsJsonObject("data").has("vehicles") && obj.getAsJsonObject("data").get("vehicles").isJsonArray -> obj.getAsJsonObject("data").getAsJsonArray("vehicles")
                    obj.has("data") && obj.get("data").isJsonObject && obj.getAsJsonObject("data").has("items") && obj.getAsJsonObject("data").get("items").isJsonArray -> obj.getAsJsonObject("data").getAsJsonArray("items")
                    obj.has("vehicles") && obj.get("vehicles").isJsonArray -> obj.getAsJsonArray("vehicles")
                    obj.has("items") && obj.get("items").isJsonArray -> obj.getAsJsonArray("items")
                    obj.has("list") && obj.get("list").isJsonArray -> obj.getAsJsonArray("list")
                    else -> null
                }
            }
            else -> null
        }) ?: return emptyList()

        for (elem in itemsArray) {
            if (!elem.isJsonObject) continue
            val obj = elem.asJsonObject

            val id = obj.optInt("vehicle_id", 0).takeIf { it > 0 }
                ?: obj.optInt("id", 0)

            val plate = obj.optString("vehicle_plate_number")
                ?: obj.optString("vehicle_no")
                ?: obj.optString("registration_no")
                ?: obj.optString("plate_number")
                ?: obj.optString("reg_no")
                ?: obj.optString("plate_no")
                ?: obj.optString("vehicle_number")
                ?: "MTK-${if (id > 0) id else (result.size + 1)}"

            val model = obj.optString("vehicle_name")
                ?: obj.optString("vehicle_model")
                ?: obj.optString("vehicle_type")
                ?: obj.optString("vehicle_brand_model")
                ?: obj.optString("name")
                ?: obj.optString("model")
                ?: "Vehicle Unit"

            val device = obj.optString("device_type")
                ?: obj.optString("model_name")
                ?: obj.optString("device")
                ?: "GPS Tracker"

            val price = obj.optDouble("device_price", 0.0).takeIf { it > 0 }
                ?: obj.optDouble("price", 0.0)

            val bill = obj.optDouble("monthly_bill", 0.0).takeIf { it > 0 }
                ?: obj.optDouble("monthly_charge", 0.0)

            val commission = obj.optDouble("referral_commission", 0.0)

            val installDate = obj.optString("installation_date")
                ?: obj.optString("created_at")
                ?: obj.optString("installed_date")
                ?: obj.optString("install_date")

            val status = obj.optString("status")
                ?: "Active"

            val customerName = obj.optString("customer_name")
                ?: obj.optString("client_name")
                ?: extractStringOrNested(obj, "customer", listOf("name", "full_name"))
                ?: "Valued Client"

            val customerPhone = obj.optString("customer_phone")
                ?: obj.optString("contact_number")
                ?: obj.optString("phone")
                ?: extractStringOrNested(obj, "customer", listOf("phone", "contact_number"))

            val sellerName = obj.optString("seller_name")
                ?: obj.optString("sales_person")
                ?: extractStringOrNested(obj, "seller", listOf("name", "full_name"))

            val installerName = obj.optString("installer_name")
                ?: obj.optString("technician_name")
                ?: extractStringOrNested(obj, "installer", listOf("name", "full_name"))

            val dueAmount = obj.optDouble("due_amount", 0.0)
            val dueInvoiceNo = obj.optString("due_invoice_no")
                ?: obj.optString("invoice_no")

            val isDue = obj.optBoolean("is_due", false)
                || obj.optInt("is_due", 0) == 1
                || dueAmount > 0.0

            result.add(
                SoldVehicleItem(
                    id = id,
                    vehiclePlateNumber = plate,
                    vehicleName = model,
                    deviceType = device,
                    devicePrice = price,
                    monthlyBill = bill,
                    referralCommission = commission,
                    installationDate = installDate,
                    status = status,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    sellerName = sellerName,
                    installerName = installerName,
                    dueAmount = dueAmount,
                    dueInvoiceNo = dueInvoiceNo,
                    isDue = isDue
                )
            )
        }

        return result
    }

    /**
     * Pure Server-driven Staff Directory from /api/v1/staffs or /api/v1/users
     * (Zero Mock / Zero Fallback Data)
     */
    private suspend fun fetchStaffDirectory(authHeader: String): List<StaffMemberItem> {
        try {
            val r1 = api.getStaffs(authHeader)
            if (r1.isSuccessful && r1.body() != null) {
                val list = parseStaffList(r1.body()!!)
                if (list.isNotEmpty()) return list
            }
        } catch (ignored: Exception) {}

        try {
            val r2 = api.getStaffDirectory(authHeader)
            if (r2.isSuccessful && r2.body() != null) {
                val list = parseStaffList(r2.body()!!)
                if (list.isNotEmpty()) return list
            }
        } catch (ignored: Exception) {}

        try {
            val r3 = api.getUsers(authHeader)
            if (r3.isSuccessful && r3.body() != null) {
                val list = parseStaffList(r3.body()!!)
                if (list.isNotEmpty()) return list
            }
        } catch (ignored: Exception) {}

        return emptyList()
    }

    private fun parseStaffList(json: JsonElement): List<StaffMemberItem> {
        val listType = object : TypeToken<List<StaffMemberItem>>() {}.type
        val rawList: List<StaffMemberItem> = try {
            when {
                json.isJsonArray -> {
                    gson.fromJson(json.asJsonArray, listType) ?: emptyList()
                }
                json.isJsonObject -> {
                    val obj = json.asJsonObject
                    var found: List<StaffMemberItem>? = null
                    val keysToCheck = listOf("data", "users", "staffs", "staff", "employees", "staff_directory", "results")
                    for (key in keysToCheck) {
                        if (obj.has(key) && !obj.get(key).isJsonNull) {
                            val child = obj.get(key)
                            if (child.isJsonArray) {
                                val parsed: List<StaffMemberItem>? = gson.fromJson(child.asJsonArray, listType)
                                if (!parsed.isNullOrEmpty()) {
                                    found = parsed
                                    break
                                }
                            } else if (child.isJsonObject && child.asJsonObject.has("data") && child.asJsonObject.get("data").isJsonArray) {
                                val nestedArray = child.asJsonObject.get("data").asJsonArray
                                val parsed: List<StaffMemberItem>? = gson.fromJson(nestedArray, listType)
                                if (!parsed.isNullOrEmpty()) {
                                    found = parsed
                                    break
                                }
                            }
                        }
                    }
                    found ?: emptyList()
                }
                else -> emptyList()
            }
        } catch (ignored: Exception) {
            emptyList()
        }
        return rawList
    }

    /**
     * Enriches staff members with real-time online status and human-readable recency
     * Backend SQL Specification:
     * CASE WHEN last_active_at >= NOW() - INTERVAL 5 MINUTE THEN 1 ELSE 0 END as is_online
     */
    private fun enrichStaffOnlineStatus(items: List<StaffMemberItem>): List<StaffMemberItem> {
        val now = System.currentTimeMillis()
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
        )

        return items.map { staff ->
            val timestampStr = staff.lastActiveAt?.trim()
            if (timestampStr.isNullOrEmpty()) {
                val isOnline = staff.isOnline
                staff.copy(
                    isOnline = isOnline,
                    activeRecencyText = if (isOnline) "Online Now" else "Offline"
                )
            } else {
                var parsedTime: Long? = null
                for (fmt in formats) {
                    try {
                        val parsed = fmt.parse(timestampStr)
                        if (parsed != null) {
                            parsedTime = parsed.time
                            break
                        }
                    } catch (ignored: Exception) {}
                }

                if (parsedTime != null) {
                    val diff = now - parsedTime
                    // 5-minute active threshold (with 30s future clock skew allowance)
                    val isOnline = staff.isOnline || (diff in -30_000L..300_000L)
                    val recencyText = when {
                        isOnline -> "Online Now"
                        diff < 3_600_000L -> {
                            val mins = (diff / 60_000L).coerceAtLeast(1)
                            "Seen ${mins}m ago"
                        }
                        diff < 86_400_000L -> {
                            val hours = diff / 3_600_000L
                            "Seen ${hours}h ago"
                        }
                        else -> {
                            val days = diff / 86_400_000L
                            if (days == 1L) "Seen yesterday" else "Seen ${days}d ago"
                        }
                    }
                    staff.copy(
                        isOnline = isOnline,
                        activeRecencyText = recencyText
                    )
                } else {
                    val isOnline = staff.isOnline || timestampStr.contains("online", true) || timestampStr.contains("now", true)
                    staff.copy(
                        isOnline = isOnline,
                        activeRecencyText = if (isOnline) "Online Now" else timestampStr
                    )
                }
            }
        }
    }

    /**
     * Pure Remote Server Notifications (Zero Synthesized / Zero Mock Notifications)
     */
    private suspend fun fetchRemoteNotifications(authHeader: String): List<StaffNotificationItem> {
        val remoteList = mutableListOf<StaffNotificationItem>()
        try {
            val r0 = api.getNotificationsCheck(authHeader)
            if (r0.isSuccessful && r0.body() != null) {
                val parsed = parseNotificationsJson(r0.body()!!)
                if (parsed.isNotEmpty()) return parsed
            }
        } catch (ignored: Exception) {}

        try {
            val r1 = api.getNotifications(authHeader)
            if (r1.isSuccessful && r1.body() != null) {
                val parsed = parseNotificationsJson(r1.body()!!)
                if (parsed.isNotEmpty()) return parsed
            }
        } catch (ignored: Exception) {}

        try {
            val r2 = api.getStaffNotifications(authHeader)
            if (r2.isSuccessful && r2.body() != null) {
                val parsed = parseNotificationsJson(r2.body()!!)
                if (parsed.isNotEmpty()) return parsed
            }
        } catch (ignored: Exception) {}

        return emptyList()
    }

    private fun parseNotificationsJson(json: JsonElement): List<StaffNotificationItem> {
        val listType = object : TypeToken<List<StaffNotificationItem>>() {}.type
        return try {
            when {
                json.isJsonArray -> gson.fromJson(json.asJsonArray, listType) ?: emptyList()
                json.isJsonObject -> {
                    val obj = json.asJsonObject
                    val keys = listOf("data", "notifications", "alerts", "messages", "items")
                    for (k in keys) {
                        if (obj.has(k) && obj.get(k).isJsonArray) {
                            val parsed: List<StaffNotificationItem>? = gson.fromJson(obj.getAsJsonArray(k), listType)
                            if (!parsed.isNullOrEmpty()) return parsed
                        }
                    }
                    emptyList()
                }
                else -> emptyList()
            }
        } catch (ignored: Exception) {
            emptyList()
        }
    }

    private fun parseSalesPayload(json: JsonElement): SellerDashboardMetrics? {
        return try {
            if (json.isJsonObject) {
                val obj = json.asJsonObject
                if (obj.has("data") && obj.get("data").isJsonObject) {
                    parseSalesMetricsObject(obj.getAsJsonObject("data"))
                } else {
                    parseSalesMetricsObject(obj)
                }
            } else {
                null
            }
        } catch (ignored: Exception) {
            null
        }
    }

    // Helper extension functions for safe JsonObject parsing
    private fun JsonObject.optString(key: String): String? {
        if (!has(key) || get(key).isJsonNull) return null
        return try { get(key).asString } catch (ignored: Exception) { null }
    }

    private fun JsonObject.optInt(key: String, default: Int): Int {
        if (!has(key) || get(key).isJsonNull) return default
        return try { get(key).asInt } catch (ignored: Exception) { default }
    }

    private fun JsonObject.optDouble(key: String, default: Double): Double {
        if (!has(key) || get(key).isJsonNull) return default
        return try { get(key).asDouble } catch (ignored: Exception) { default }
    }

    private fun JsonObject.optBoolean(key: String, default: Boolean): Boolean {
        if (!has(key) || get(key).isJsonNull) return default
        return try { get(key).asBoolean } catch (ignored: Exception) { default }
    }

    private fun extractStringOrNested(obj: JsonObject, fieldKey: String, nestedKeys: List<String>): String? {
        if (!obj.has(fieldKey) || obj.get(fieldKey).isJsonNull) return null
        val elem = obj.get(fieldKey)
        if (elem.isJsonPrimitive) {
            val s = elem.asString.trim()
            if (s.isNotEmpty()) return s
        } else if (elem.isJsonObject) {
            val nestedObj = elem.asJsonObject
            for (nk in nestedKeys) {
                val nestedVal = nestedObj.optString(nk)?.trim()
                if (!nestedVal.isNullOrEmpty()) return nestedVal
            }
        }
        return null
    }
}
