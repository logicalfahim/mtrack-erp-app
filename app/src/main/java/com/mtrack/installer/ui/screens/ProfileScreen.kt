package com.mtrack.installer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtrack.installer.data.model.UserProfile
import com.mtrack.installer.ui.theme.*
import com.mtrack.installer.ui.viewmodel.DashboardViewModel

@Composable
fun ProfileScreen(
    dashboardViewModel: DashboardViewModel,
    onNavigateToEdit: () -> Unit,
    onLogout: () -> Unit
) {
    val state by dashboardViewModel.uiState.collectAsState()
    val user = state.userProfile
    val salesData = state.dashboardData?.salesDepartment
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Executive Hero Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Large Avatar Circle with Initials
                val initials = (user?.fullName ?: "Fahim Ahmed").split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(BrandBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials.ifEmpty { "FA" },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = BrandBluePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    )
                }

                val isAdmin = user?.role?.equals("admin", true) == true
                val roleDisplay = if (isAdmin) "ADMIN" else (user?.role?.uppercase() ?: "STAFF")

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = user?.fullName ?: (if (isAdmin) "System Administrator" else "Fahim Ahmed"),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 19.sp
                        )
                    )
                    Text(
                        text = "ID: #MTK-USR-${user?.userId ?: 12} • $roleDisplay",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )
                }

                val designationDisplay = if (isAdmin) "System Administrator • Management" else "${user?.designation ?: "Sales & Marketing"} • ${user?.department ?: "Enterprise Sales"}"

                Surface(
                    color = BrandBlueLight,
                    shape = RoundedCornerShape(20.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BrandBlueBorder.copy(alpha = 0.6f)))
                ) {
                    Text(
                        text = designationDisplay,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BrandBluePrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                }

                Button(
                    onClick = onNavigateToEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.Edit, contentDescription = null, tint = CardBackground, modifier = Modifier.size(15.dp))
                        Text(
                            text = "Edit Profile",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = CardBackground,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        // Performance Scorecard Summary (3 Pillars)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ProfileMetricPill(
                    label = if (isAdmin) "Company Fleet" else "Monthly Fleet",
                    value = "${salesData?.soldVehiclesCount ?: 0} Units"
                )
                Divider(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp),
                    color = BorderSubtle
                )
                ProfileMetricPill(
                    label = if (isAdmin) "Growth Rate" else "Pacing Rate",
                    value = "${salesData?.progressPercentage ?: 0f}%"
                )
                Divider(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp),
                    color = BorderSubtle
                )
                ProfileMetricPill(
                    label = if (isAdmin) "Total Dues" else "Fleet Dues",
                    value = "BDT ${numberFormatShort(salesData?.totalDueAmount ?: 0.0)}"
                )
            }
        }

        // Official Contact & Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "CONTACT & OFFICE INFO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.5.sp
                    )
                )

                ProfileInfoRow(
                    icon = Icons.Outlined.Phone,
                    title = "Official Mobile",
                    value = user?.phone ?: "+880 1711-223344"
                )

                HorizontalDivider(color = BorderSubtle)

                ProfileInfoRow(
                    icon = Icons.Outlined.Email,
                    title = "Work Email",
                    value = user?.email ?: "support@mtrack.com.bd"
                )

                HorizontalDivider(color = BorderSubtle)

                ProfileInfoRow(
                    icon = Icons.Outlined.LocationOn,
                    title = "Base Station",
                    value = user?.address?.ifBlank { null } ?: "Dhaka, Bangladesh"
                )
            }
        }

        // Security & App Specs Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "SECURITY & PREFERENCES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.5.sp
                    )
                )

                ProfileInfoRow(
                    icon = Icons.Outlined.Lock,
                    title = "Account Password",
                    value = "Last updated 30 days ago"
                )

                HorizontalDivider(color = BorderSubtle)

                ProfileInfoRow(
                    icon = Icons.Outlined.Info,
                    title = "App Version",
                    value = "v2.6.0 (Enterprise 2026 Build)"
                )
            }
        }

        // Sign Out Danger Button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { showLogoutDialog = true },
            color = RoseBg,
            shape = RoundedCornerShape(14.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RoseError.copy(alpha = 0.25f)))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Outlined.Logout, contentDescription = null, tint = RoseError, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign Out from M ERP",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = RoseError,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = "Sign Out Confirmation", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to end your active session on this device?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Sign Out", color = RoseError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = CardBackground
        )
    }
}

@Composable
private fun ProfileMetricPill(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                fontSize = 10.5.sp
            )
        )
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    fontSize = 12.5.sp
                )
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                fontSize = 12.5.sp
            )
        )
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
