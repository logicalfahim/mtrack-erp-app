package com.mtrack.installer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtrack.installer.data.model.StaffNotificationItem

/**
 * Enterprise Half-Drawer Notification Sheet
 * Opens at 50% height initially; smoothly expands to full screen when dragged upwards.
 * Built with Material 3 ModalBottomSheet (skipPartiallyExpanded = false).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHalfDrawerSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    notifications: List<StaffNotificationItem> = sampleNotifications(),
    onNotificationClick: (StaffNotificationItem) -> Unit = {}
) {
    if (!isOpen) return

    // skipPartiallyExpanded = false ensures initial half-drawer presentation
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    var selectedTab by remember { mutableStateOf("All") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        scrimColor = Color(0x660F172A),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color(0xFFCBD5E1),
                width = 36.dp,
                height = 4.dp
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Notifications",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Text(
                            text = "${notifications.count { !it.isRead }} New",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                TextButton(
                    onClick = { /* Mark all as read */ },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Mark all read",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB)
                    )
                }
            }

            // Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Installs", "Billing", "System").forEach { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        modifier = Modifier
                            .clickable { selectedTab = tab },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Text(
                            text = tab,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Notifications Scrollable Feed
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val filteredList = when (selectedTab) {
                    "Installs" -> notifications.filter { it.category == "Installation" }
                    "Billing" -> notifications.filter { it.category == "Billing" }
                    "System" -> notifications.filter { it.category == "System" }
                    else -> notifications
                }

                if (filteredList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsNone,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No notifications in this category",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                } else {
                    items(filteredList) { item ->
                        NotificationCard(
                            item = item,
                            onClick = { onNotificationClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    item: StaffNotificationItem,
    onClick: () -> Unit
) {
    val (icon, badgeBg, iconColor) = when (item.category) {
        "Installation" -> Triple(Icons.Filled.Build, Color(0xFFEFF6FF), Color(0xFF2563EB))
        "Billing" -> Triple(Icons.Filled.AccountBalanceWallet, Color(0xFFFEF3C7), Color(0xFFD97706))
        "Alert" -> Triple(Icons.Filled.Warning, Color(0xFFFEE2E2), Color(0xFFDC2626))
        else -> Triple(Icons.Filled.Notifications, Color(0xFFF1F5F9), Color(0xFF475569))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (item.isRead) Color.White else Color(0xFFFAFCFF),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.isRead) Color(0xFFF1F5F9) else Color(0xFFDBEAFE)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(badgeBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        fontWeight = if (item.isRead) FontWeight.SemiBold else FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(Color(0xFF2563EB), CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.message,
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = item.timeAgo,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

fun sampleNotifications(): List<StaffNotificationItem> = listOf(
    StaffNotificationItem(
        id = "1",
        title = "Vehicle Commission Credited",
        message = "BDT 500 commission for Toyota Premio (GA 11-2233) has been approved and added to your monthly payout.",
        category = "Billing",
        timeAgo = "12m ago",
        isRead = false
    ),
    StaffNotificationItem(
        id = "2",
        title = "New Installation Scheduled",
        message = "Customer 'Hanif Paribahan' scheduled FMC130 fuel sensor setup tomorrow at 10:30 AM in Gabtoli.",
        category = "Installation",
        timeAgo = "45m ago",
        isRead = false
    ),
    StaffNotificationItem(
        id = "3",
        title = "Monthly Target Milestone",
        message = "Congratulations! You have reached 70% of your November vehicle tracking installation quota.",
        category = "System",
        timeAgo = "3h ago",
        isRead = true
    ),
    StaffNotificationItem(
        id = "4",
        title = "Firmware VL03 v2.4 Rollout",
        message = "All newly dispatched Voice Tracker VL03 units require mandatory GPS calibration update.",
        category = "System",
        timeAgo = "Yesterday",
        isRead = true
    ),
    StaffNotificationItem(
        id = "5",
        title = "Client Overdue Payment Alert",
        message = "Invoice #INV-2026-1088 for Hanif Paribahan (BDT 12,000) is pending collection.",
        category = "Billing",
        timeAgo = "2 days ago",
        isRead = true
    )
)
