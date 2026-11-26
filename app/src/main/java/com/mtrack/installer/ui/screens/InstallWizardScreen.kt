package com.mtrack.installer.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mtrack.installer.data.model.VehicleItem
import com.mtrack.installer.ui.theme.*
import com.mtrack.installer.ui.viewmodel.InstallViewModel
import com.mtrack.installer.util.NidOcrParser
import java.util.*

@Composable
fun InstallWizardScreen(
    viewModel: InstallViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBackground)
    ) {
        // Step Tracker Header (6 Progressive Steps)
        StepTrackerHeader(
            currentStep = uiState.currentStep,
            totalSteps = 6
        )

        // Error Banner
        AnimatedVisibility(visible = uiState.errorMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = uiState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Wizard Step Body
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            when (uiState.currentStep) {
                1 -> Step1NidCapture(viewModel = viewModel)
                2 -> Step2CustomerIdentity(viewModel = viewModel)
                3 -> Step3ContactProfile(viewModel = viewModel)
                4 -> Step4VehiclesFleet(viewModel = viewModel)
                5 -> Step5ScheduleAndReferral(viewModel = viewModel)
                6 -> Step6ReviewAndSubmit(viewModel = viewModel)
            }
        }
    }
}

// =============================================================================
// Step Tracker (6 Steps)
// =============================================================================
@Composable
private fun StepTrackerHeader(
    currentStep: Int,
    totalSteps: Int
) {
    val stepTitles = listOf("NID", "Identity", "Contact", "Vehicles", "Schedule", "Review")

    Surface(
        color = CardBackground,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                stepTitles.forEachIndexed { index, title ->
                    val stepNum = index + 1
                    val isPassed = stepNum < currentStep
                    val isCurrent = stepNum == currentStep

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isPassed -> EmeraldSuccess
                                        isCurrent -> BrandBluePrimary
                                        else -> BorderSubtle
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPassed) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            } else {
                                Text(
                                    text = "$stepNum",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isCurrent) Color.White else TextSecondary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isCurrent) BrandBluePrimary else TextSecondary,
                                fontSize = 9.5.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// STEP 1: Ultra-Modern On-Screen NID Capture & Smart OCR Parsing
// =============================================================================
@Composable
private fun Step1NidCapture(
    viewModel: InstallViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var frontBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var backBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isFrontScanning by remember { mutableStateOf(false) }
    var isBackScanning by remember { mutableStateOf(false) }

    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // Front Camera & Gallery Launchers
    val frontCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            frontBitmap = bitmap
            isFrontScanning = true
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    isFrontScanning = false
                    val result = NidOcrParser.parseFront(visionText.text)
                    if (result.fullName.isNotBlank() || result.nidNumber.isNotBlank()) {
                        viewModel.updateIdentityDetails(
                            name = result.fullName.ifEmpty { null },
                            nid = result.nidNumber.ifEmpty { null },
                            scanned = true
                        )
                    }
                }
                .addOnFailureListener { isFrontScanning = false }
        }
    }

    val frontGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    frontBitmap = bitmap
                    isFrontScanning = true
                    val image = InputImage.fromBitmap(bitmap, 0)
                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            isFrontScanning = false
                            val result = NidOcrParser.parseFront(visionText.text)
                            if (result.fullName.isNotBlank() || result.nidNumber.isNotBlank()) {
                                viewModel.updateIdentityDetails(
                                    name = result.fullName.ifEmpty { null },
                                    nid = result.nidNumber.ifEmpty { null },
                                    scanned = true
                                )
                            }
                        }
                        .addOnFailureListener { isFrontScanning = false }
                }
            } catch (_: Exception) {
                isFrontScanning = false
            }
        }
    }

    // Back Camera & Gallery Launchers
    val backCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            backBitmap = bitmap
            isBackScanning = true
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    isBackScanning = false
                    val result = NidOcrParser.parseBack(visionText.text)
                    if (result.address.isNotBlank()) {
                        viewModel.updateIdentityDetails(
                            address = result.address,
                            scanned = true
                        )
                    }
                }
                .addOnFailureListener { isBackScanning = false }
        }
    }

    val backGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    backBitmap = bitmap
                    isBackScanning = true
                    val image = InputImage.fromBitmap(bitmap, 0)
                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            isBackScanning = false
                            val result = NidOcrParser.parseBack(visionText.text)
                            if (result.address.isNotBlank()) {
                                viewModel.updateIdentityDetails(
                                    address = result.address,
                                    scanned = true
                                )
                            }
                        }
                        .addOnFailureListener { isBackScanning = false }
                }
            } catch (_: Exception) {
                isBackScanning = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Modern Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandBlueSoft),
            border = BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BrandBluePrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DocumentScanner,
                        contentDescription = null,
                        tint = BrandBluePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "National ID Card Scanner",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = BrandBluePrimary
                        )
                    )
                    Text(
                        text = "Capture both front and back sides to automatically extract customer details",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    )
                }
            }
        }

        // Slot 1: NID Front Side Card (Premium Modern Design)
        ModernNidImageSlot(
            title = "NID Card — Front Side",
            subtitle = "Extracts Customer Name and NID Number",
            icon = Icons.Outlined.Badge,
            bitmap = frontBitmap,
            isScanning = isFrontScanning,
            onCameraClick = { frontCameraLauncher.launch() },
            onGalleryClick = { frontGalleryLauncher.launch("image/*") }
        )

        // Slot 2: NID Back Side Card (Premium Modern Design)
        ModernNidImageSlot(
            title = "NID Card — Back Side",
            subtitle = "Extracts Full Billing Address",
            icon = Icons.Outlined.HomeWork,
            bitmap = backBitmap,
            isScanning = isBackScanning,
            onCameraClick = { backCameraLauncher.launch() },
            onGalleryClick = { backGalleryLauncher.launch("image/*") }
        )

        // Live Extraction Preview Chip
        if (uiState.fullName.isNotBlank() || uiState.nidNumber.isNotBlank() || uiState.fullAddress.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = VerifiedGreenBg),
                border = BorderStroke(1.dp, VerifiedGreenBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = VerifiedGreenText,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "NID Data Extracted Successfully",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = VerifiedGreenText,
                                fontSize = 13.sp
                            )
                        )
                    }
                    if (uiState.fullName.isNotBlank()) {
                        Text(
                            text = "Name: ${uiState.fullName}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    if (uiState.nidNumber.isNotBlank()) {
                        Text(
                            text = "NID: ${uiState.nidNumber}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                        )
                    }
                    if (uiState.fullAddress.isNotBlank()) {
                        Text(
                            text = "Address: ${uiState.fullAddress}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            ),
                            maxLines = 2
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Action: Continue with Extracted Data
        Button(
            onClick = {
                viewModel.proceedFromNidScan()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
        ) {
            Text(
                text = "Continue with Scanned Data →",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.White
                )
            )
        }

        // Action: Skip & Enter Manually
        TextButton(
            onClick = {
                viewModel.skipNidToManual()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "No ID Card? Enter Details Manually →",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = BrandOrangeAccent
                )
            )
        }
    }
}

/**
 * Premium Modern NID Slot with refined borders, rounded styling, and high-clarity buttons
 */
@Composable
private fun ModernNidImageSlot(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bitmap: Bitmap?,
    isScanning: Boolean,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, if (bitmap != null) VerifiedGreenBorder else BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (bitmap != null) VerifiedGreenBg else BrandBlueSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (bitmap != null) Icons.Outlined.Check else icon,
                            contentDescription = null,
                            tint = if (bitmap != null) VerifiedGreenText else BrandBluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        )
                    }
                }

                if (bitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = VerifiedGreenBg,
                        border = BorderStroke(1.dp, VerifiedGreenBorder)
                    ) {
                        Text(
                            text = "Captured",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = VerifiedGreenText,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Image Preview or Empty State
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "NID Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    if (isScanning) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Action Links
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCameraClick) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retake Photo", fontSize = 11.sp)
                    }
                    TextButton(onClick = onGalleryClick) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick Gallery", fontSize = 11.sp)
                    }
                }
            } else {
                // Empty State with Refined Buttons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(105.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceBackground)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = BrandBluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onCameraClick,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoCamera,
                                    contentDescription = "Camera",
                                    tint = BrandBluePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Camera",
                                    fontSize = 12.sp,
                                    color = BrandBluePrimary
                                )
                            }

                            OutlinedButton(
                                onClick = onGalleryClick,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BorderSubtle),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoLibrary,
                                    contentDescription = "Gallery",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Gallery",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// STEP 2: Customer Identity (Name, NID Number, Billing Address ONLY)
