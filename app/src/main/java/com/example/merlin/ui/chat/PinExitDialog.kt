package com.example.merlin.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.merlin.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * PIN authentication dialog for exiting the lock screen overlay.
 * Provides secure parent authentication to return to the main app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinExitDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onPinVerified: () -> Unit,
    onPinVerification: suspend (String) -> Boolean,
    maxAttempts: Int = Int.MAX_VALUE, // Default to unlimited attempts
    modifier: Modifier = Modifier
) {
    var pinInput by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var attemptCount by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Reset state when dialog becomes visible
    LaunchedEffect(isVisible) {
        if (isVisible) {
            pinInput = ""
            showPin = false
            isVerifying = false
            errorMessage = ""
            showError = false
            attemptCount = 0
        }
    }

    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = IvoryWhite
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Text(
                        text = "🔓",
                        fontSize = 48.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Parent Authentication",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ApplePrimaryLabel
                        ),
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Enter your parent PIN to access settings",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = AppleSecondaryLabel
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // PIN Input Field
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { newValue ->
                            if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                                pinInput = newValue
                                if (showError) {
                                    showError = false
                                    errorMessage = ""
                                }
                            }
                        },
                        label = { 
                            Text(
                                "Parent PIN (4 digits)",
                                color = AppleSecondaryLabel
                            ) 
                        },
                        singleLine = true,
                        enabled = !isVerifying,
                        visualTransformation = if (showPin) {
                            VisualTransformation.None 
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        trailingIcon = {
                            IconButton(
                                onClick = { showPin = !showPin },
                                enabled = !isVerifying
                            ) {
                                Icon(
                                    imageVector = if (showPin) {
                                        Icons.Filled.Visibility
                                    } else {
                                        Icons.Filled.VisibilityOff
                                    },
                                    contentDescription = if (showPin) "Hide PIN" else "Show PIN",
                                    tint = AppleSecondaryLabel
                                )
                            }
                        },
                        isError = showError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppleBlue,
                            unfocusedBorderColor = AppleGray3,
                            focusedLabelColor = AppleBlue,
                            cursorColor = AppleBlue,
                            focusedTextColor = ApplePrimaryLabel,
                            unfocusedTextColor = ApplePrimaryLabel
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Error Message
                    if (showError) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel Button
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !isVerifying,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AppleSecondaryLabel
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 1.dp
                            )
                        ) {
                            Text("Cancel")
                        }

                        // Verify Button
                        Button(
                            onClick = {
                                if (pinInput.length == 4) {
                                    isVerifying = true
                                    // Launch coroutine for PIN verification
                                    coroutineScope.launch {
                                        try {
                                            val isValid = onPinVerification(pinInput)
                                            if (isValid) {
                                                onPinVerified()
                                            } else {
                                                attemptCount++
                                                if (attemptCount >= maxAttempts) {
                                                    // Close dialog after max attempts reached
                                                    onDismiss()
                                                } else {
                                                    errorMessage = "Incorrect PIN. Please try again."
                                                    showError = true
                                                    pinInput = ""
                                                }
                                            }
                                        } catch (e: Exception) {
                                            attemptCount++
                                            if (attemptCount >= maxAttempts) {
                                                // Close dialog after max attempts reached
                                                onDismiss()
                                            } else {
                                                errorMessage = "Authentication error. Please try again."
                                                showError = true
                                                pinInput = ""
                                            }
                                        } finally {
                                            isVerifying = false
                                        }
                                    }
                                }
                            },
                            enabled = !isVerifying && pinInput.length == 4,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppleBlue,
                                contentColor = AppleSystemBackground,
                                disabledContainerColor = AppleGray4,
                                disabledContentColor = AppleGray
                            )
                        ) {
                            if (isVerifying) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = AppleSystemBackground
                                    )
                                    Text("Verifying...")
                                }
                            } else {
                                Text("Unlock")
                            }
                        }
                    }

                    // Helper Text
                    Text(
                        text = "This PIN was set during initial setup",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppleSecondaryLabel.copy(alpha = 0.7f)
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Preview for PIN exit dialog
 */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PinExitDialogPreview() {
    com.example.merlin.ui.theme.MerlinTheme {
        PinExitDialog(
            isVisible = true,
            onDismiss = {},
            onPinVerified = {},
            onPinVerification = { false }
        )
    }
} 