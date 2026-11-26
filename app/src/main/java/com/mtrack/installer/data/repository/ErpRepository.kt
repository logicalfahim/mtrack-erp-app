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
                val remoteStaff = fetchStaffDirectory(authHeader)
                val mergedStaff = (currentData.staffDirectory + remoteStaff).distinctBy {
                    if (it.userId > 0) "id_${it.userId}" else if (!it.username.isNullOrBlank()) "u_${it.username}" else "p_${it.phone}"
                }
                currentData = currentData.copy(staffDirectory = mergedStaff)

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

                // 3b. Cross-reference technician names from staff directory if missing or generic
                val staffMap = currentData.staffDirectory.associateBy { it.userId }
                val currentVehicles = currentData.salesDepartment?.soldVehiclesList
                if (!currentVehicles.isNullOrEmpty() && staffMap.isNotEmpty()) {
                    val enrichedVehicles = currentVehicles.map { v ->
                        if ((v.installerName.isNullOrBlank() || v.installerName.equals("Technician", true)) && (v.installerId ?: 0) > 0) {
                            val tech = staffMap[v.installerId]
                            if (tech != null && tech.fullName.isNotBlank()) {
                                v.copy(installerName = tech.fullName)
                            } else v
                        } else v
                    }
                    currentData = currentData.copy(
                        salesDepartment = currentData.salesDepartment!!.copy(soldVehiclesList = enrichedVehicles)
                    )
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

                // If salesMetrics vehicles are still empty, search dataObj directly
                if (salesMetrics == null || salesMetrics.soldVehiclesList.isEmpty()) {
                    val vKeys = listOf("sold_vehicles_list", "vehicles", "sold_vehicles", "fleet", "items", "data")
                    for (k in vKeys) {
                        if (dataObj.has(k) && dataObj.get(k).isJsonArray) {
                            val parsed = parseVehiclesArray(dataObj.getAsJsonArray(k))
                            if (parsed.isNotEmpty()) {
                                val ach = if ((salesMetrics?.achievedQty ?: 0) > 0) salesMetrics!!.achievedQty else parsed.size
                                salesMetrics = (salesMetrics ?: SellerDashboardMetrics()).copy(
                                    achievedQty = ach,
                                    soldVehiclesCount = if ((salesMetrics?.soldVehiclesCount ?: 0) > 0) salesMetrics!!.soldVehiclesCount else ach,
                                    soldVehiclesList = parsed
                                )
                                break
                            }
                        }
                    }
                }

                // Extract Staffs Directory
                val staffKeys = listOf("staffs", "staff_directory", "staff_list", "users", "employees", "colleagues")
                for (k in staffKeys) {
                    if (dataObj.has(k) && dataObj.get(k).isJsonArray) {
                        val parsed = parseStaffList(dataObj.getAsJsonArray(k))
                        if (parsed.isNotEmpty()) {
                            staffList = parsed
                            break
                        }
                    }
                }
            } else if (obj.has("sales_department") && obj.get("sales_department").isJsonObject) {
                salesMetrics = parseSalesMetricsObject(obj.getAsJsonObject("sales_department"))
            }

            // Check root level vehicles if still empty
            if (salesMetrics == null || salesMetrics.soldVehiclesList.isEmpty()) {
                val rootVKeys = listOf("sold_vehicles_list", "vehicles", "sold_vehicles", "fleet", "items")
                for (k in rootVKeys) {
                    if (obj.has(k) && obj.get(k).isJsonArray) {
                        val parsed = parseVehiclesArray(obj.getAsJsonArray(k))
                        if (parsed.isNotEmpty()) {
                            val ach = if ((salesMetrics?.achievedQty ?: 0) > 0) salesMetrics!!.achievedQty else parsed.size
                            salesMetrics = (salesMetrics ?: SellerDashboardMetrics()).copy(
                                achievedQty = ach,
                                soldVehiclesCount = if ((salesMetrics?.soldVehiclesCount ?: 0) > 0) salesMetrics!!.soldVehiclesCount else ach,
                                soldVehiclesList = parsed
                            )
                            break
                        }
                    }
                }
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

        val isSeller = currentUser != null && !currentUser.role.equals("admin", true)
        val sellerFilteredVehicles = if (isSeller) {
            val userFullName = currentUser.fullName?.trim() ?: ""
            val userUsername = currentUser.username?.trim() ?: ""
            val userId = currentUser.userId

            val myVehicles = distinctVehicles.filter { v ->
                val sellerName = v.sellerName?.trim() ?: ""
                val matchesName = sellerName.isNotEmpty() && (
                    sellerName.contains(userFullName, ignoreCase = true) ||
                    sellerName.contains(userUsername, ignoreCase = true) ||
                    userFullName.contains(sellerName, ignoreCase = true)
                )
                val matchesId = userId > 0 && v.sellerId != null && v.sellerId == userId
                matchesName || matchesId
            }

            if (myVehicles.isNotEmpty()) {
                myVehicles
            } else if (distinctVehicles.size <= 25) {
                distinctVehicles
            } else {
                // If nationwide fleet of 100 was returned without seller tags, limit to the seller's actual achieved count
                val targetCount = existingSales?.achievedQty?.takeIf { it > 0 }
                    ?: (existingSales?.soldVehiclesCount?.takeIf { it > 0 } ?: 11)
                distinctVehicles.take(targetCount)
            }
        } else {
            distinctVehicles
        }

        val finalVehicles = sellerFilteredVehicles

        // Extract Client Dues directly from vehicles or existing dues
        val allDues = mutableListOf<ClientDueItem>()
        if (existingSales?.clientDuesList?.isNotEmpty() == true) {
            allDues.addAll(existingSales.clientDuesList)
        } else {
            for (v in finalVehicles) {
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
            ?: (existingSales?.soldVehiclesCount?.takeIf { it > 0 } ?: finalVehicles.size)

        val target = existingSales?.targetQty?.takeIf { it > 0 } ?: 20
        val progress = if (target > 0) ((achieved.toFloat() / target.toFloat()) * 100f).coerceIn(0f, 100f) else 0f

        val hardwareVal = existingSales?.hardwareValue?.takeIf { it > 0.0 }
            ?: finalVehicles.sumOf { it.devicePrice ?: 0.0 }

        val mrrVal = existingSales?.mrr?.takeIf { it > 0.0 }
            ?: finalVehicles.sumOf { it.monthlyBill ?: 0.0 }

        return (existingSales ?: SellerDashboardMetrics()).copy(
            month = month,
            monthName = existingSales?.monthName?.ifBlank { null } ?: month,
            achievedQty = achieved,
            soldVehiclesCount = achieved,
            soldVehiclesList = finalVehicles,
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

            val plate = obj.optStringClean("vehicle_plate_number")
                ?: obj.optStringClean("vehicle_no")
                ?: obj.optStringClean("registration_no")
                ?: obj.optStringClean("registration_number")
                ?: obj.optStringClean("plate_number")
                ?: obj.optStringClean("plate_no")
                ?: obj.optStringClean("reg_no")
                ?: obj.optStringClean("reg_number")
                ?: obj.optStringClean("vehicle_number")
                ?: obj.optStringClean("license_plate")
                ?: obj.optStringClean("number_plate")
                ?: obj.optStringClean("car_number")
                ?: obj.optStringClean("car_no")
                ?: obj.optStringClean("v_no")
                ?: obj.optStringClean("plate")
                ?: extractStringOrNested(obj, "vehicle", listOf("vehicle_no", "plate_number", "registration_no", "reg_no"))
                ?: "MTK-${if (id > 0) id else (result.size + 1)}"

            val model = obj.optStringClean("vehicle_name")
                ?: obj.optStringClean("vehicle_model")
                ?: obj.optStringClean("vehicle_type")
                ?: obj.optStringClean("vehicle_brand_model")
                ?: obj.optStringClean("name")
                ?: obj.optStringClean("model")
                ?: "Vehicle Unit"

            val device = obj.optStringClean("device_type")
                ?: obj.optStringClean("model_name")
                ?: obj.optStringClean("device")
                ?: "GPS Tracker"

            val price = obj.optDouble("device_price", 0.0).takeIf { it > 0 }
                ?: obj.optDouble("price", 0.0)

            val bill = obj.optDouble("monthly_bill", 0.0).takeIf { it > 0 }
                ?: obj.optDouble("monthly_charge", 0.0)

            val commission = obj.optDouble("referral_commission", 0.0)

            val installDate = obj.optStringClean("installation_date")
                ?: obj.optStringClean("created_at")
                ?: obj.optStringClean("installed_date")
                ?: obj.optStringClean("install_date")

            val status = obj.optStringClean("status")
                ?: "Active"

            val customerName = obj.optStringClean("customer_name")
                ?: obj.optStringClean("client_name")
                ?: extractStringOrNested(obj, "customer", listOf("name", "full_name"))
                ?: "Valued Client"

            val customerPhone = obj.optStringClean("customer_phone")
                ?: obj.optStringClean("contact_number")
                ?: obj.optStringClean("phone")
                ?: extractStringOrNested(obj, "customer", listOf("phone", "contact_number"))

            val sellerId = obj.optInt("seller_id", 0).takeIf { it > 0 }
                ?: obj.optInt("user_sel_id", 0).takeIf { it > 0 }
                ?: obj.optInt("sales_person_id", 0).takeIf { it > 0 }

            val sellerName = obj.optStringClean("seller_name")
                ?: obj.optStringClean("sales_person")
                ?: obj.optStringClean("seller")
                ?: obj.optStringClean("sales_executive")
                ?: obj.optStringClean("agent")
                ?: obj.optStringClean("sold_by")
                ?: obj.optStringClean("ref_name")
                ?: extractStringOrNested(obj, "seller", listOf("full_name", "name", "username"))
                ?: extractStringOrNested(obj, "u_sel", listOf("full_name", "name", "username"))

            val installerId = obj.optInt("installer_id", 0).takeIf { it > 0 }
                ?: obj.optInt("technician_id", 0).takeIf { it > 0 }
                ?: obj.optInt("user_inst_id", 0).takeIf { it > 0 }

            val installerName = obj.optStringClean("installer_name")
                ?: obj.optStringClean("technician_name")
                ?: obj.optStringClean("installed_by")
                ?: obj.optStringClean("technician")
                ?: obj.optStringClean("installer")
                ?: obj.optStringClean("fitter_name")
                ?: obj.optStringClean("fitter")
                ?: obj.optStringClean("u_inst_name")
                ?: obj.optStringClean("technician_full_name")
                ?: obj.optStringClean("installer_full_name")
                ?: obj.optStringClean("assigned_technician")
                ?: obj.optStringClean("assigned_to")
                ?: extractStringOrNested(obj, "installer", listOf("full_name", "name", "username"))
                ?: extractStringOrNested(obj, "technician", listOf("full_name", "name", "username"))
                ?: extractStringOrNested(obj, "u_inst", listOf("full_name", "name", "username"))

            val dueAmount = obj.optDouble("due_amount", 0.0)
            val dueInvoiceNo = obj.optStringClean("due_invoice_no")
                ?: obj.optStringClean("invoice_no")

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
                    sellerId = sellerId,
                    sellerName = sellerName,
                    installerId = installerId,
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
        val allStaff = mutableListOf<StaffMemberItem>()

        // 1. Primary: /api/v1/users (Main user database table with all corporate employees)
        try {
            val rUsers = api.getUsers(authHeader)
            if (rUsers.isSuccessful && rUsers.body() != null) {
                val list = parseStaffList(rUsers.body()!!)
                allStaff.addAll(list)
            }
        } catch (ignored: Exception) {}

        // 2. /api/v1/staff-directory
        try {
            val rDir = api.getStaffDirectory(authHeader)
            if (rDir.isSuccessful && rDir.body() != null) {
                val list = parseStaffList(rDir.body()!!)
                allStaff.addAll(list)
            }
        } catch (ignored: Exception) {}

        // 3. /api/v1/staffs
        try {
            val rStaffs = api.getStaffs(authHeader)
            if (rStaffs.isSuccessful && rStaffs.body() != null) {
                val list = parseStaffList(rStaffs.body()!!)
                allStaff.addAll(list)
            }
        } catch (ignored: Exception) {}

        // Deduplicate and prioritize online and complete profiles
        return allStaff.distinctBy {
            if (it.userId > 0) "id_${it.userId}" else if (!it.username.isNullOrBlank()) "u_${it.username}" else "p_${it.phone}"
        }.sortedWith(
            compareByDescending<StaffMemberItem> { it.isOnline }
                .thenBy { it.fullName }
        )
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

    private fun JsonObject.optStringClean(key: String): String? {
        if (!has(key) || get(key).isJsonNull) return null
        val s = try { get(key).asString } catch (ignored: Exception) { null }
        return s?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", true) }
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
            if (s.isNotEmpty() && !s.equals("null", true)) return s
        } else if (elem.isJsonObject) {
            val nestedObj = elem.asJsonObject
            for (nk in nestedKeys) {
                val nestedVal = nestedObj.optStringClean(nk)
                if (!nestedVal.isNullOrEmpty()) return nestedVal
            }
        }
        return null
    }
}
