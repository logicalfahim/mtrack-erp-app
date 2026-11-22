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
import com.mtrack.installer.ui.theme.*

@Composable
fun AppDrawer(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val context = LocalContext.current

    ModalDrawerSheet(
        modifier = Modifier.width(310.dp),
        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        drawerContainerColor = CardBackground
    ) {
        // Drawer Header with Brand Logo & Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandBluePrimary)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CardBackground.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.GpsFixed,
                    contentDescription = "Logo",
                    tint = CardBackground,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "M TRACK",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = CardBackground,
                    letterSpacing = 0.5.sp
                )
            )

            Text(
                text = "Vehicle Intelligence & GPS Fleet",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = CardBackground.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Normal
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Version Pill
            Surface(
                color = CardBackground.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "v1.0.0 Enterprise",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CardBackground,
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Items
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DrawerMenuItem(
                title = "New Installation Request",
                icon = Icons.Outlined.AddCircleOutline,
                isSelected = currentRoute == "install_wizard",
                onClick = {
                    onNavigate("install_wizard")
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                title = "About Us (Company)",
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
                text = "DIRECT SUPPORT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
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

            DrawerMenuItem(
                title = "Visit mtrack.com.bd",
                icon = Icons.Outlined.Language,
                isSelected = false,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mtrack.com.bd"))
                    context.startActivity(intent)
                }
            )
        }

        // Drawer Footer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "© 2026 M TRACK ERP. All rights reserved.",
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
