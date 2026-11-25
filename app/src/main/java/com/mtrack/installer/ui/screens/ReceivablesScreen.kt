package com.mtrack.installer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtrack.installer.data.model.ClientDueItem
import com.mtrack.installer.ui.theme.*
import com.mtrack.installer.ui.viewmodel.DashboardViewModel
import java.util.Locale

@Composable
fun ReceivablesScreen(
    dashboardViewModel: DashboardViewModel
) {
    val state by dashboardViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val salesData = state.dashboardData?.salesDepartment

    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableIntStateOf(0) } // 0 = All, 1 = High (>BDT 5k), 2 = Standard

    val duesList = salesData?.clientDuesList ?: emptyList()

    val filteredDues = remember(duesList, searchQuery, filterType) {
        val q = searchQuery.trim().lowercase()
        duesList.filter { d ->
            val matchQuery = q.isEmpty() ||
                    (d.vehiclePlateNumber?.lowercase()?.contains(q) == true) ||
                    (d.customerName?.lowercase()?.contains(q) == true) ||
                    (d.customerPhone?.contains(q) == true) ||
                    d.invoiceNo.lowercase().contains(q)

            val matchFilter = when (filterType) {
                1 -> d.dueAmount >= 5000.0 // High priority
                2 -> d.dueAmount < 5000.0  // Standard
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

        // Screen Header: Title & Total Receivables Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Receivables Due",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 20.sp
                    )
                )
                Text(
                    text = "${filteredDues.size} unpaid client invoices awaiting recovery",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                )
            }

            Surface(
                color = AmberWarning.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AmberWarning.copy(alpha = 0.3f)))
            ) {
                Text(
                    text = "BDT ${numberFormatShort(salesData?.totalDueAmount ?: 0.0)}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AmberWarning,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            placeholder = {
                Text(
                    text = "Search invoice number, client or phone...",
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

        // Segmented Filter Capsules
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ReceivableFilterPill(title = "All Invoices (${duesList.size})", isSelected = filterType == 0, onClick = { filterType = 0 })
            ReceivableFilterPill(title = "High Value (>5k)", isSelected = filterType == 1, onClick = { filterType = 1 })
            ReceivableFilterPill(title = "Standard Dues", isSelected = filterType == 2, onClick = { filterType = 2 })
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Virtualized Due Recovery List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (filteredDues.isEmpty()) {
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
                            Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "All Client Receivables Settled",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Text(
                                text = "No unpaid accounts match your criteria.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                }
            } else {
                items(filteredDues, key = { it.invoiceId }) { dueItem ->
                    ReceivableCardItem(due = dueItem, context = context)
                }
            }
        }
    }
}

@Composable
private fun ReceivableFilterPill(
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
private fun ReceivableCardItem(
    due: ClientDueItem,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val initials = (due.customerName ?: "Client").split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AmberWarning.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials.ifEmpty { "C" },
                            style = MaterialTheme.typography.labelMedium.copy(color = AmberWarning, fontWeight = FontWeight.Bold)
                        )
                    }

                    Column {
                        Text(
                            text = due.customerName ?: "Client",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        )
                        Text(
                            text = due.customerPhone ?: "No phone on record",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "BDT ${numberFormatShort(due.dueAmount)} Due",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AmberWarning,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.5.sp
                        )
                    )
                    Text(
                        text = "#${due.invoiceNo}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                    )
                }
            }

            // Progress Bar & Due Details
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
                        Text(
                            text = due.vehiclePlateNumber ?: "Device Unit",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Total: BDT ${numberFormatShort(due.grandTotal)}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }

                    val paidRatio = if (due.grandTotal > 0) ((due.paidAmount / due.grandTotal).toFloat()).coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { paidRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = EmeraldSuccess,
                        trackColor = BorderSubtle
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Paid: BDT ${numberFormatShort(due.paidAmount)}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                        Text(
                            text = "Billed: ${due.invoiceDate?.take(10) ?: "N/A"}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                    }
                }
            }

            // Direct Call Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status: Action Required",
                    style = MaterialTheme.typography.labelSmall.copy(color = AmberWarning, fontWeight = FontWeight.Medium, fontSize = 11.sp)
                )

                if (!due.customerPhone.isNullOrBlank()) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${due.customerPhone}"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.Phone, contentDescription = null, tint = CardBackground, modifier = Modifier.size(13.dp))
                            Text(
                                text = "Direct Call",
                                style = MaterialTheme.typography.labelMedium.copy(
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

private fun numberFormatShort(amount: Double): String {
    return if (amount >= 100000) {
        String.format(Locale.US, "%.1fL", amount / 100000)
    } else if (amount >= 1000) {
        String.format(Locale.US, "%.1fK", amount / 1000)
    } else {
        String.format(Locale.US, "%.0f", amount)
    }
}
