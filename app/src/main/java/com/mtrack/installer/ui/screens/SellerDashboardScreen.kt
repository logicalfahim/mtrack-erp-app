package com.mtrack.installer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtrack.installer.data.model.SoldVehicleItem
import com.mtrack.installer.ui.theme.*
import com.mtrack.installer.ui.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SellerDashboardScreen(
    dashboardViewModel: DashboardViewModel,
    onNavigateToInstall: () -> Unit
) {
    val state by dashboardViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val salesData = state.dashboardData?.salesDepartment
    val user = state.userProfile

    val bdtFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("bn", "BD")).apply {
            maximumFractionDigits = 0
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // 1. User Header & Month Pacing
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hello, ${user?.fullName ?: "Executive"}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Surface(
                                color = BrandBlueLight,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = user?.designation ?: "Sales & Marketing",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = BrandBluePrimary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 10.5.sp
                                    )
                                )
                            }
                            Text(
                                text = "• ${salesData?.monthName ?: "Running Month"}",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    IconButton(
                        onClick = { dashboardViewModel.loadDashboard() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceBackground)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = BrandBluePrimary)
                        } else {
                            Icon(imageVector = Icons.Outlined.Refresh, contentDescription = "Refresh", tint = BrandBluePrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // 2. Target Progress & Achievement Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(BrandBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Outlined.GpsFixed, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(
                                    text = "Monthly Sales Target",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                )
                                Text(
                                    text = "Vehicle Units Pacing Engine",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                )
                            }
                        }

                        Surface(
                            color = if ((salesData?.progressPercentage ?: 0f) >= 100f) EmeraldBg else BrandBlueLight,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${salesData?.progressPercentage ?: 0f}%",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = if ((salesData?.progressPercentage ?: 0f) >= 100f) EmeraldSuccess else BrandBluePrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    // Progress Bar
                    val progress = ((salesData?.progressPercentage ?: 0f) / 100f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = BrandBluePrimary,
                        trackColor = BorderSubtle
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Achieved: ${salesData?.achievedQty ?: 0} Units",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "Target: ${salesData?.targetQty ?: 0} Units",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }
            }
        }

        // 3. KPI Metrics Grid (3 Key Pillars: Due, Company Sales, Commission)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Client Due Card (বাকি)
                KpiMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Client Dues (বাকি)",
                    value = "৳${numberFormatShort(salesData?.totalDueAmount ?: 0.0)}",
                    subtitle = "Unpaid from clients",
                    icon = Icons.Outlined.ReceiptLong,
                    iconTint = AmberWarning,
                    iconBg = AmberWarning.copy(alpha = 0.12f)
                )

                // Company Total Sales (কোম্পানির মোট সেল)
                KpiMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Company Sales",
                    value = "${salesData?.companyTotalSales ?: 0}",
                    subtitle = "Total fleet this month",
                    icon = Icons.Outlined.Domain,
                    iconTint = BrandBluePrimary,
                    iconBg = BrandBlueLight
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Earned Commission
                KpiMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Commission",
                    value = "৳${numberFormatShort(salesData?.earnedCommission ?: 0.0)}",
                    subtitle = "Month-to-date earnings",
                    icon = Icons.Outlined.Payments,
                    iconTint = EmeraldSuccess,
                    iconBg = EmeraldBg
                )

                // Quick Action: New Install
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onNavigateToInstall),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandBluePrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(CardBackground.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Outlined.AddCircleOutline, contentDescription = null, tint = CardBackground, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "New Sale / Install",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = CardBackground, fontSize = 13.5.sp)
                        )
                        Text(
                            text = "Onboard vehicle fleet",
                            style = MaterialTheme.typography.labelSmall.copy(color = CardBackground.copy(alpha = 0.85f), fontSize = 10.sp)
                        )
                    }
                }
            }
        }

        // 4. Sold Vehicles Fleet Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sold Vehicles Fleet (${salesData?.soldVehiclesCount ?: 0})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Plate Numbers & Status",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
            }
        }

        // 5. Sold Vehicles List
        val vehiclesList = salesData?.soldVehiclesList ?: emptyList()
        if (vehiclesList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Outlined.DirectionsCar, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No sold vehicles recorded yet.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "Click 'New Sale / Install' to register your first vehicle.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                }
            }
        } else {
            items(vehiclesList, key = { it.id }) { vehicle ->
                SoldVehicleRowItem(vehicle, context)
            }
        }
    }
}

@Composable
private fun KpiMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    iconBg: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 18.sp
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.5.sp)
            )
        }
    }
}

@Composable
private fun SoldVehicleRowItem(
    vehicle: SoldVehicleItem,
    context: android.content.Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Plate Number Badge
                Surface(
                    color = SurfaceBackground,
                    shape = RoundedCornerShape(6.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
                ) {
                    Text(
                        text = vehicle.vehiclePlateNumber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = BrandBluePrimary,
                            fontSize = 12.sp
                        )
                    )
                }

                // Status Badge
                val isAct = vehicle.status?.lowercase() == "active"
                Surface(
                    color = if (isAct) EmeraldBg else AmberWarning.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = vehicle.status ?: "Pending",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isAct) EmeraldSuccess else AmberWarning,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vehicle.customerName ?: "Customer",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "${vehicle.vehicleName ?: "Vehicle"} • Installed: ${vehicle.installationDate ?: "N/A"}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.5.sp
                        )
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
                        Icon(imageVector = Icons.Outlined.Phone, contentDescription = "Call", tint = BrandBluePrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

private fun numberFormatShort(amount: Double): String {
    return if (amount >= 100000) {
        String.format(Locale.US, "%.1fL", amount / 100000)
    } else if (amount >= 1000) {
        String.format(Locale.US, "%.1fK", amount / 1000)
    } else {
        String.format(Locale.US, "%.0f", amount)
    }
}
