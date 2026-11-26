package com.mtrack.installer.data.model

import com.google.gson.annotations.SerializedName

/**
 * Individual vehicle specification in fleet repeater
 */
data class VehicleItem(
    @SerializedName("vehicle_type")
    val vehicleType: String = "Car",

    @SerializedName("bus_company")
    val busCompany: String? = null,

    @SerializedName("vehicle_brand_model")
    val vehicleBrandModel: String = "",

    @SerializedName("registration_no")
    val registrationNo: String = "",

    @SerializedName("chassis_no")
    val chassisNo: String? = null,

    @SerializedName("engine_no")
    val engineNo: String? = null,

    @SerializedName("route_name")
    val routeName: String? = null,

    @SerializedName("device_type")
    val deviceType: String = "Voice", // Voice, Non-Voice, Two Way Communication

    @SerializedName("start_type")
    val startType: String = "Key Start", // Key Start, Push Start

    @SerializedName("engine_cut")
    val engineCut: String = "Yes" // Yes, No
)

/**
 * Full submission payload for /api/v1/install-requests (Aligned with index.php DB schema)
 */
data class InstallationRequestPayload(
    @SerializedName("full_name")
    val fullName: String,

    @SerializedName("contact_number")
    val contactNumber: String,

    @SerializedName("alt_number")
    val altNumber: String? = null,

    @SerializedName("email_address")
    val emailAddress: String? = null,

    @SerializedName("profession")
    val profession: String? = null,

    @SerializedName("nid_number")
    val nidNumber: String,

    @SerializedName("full_address")
    val fullAddress: String,

    @SerializedName("install_location")
    val installLocation: String,

    @SerializedName("pref_date_time")
    val prefDateTime: String,

    @SerializedName("ref_name")
    val refName: String? = null,

    @SerializedName("ref_phone")
    val refPhone: String? = null,

    @SerializedName("ref_area")
    val refArea: String? = null,

    @SerializedName("vehicles")
    val vehicles: List<VehicleItem>
)

/**
 * Standard M Track REST Gateway Response
 */
data class ApiResponse<T>(
    @SerializedName("status")
    val status: String,

    @SerializedName("code")
    val code: Int,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("meta")
    val meta: ResponseMeta? = null,

    @SerializedName("data")
    val data: T? = null
)

data class ResponseMeta(
    @SerializedName("batch_id")
    val batchId: String? = null,

    @SerializedName("vehicles_count")
    val vehiclesCount: Int? = null,

    @SerializedName("timestamp")
    val timestamp: String? = null
)

data class InstallSubmissionResult(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("batch_id")
    val batchId: String? = null
)
