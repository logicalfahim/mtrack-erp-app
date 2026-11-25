package com.mtrack.installer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import com.mtrack.installer.data.model.ClientDueItem
import com.mtrack.installer.data.model.SoldVehicleItem
import com.mtrack.installer.ui.theme.*
import com.mtrack.installer.ui.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SellerDashboardScreen(
    dashboardViewModel: DashboardViewModel,
    onNavigateToInstall: () -> Unit
) {
    val state by dashboardViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val salesData = state.dashboardData?.salesDepartment
    val user = state.userProfile

    // Tab state: 0 = Sold Fleet, 1 = Client Dues List
    var selectedTab by remember { mutableIntStateOf(0) }

    // Current month string from API or fallback
    val activeMonth = salesData?.month ?: SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // 1. User Header & Profile
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
                                text = "• Personal Scope",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    IconButton(
                        onClick = { dashboardViewModel.loadDashboard(activeMonth) },
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

        // 2. Interactive Month Navigator Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Month Button
                    IconButton(
                        onClick = {
                            val prev = shiftMonth(activeMonth, -1)
                            dashboardViewModel.loadDashboard(prev)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Month Display with Calendar Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = salesData?.monthName ?: formatMonthDisplay(activeMonth),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 14.5.sp
                            )
                        )
                    }

                    // Next Month Button
                    IconButton(
                        onClick = {
                            val next = shiftMonth(activeMonth, 1)
                            dashboardViewModel.loadDashboard(next)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = "Next Month",
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 3. Target Progress & MoM Growth Card
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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

                        // Achievement Percentage Badge
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

                    // Linear Progress Bar
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Achieved: ${salesData?.achievedQty ?: 0} Units",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Target: ${salesData?.targetQty ?: 0} Units",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }

                    Divider(color = BorderSubtle, thickness = 0.8.dp)

                    // Month-over-Month (MoM) Growth Comparison Banner
                    val growth = salesData?.growthRate ?: 0f
                    val lastAchieved = salesData?.lastMonthAchieved ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val isPositive = growth >= 0f
                            val badgeBg = if (isPositive) EmeraldBg else AmberWarning.copy(alpha = 0.12f)
                            val badgeColor = if (isPositive) EmeraldSuccess else AmberWarning
                            val icon = if (isPositive) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown

                            Surface(
                                color = badgeBg,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(imageVector = icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "${if (growth > 0) "+" else ""}${growth}% MoM Growth",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = badgeColor,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Last Month: $lastAchieved Units",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
                        )
                    }
                }
            }
        }

        // 4. KPI Metrics Grid (Client Dues, Company Sales, Commission, New Install)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Client Due Card (বাকি) - Clicking switches to Due Tab
                KpiMetricCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = 1 },
                    title = "Client Dues (বাকি)",
                    value = "৳${numberFormatShort(salesData?.totalDueAmount ?: 0.0)}",
                    subtitle = "${salesData?.clientDuesCount ?: 0} unpaid invoices",
                    icon = Icons.Outlined.ReceiptLong,
                    iconTint = AmberWarning,
                    iconBg = AmberWarning.copy(alpha = 0.12f)
                )

                // Company Total Sales (কোম্পানির মোট সেল)
                KpiMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Company Total",
                    value = "${salesData?.companyTotalSales ?: 0}",
                    subtitle = "Fleet sold nationwide",
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

                // Quick Action: New Install / Sale
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
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(CardBackground.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Outlined.AddCircleOutline, contentDescription = null, tint = CardBackground, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "New Sale / Install",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = CardBackground, fontSize = 13.sp)
                        )
                        Text(
                            text = "Onboard vehicle fleet",
                            style = MaterialTheme.typography.labelSmall.copy(color = CardBackground.copy(alpha = 0.85f), fontSize = 10.sp)
                        )
                    }
                }
            }
        }

        // 5. Segmented Tab Bar (Sold Fleet vs Client Due List)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = CardBackground,
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tab 1: Sold Fleet
                    TabPillButton(
                        modifier = Modifier.weight(1f),
                        title = "Sold Fleet (${salesData?.soldVehiclesCount ?: 0})",
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )

                    // Tab 2: Client Dues
                    TabPillButton(
                        modifier = Modifier.weight(1f),
                        title = "Due List (${salesData?.clientDuesCount ?: 0})",
                        isSelected = selectedTab == 1,
                        badgeColor = AmberWarning,
                        onClick = { selectedTab = 1 }
                    )
                }
            }
        }

        // 6. Tab Content Display
        if (selectedTab == 0) {
            // TAB 0: Sold Vehicles Fleet
            val vehiclesList = salesData?.soldVehiclesList ?: emptyList()
            if (vehiclesList.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No vehicles sold in this month.",
                        subtitle = "Click 'New Sale / Install' to onboard your first customer vehicle."
                    )
                }
            } else {
                items(vehiclesList, key = { it.id }) { vehicle ->
                    SoldVehicleRowItem(vehicle, context)
                }
            }
        } else {
            // TAB 1: Client Dues List with Direct Call Action
            val duesList = salesData?.clientDuesList ?: emptyList()
            if (duesList.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No outstanding client dues!",
                        subtitle = "All devices and service accounts sold by you are fully paid."
                    )
                }
            } else {
                items(duesList, key = { it.invoiceId }) { dueItem ->
                    ClientDueRowItem(dueItem, context)
                }
            }
        }
    }
}

