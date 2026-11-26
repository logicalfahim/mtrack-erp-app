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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    val isAdmin = user?.role?.equals("admin", true) == true

    // Tab state: 0 = Sold Fleet, 1 = Receivables Due
    var selectedTab by remember { mutableIntStateOf(0) }

    // Instant search state
    var searchQuery by remember { mutableStateOf("") }

    // Filter pill state (0 = All, 1 = Active/High, 2 = Pending/Standard)
    var filterType by remember { mutableIntStateOf(0) }

    // Selected month string from state
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
                1 -> d.dueAmount >= 5000.0 // High priority due
                2 -> d.dueAmount < 5000.0  // Standard due
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
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // 1. Month Slider Bar (Linear Capsule Style)
        item(key = "month_navigator", contentType = "nav") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
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
                        Icon(imageVector = Icons.Outlined.ChevronLeft, contentDescription = "Previous Month", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(BrandBluePrimary)
                        )
                        Text(
                            text = salesData?.monthName ?: formatMonthDisplay(activeMonth),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        )
                        Surface(
                            color = BrandBlueLight,
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BrandBlueBorder.copy(alpha = 0.5f)))
                        ) {
                            Text(
                                text = "Live Period",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BrandBluePrimary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val next = shiftMonth(activeMonth, 1)
                            dashboardViewModel.loadDashboard(next)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = "Next Month", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // 2. Trending Bento Hero: Target Pacing & MoM Growth Gradient Card
        item(key = "target_growth_card", contentType = "metric_hero") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1E3A8A),
                                    Color(0xFF2563EB),
                                    Color(0xFF3B82F6)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isAdmin) "COMPANY SALES PACING" else "SALES TARGET PACING",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFDBEAFE),
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.8.sp,
                                        fontSize = 10.5.sp
                                    )
                                )
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = "${salesData?.achievedQty ?: 0}",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 26.sp
                                        )
                                    )
                                    Text(
                                        text = "/ ${salesData?.targetQty ?: 0} Units Sold",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFBFDBFE),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp
                                        ),
                                        modifier = Modifier.padding(bottom = 3.dp)
                                    )
                                }
                            }

                            // Glassmorphic Progress Badge
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "${salesData?.progressPercentage ?: 0f}%",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        )
                                    )
                                    Text(
                                        text = "Run Rate",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFDBEAFE),
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        }

                        // Progress Bar
                        val progress = ((salesData?.progressPercentage ?: 0f) / 100f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color.White,
                            trackColor = Color.Black.copy(alpha = 0.2f)
                        )

                        // MoM Benchmark Pill
                        val growth = salesData?.growthRate ?: 0f
                        val lastAchieved = salesData?.lastMonthAchieved ?: 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val icon = if (growth >= 0f) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown
                                    Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(13.dp))
                                    Text(
                                        text = "${if (growth > 0) "+" else ""}${growth}% Growth",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF6EE7B7),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Text(
                                text = "Prior Month: $lastAchieved Units",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFDBEAFE),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Bento KPI Cards (Wide Receivables Card + 2 Columns)
        item(key = "kpi_receivables_hero", contentType = "kpi_wide") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedTab = 1 },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
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
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AmberWarning.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Outlined.ReceiptLong, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "Outstanding Receivables",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            )
                        }

                        Surface(
                            color = AmberWarning.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${salesData?.clientDuesCount ?: 0} Unpaid Accounts",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AmberWarning,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.5.sp
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "BDT ${numberFormatShort(salesData?.totalDueAmount ?: 0.0)}",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AmberWarning,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 22.sp
                                )
                            )
                            Text(
                                text = "Actionable balances to recover",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.5.sp)
                            )
                        }

                        Surface(
                            color = AmberWarning.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Recover",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AmberWarning,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }

        // 4. Two Sub-KPI Columns: Personal Fleet & Company Benchmark
        item(key = "kpi_columns_2", contentType = "kpi_pair") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Personal Deliveries
                BentoSubCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = 0 },
                    title = "Personal Deliveries",
                    value = "${salesData?.soldVehiclesCount ?: 0}",
                    subtitle = "100% Verified",
                    icon = Icons.Outlined.DirectionsCar,
                    iconTint = BrandBluePrimary,
                    iconBg = BrandBlueLight
                )

                // Nationwide Fleet
                BentoSubCard(
                    modifier = Modifier.weight(1f),
                    title = "Nationwide Total",
                    value = "${salesData?.companyTotalSales ?: 0}",
                    subtitle = "Company Benchmark",
                    icon = Icons.Outlined.Domain,
                    iconTint = TextSecondary,
                    iconBg = SurfaceBackground
                )
            }
        }

        // 5. Linear-Style Modern Floating Pill Switcher
        item(key = "tab_bar", contentType = "tab_control") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFE2E8F0).copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ModernTabPill(
                        modifier = Modifier.weight(1f),
                        title = if (isAdmin) "Enterprise Fleet" else "Sold Fleet",
                        count = filteredVehicles.size,
                        isSelected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            filterType = 0
                        }
                    )

                    ModernTabPill(
                        modifier = Modifier.weight(1f),
                        title = "Receivables Due",
                        count = filteredDues.size,
                        isSelected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            filterType = 0
                        }
                    )
                }
            }
        }

        // 6. Search Capsule & Filters
        item(key = "search_capsule", contentType = "search") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    placeholder = {
                        Text(
                            text = if (selectedTab == 0) "Search plate number, client or phone..." else "Search due invoice or phone...",
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

                // Capsule Filter Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterCapsule(title = "All Records", isSelected = filterType == 0, onClick = { filterType = 0 })
                    if (selectedTab == 0) {
                        FilterCapsule(title = "Active Units", isSelected = filterType == 1, onClick = { filterType = 1 })
                        FilterCapsule(title = "Pending Setup", isSelected = filterType == 2, onClick = { filterType = 2 })
                    } else {
                        FilterCapsule(title = "High Value (>BDT 5k)", isSelected = filterType == 1, onClick = { filterType = 1 })
                        FilterCapsule(title = "Standard Dues", isSelected = filterType == 2, onClick = { filterType = 2 })
                    }
                }
            }
        }

        // 7. Dynamic Virtualized List Feed
        if (selectedTab == 0) {
            // TAB 0: Sold Vehicles Fleet
            if (filteredVehicles.isEmpty()) {
                item(key = "empty_fleet") {
                    EmptyStateCard(
                        title = if (searchQuery.isNotEmpty()) "No vehicles match '$searchQuery'" else "No vehicle deliveries recorded this month.",
                        subtitle = "Installed GPS units assigned to your profile will appear here automatically."
                    )
                }
            } else {
                items(
                    items = filteredVehicles,
                    key = { vehicle -> vehicle.id },
                    contentType = { "vehicle_item" }
                ) { vehicle ->
                    ModernSoldVehicleRow(vehicle, context)
                }
            }
        } else {
            // TAB 1: Receivables Recovery List
            if (filteredDues.isEmpty()) {
                item(key = "empty_dues") {
                    EmptyStateCard(
                        title = if (searchQuery.isNotEmpty()) "No due invoices match '$searchQuery'" else "All client receivables are settled!",
                        subtitle = "There are no outstanding invoices for devices in your account."
                    )
                }
            } else {
                items(
                    items = filteredDues,
                    key = { dueItem -> dueItem.invoiceId },
                    contentType = { "due_item" }
                ) { dueItem ->
                    ModernClientDueRow(dueItem, context)
                }
            }
        }
    }
}

