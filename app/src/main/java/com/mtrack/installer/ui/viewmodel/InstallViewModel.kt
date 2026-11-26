package com.mtrack.installer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtrack.installer.data.model.InstallationRequestPayload
import com.mtrack.installer.data.model.VehicleItem
import com.mtrack.installer.data.repository.InstallRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InstallFormUiState(
    // 5-Step Progressive Onboarding:
    // 1: Identity & NID, 2: Contact & Profile, 3: Vehicles Fleet, 4: Schedule & Referral, 5: Review, 6: Success
    val currentStep: Int = 1,

    // Step 1: Identity & NID Verification
    val fullName: String = "",
    val nidNumber: String = "",
    val fullAddress: String = "",
    val isNidScanned: Boolean = false, // When true, shows locked/verified scan indicator

    // Step 2: Contact & Additional Profile
    val contactNumber: String = "",
    val altNumber: String = "",
    val emailAddress: String = "",
    val profession: String = "",

    // Step 3: Vehicles Fleet (Multi-Vehicle Repeater)
    val vehicles: List<VehicleItem> = listOf(VehicleItem()),

    // Step 4: Installation Schedule & Referral Information
    val installLocation: String = "",
    val prefDateTime: String = "",
    val refName: String = "",
    val refPhone: String = "",
    val refArea: String = "",

    // Submission & Gateway State
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val submittedBatchId: String? = null
)