@Composable
private fun TabPillButton(
    modifier: Modifier = Modifier,
    title: String,
    isSelected: Boolean,
    badgeColor: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) BrandBluePrimary else SurfaceBackground,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) CardBackground else TextSecondary,
                    fontSize = 12.sp
                )
            )
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
private fun EmptyStateCard(
    title: String,
    subtitle: String
) {
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
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontWeight = FontWeight.Medium)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
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
                        Icon(imageVector = Icons.Outlined.Phone, contentDescription = "Call Customer", tint = BrandBluePrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Client Due Row Card with Direct Calling Button (কল করুন)
 */
@Composable
private fun ClientDueRowItem(
    due: ClientDueItem,
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Vehicle Plate + Invoice #
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = SurfaceBackground,
                    shape = RoundedCornerShape(6.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
                ) {
                    Text(
                        text = due.vehiclePlateNumber ?: "Device Unit",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = BrandBluePrimary,
                            fontSize = 12.sp
                        )
                    )
                }

                Text(
                    text = "#${due.invoiceNo}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                )
            }

            // Middle Row: Customer Info & Due Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = due.customerName ?: "Client",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "${due.vehicleName ?: "Device"} • Invoice: ${due.invoiceDate?.take(10) ?: "N/A"}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.5.sp
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "৳${numberFormatShort(due.dueAmount)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = AmberWarning,
                            fontSize = 15.sp
                        )
                    )
                    Text(
                        text = "Total: ৳${numberFormatShort(due.grandTotal)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Divider(color = BorderSubtle, thickness = 0.8.dp)

            // Bottom Action Row: Direct Call Customer Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (!due.customerPhone.isNullOrBlank()) "Phone: ${due.customerPhone}" else "No phone attached",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                )

                if (!due.customerPhone.isNullOrBlank()) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${due.customerPhone}"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.Phone, contentDescription = null, tint = CardBackground, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Call Customer (কল করুন)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = CardBackground,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun shiftMonth(monthStr: String, delta: Int): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(monthStr) ?: Date()
        cal.add(Calendar.MONTH, delta)
        sdf.format(cal.time)
    } catch (e: Exception) {
        monthStr
    }
}

private fun formatMonthDisplay(monthStr: String): String {
    return try {
        val input = SimpleDateFormat("yyyy-MM", Locale.US).parse(monthStr) ?: Date()
        SimpleDateFormat("MMMM yyyy", Locale.US).format(input)
    } catch (e: Exception) {
        monthStr
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
