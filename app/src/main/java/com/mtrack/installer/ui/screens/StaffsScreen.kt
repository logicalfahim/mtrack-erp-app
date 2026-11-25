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
import com.mtrack.installer.data.model.StaffMemberItem
import com.mtrack.installer.ui.theme.*
import com.mtrack.installer.ui.viewmodel.DashboardViewModel

@Composable
fun StaffsScreen(
    dashboardViewModel: DashboardViewModel
) {
    val state by dashboardViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val staffList = state.dashboardData?.staffDirectory ?: emptyList()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(0) } // 0 = All, 1 = Online, 2 = Sales, 3 = Tech

    val filteredStaffs = remember(staffList, searchQuery, selectedFilter) {
        val q = searchQuery.trim().lowercase()
        staffList.filter { s ->
            val matchQuery = q.isEmpty() ||
                    s.fullName.lowercase().contains(q) ||
                    (s.phone?.contains(q) == true) ||
                    (s.department?.lowercase()?.contains(q) == true) ||
                    (s.designation?.lowercase()?.contains(q) == true) ||
                    (s.bloodGroup?.lowercase()?.contains(q) == true) ||
                    (s.address?.lowercase()?.contains(q) == true)

            val matchFilter = when (selectedFilter) {
                1 -> s.isOnline
                2 -> s.department?.lowercase()?.contains("sale") == true || s.role?.lowercase()?.contains("sale") == true
                3 -> s.department?.lowercase()?.contains("tech") == true || s.role?.lowercase()?.contains("tech") == true
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

        // Screen Title & Staff Count Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Staffs & Colleagues",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 20.sp
                    )
                )
                Text(
                    text = "${filteredStaffs.size} active colleagues in organization",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                )
            }

            Surface(
                color = BrandBlueLight,
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BrandBlueBorder.copy(alpha = 0.5f)))
            ) {
                Text(
                    text = "Live Directory",
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

        // Search Bar (Supports Name, Phone, Blood Group like "O+", or City)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            placeholder = {
                Text(
                    text = "Search by name, phone, blood group (e.g. O+)...",
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

        // Filter Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StaffFilterCapsule(title = "All (${staffList.size})", isSelected = selectedFilter == 0, onClick = { selectedFilter = 0 })
            StaffFilterCapsule(title = "Online Now", isSelected = selectedFilter == 1, onClick = { selectedFilter = 1 })
            StaffFilterCapsule(title = "Sales Team", isSelected = selectedFilter == 2, onClick = { selectedFilter = 2 })
            StaffFilterCapsule(title = "Technical", isSelected = selectedFilter == 3, onClick = { selectedFilter = 3 })
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Staff Members Feed
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (filteredStaffs.isEmpty()) {
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
                            Icon(imageVector = Icons.Outlined.PeopleOutline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Staff Members Found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Text(
                                text = "Try adjusting your search query or filter tags.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                }
            } else {
                items(filteredStaffs, key = { it.userId }) { staff ->
                    StaffMemberCardItem(staff = staff, context = context)
                }
            }
        }
    }
}

@Composable
private fun StaffFilterCapsule(
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
private fun StaffMemberCardItem(
    staff: StaffMemberItem,
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
            // Row 1: Avatar + Name + Online / Recency Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val initials = staff.fullName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials.ifEmpty { "ST" },
                            style = MaterialTheme.typography.labelMedium.copy(color = BrandBluePrimary, fontWeight = FontWeight.Bold)
                        )
                    }

                    Column {
                        Text(
                            text = staff.fullName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Text(
                            text = "${staff.designation ?: "Staff"} • ${staff.department ?: "Operations"}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                        )
                    }
                }

                // Active Recency Badge (e.g. Active now, 3m ago, Idle 25m, Off 2h)
                val isOnline = staff.isOnline
                val badgeBg = if (isOnline) EmeraldBg else SurfaceBackground
                val badgeColor = if (isOnline) EmeraldSuccess else TextMuted
                val dotColor = if (isOnline) EmeraldSuccess else TextMuted

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(badgeColor.copy(alpha = 0.25f)))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Text(
                            text = staff.activeRecencyText ?: "Offline",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = badgeColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Row 2: Blood Group & Address Capsule
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
                        // Blood Group Badge (Red Blood Tag)
                        if (!staff.bloodGroup.isNullOrBlank()) {
                            Surface(
                                color = RoseBg,
                                shape = RoundedCornerShape(8.dp),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RoseError.copy(alpha = 0.3f)))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(imageVector = Icons.Outlined.WaterDrop, contentDescription = null, tint = RoseError, modifier = Modifier.size(11.dp))
                                    Text(
                                        text = "Blood: ${staff.bloodGroup}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = RoseError,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        )
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Blood: N/A",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.5.sp)
                            )
                        }

                        // Phone Number
                        Text(
                            text = staff.phone ?: "No phone",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        )
                    }

                    // Address Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.LocationOn, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                        Text(
                            text = staff.address?.ifBlank { null } ?: "Dhaka HQ Office",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.5.sp),
                            maxLines = 1
                        )
                    }
                }
            }

            // Row 3: Action Buttons (1-Click Direct Call & WhatsApp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!staff.phone.isNullOrBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // WhatsApp Button
                        OutlinedButton(
                            onClick = {
                                val clean = staff.phone.replace("+", "").replace("-", "").replace(" ", "")
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$clean"))
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                            modifier = Modifier.height(34.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.Chat, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "WhatsApp",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Call Button
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${staff.phone}"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.Phone, contentDescription = null, tint = CardBackground, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "Call",
                                    style = MaterialTheme.typography.labelSmall.copy(
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
}
