package com.mtrack.installer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
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
    var filterType by remember { mutableIntStateOf(0) } // 0 = All, 1 = Unpaid Dues, 2 = Fully Paid

    val vehiclesList = salesData?.soldVehiclesList ?: emptyList()
    val dueVehiclesCount = vehiclesList.count { it.isDue || (it.dueAmount ?: 0.0) > 0 }
    val paidVehiclesCount = vehiclesList.size - dueVehiclesCount

    val filteredVehicles = remember(vehiclesList, searchQuery, filterType) {
        val q = searchQuery.trim().lowercase()
        vehiclesList.filter { v ->
            val matchQuery = q.isEmpty() ||
                    v.vehiclePlateNumber.lowercase().contains(q) ||
                    (v.customerName?.lowercase()?.contains(q) == true) ||
                    (v.customerPhone?.contains(q) == true) ||
                    (v.vehicleName?.lowercase()?.contains(q) == true)

            val matchFilter = when (filterType) {
                1 -> v.isDue || (v.dueAmount ?: 0.0) > 0 // Unpaid Dues only
                2 -> !v.isDue && (v.dueAmount ?: 0.0) <= 0 // Fully Paid only
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
                    text = "Sold Fleet Deliveries",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 20.sp
                    )
                )
                Text(
                    text = "${filteredVehicles.size} vehicles installed this month under your account",
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

        // Segmented Filter Capsules: All, Unpaid Dues, Fully Paid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FleetFilterPill(title = "All (${vehiclesList.size})", isSelected = filterType == 0, onClick = { filterType = 0 })
            FleetFilterPill(title = "Unpaid Dues ($dueVehiclesCount)", isSelected = filterType == 1, alertColor = AmberWarning, onClick = { filterType = 1 })
            FleetFilterPill(title = "Fully Paid ($paidVehiclesCount)", isSelected = filterType == 2, onClick = { filterType = 2 })
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Virtualized Fleet List with Integrated Dues
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
                                text = "No Installed Vehicles Found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Text(
                                text = "No vehicle deliveries recorded under this filter for this month.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                }
            } else {
                items(filteredVehicles, key = { it.id }) { vehicle ->
                    FleetWithDueCardItem(vehicle = vehicle, context = context)
                }
            }
        }
    }
}

@Composable
private fun FleetFilterPill(
    title: String,
    isSelected: Boolean,
    alertColor: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) (alertColor ?: BrandBluePrimary) else CardBackground,
        shape = RoundedCornerShape(20.dp),
        border = if (isSelected) null else CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isSelected) CardBackground else (alertColor ?: TextSecondary),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun FleetWithDueCardItem(
    vehicle: SoldVehicleItem,
    context: android.content.Context
) {
    val isDue = vehicle.isDue || (vehicle.dueAmount ?: 0.0) > 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (isDue) AmberWarning.copy(alpha = 0.5f) else BorderSubtle)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Client Name + Telematics Online/Pending Status
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
                            .background(if (isDue) AmberWarning.copy(alpha = 0.12f) else BrandBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials.ifEmpty { "C" },
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (isDue) AmberWarning else BrandBluePrimary,
                                fontWeight = FontWeight.Bold
                            )
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

                // Active Telematics Status
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

            // Row 2: Integrated Due Warning Banner (if vehicle has outstanding balance)
            if (isDue) {
                Surface(
                    color = AmberWarning.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AmberWarning.copy(alpha = 0.35f)))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Outlined.ReceiptLong, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(15.dp))
                            Text(
                                text = "Outstanding Due: BDT ${numberFormatShort(vehicle.dueAmount ?: 0.0)}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = AmberWarning,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        if (!vehicle.dueInvoiceNo.isNullOrBlank()) {
                            Text(
                                text = "#${vehicle.dueInvoiceNo}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            } else {
                Surface(
                    color = EmeraldBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(12.dp))
                        Text(
                            text = "Payment Status: Fully Paid",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Row 3: Plate Number, Installed Date & Direct Call Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceBackground)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = SlateNavyDark,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess)
                            )
                            Text(
                                text = vehicle.vehiclePlateNumber.ifBlank { "REG PENDING" },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                    Text(
                        text = "Device: ${vehicle.deviceType ?: "GPS Tracker"} • Installed: ${vehicle.installationDate ?: "N/A"}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                    )
                }

                if (!vehicle.customerPhone.isNullOrBlank()) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${vehicle.customerPhone}"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDue) AmberWarning else BrandBluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Outlined.Phone, contentDescription = null, tint = CardBackground, modifier = Modifier.size(12.dp))
                            Text(
                                text = if (isDue) "Call for Due" else "Call Client",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CardBackground,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.5.sp
                                )
                            )
                        }
                    }
                }
            }

            // Row 4: Seller & Installer Attribution
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val seller = vehicle.sellerName?.ifBlank { null } ?: "Direct"
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(imageVector = Icons.Outlined.Person, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                        Text(
                            text = "Seller: $seller",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Normal)
                        )
                    }

                    val installer = vehicle.installerName?.trim()?.takeIf {
                        it.isNotEmpty() && !it.equals("Technician", true) && !it.equals("null", true)
                    } ?: "Unassigned"
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(imageVector = Icons.Outlined.Build, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(13.dp))
                        Text(
                            text = "Technician: $installer",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (installer == "Unassigned") TextMuted else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (installer == "Unassigned") FontWeight.Normal else FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Hardware Price & Monthly Bill Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = BrandBlueLight.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Device: BDT ${vehicle.devicePrice?.toInt() ?: 0}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BrandBluePrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp
                        )
                    )
                }

                val bill = vehicle.monthlyBill ?: 0.0
                if (bill > 0.0) {
                    Surface(
                        color = EmeraldBg.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Bill: BDT ${bill.toInt()}/mo",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun numberFormatShort(amount: Double): String {
    return if (amount >= 100000) {
        String.format(java.util.Locale.US, "%.1fL", amount / 100000)
    } else if (amount >= 1000) {
        String.format(java.util.Locale.US, "%.1fK", amount / 1000)
    } else {
        String.format(java.util.Locale.US, "%.0f", amount)
    }
}