@Composable
private fun BentoSubCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp
                )
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = EmeraldSuccess,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
private fun ModernTabPill(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) CardBackground else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (isSelected) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) BrandBluePrimary else TextSecondary,
                    fontSize = 12.sp
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                color = if (isSelected) BrandBlueLight else SurfaceBackground,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "$count",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isSelected) BrandBluePrimary else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun FilterCapsule(
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
private fun ModernSoldVehicleRow(
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
                    // Initials Avatar
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

            // Plate & Date Capsule
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceBackground)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
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
                        text = "Installed: ${vehicle.installationDate ?: "N/A"}",
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

            // Seller & Installer Attribution
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

                    val installer = vehicle.installerName?.ifBlank { null } ?: "Technician"
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(imageVector = Icons.Outlined.Build, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                        Text(
                            text = "Installer: $installer",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Normal)
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

@Composable
private fun ModernClientDueRow(
    due: ClientDueItem,
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
                    val initials = (due.customerName ?: "Client").split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AmberWarning.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials.ifEmpty { "C" },
                            style = MaterialTheme.typography.labelMedium.copy(color = AmberWarning, fontWeight = FontWeight.Bold)
                        )
                    }

                    Column {
                        Text(
                            text = due.customerName ?: "Client",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        )
                        Text(
                            text = due.customerPhone ?: "No phone",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "BDT ${numberFormatShort(due.dueAmount)} Due",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AmberWarning,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.5.sp
                        )
                    )
                    Text(
                        text = "#${due.invoiceNo}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                    )
                }
            }

            // Progress & Plate Capsule
            Surface(
                color = SurfaceBackground,
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = due.vehiclePlateNumber ?: "Device Unit",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Total: BDT ${numberFormatShort(due.grandTotal)}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }

                    val paidRatio = if (due.grandTotal > 0) ((due.paidAmount / due.grandTotal).toFloat()).coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { paidRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = EmeraldSuccess,
                        trackColor = BorderSubtle
                    )
                }
            }

            // Direct Call Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Billed: ${due.invoiceDate?.take(10) ?: "N/A"}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.5.sp)
                )

                if (!due.customerPhone.isNullOrBlank()) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${due.customerPhone}"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.Phone, contentDescription = null, tint = CardBackground, modifier = Modifier.size(13.dp))
                            Text(
                                text = "Direct Call",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = CardBackground,
                                    fontWeight = FontWeight.SemiBold,
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

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
            )
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
