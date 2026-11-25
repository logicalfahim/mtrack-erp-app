package com.mtrack.installer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Menu
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

                if (!authState.isLoggedIn) {
                    // Mandatory Access Gatekeeper: Must sign in before entering ERP
                    LoginScreen(authViewModel = authViewModel)
                } else {
                    // Authenticated Workspace Shell
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    val scope = rememberCoroutineScope()
                    var currentRoute by remember { mutableStateOf("dashboard") }
                    val installState by installViewModel.uiState.collectAsState()

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            AppDrawer(
                                currentRoute = currentRoute,
                                currentUser = authState.currentUser,
                                onNavigate = { route ->
                                    currentRoute = route
                                    if (route == "install_wizard" && installState.currentStep == 5) {
                                        installViewModel.resetForm()
                                    }
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
                                                    .clip(CircleShape)
                                                .background(CardBackground.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.GpsFixed,
                                                    contentDescription = null,
                                                    tint = CardBackground,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Column {
                                                val headerTitle = when (currentRoute) {
                                                    "dashboard"      -> "M Track ERP"
                                                    "install_wizard" -> "Vehicle Onboarding"
                                                    "about_us"       -> "About M Track"
                                                    else             -> "M Track Workspace"
                                                }
                                                val headerSubtitle = when (currentRoute) {
                                                    "dashboard"      -> authState.currentUser?.department ?: "Department Dashboard"
                                                    "install_wizard" -> "Multi-Vehicle Hardware Wizard"
                                                    else             -> "Enterprise Telematics"
                                                }

                                                Text(
                                                    text = headerTitle,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
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
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = BrandBluePrimary,
                                        titleContentColor = CardBackground,
                                        navigationIconContentColor = CardBackground
                                    )
                                )
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
                                    "install_wizard" -> {
                                        if (installState.currentStep == 5) {
                                            SuccessScreen(
                                                batchId = installState.submittedBatchId ?: "MTK-SUCCESS",
                                                viewModel = installViewModel
                                            )
                                        } else {
                                            InstallWizardScreen(viewModel = installViewModel)
                                        }
                                    }
                                    "about_us" -> {
                                        AboutUsScreen()
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
