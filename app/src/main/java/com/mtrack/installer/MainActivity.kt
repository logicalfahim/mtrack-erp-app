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
import com.mtrack.installer.ui.screens.AboutUsScreen
import com.mtrack.installer.ui.screens.InstallWizardScreen
import com.mtrack.installer.ui.screens.SuccessScreen
import com.mtrack.installer.ui.theme.*
import com.mtrack.installer.ui.viewmodel.InstallViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: InstallViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MTrackInstallerTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var currentRoute by remember { mutableStateOf("install_wizard") }
                val uiState by viewModel.uiState.collectAsState()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppDrawer(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                currentRoute = route
                                if (route == "install_wizard" && uiState.currentStep == 5) {
                                    viewModel.resetForm()
                                }
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
                                            Text(
                                                text = if (currentRoute == "about_us") "About M Track" else "M Track Installer",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = CardBackground,
                                                    fontSize = 15.sp
                                                )
                                            )
                                            Text(
                                                text = "Vehicle Onboarding Portal",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Normal,
                                                    color = CardBackground.copy(alpha = 0.8f),
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
                                            contentDescription = "Open Sidebar",
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
                            if (currentRoute == "about_us") {
                                AboutUsScreen()
                            } else {
                                if (uiState.currentStep == 5) {
                                    SuccessScreen(
                                        batchId = uiState.submittedBatchId ?: "MTK-SUCCESS",
                                        viewModel = viewModel
                                    )
                                } else {
                                    InstallWizardScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
