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
    val currentStep: Int = 1, // 1: Customer, 2: Vehicles, 3: Schedule, 4: Review, 5: Success

    // Step 1: Customer Details
    val fullName: String = "",
    val contactNumber: String = "",
    val altNumber: String = "",
    val profession: String = "",
    val nidNumber: String = "",
    val fullAddress: String = "",

    // Step 2: Vehicles Fleet
    val vehicles: List<VehicleItem> = listOf(VehicleItem()),

    // Step 3: Installation & Schedule
    val installLocation: String = "",
    val prefDateTime: String = "",
    val refName: String = "",
    val refPhone: String = "",

    // Process & Network state
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val submittedBatchId: String? = null
)

class InstallViewModel(
    private val repository: InstallRepository = InstallRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(InstallFormUiState())
    val uiState: StateFlow<InstallFormUiState> = _uiState.asStateFlow()

    fun updateCustomerDetails(
        name: String? = null,
        phone: String? = null,
        altPhone: String? = null,
        profession: String? = null,
        nid: String? = null,
        address: String? = null
    ) {
        _uiState.update { current ->
            current.copy(
                fullName = name ?: current.fullName,
                contactNumber = phone ?: current.contactNumber,
                altNumber = altPhone ?: current.altNumber,
                profession = profession ?: current.profession,
                nidNumber = nid ?: current.nidNumber,
                fullAddress = address ?: current.fullAddress,
                errorMessage = null
            )
        }
    }

    // -------------------------------------------------------------------------
    // Dynamic Vehicles Fleet Operations
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
    // Step 3: Schedule & Location Operations
    // -------------------------------------------------------------------------
    fun updateScheduleDetails(
        location: String? = null,
        dateTime: String? = null,
        refName: String? = null,
        refPhone: String? = null
    ) {
        _uiState.update { current ->
            current.copy(
                installLocation = location ?: current.installLocation,
                prefDateTime = dateTime ?: current.prefDateTime,
                refName = refName ?: current.refName,
                refPhone = refPhone ?: current.refPhone,
                errorMessage = null
            )
        }
    }

    // -------------------------------------------------------------------------
    // Step Navigation
    // -------------------------------------------------------------------------
    fun nextStep() {
        val state = _uiState.value
        when (state.currentStep) {
            1 -> {
                if (state.fullName.isBlank() || state.contactNumber.isBlank() || state.nidNumber.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "Please enter Full Name, Phone, and NID.") }
                    return
                }
            }
            2 -> {
                if (state.vehicles.any { it.registrationNo.isBlank() }) {
                    _uiState.update { it.copy(errorMessage = "Please enter Registration Number for each vehicle.") }
                    return
                }
            }
            3 -> {
                if (state.installLocation.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "Please specify Installation Location/City.") }
                    return
                }
            }
        }

        _uiState.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(4), errorMessage = null) }
    }

    fun prevStep() {
        _uiState.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(1), errorMessage = null) }
    }

    // -------------------------------------------------------------------------
    // Submit Live Request to API Gateway
    // -------------------------------------------------------------------------
    fun submitRequest() {
        val state = _uiState.value
        val payload = InstallationRequestPayload(
            fullName = state.fullName.trim(),
            contactNumber = state.contactNumber.trim(),
            altNumber = state.altNumber.trim().ifEmpty { null },
            profession = state.profession.trim().ifEmpty { null },
            nidNumber = state.nidNumber.trim(),
            fullAddress = state.fullAddress.trim(),
            installLocation = state.installLocation.trim(),
            prefDateTime = state.prefDateTime.trim().ifEmpty { "Immediate" },
            refName = state.refName.trim().ifEmpty { null },
            refPhone = state.refPhone.trim().ifEmpty { null },
            vehicles = state.vehicles
        )

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.submitRequest(payload)
            result.fold(
                onSuccess = { res ->
                    val batchId = res.meta?.batchId ?: res.data?.batchId ?: "MTK-${System.currentTimeMillis() % 100000}"
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            currentStep = 5, // Show Success Confirmation Screen
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