// =============================================================================
@Composable
private fun Step2CustomerIdentity(
    viewModel: InstallViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Customer Identity",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Full Name, National ID, and Billing Address",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        )
                    }

                    if (uiState.isNidScanned) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VerifiedGreenBg,
                            border = BorderStroke(1.dp, VerifiedGreenBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Verified,
                                    contentDescription = null,
                                    tint = VerifiedGreenText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Auto-Scanned",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = VerifiedGreenText,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                // Full Name
                OutlinedTextField(
                    value = uiState.fullName,
                    onValueChange = { viewModel.updateIdentityDetails(name = it) },
                    label = { Text("Customer Full Name *") },
                    placeholder = { Text("e.g. Mohammad Fahim") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (uiState.isNidScanned) VerifiedGreenBg.copy(alpha = 0.3f) else CardBackground,
                        unfocusedContainerColor = if (uiState.isNidScanned) VerifiedGreenBg.copy(alpha = 0.15f) else CardBackground,
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = if (uiState.isNidScanned) VerifiedGreenBorder else BorderSubtle
                    )
                )

                // NID Number
                OutlinedTextField(
                    value = uiState.nidNumber,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }
                        viewModel.updateIdentityDetails(nid = digitsOnly)
                    },
                    label = { Text("National ID (NID) Number *") },
                    placeholder = { Text("10, 13, or 17 digit NID") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Badge,
                            contentDescription = null,
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (uiState.isNidScanned) VerifiedGreenBg.copy(alpha = 0.3f) else CardBackground,
                        unfocusedContainerColor = if (uiState.isNidScanned) VerifiedGreenBg.copy(alpha = 0.15f) else CardBackground,
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = if (uiState.isNidScanned) VerifiedGreenBorder else BorderSubtle
                    )
                )

                // Billing Address
                OutlinedTextField(
                    value = uiState.fullAddress,
                    onValueChange = { viewModel.updateIdentityDetails(address = it) },
                    label = { Text("Full Billing Address *") },
                    placeholder = { Text("House, Road, Area, City, District") },
                    minLines = 2,
                    maxLines = 3,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = null,
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (uiState.isNidScanned) VerifiedGreenBg.copy(alpha = 0.3f) else CardBackground,
                        unfocusedContainerColor = if (uiState.isNidScanned) VerifiedGreenBg.copy(alpha = 0.15f) else CardBackground,
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = if (uiState.isNidScanned) VerifiedGreenBorder else BorderSubtle
                    )
                )
            }
        }

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.prevStep() },
                modifier = Modifier
                    .weight(0.8f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("NID Scan", color = TextSecondary, fontSize = 13.sp)
            }

            Button(
                onClick = { viewModel.nextStep() },
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
            ) {
                Text("Next: Contact →", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

// =============================================================================
// STEP 3: Contact & Profile Details (Separated into its own step!)
// =============================================================================
@Composable
private fun Step3ContactProfile(
    viewModel: InstallViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Contact & Profile Details",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Primary mobile number, alternative contact, and email",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                )

                HorizontalDivider(color = BorderSubtle)

                // Contact Phone Number (Mandatory 11 digits)
                OutlinedTextField(
                    value = uiState.contactNumber,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }.take(11)
                        viewModel.updateContactDetails(phone = digitsOnly)
                    },
                    label = { Text("Primary Contact Number *") },
                    placeholder = { Text("01XXXXXXXXX") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = null,
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                // Alternative Phone Number
                OutlinedTextField(
                    value = uiState.altNumber,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }.take(11)
                        viewModel.updateContactDetails(altPhone = digitsOnly)
                    },
                    label = { Text("Alternative Phone Number (Optional)") },
                    placeholder = { Text("01XXXXXXXXX") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.ContactPhone,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                // Email Address
                OutlinedTextField(
                    value = uiState.emailAddress,
                    onValueChange = { viewModel.updateContactDetails(email = it) },
                    label = { Text("Email Address (Optional)") },
                    placeholder = { Text("customer@example.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                // Profession
                OutlinedTextField(
                    value = uiState.profession,
                    onValueChange = { viewModel.updateContactDetails(profession = it) },
                    label = { Text("Profession / Business (Optional)") },
                    placeholder = { Text("e.g. Fleet Owner, Businessman") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.WorkOutline,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = BorderSubtle
                    )
                )
            }
        }

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.prevStep() },
                modifier = Modifier
                    .weight(0.8f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Identity", color = TextSecondary, fontSize = 13.sp)
            }

            Button(
                onClick = { viewModel.nextStep() },
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
            ) {
                Text("Next: Vehicles →", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

// =============================================================================
// STEP 4: Vehicles Fleet & Conditional Bus Specifications
// =============================================================================
@Composable
private fun Step4VehiclesFleet(
    viewModel: InstallViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Vehicle Fleet Specification",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Select vehicle category and hardware features",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                )
            }

            Button(
                onClick = { viewModel.addVehicle() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add Vehicle",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Add Car", fontSize = 11.sp, color = Color.White)
            }
        }

        uiState.vehicles.forEachIndexed { index, vehicle ->
            VehicleCardItem(
                index = index,
                vehicle = vehicle,
                canRemove = uiState.vehicles.size > 1,
                onUpdate = { updated -> viewModel.updateVehicle(index, updated) },
                onRemove = { viewModel.removeVehicle(index) }
            )
        }

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.prevStep() },
                modifier = Modifier
                    .weight(0.8f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Contact", color = TextSecondary, fontSize = 13.sp)
            }

            Button(
                onClick = { viewModel.nextStep() },
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
            ) {
                Text("Next: Schedule →", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun VehicleCardItem(
    index: Int,
    vehicle: VehicleItem,
    canRemove: Boolean,
    onUpdate: (VehicleItem) -> Unit,
    onRemove: () -> Unit
) {
    val vehicleTypes = listOf("Private Car", "Bus", "Microbus", "Truck", "Pickup", "Motorcycle", "CNG / Auto", "Other")
    val deviceTypes = listOf("Voice", "Non-Voice", "Two Way Communication", "OBD")
    val startTypes = listOf("Key Start", "Push Start")
    val engineCutOptions = listOf("Yes", "No")

    val isBus = vehicle.vehicleType == "Bus"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, if (isBus) BrandOrangeAccent.copy(alpha = 0.6f) else BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isBus) BrandOrangeAccent.copy(alpha = 0.15f) else BrandBlueSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${index + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isBus) BrandOrangeAccent else BrandBluePrimary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Text(
                        text = if (isBus) "Bus Fleet Unit" else "Vehicle Unit #${index + 1}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    )
                }

                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderSubtle)

            // Vehicle Category Dropdown
            FormDropdownField(
                label = "Vehicle Type Category *",
                selectedValue = vehicle.vehicleType,
                options = vehicleTypes,
                onOptionSelected = { onUpdate(vehicle.copy(vehicleType = it)) }
            )

            // Conditional BUS Fleet Specifications
            if (isBus) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BrandOrangeAccent.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, BrandOrangeAccent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DirectionsBus,
                                contentDescription = null,
                                tint = BrandOrangeAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Bus Fleet Specifications",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandOrangeAccent,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        // Bus Company / Operator *
                        OutlinedTextField(
                            value = vehicle.busCompany ?: "",
                            onValueChange = { onUpdate(vehicle.copy(busCompany = it)) },
                            label = { Text("Bus Company / Operator *") },
                            placeholder = { Text("e.g. Hanif Enterprise, Shyamoli") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandOrangeAccent,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        // Bus Registration Number *
                        OutlinedTextField(
                            value = vehicle.registrationNo,
                            onValueChange = { onUpdate(vehicle.copy(registrationNo = it.uppercase())) },
                            label = { Text("Bus Registration Number *") },
                            placeholder = { Text("e.g. DHAKA-METRO-BA-11-2345") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandOrangeAccent,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        // Chassis Number *
                        OutlinedTextField(
                            value = vehicle.chassisNo ?: "",
                            onValueChange = { onUpdate(vehicle.copy(chassisNo = it.uppercase())) },
                            label = { Text("Chassis Number *") },
                            placeholder = { Text("Enter bus chassis number") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandOrangeAccent,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )
                    }
                }
            } else {
                // Non-Bus: Registration Number & Vehicle Brand & Year
                OutlinedTextField(
                    value = vehicle.registrationNo,
                    onValueChange = { onUpdate(vehicle.copy(registrationNo = it.uppercase())) },
                    label = { Text("Registration Number *") },
                    placeholder = { Text("e.g. DHAKA-METRO-GA-12-3456") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                OutlinedTextField(
                    value = vehicle.vehicleBrandModel,
                    onValueChange = { onUpdate(vehicle.copy(vehicleBrandModel = it)) },
                    label = { Text("Vehicle Brand & Year *") },
                    placeholder = { Text("e.g. Toyota Allion 2018") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = BorderSubtle
                    )
                )
            }

            // Device Type Dropdown
            FormDropdownField(
                label = "GPS Device Type *",
                selectedValue = vehicle.deviceType,
                options = deviceTypes,
                onOptionSelected = { onUpdate(vehicle.copy(deviceType = it)) }
            )

            // Row: Ignition Start Type & Engine Cut
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FormDropdownField(
                    label = "Ignition Type",
                    selectedValue = vehicle.startType,
                    options = startTypes,
                    onOptionSelected = { onUpdate(vehicle.copy(startType = it)) },
                    modifier = Modifier.weight(1f)
                )

                FormDropdownField(
                    label = "Engine Cut",
                    selectedValue = vehicle.engineCut,
                    options = engineCutOptions,
                    onOptionSelected = { onUpdate(vehicle.copy(engineCut = it)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// =============================================================================
// STEP 5: Installation Schedule & Referral Information
// =============================================================================
@Composable
private fun Step5ScheduleAndReferral(
    viewModel: InstallViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    val openPicker = {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        val amPm = if (hourOfDay < 12) "AM" else "PM"
                        val formattedHour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                        val formattedMinute = String.format("%02d", minute)
                        val formattedDate = String.format("%04d-%02d-%02d %02d:%s %s", year, month + 1, dayOfMonth, formattedHour, formattedMinute, amPm)
                        viewModel.updateSchedule(dateTime = formattedDate)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Schedule Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Installation Schedule",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                )

                HorizontalDivider(color = BorderSubtle)

                // Installation Location / City *
                OutlinedTextField(
                    value = uiState.installLocation,
                    onValueChange = { viewModel.updateSchedule(location = it) },
                    label = { Text("Installation Location / City *") },
                    placeholder = { Text("e.g. Mirpur, Dhaka") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                // Preferred Date & Time Picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.prefDateTime.ifEmpty { "Select Date & Time" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Preferred Date & Time") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = BrandBluePrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBluePrimary,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { openPicker() }
                    )
                }
            }
        }

        // Referral Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Referral Information (Optional)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                )

                HorizontalDivider(color = BorderSubtle)

                // Referrer Name
                OutlinedTextField(
                    value = uiState.refName,
                    onValueChange = { viewModel.updateSchedule(refName = it) },
                    label = { Text("Referrer Name") },
                    placeholder = { Text("Name of person who referred") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.PersonPin,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Referrer Phone
                OutlinedTextField(
                    value = uiState.refPhone,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(11)
                        viewModel.updateSchedule(refPhone = digits)
                    },
                    label = { Text("Referrer Mobile Number") },
                    placeholder = { Text("01XXXXXXXXX") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.PhoneAndroid,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Referrer Area / Branch
                OutlinedTextField(
                    value = uiState.refArea,
                    onValueChange = { viewModel.updateSchedule(refArea = it) },
                    label = { Text("Referrer Area / Branch") },
                    placeholder = { Text("e.g. Uttara Branch") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Domain,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.prevStep() },
                modifier = Modifier
                    .weight(0.8f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Vehicles", color = TextSecondary, fontSize = 13.sp)
            }

            Button(
                onClick = { viewModel.nextStep() },
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
            ) {
                Text("Next: Review →", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

// =============================================================================
// STEP 6: Review & Submit
// =============================================================================
@Composable
private fun Step6ReviewAndSubmit(
    viewModel: InstallViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Customer Identity Summary Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customer Identity & Contact",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = BrandBluePrimary
                        )
                    )
                    if (uiState.isNidScanned) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = VerifiedGreenBg,
                            border = BorderStroke(1.dp, VerifiedGreenBorder)
                        ) {
                            Text(
                                text = "NID Verified",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = VerifiedGreenText,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                ReviewRow(label = "Full Name", value = uiState.fullName)
                ReviewRow(label = "NID Number", value = uiState.nidNumber)
                ReviewRow(label = "Billing Address", value = uiState.fullAddress)
                ReviewRow(label = "Primary Mobile", value = uiState.contactNumber)
                if (uiState.altNumber.isNotBlank()) {
                    ReviewRow(label = "Alternative Mobile", value = uiState.altNumber)
                }
                if (uiState.emailAddress.isNotBlank()) {
                    ReviewRow(label = "Email Address", value = uiState.emailAddress)
                }
                if (uiState.profession.isNotBlank()) {
                    ReviewRow(label = "Profession", value = uiState.profession)
                }
            }
        }

        // Vehicles Summary Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Fleet Units (${uiState.vehicles.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = BrandBluePrimary
                    )
                )

                HorizontalDivider(color = BorderSubtle)

                uiState.vehicles.forEachIndexed { index, veh ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceBackground,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "#${index + 1} ${veh.vehicleType}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = veh.registrationNo,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = BrandBluePrimary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                            if (veh.vehicleType == "Bus") {
                                Text("Company: ${veh.busCompany ?: "N/A"} | Chassis: ${veh.chassisNo ?: "N/A"}", fontSize = 11.sp, color = TextSecondary)
                            } else {
                                Text("Model: ${veh.vehicleBrandModel}", fontSize = 11.sp, color = TextSecondary)
                            }
                            Text(
                                text = "Device: ${veh.deviceType} • Ignition: ${veh.startType} • Cut: ${veh.engineCut}",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Schedule & Referral Summary Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Installation & Referral",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = BrandBluePrimary
                    )
                )

                HorizontalDivider(color = BorderSubtle)

                ReviewRow(label = "Install Location", value = uiState.installLocation)
                ReviewRow(label = "Date & Time", value = uiState.prefDateTime.ifEmpty { "Immediate" })
                if (uiState.refName.isNotBlank()) {
                    ReviewRow(label = "Referrer", value = "${uiState.refName} (${uiState.refPhone}) - ${uiState.refArea}")
                }
            }
        }

        // Final Submit & Back Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.prevStep() },
                modifier = Modifier
                    .weight(0.8f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit", color = TextSecondary, fontSize = 13.sp)
            }

            Button(
                onClick = { viewModel.submitRequest() },
                modifier = Modifier
                    .weight(1.2f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrangeAccent),
                enabled = !uiState.isSubmitting
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.CloudUpload,
                        contentDescription = "Submit",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Submit Request",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun FormDropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandBluePrimary,
                unfocusedBorderColor = BorderSubtle
            ),
            singleLine = true
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CardBackground)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (option == selectedValue) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (option == selectedValue) BrandBluePrimary else TextPrimary
                            )
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Normal,
                color = TextSecondary,
                fontSize = 12.sp
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                fontSize = 12.sp
            ),
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
