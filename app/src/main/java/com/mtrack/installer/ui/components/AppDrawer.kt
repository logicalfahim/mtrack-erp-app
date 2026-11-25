package com.mtrack.installer.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtrack.installer.data.model.UserProfile
import com.mtrack.installer.ui.theme.*

@Composable
fun AppDrawer(
    currentRoute: String,
    currentUser: UserProfile?,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val context = LocalContext.current

    ModalDrawerSheet(
        modifier = Modifier.width(310.dp),
        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        drawerContainerColor = CardBackground
    ) {
        // Drawer Header with User Profile / Brand Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandBluePrimary)
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(CardBackground.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (currentUser?.fullName?.take(1) ?: "M").uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = CardBackground,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Column {
                    Text(
                        text = currentUser?.fullName ?: "Staff Member",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = CardBackground
                        )
                    )
                    Text(
                        text = currentUser?.department ?: "M TRACK ERP",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CardBackground.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Role Badge
            Surface(
                color = CardBackground.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "${currentUser?.designation ?: "Staff"} • ${currentUser?.role?.uppercase() ?: "USER"}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CardBackground,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.5.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Navigation Items
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isSales = currentUser?.role?.lowercase() in listOf("seller", "sales", "marketing", "sales_executive") || currentUser?.department?.lowercase()?.contains("sales") == true

            DrawerMenuItem(
                title = if (isSales) "Sales Dashboard" else "Department Dashboard",
                icon = Icons.Outlined.Dashboard,
                isSelected = currentRoute == "dashboard",
                onClick = {
                    onNavigate("dashboard")
                    onCloseDrawer()
                }
            )

            // Installation requests are strictly for Technical / Support roles
            if (!isSales && currentUser?.role?.lowercase() in listOf("technician", "installer", "support", "technical", "admin", "superadmin")) {
                DrawerMenuItem(
                    title = "Installation Operations",
                    icon = Icons.Outlined.Build,
                    isSelected = currentRoute == "install_wizard",
                    onClick = {
                        onNavigate("install_wizard")
                        onCloseDrawer()
                    }
                )
            }

            DrawerMenuItem(
                title = "About M TRACK ERP",
                icon = Icons.Outlined.Info,
                isSelected = currentRoute == "about_us",
                onClick = {
                    onNavigate("about_us")
                    onCloseDrawer()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = BorderSubtle
            )

            Text(
                text = "SUPPORT & HELPLINE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)
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
                modifier = Modifier.padding(vertical = 8.dp),
                color = BorderSubtle
            )

            // Logout Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        onLogout()
                        onCloseDrawer()
                    }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Logout,
                    contentDescription = "Logout",
                    tint = RoseError,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = RoseError
                    )
                )
            }
        }

        // Drawer Footer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "© 2026 M TRACK ERP Enterprise Mobile v1.1.0",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextMuted,
                    fontWeight = FontWeight.Normal
                )
            )
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
    val bgColor = if (isSelected) BrandBlueLight else CardBackground
    val contentColor = if (isSelected) BrandBluePrimary else TextPrimary
    val fw = if (isSelected) FontWeight.Medium else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = fw,
                color = contentColor
            )
        )
    }
}
