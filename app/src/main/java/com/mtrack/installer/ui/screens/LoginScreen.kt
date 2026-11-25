package com.mtrack.installer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtrack.installer.ui.theme.*
import com.mtrack.installer.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel
) {
    val state by authViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBackground)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Enterprise Brand Logo & Headline
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BrandBluePrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = CardBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "M ERP",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp,
                        fontSize = 22.sp
                    )
                )
                Text(
                    text = "Enterprise Fleet & Sales Command",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Main Authentication Card (Ramp/Stripe Style)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Sign In to Your Workspace",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Enter your official staff credentials to access your departmental data.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        )
                    }

                    // Error Notification Banner
                    if (!state.errorMessage.isNullOrBlank()) {
                        Surface(
                            color = RoseBg,
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RoseError.copy(alpha = 0.3f)))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = RoseError,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = state.errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = RoseError,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    // Username Input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Username, Phone or Email",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = state.usernameInput,
                            onValueChange = { authViewModel.onUsernameChange(it) },
                            placeholder = {
                                Text(text = "e.g. fahim_sales or 017XXXXXXXX", style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted))
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Outlined.Person, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = BorderSubtle,
                                focusedBorderColor = BrandBluePrimary,
                                unfocusedContainerColor = SurfaceBackground,
                                focusedContainerColor = CardBackground
                            )
                        )
                    }

                    // Password Input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Password",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = state.passwordInput,
                            onValueChange = { authViewModel.onPasswordChange(it) },
                            placeholder = {
                                Text(text = "Enter your staff password", style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted))
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Outlined.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { authViewModel.togglePasswordVisibility() }) {
                                    Icon(
                                        imageVector = if (state.isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = "Toggle password",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    authViewModel.submitLogin()
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = BorderSubtle,
                                focusedBorderColor = BrandBluePrimary,
                                unfocusedContainerColor = SurfaceBackground,
                                focusedContainerColor = CardBackground
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Sign In Submit Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            authViewModel.submitLogin()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandBluePrimary,
                            disabledContainerColor = BrandBluePrimary.copy(alpha = 0.5f)
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = CardBackground
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Sign In to ERP",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = CardBackground,
                                        fontSize = 15.sp
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Outlined.ArrowForward,
                                    contentDescription = null,
                                    tint = CardBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Helpline Capsule
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBackground)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Phone,
                    contentDescription = null,
                    tint = BrandBluePrimary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "IT Helpdesk: 09638-988988",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp
                    )
                )
            }
        }
    }
}
