package com.mtrack.installer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtrack.installer.data.model.VehicleItem
import com.mtrack.installer.ui.theme.*
import com.mtrack.installer.ui.viewmodel.InstallFormUiState
import com.mtrack.installer.ui.viewmodel.InstallViewModel

@Composable
fun InstallWizardScreen(
    viewModel: InstallViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step Indicator Progress Header
        StepIndicatorHeader(currentStep = state.currentStep)

        // Error Banner if any
        if (state.errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = RoseBg),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RoseError.copy(alpha = 0.3f))),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.ErrorOutline, contentDescription = null, tint = RoseError, modifier = Modifier.size(18.dp))
                    Text(
                        text = state.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = RoseError, fontWeight = FontWeight.Medium)
                    )
                }
            }
        }

        // Dynamic Wizard Body Based on Step
        when (state.currentStep) {
            1 -> Step1CustomerDetails(state, viewModel)
            2 -> Step2VehiclesFleet(state, viewModel)
            3 -> Step3ScheduleLocation(state, viewModel)
            4 -> Step4ReviewAndSubmit(state, viewModel)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// =============================================================================
// STEP 1: CUSTOMER DETAILS
// =============================================================================
@Composable
private fun Step1CustomerDetails(state: InstallFormUiState, viewModel: InstallViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Step 1: Customer Identification",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
            )
            Text(
                text = "Provide customer contact and NID number for vehicle registration.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )

            FormInputField(
                label = "Full Name *",
                value = state.fullName,
                onValueChange = { viewModel.updateCustomerDetails(name = it) },
                placeholder = "e.g. Md. Kabir Hossain",
                icon = Icons.Outlined.Person
            )

            FormInputField(
                label = "Contact Number *",
                value = state.contactNumber,
                onValueChange = { viewModel.updateCustomerDetails(phone = it) },
                placeholder = "017XXXXXXXX",
                icon = Icons.Outlined.Phone
            )

            FormInputField(
                label = "Alternative Phone",
                value = state.altNumber,
                onValueChange = { viewModel.updateCustomerDetails(altPhone = it) },
                placeholder = "018XXXXXXXX",
                icon = Icons.Outlined.Call
            )

            FormInputField(
                label = "NID Number *",
                value = state.nidNumber,
                onValueChange = { viewModel.updateCustomerDetails(nid = it) },
                placeholder = "19901234567890123",
                icon = Icons.Outlined.Badge
            )

            FormInputField(
                label = "Profession / Business",
                value = state.profession,
                onValueChange = { viewModel.updateCustomerDetails(profession = it) },
                placeholder = "e.g. Businessman, Transport Owner",
                icon = Icons.Outlined.WorkOutline
            )

            FormInputField(
                label = "Full Billing Address",
                value = state.fullAddress,
                onValueChange = { viewModel.updateCustomerDetails(address = it) },
                placeholder = "House, Road, Area, District",
                icon = Icons.Outlined.Home
            )

            Button(
                onClick = { viewModel.nextStep() },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
            ) {
                Text(text = "Next: Add Vehicles", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium, color = CardBackground))
                Spacer(modifier = Modifier.width(6.dp))
                Icon(imageVector = Icons.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// =============================================================================
// STEP 2: VEHICLES FLEET REPEATER
// =============================================================================
@Composable
private fun Step2VehiclesFleet(state: InstallFormUiState, viewModel: InstallViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.between,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Step 2: Vehicles Fleet", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
                Text(text = "${state.vehicles.size} vehicle(s) configured", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            }
            OutlinedButton(
                onClick = { viewModel.addVehicle() },
                shape = RoundedCornerShape(10.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BrandBluePrimary))
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Add Vehicle", style = MaterialTheme.typography.labelSmall.copy(color = BrandBluePrimary, fontWeight = FontWeight.Medium))
            }
        }

        state.vehicles.forEachIndexed { index, vehicle ->
            VehicleCardItem(
                index = index,
                total = state.vehicles.size,
                vehicle = vehicle,
                onUpdate = { updated -> viewModel.updateVehicle(index, updated) },
                onRemove = { viewModel.removeVehicle(index) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.prevStep() },
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Back", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
            }

            Button(
                onClick = { viewModel.nextStep() },
                modifier = Modifier.weight(1.5f).height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
            ) {
                Text(text = "Next: Schedule", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium, color = CardBackground))
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = Icons.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun VehicleCardItem(
    index: Int,
    total: Int,
    vehicle: VehicleItem,
    onUpdate: (VehicleItem) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(BrandBlueLight), contentAlignment = Alignment.Center) {
                        Text(text = "${index + 1}", style = MaterialTheme.typography.labelSmall.copy(color = BrandBluePrimary, fontWeight = FontWeight.SemiBold))
                    }
                    Text(text = "Vehicle #${index + 1}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp))
                }
                if (total > 1) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = RoseError, modifier = Modifier.size(18.dp))
                    }
                }
            }

            FormInputField(
                label = "Registration / Plate No. *",
                value = vehicle.registrationNo,
                onValueChange = { onUpdate(vehicle.copy(registrationNo = it)) },
                placeholder = "DHAKA-METRO-GA-11-2233",
                icon = Icons.Outlined.DirectionsCar
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    FormInputField(
                        label = "Vehicle Type",
                        value = vehicle.vehicleType,
                        onValueChange = { onUpdate(vehicle.copy(vehicleType = it)) },
                        placeholder = "Car, Bike, Bus, Truck",
                        icon = Icons.Outlined.Commute
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    FormInputField(
                        label = "Device Tier",
                        value = vehicle.deviceType,
                        onValueChange = { onUpdate(vehicle.copy(deviceType = it)) },
                        placeholder = "Voice, Basic, Fuel",
                        icon = Icons.Outlined.Sensors
                    )
                }
            }
        }
    }
}

