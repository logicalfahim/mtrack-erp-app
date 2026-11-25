package com.mtrack.installer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mtrack.installer.data.location.LocationTrackingManager
import com.mtrack.installer.data.location.StaffLocationForegroundService
import com.mtrack.installer.ui.components.AppDrawer
import com.mtrack.installer.ui.components.NotificationHalfDrawerSheet
import com.mtrack.installer.ui.screens.*
import com.mtrack.installer.ui.theme.*
import com.mtrack.installer.ui.viewmodel.AuthViewModel
import com.mtrack.installer.ui.viewmodel.DashboardViewModel
import com.mtrack.installer.ui.viewmodel.InstallViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val installViewModel: InstallViewModel by viewModels()

    private lateinit var locationTrackingManager: LocationTrackingManager

    // Permission Launcher for Google Play Store Compliant Runtime Request
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            StaffLocationForegroundService.startService(this)
            locationTrackingManager.startTracking()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationTrackingManager = LocationTrackingManager(this)

        setContent {
            MTrackInstallerTheme {
                val authState by authViewModel.uiState.collectAsState()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var currentRoute by remember { mutableStateOf("dashboard") }

                // Notification Half-Drawer State
                var isNotificationSheetOpen by remember { mutableStateOf(false) }

                // Google Play Store Prominent Disclosure Dialog State
                var showLocationDisclosureDialog by remember { mutableStateOf(false) }

                val isTrackingActive by locationTrackingManager.isTrackingActive.collectAsState()

                // Check permissions when authenticated
                LaunchedEffect(authState.isAuthenticated) {
                    if (authState.isAuthenticated) {
                        if (locationTrackingManager.hasLocationPermissions()) {
                            StaffLocationForegroundService.startService(this@MainActivity)
                            locationTrackingManager.startTracking()
                        } else {
                            showLocationDisclosureDialog = true
                        }
                    } else {
                        StaffLocationForegroundService.stopService(this@MainActivity)
                        locationTrackingManager.stopTracking()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SurfaceBackground
                ) {
                    if (!authState.isAuthenticated) {
                        // MANDATORY AUTHENTICATION GATEWAY
                        LoginScreen(authViewModel = authViewModel)
                    } else {
                        // AUTHENTICATED EXECUTIVE WORKSPACE
                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            drawerContent = {
                                AppDrawer(
                                    currentRoute = currentRoute,
                                    currentUser = authState.currentUser,
                                    onNavigate = { route ->
                                        currentRoute = route
                                    },
                                    onLogout = {
                                        StaffLocationForegroundService.stopService(this@MainActivity)
                                        locationTrackingManager.stopTracking()
                                        authViewModel.logout()
                                    },
                                    onCloseDrawer = {
                                        scope.launch { drawerState.close() }
                                    }
                                )
                            }
                        ) {
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(CardBackground.copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "M",
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            color = CardBackground,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp
                                                        )
                                                    )
                                                }
                                                Column {
                                                    val headerTitle = when (currentRoute) {
                                                        "dashboard"    -> "M TRACK ERP"
                                                        "staffs"       -> "Staffs Directory"
                                                        "fleet"        -> "Sold Fleet"
                                                        "profile"      -> "Executive Profile"
                                                        "profile_edit" -> "Edit Profile"
                                                        "about_us"     -> "About M TRACK"
                                                        else           -> "Workspace"
                                                    }
                                                    val headerSubtitle = when (currentRoute) {
                                                        "dashboard"    -> "Sales Operations Hub"
                                                        "staffs"       -> "Live Active Colleagues"
                                                        "fleet"        -> "Deliveries & Dues Pacing"
                                                        "profile"      -> authState.currentUser?.fullName ?: "Staff Identity"
                                                        "profile_edit" -> "Personal & Security Details"
                                                        else           -> "Enterprise Fleet Telematics"
                                                    }

                                                    Text(
                                                        text = headerTitle,
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = CardBackground,
                                                            fontSize = 15.sp
                                                        )
                                                    )
                                                    Text(
                                                        text = headerSubtitle,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Normal,
                                                            color = CardBackground.copy(alpha = 0.85f),
                                                            fontSize = 10.sp
                                                        )
                                                    )
                                                }
                                            }
                                        },
                                        navigationIcon = {
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Menu,
                                                    contentDescription = "Open Navigation Menu",
                                                    tint = CardBackground
                                                )
                                            }
                                        },
                                        actions = {
                                            // Live GPS Tracking Status Pill
                                            Surface(
                                                color = if (isTrackingActive) Color(0x3310B981) else Color(0x33EF4444),
                                                shape = RoundedCornerShape(9999.dp),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isTrackingActive) Color(0x6610B981) else Color(0x66EF4444)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .background(
                                                                if (isTrackingActive) Color(0xFF10B981) else Color(0xFFEF4444),
                                                                CircleShape
                                                            )
                                                    )
                                                    Text(
                                                        text = if (isTrackingActive) "GPS Live" else "GPS Off",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CardBackground
                                                    )
                                                }
                                            }

                                            // Notification Bell with Badge (Triggers Half-Drawer Sheet)
                                            IconButton(
                                                onClick = { isNotificationSheetOpen = true }
                                            ) {
                                                BadgedBox(
                                                    badge = {
                                                        Badge(
                                                            containerColor = Color(0xFFEF4444),
                                                            contentColor = Color.White
                                                        ) {
                                                            Text("2", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Notifications,
                                                        contentDescription = "Notifications",
                                                        tint = CardBackground
                                                    )
                                                }
                                            }

                                            // Profile Avatar Shortcut
                                            IconButton(
                                                onClick = { currentRoute = "profile" }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.AccountCircle,
                                                    contentDescription = "Profile",
                                                    tint = CardBackground
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = BrandBluePrimary,
                                            titleContentColor = CardBackground,
                                            navigationIconContentColor = CardBackground,
                                            actionIconContentColor = CardBackground
                                        )
                                    )
                                },
                                bottomBar = {
                                    NavigationBar(
                                        containerColor = CardBackground,
                                        tonalElevation = 2.dp
                                    ) {
                                        // 1. Overview
                                        NavigationBarItem(
                                            selected = currentRoute == "dashboard",
                                            onClick = { currentRoute = "dashboard" },
                                            icon = { Icon(Icons.Outlined.Dashboard, contentDescription = "Overview") },
                                            label = { Text("Overview", fontSize = 11.sp, fontWeight = if (currentRoute == "dashboard") FontWeight.Bold else FontWeight.Normal) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = BrandBluePrimary,
                                                selectedTextColor = BrandBluePrimary,
                                                indicatorColor = BrandBlueLight
                                            )
                                        )

                                        // 2. Staffs
                                        NavigationBarItem(
                                            selected = currentRoute == "staffs",
                                            onClick = { currentRoute = "staffs" },
                                            icon = { Icon(Icons.Outlined.PeopleOutline, contentDescription = "Staffs") },
                                            label = { Text("Staffs", fontSize = 11.sp, fontWeight = if (currentRoute == "staffs") FontWeight.Bold else FontWeight.Normal) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = BrandBluePrimary,
                                                selectedTextColor = BrandBluePrimary,
                                                indicatorColor = BrandBlueLight
                                            )
                                        )

                                        // 3. Fleet
                                        NavigationBarItem(
                                            selected = currentRoute == "fleet",
                                            onClick = { currentRoute = "fleet" },
                                            icon = { Icon(Icons.Outlined.DirectionsCar, contentDescription = "Fleet") },
                                            label = { Text("Fleet", fontSize = 11.sp, fontWeight = if (currentRoute == "fleet") FontWeight.Bold else FontWeight.Normal) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = BrandBluePrimary,
                                                selectedTextColor = BrandBluePrimary,
                                                indicatorColor = BrandBlueLight
                                            )
                                        )

                                        // 4. Profile
                                        NavigationBarItem(
                                            selected = currentRoute == "profile" || currentRoute == "profile_edit",
                                            onClick = { currentRoute = "profile" },
                                            icon = { Icon(Icons.Outlined.Person, contentDescription = "Profile") },
                                            label = { Text("Profile", fontSize = 11.sp, fontWeight = if (currentRoute == "profile") FontWeight.Bold else FontWeight.Normal) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = BrandBluePrimary,
                                                selectedTextColor = BrandBluePrimary,
                                                indicatorColor = BrandBlueLight
                                            )
                                        )
                                    }
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    when (currentRoute) {
                                        "dashboard" -> {
                                            SellerDashboardScreen(
                                                dashboardViewModel = dashboardViewModel
                                            )
                                        }
                                        "staffs" -> {
                                            StaffsScreen(
                                                dashboardViewModel = dashboardViewModel
                                            )
                                        }
                                        "fleet" -> {
                                            FleetScreen(
                                                dashboardViewModel = dashboardViewModel
                                            )
                                        }
                                        "profile" -> {
                                            ProfileScreen(
                                                dashboardViewModel = dashboardViewModel,
                                                onNavigateToEdit = { currentRoute = "profile_edit" },
                                                onLogout = {
                                                    StaffLocationForegroundService.stopService(this@MainActivity)
                                                    locationTrackingManager.stopTracking()
                                                    authViewModel.logout()
                                                }
                                            )
                                        }
                                        "profile_edit" -> {
                                            EditProfileScreen(
                                                dashboardViewModel = dashboardViewModel,
                                                onNavigateBack = { currentRoute = "profile" }
                                            )
                                        }
                                        "about_us" -> {
                                            AboutUsScreen()
                                        }
                                        else -> {
                                            SellerDashboardScreen(
                                                dashboardViewModel = dashboardViewModel
                                            )
                                        }
                                    }
                                }

                                // NOTIFICATION HALF-DRAWER EXPANDABLE MODAL SHEET
                                NotificationHalfDrawerSheet(
                                    isOpen = isNotificationSheetOpen,
                                    onDismiss = { isNotificationSheetOpen = false }
                                )

                                // GOOGLE PLAY STORE PROMINENT DISCLOSURE DIALOG
                                if (showLocationDisclosureDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showLocationDisclosureDialog = false },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Outlined.LocationOn,
                                                contentDescription = null,
                                                tint = BrandBluePrimary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        },
                                        title = {
                                            Text(
                                                text = "Staff Telematics & Location Access",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = "M TRACK ERP collects location data in real time to enable accurate field staff dispatching, attendance verification, and fleet operations even when the app is running in the background or closed.\n\nYour location is encrypted and transmitted securely to M TRACK Enterprise ERP servers.",
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp,
                                                color = Color(0xFF475569)
                                            )
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    showLocationDisclosureDialog = false
                                                    val perms = mutableListOf(
                                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                                    )
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                        perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                                    }
                                                    locationPermissionLauncher.launch(perms.toTypedArray())
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
                                            ) {
                                                Text("Agree & Continue", fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(
                                                onClick = { showLocationDisclosureDialog = false }
                                            ) {
                                                Text("Not Now", color = Color(0xFF64748B))
                                            }
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        containerColor = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        StaffLocationForegroundService.stopService(this)
        locationTrackingManager.stopTracking()
    }
}
