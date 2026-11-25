package com.mtrack.installer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtrack.installer.data.model.SoldVehicleItem
import com.mtrack.installer.ui.theme.*
import com.mtrack.installer.ui.viewmodel.DashboardViewModel

@Composable
fun FleetScreen(
    dashboardViewModel: DashboardViewModel
) {
    val state by dashboardViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val salesData = state.dashboardData?.salesDepartment

    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableIntStateOf(0) } // 0 = All, 1 = Active, 2 = Pending

    val vehiclesList = salesData?.soldVehiclesList ?: emptyList()

    val filteredVehicles = remember(vehiclesList, searchQuery, filterType) {
        val q = searchQuery.trim().lowercase()
        vehiclesList.filter { v ->
            val matchQuery = q.isEmpty() ||
                    v.vehiclePlateNumber.lowercase().contains(q) ||
                    (v.customerName?.lowercase()?.contains(q) == true) ||
                    (v.customerPhone?.contains(q) == true) ||
                    (v.vehicleName?.lowercase()?.contains(q) == true)

            val matchFilter = when (filterType) {
                1 -> v.status?.lowercase() == "active"
                2 -> v.status?.lowercase() != "active"
                else -> true
            }
            matchQuery && matchFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Screen Title & Summary Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sold Fleet Portfolio",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 20.sp
                    )
                )
                Text(
                    text = "${filteredVehicles.size} total vehicles under your account",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                )
            }

            Surface(
                color = BrandBlueLight,
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BrandBlueBorder.copy(alpha = 0.5f)))
            ) {
                Text(
                    text = "${salesData?.monthName ?: "Running Month"}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BrandBluePrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Input Capsule
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            placeholder = {
                Text(
                    text = "Search plate number, vehicle or client...",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 12.sp)
                )
            },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandBluePrimary,
                unfocusedBorderColor = BorderSubtle,
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Segmented Filter Capsules
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FleetFilterPill(title = "All Fleet (${vehiclesList.size})", isSelected = filterType == 0, onClick = { filterType = 0 })
            FleetFilterPill(title = "Active Units", isSelected = filterType == 1, onClick = { filterType = 1 })
            FleetFilterPill(title = "Pending Setup", isSelected = filterType == 2, onClick = { filterType = 2 })
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Lazy Virtualized Fleet List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (filteredVehicles.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Outlined.DirectionsCar, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Fleet Records Found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Text(
                                text = "Try modifying your search phrase or clear active filters.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                }
            } else {
                items(filteredVehicles, key = { it.id }) { vehicle ->
                    FleetCardItem(vehicle = vehicle, context = context)
                }
            }
        }
    }
}

@Composable
private fun FleetFilterPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) BrandBluePrimary else CardBackground,
        shape = RoundedCornerShape(20.dp),
        border = if (isSelected) null else CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isSelected) CardBackground else TextSecondary,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun FleetCardItem(
    vehicle: SoldVehicleItem,
    context: android.content.Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val initials = (vehicle.customerName ?: "Client").split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BrandBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials.ifEmpty { "C" },
                            style = MaterialTheme.typography.labelMedium.copy(color = BrandBluePrimary, fontWeight = FontWeight.Bold)
                        )
                    }

                    Column {
                        Text(
                            text = vehicle.customerName ?: "Client",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        )
                        Text(
                            text = vehicle.vehicleName ?: "Vehicle Unit",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.5.sp)
                        )
                    }
                }

                val isAct = vehicle.status?.lowercase() == "active"
                Surface(
                    color = if (isAct) EmeraldBg else AmberWarning.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(if (isAct) EmeraldSuccess else AmberWarning))
                        Text(
                            text = vehicle.status ?: "Pending",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isAct) EmeraldSuccess else AmberWarning,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Plate & Telematics Info Capsule
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceBackground)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = vehicle.vehiclePlateNumber,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = "Device: ${vehicle.deviceType ?: "Voice Tracker"} • Installed: ${vehicle.installationDate ?: "N/A"}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                    )
                }

                if (!vehicle.customerPhone.isNullOrBlank()) {
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${vehicle.customerPhone}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.Phone, contentDescription = "Call Client", tint = BrandBluePrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
