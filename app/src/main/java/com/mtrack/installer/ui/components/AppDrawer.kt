package com.mtrack.installer.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtrack.installer.data.model.UserProfile
import com.mtrack.installer.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppDrawer(
    currentRoute: String,
    currentUser: UserProfile?,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val context = LocalContext.current

    // Live Date & Time Formatter
    val currentDateStr = remember {
        SimpleDateFormat("EEEE, dd MMM yyyy", Locale.US).format(Date())
    }
    val currentTimeStr = remember {
        SimpleDateFormat("hh:mm a", Locale.US).format(Date())
    }

    ModalDrawerSheet(
        modifier = Modifier.width(310.dp),
        drawerShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
        drawerContainerColor = CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Top Brand Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandBluePrimary)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardBackground.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "M",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = CardBackground,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Column {
                            Text(
                                text = "M ERP",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CardBackground,
                                    fontSize = 16.sp
                                )
                            )
                            Text(
                                text = "Sales Operations Portal",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CardBackground.copy(alpha = 0.85f),
                                    fontSize = 10.5.sp
                                )
                            )
                        }
                    }

                    // Live Date & Time Capsule Widget in Sidebar
                    Surface(
                        color = CardBackground.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    tint = CardBackground,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = currentDateStr,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CardBackground,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 10.sp
                                    )
                                )
                            }

                            Text(
                                text = currentTimeStr,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CardBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp
                                )
                            )
                        }
                    }
                }
            }

            // 2. User Identity Snippet
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onNavigate("profile")
                        onCloseDrawer()
                    },
                color = SurfaceBackground
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val initials = (currentUser?.fullName ?: "Fahim Ahmed").split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BrandBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials.ifEmpty { "FA" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = BrandBluePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.fullName ?: "Fahim Ahmed",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "${currentUser?.designation ?: "Sales & Marketing"} • ${currentUser?.department ?: "Sales"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 10.5.sp
                            )
                        )
                    }

                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "Profile",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = BorderSubtle)

            // 3. Main Navigation Menu
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "MENU MODULES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                )

                DrawerMenuItem(
                    title = "Sales Dashboard",
                    icon = Icons.Outlined.Dashboard,
                    isSelected = currentRoute == "dashboard",
                    onClick = {
                        onNavigate("dashboard")
                        onCloseDrawer()
                    }
                )

                DrawerMenuItem(
                    title = "Staffs Directory",
                    icon = Icons.Outlined.PeopleOutline,
                    isSelected = currentRoute == "staffs",
                    onClick = {
                        onNavigate("staffs")
                        onCloseDrawer()
                    }
                )

                DrawerMenuItem(
                    title = "Sold Fleet Portfolio",
                    icon = Icons.Outlined.DirectionsCar,
                    isSelected = currentRoute == "fleet",
                    onClick = {
                        onNavigate("fleet")
                        onCloseDrawer()
                    }
                )

                DrawerMenuItem(
                    title = "Executive Profile",
                    icon = Icons.Outlined.Person,
                    isSelected = currentRoute == "profile" || currentRoute == "profile_edit",
                    onClick = {
                        onNavigate("profile")
                        onCloseDrawer()
                    }
                )

                DrawerMenuItem(
                    title = "About M ERP",
                    icon = Icons.Outlined.Info,
                    isSelected = currentRoute == "about_us",
                    onClick = {
                        onNavigate("about_us")
                        onCloseDrawer()
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = BorderSubtle
                )

                Text(
                    text = "SUPPORT & HELPLINE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                )

                DrawerMenuItem(
                    title = "24/7 Helpline: 09638-988988",
                    icon = Icons.Outlined.Phone,
                    isSelected = false,
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:09638988988"))
                        context.startActivity(intent)
                    }
                )

                DrawerMenuItem(
                    title = "WhatsApp Support",
                    icon = Icons.Outlined.Chat,
                    isSelected = false,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801321119888"))
                        context.startActivity(intent)
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = BorderSubtle
                )

                // Sign Out Action Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onLogout()
                            onCloseDrawer()
                        },
                    color = RoseBg,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Logout,
                            contentDescription = "Sign Out",
                            tint = RoseError,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Sign Out",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = RoseError,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) BrandBlueLight else CardBackground,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) BrandBluePrimary else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isSelected) BrandBluePrimary else TextPrimary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 13.sp
                )
            )
        }
    }
}
