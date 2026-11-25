package com.mtrack.installer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtrack.installer.data.model.ClientDueItem
import com.mtrack.installer.data.model.SoldVehicleItem
import com.mtrack.installer.ui.theme.*
import com.mtrack.installer.ui.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SellerDashboardScreen(
    dashboardViewModel: DashboardViewModel,
    onNavigateToInstall: (() -> Unit)? = null
) {
    val state by dashboardViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val salesData = state.dashboardData?.salesDepartment
    val user = state.userProfile

    // Tab state: 0 = Sold Fleet, 1 = Client Dues List
    var selectedTab by remember { mutableIntStateOf(0) }

    // Instant Search query state for high-volume data optimization
    var searchQuery by remember { mutableStateOf("") }

    // Filter pill state (0 = All, 1 = Active/High, 2 = Pending)
    var filterType by remember { mutableIntStateOf(0) }

    // Current month string from API or fallback
    val activeMonth = salesData?.month ?: SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    // -------------------------------------------------------------
    // MEMOIZED HIGH-PERFORMANCE DATA FILTERING
    // -------------------------------------------------------------
    val vehiclesList = salesData?.soldVehiclesList ?: emptyList()
    val duesList = salesData?.clientDuesList ?: emptyList()

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

    val filteredDues = remember(duesList, searchQuery, filterType) {
        val q = searchQuery.trim().lowercase()
        duesList.filter { d ->
            val matchQuery = q.isEmpty() ||
                    (d.vehiclePlateNumber?.lowercase()?.contains(q) == true) ||
                    (d.customerName?.lowercase()?.contains(q) == true) ||
                    (d.customerPhone?.contains(q) == true) ||
                    d.invoiceNo.lowercase().contains(q)

            val matchFilter = when (filterType) {
                1 -> d.dueAmount >= 5000.0 // High due
                2 -> d.dueAmount < 5000.0  // Regular due
                else -> true
            }
            matchQuery && matchFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // 1. Header Profile & Role Identity
        item(key = "header_profile", contentType = "header") {
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
        item(key = "month_navigator", contentType = "nav") {
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
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val prev = shiftMonth(activeMonth, -1)
                            dashboardViewModel.loadDashboard(prev)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.ChevronLeft, contentDescription = "Prev", tint = BrandBluePrimary, modifier = Modifier.size(20.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.CalendarMonth, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(18.dp))
                        Text(
                            text = salesData?.monthName ?: formatMonthDisplay(activeMonth),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = {
                            val next = shiftMonth(activeMonth, 1)
                            dashboardViewModel.loadDashboard(next)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = "Next", tint = BrandBluePrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // 3. Target Achievement & MoM Growth Card
        item(key = "target_growth_card", contentType = "metric_hero") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
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

                    HorizontalDivider(color = BorderSubtle, thickness = 0.8.dp)

                    // MoM Growth Comparison Indicator
                    val growth = salesData?.growthRate ?: 0f
                    val lastAchieved = salesData?.lastMonthAchieved ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                        Text(
                            text = "Last Month: $lastAchieved Units",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
                        )
                    }
                }
            }
        }

        // 4. Core Sales Performance Pillars (Client Dues & Company Sales)
        item(key = "kpi_row_1", contentType = "kpi_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Client Due Amount (বাকি)
                KpiMetricCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = 1 },
                    title = "Client Dues (বাকি)",
                    value = "৳${numberFormatShort(salesData?.totalDueAmount ?: 0.0)}",
                    subtitle = "${salesData?.clientDuesCount ?: 0} Unpaid accounts",
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

        // 5. Volume Counter Row (Sold Fleet Count vs Pending Dues)
        item(key = "kpi_row_2", contentType = "kpi_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sold Fleet Count
                KpiMetricCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = 0 },
                    title = "Sold Fleet (গাড়ি)",
                    value = "${salesData?.soldVehiclesCount ?: 0}",
                    subtitle = "Vehicles sold this month",
                    icon = Icons.Outlined.DirectionsCar,
                    iconTint = BrandBluePrimary,
                    iconBg = BrandBlueLight
                )

                // Pending Dues Count
                KpiMetricCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = 1 },
                    title = "Pending Invoices",
                    value = "${salesData?.clientDuesCount ?: 0}",
                    subtitle = "Clients awaiting recovery",
                    icon = Icons.Outlined.PhoneCallback,
                    iconTint = AmberWarning,
                    iconBg = AmberWarning.copy(alpha = 0.12f)
                )
            }
        }

        // 6. Fast Search & Filter Toolbar
        item(key = "search_filter_bar", contentType = "search") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Search Text Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        placeholder = {
                            Text(
                                text = if (selectedTab == 0) "Search plate number or customer..." else "Search due invoice or phone...",
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
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBluePrimary,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = SurfaceBackground,
                            unfocusedContainerColor = SurfaceBackground
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                    )

                    // Filter Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterPill(title = "All", isSelected = filterType == 0, onClick = { filterType = 0 })
                        if (selectedTab == 0) {
                            FilterPill(title = "Active Only", isSelected = filterType == 1, onClick = { filterType = 1 })
                            FilterPill(title = "Pending", isSelected = filterType == 2, onClick = { filterType = 2 })
                        } else {
                            FilterPill(title = "High Dues (>৳5k)", isSelected = filterType == 1, onClick = { filterType = 1 })
                            FilterPill(title = "Regular Dues", isSelected = filterType == 2, onClick = { filterType = 2 })
                        }
                    }
                }
            }
        }

        // 7. Segmented Tab Control (Sold Fleet vs Due Recovery)
        item(key = "tab_bar", contentType = "tab_control") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
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
                    TabPillButton(
                        modifier = Modifier.weight(1f),
                        title = "Sold Fleet (${filteredVehicles.size})",
                        isSelected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            filterType = 0
                        }
                    )

                    TabPillButton(
                        modifier = Modifier.weight(1f),
                        title = "Due Recovery (${filteredDues.size})",
                        isSelected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            filterType = 0
                        }
                    )
                }
            }
        }

        // 8. Dynamic List Section (Virtualized Lazy Items)
        if (selectedTab == 0) {
            // TAB 0: Sold Vehicles Fleet
            if (filteredVehicles.isEmpty()) {
                item(key = "empty_fleet") {
                    EmptyStateCard(
                        title = if (searchQuery.isNotEmpty()) "No vehicles match '$searchQuery'" else "No vehicles sold in this month.",
                        subtitle = "When you sell a GPS tracker, the fleet unit will appear here instantly."
                    )
                }
            } else {
                items(
                    items = filteredVehicles,
                    key = { vehicle -> vehicle.id },
                    contentType = { "vehicle_item" }
                ) { vehicle ->
                    SoldVehicleRowItem(vehicle, context)
                }
            }
        } else {
            // TAB 1: Client Dues Recovery List
            if (filteredDues.isEmpty()) {
                item(key = "empty_dues") {
                    EmptyStateCard(
                        title = if (searchQuery.isNotEmpty()) "No due records match '$searchQuery'" else "All client dues are fully recovered!",
                        subtitle = "No outstanding invoices remain unpaid for your sold devices."
                    )
                }
            } else {
                items(
                    items = filteredDues,
                    key = { dueItem -> dueItem.invoiceId },
                    contentType = { "due_item" }
                ) { dueItem ->
                    ClientDueRowItem(dueItem, context)
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) BrandBluePrimary.copy(alpha = 0.12f) else SurfaceBackground,
        shape = RoundedCornerShape(6.dp),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BrandBluePrimary.copy(alpha = 0.4f))) else null
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isSelected) BrandBluePrimary else TextSecondary,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 10.5.sp
            )
        )
    }
}

@Composable
private fun TabPillButton(
    modifier: Modifier = Modifier,
    title: String,
    isSelected: Boolean,
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

            HorizontalDivider(color = BorderSubtle, thickness = 0.8.dp)

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
