package com.mtrack.installer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtrack.installer.ui.components.AppDrawer
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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MTrackInstallerTheme {
                val authState by authViewModel.uiState.collectAsState()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var currentRoute by remember { mutableStateOf("dashboard") }

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

                                        // 2. Staffs (Right beside Overview as requested)
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

                                        // 3. Fleet (Monthly sold fleet with integrated Dues)
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
                                                onLogout = { authViewModel.logout() }
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
                            }
                        }
                    }
                }
            }
        }
    }
}