class InstallViewModel(
    private val repository: InstallRepository = InstallRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(InstallFormUiState())
    val uiState: StateFlow<InstallFormUiState> = _uiState.asStateFlow()

    // -------------------------------------------------------------------------
    // Step 1: Identity & NID Operations
    // -------------------------------------------------------------------------
    fun updateIdentityDetails(
        name: String? = null,
        nid: String? = null,
        address: String? = null,
        scanned: Boolean? = null
    ) {
        _uiState.update { current ->
            current.copy(
                fullName = name ?: current.fullName,
                nidNumber = nid ?: current.nidNumber,
                fullAddress = address ?: current.fullAddress,
                isNidScanned = scanned ?: current.isNidScanned,
                errorMessage = null
            )
        }
    }

    /**
     * Auto-populates data extracted from Front & Back NID OCR scan
     */
    fun applyNidScanResult(name: String, nid: String, address: String) {
        _uiState.update { current ->
            current.copy(
                fullName = name.ifEmpty { current.fullName },
                nidNumber = nid.ifEmpty { current.nidNumber },
                fullAddress = address.ifEmpty { current.fullAddress },
                isNidScanned = true,
                errorMessage = null
            )
        }
    }

    // -------------------------------------------------------------------------
    // Step 2: Contact & Profile Operations
    // -------------------------------------------------------------------------
    fun updateContactDetails(
        phone: String? = null,
        altPhone: String? = null,
        email: String? = null,
        profession: String? = null
    ) {
        _uiState.update { current ->
            current.copy(
                contactNumber = phone ?: current.contactNumber,
                altNumber = altPhone ?: current.altNumber,
                emailAddress = email ?: current.emailAddress,
                profession = profession ?: current.profession,
                errorMessage = null
            )
        }
    }

    // -------------------------------------------------------------------------
    // Step 3: Vehicles Fleet Operations (Repeater)
    // -------------------------------------------------------------------------
    fun addVehicle() {
        _uiState.update { current ->
            current.copy(vehicles = current.vehicles + VehicleItem())
        }
    }

    fun removeVehicle(index: Int) {
        _uiState.update { current ->
            if (current.vehicles.size > 1) {
                val updated = current.vehicles.toMutableList()
                updated.removeAt(index)
                current.copy(vehicles = updated)
            } else {
                current
            }
        }
    }

    fun updateVehicle(index: Int, vehicle: VehicleItem) {
        _uiState.update { current ->
            if (index in current.vehicles.indices) {
                val updated = current.vehicles.toMutableList()
                updated[index] = vehicle
                current.copy(vehicles = updated)
            } else {
                current
            }
        }
    }

    // -------------------------------------------------------------------------
    // Step 4: Schedule & Referral Operations
    // -------------------------------------------------------------------------
    fun updateScheduleDetails(
        location: String? = null,
        dateTime: String? = null,
        refName: String? = null,
        refPhone: String? = null,
        refArea: String? = null
    ) {
        _uiState.update { current ->
            current.copy(
                installLocation = location ?: current.installLocation,
                prefDateTime = dateTime ?: current.prefDateTime,
                refName = refName ?: current.refName,
                refPhone = refPhone ?: current.refPhone,
                refArea = refArea ?: current.refArea,
                errorMessage = null
            )
        }
    }

    fun updateSchedule(
        location: String? = null,
        dateTime: String? = null,
        refName: String? = null,
        refPhone: String? = null,
        refArea: String? = null
    ) {
        updateScheduleDetails(location, dateTime, refName, refPhone, refArea)
    }

    // -------------------------------------------------------------------------
    /**
     * Skip NID scan and proceed directly to manual entry in Step 2
     */
    fun skipNidToManual() {
        _uiState.update { current ->
            current.copy(
                isNidScanned = false,
                currentStep = 2,
                errorMessage = null
            )
        }
    }

    /**
     * Proceed to Step 2 with data extracted from NID scan
     */
    fun proceedFromNidScan() {
        _uiState.update { current ->
            current.copy(
                isNidScanned = true,
                currentStep = 2,
                errorMessage = null
            )
        }
    }

    // -------------------------------------------------------------------------
    // 5-Step Progressive Navigation & Validation
    // -------------------------------------------------------------------------
    fun nextStep() {
        val state = _uiState.value
        when (state.currentStep) {
            1 -> {
                // Step 1: NID capture - allow advancing to Step 2
            }
            2 -> {
                // Step 2: Customer Identity Validation
                if (state.fullName.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "Please enter Customer Full Name.") }
                    return
                }
                if (state.nidNumber.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "Please enter National ID (NID) Number.") }
                    return
                }
                if (state.fullAddress.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "Please enter Full Billing Address.") }
                    return
                }
            }
            3 -> {
                // Step 3: Contact & Profile Validation
                if (state.contactNumber.isBlank() || state.contactNumber.length != 11 || !state.contactNumber.startsWith("01")) {
                    _uiState.update { it.copy(errorMessage = "Please enter a valid 11-digit mobile number (e.g. 017XXXXXXXX).") }
                    return
                }
                if (state.emailAddress.isNotBlank() && !state.emailAddress.contains("@")) {
                    _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
                    return
                }
            }
            4 -> {
                // Step 4: Vehicles Fleet Validation (Bus vs Non-Bus)
                state.vehicles.forEachIndexed { index, veh ->
                    val num = index + 1
                    if (veh.vehicleType == "Bus") {
                        if (veh.busCompany.isNullOrBlank()) {
                            _uiState.update { it.copy(errorMessage = "Vehicle #$num (Bus): Please provide Bus Company / Fleet Operator Name.") }
                            return
                        }
                        if (veh.registrationNo.isBlank()) {
                            _uiState.update { it.copy(errorMessage = "Vehicle #$num (Bus): Please provide Vehicle Registration Number.") }
                            return
                        }
                        if (veh.chassisNo.isNullOrBlank()) {
                            _uiState.update { it.copy(errorMessage = "Vehicle #$num (Bus): Please provide Chassis Number.") }
                            return
                        }
                    } else {
                        if (veh.registrationNo.isBlank()) {
                            _uiState.update { it.copy(errorMessage = "Vehicle #$num (${veh.vehicleType}): Please provide Registration Number.") }
                            return
                        }
                        if (veh.vehicleBrandModel.isBlank()) {
                            _uiState.update { it.copy(errorMessage = "Vehicle #$num (${veh.vehicleType}): Please provide Vehicle Brand & Year (e.g. Toyota 2019).") }
                            return
                        }
                    }
                }
            }
            5 -> {
                // Step 5: Schedule Validation
                if (state.installLocation.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "Please specify Installation Location / City.") }
                    return
                }
            }
        }

        _uiState.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(6), errorMessage = null) }
    }

    fun prevStep() {
        _uiState.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(1), errorMessage = null) }
    }

    // -------------------------------------------------------------------------
    // Step 5: Submit Request to ERP Gateway
    // -------------------------------------------------------------------------
    fun submitRequest() {
        val state = _uiState.value

        // Format vehicles to strictly align with index.php DB mapping
        val cleanVehicles = state.vehicles.map { v ->
            if (v.vehicleType == "Bus") {
                v.copy(
                    vehicleBrandModel = "Commercial Bus",
                    registrationNo = v.registrationNo.trim().uppercase(),
                    chassisNo = v.chassisNo?.trim()?.uppercase()
                )
            } else {
                v.copy(
                    busCompany = null,
                    chassisNo = null,
                    registrationNo = v.registrationNo.trim().uppercase()
                )
            }
        }

        val payload = InstallationRequestPayload(
            fullName = state.fullName.trim(),
            contactNumber = state.contactNumber.trim(),
            altNumber = state.altNumber.trim().ifEmpty { null },
            emailAddress = state.emailAddress.trim().ifEmpty { null },
            profession = state.profession.trim().ifEmpty { "General Client" },
            nidNumber = state.nidNumber.trim(),
            fullAddress = state.fullAddress.trim(),
            installLocation = state.installLocation.trim(),
            prefDateTime = state.prefDateTime.trim().ifEmpty { "Immediate" },
            refName = state.refName.trim().ifEmpty { null },
            refPhone = state.refPhone.trim().ifEmpty { null },
            refArea = state.refArea.trim().ifEmpty { null },
            vehicles = cleanVehicles
        )

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.submitRequest(payload)
            result.fold(
                onSuccess = { res ->
                    val batchId = res.meta?.batchId ?: res.data?.batchId ?: "BATCH-${System.currentTimeMillis() % 1000000}"
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            currentStep = 7, // Step 7 = Success Screen
                            submittedBatchId = batchId
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = err.localizedMessage ?: "Failed to connect to M Track API."
                        )
                    }
                }
            )
        }
    }

    fun resetForm() {
        _uiState.value = InstallFormUiState()
    }
}