// =============================================================================
// STEP 3: SCHEDULE & INSTALLATION LOCATION
// =============================================================================
@Composable
private fun Step3ScheduleLocation(state: InstallFormUiState, viewModel: InstallViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = "Step 3: Installation & Schedule", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
            Text(text = "Select when and where our technician team will perform the hardware installation.", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))

            FormInputField(
                label = "Installation Location / City *",
                value = state.installLocation,
                onValueChange = { viewModel.updateScheduleDetails(location = it) },
                placeholder = "e.g. Uttara Sector 10, Dhaka",
                icon = Icons.Outlined.LocationOn
            )

            FormInputField(
                label = "Preferred Date & Time",
                value = state.prefDateTime,
                onValueChange = { viewModel.updateScheduleDetails(dateTime = it) },
                placeholder = "e.g. Tomorrow 11:00 AM",
                icon = Icons.Outlined.CalendarToday
            )

            FormInputField(
                label = "Reference / Agent Name",
                value = state.refName,
                onValueChange = { viewModel.updateScheduleDetails(refName = it) },
                placeholder = "Optional",
                icon = Icons.Outlined.PersonPin
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.prevStep() },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Back", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
                }

                Button(
                    onClick = { viewModel.nextStep() },
                    modifier = Modifier.weight(1.5f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
                ) {
                    Text(text = "Next: Review", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium, color = CardBackground))
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// =============================================================================
// STEP 4: REVIEW & LIVE API SUBMISSION
// =============================================================================
@Composable
private fun Step4ReviewAndSubmit(state: InstallFormUiState, viewModel: InstallViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = "Step 4: Review & Live API Submission", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
            Text(text = "Verify all details before submitting to M Track ERP.", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))

            // Summary Info Card
            Surface(
                color = SurfaceBackground,
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SummaryRow("Customer Name", state.fullName)
                    SummaryRow("Contact Phone", state.contactNumber)
                    SummaryRow("NID Number", state.nidNumber)
                    SummaryRow("Installation Place", state.installLocation)
                    SummaryRow("Schedule", state.prefDateTime.ifEmpty { "Immediate" })
                    SummaryRow("Total Vehicles", "${state.vehicles.size} Unit(s)")
                }
            }

            // Vehicles breakdown preview
            state.vehicles.forEachIndexed { i, v ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "${i + 1}. ${v.registrationNo}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    Text(text = "${v.vehicleType} (${v.deviceType})", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.prevStep() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !state.isSubmitting
                ) {
                    Text(text = "Edit", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
                }

                Button(
                    onClick = { viewModel.submitRequest() },
                    modifier = Modifier.weight(2f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                    enabled = !state.isSubmitting
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CardBackground, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Submitting API...", style = MaterialTheme.typography.labelLarge.copy(color = CardBackground))
                    } else {
                        Icon(imageVector = Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Submit Request", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium, color = CardBackground))
                    }
                }
            }
        }
    }
}

// =============================================================================
// REUSABLE COMPONENTS
// =============================================================================
@Composable
private fun StepIndicatorHeader(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("Customer", "Vehicles", "Schedule", "Review").forEachIndexed { index, title ->
            val stepNum = index + 1
            val isActive = currentStep >= stepNum
            val isCurrent = currentStep == stepNum

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isActive) BrandBluePrimary else BorderSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentStep > stepNum) {
                        Icon(imageVector = Icons.Outlined.Check, contentDescription = null, tint = CardBackground, modifier = Modifier.size(14.dp))
                    } else {
                        Text(text = "$stepNum", style = MaterialTheme.typography.labelSmall.copy(color = if (isActive) CardBackground else TextSecondary, fontWeight = FontWeight.Medium))
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        color = if (isCurrent) BrandBluePrimary else TextSecondary,
                        fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun FormInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, color = TextPrimary))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = placeholder, style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)) },
            leadingIcon = { Icon(imageVector = icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = BorderSubtle,
                focusedBorderColor = BrandBluePrimary,
                unfocusedContainerColor = CardBackground,
                focusedContainerColor = CardBackground
            ),
            singleLine = true
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Medium))
    }
}
