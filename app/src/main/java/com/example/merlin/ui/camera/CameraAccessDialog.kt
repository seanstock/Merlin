package com.example.merlin.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.merlin.economy.service.PurchasableAppDto
import com.example.merlin.economy.service.AppAccessPurchaseDto
import com.example.merlin.ui.theme.*

/**
 * Dialog for purchasing camera/app access with coins
 */
@Composable
fun CameraAccessDialog(
    availableApps: List<PurchasableAppDto>,
    lastPurchaseResult: AppAccessPurchaseDto?,
    isLoading: Boolean,
    errorMessage: String?,
    onPurchaseApp: (String, Int) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
    onClearLastPurchase: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        AppleCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppleSpacing.medium),
            backgroundColor = AppleSystemBackground,
            elevation = 8,
            cornerRadius = 24
        ) {
            Column(
                modifier = Modifier.padding(AppleSpacing.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "📸 Camera & Apps",
                    style = AppleHeadline.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ApplePrimaryLabel,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(AppleSpacing.medium))
                
                Text(
                    text = "Use your Merlin Coins to access fun apps!",
                    style = AppleBody,
                    color = AppleSecondaryLabel,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(AppleSpacing.large))

                // Show purchase result if available
                lastPurchaseResult?.let { result ->
                    PurchaseResultCard(
                        result = result,
                        onDismiss = onClearLastPurchase
                    )
                    Spacer(modifier = Modifier.height(AppleSpacing.medium))
                }

                // Show error if available
                errorMessage?.let { error ->
                    ErrorCard(
                        message = error,
                        onDismiss = onClearError
                    )
                    Spacer(modifier = Modifier.height(AppleSpacing.medium))
                }

                // Loading indicator
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AppleBlue)
                            Spacer(modifier = Modifier.height(AppleSpacing.medium))
                            Text(
                                text = "Loading...",
                                style = AppleBody,
                                color = AppleSecondaryLabel
                            )
                        }
                    }
                } else {
                    // Apps list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(AppleSpacing.medium)
                    ) {
                        items(availableApps) { app ->
                            AppAccessCard(
                                app = app,
                                onPurchase = { duration -> onPurchaseApp(app.packageName, duration) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppleSpacing.large))

                // Close button
                AppleButton(
                    text = "Close",
                    onClick = onDismiss,
                    style = AppleButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AppAccessCard(
    app: PurchasableAppDto,
    onPurchase: (Int) -> Unit
) {
    AppleCard(
        backgroundColor = AppleSecondarySystemBackground,
        cornerRadius = 16
    ) {
        Column(
            modifier = Modifier.padding(AppleSpacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppleSpacing.medium)
            ) {
                // App icon (emoji for now)
                Text(
                    text = when (app.packageName) {
                        "com.android.camera" -> "📸"
                        "com.google.android.calculator" -> "🧮"
                        "com.android.settings" -> "⚙️"
                        else -> "📱"
                    },
                    fontSize = 32.sp
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.displayName,
                        style = AppleHeadline.copy(fontWeight = FontWeight.SemiBold),
                        color = ApplePrimaryLabel
                    )
                    Text(
                        text = app.description,
                        style = AppleCaption,
                        color = AppleSecondaryLabel
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "${app.costPerMinute} coins/min • Max ${app.maxDuration} min",
                        style = AppleCaption.copy(fontWeight = FontWeight.Medium),
                        color = AppleBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppleSpacing.medium))

            // Purchase options
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppleSpacing.small)
            ) {
                // Quick access buttons for common durations
                listOf(2, 5, app.maxDuration).forEach { minutes ->
                    val cost = app.costPerMinute * minutes
                    AppleButton(
                        text = "${minutes}m\n${cost}🪙",
                        onClick = { onPurchase(minutes) },
                        style = AppleButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                        enabled = app.isInstalled
                    )
                }
            }
            
            if (!app.isInstalled) {
                Spacer(modifier = Modifier.height(AppleSpacing.small))
                Text(
                    text = "App not installed on this device",
                    style = AppleCaption,
                    color = AppleSecondaryLabel,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun PurchaseResultCard(
    result: AppAccessPurchaseDto,
    onDismiss: () -> Unit
) {
    AppleCard(
        backgroundColor = if (result.success) AppleGreen.copy(alpha = 0.1f) else AppleRed.copy(alpha = 0.1f),
        cornerRadius = 12
    ) {
        Column(
            modifier = Modifier.padding(AppleSpacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (result.success) "✅ Success!" else "❌ Failed",
                    style = AppleHeadline.copy(fontWeight = FontWeight.SemiBold),
                    color = if (result.success) AppleGreen else AppleRed
                )
                
                TextButton(onClick = onDismiss) {
                    Text("✕", color = AppleSecondaryLabel)
                }
            }
            
            if (result.success) {
                Text(
                    text = "App launched! Enjoy your ${result.durationMinutes} minutes.\nRemaining coins: ${result.remainingBalance}",
                    style = AppleBody,
                    color = ApplePrimaryLabel
                )
            } else {
                Text(
                    text = result.errorMessage,
                    style = AppleBody,
                    color = AppleRed
                )
            }
        }
    }
}

@Composable
fun ErrorCard(
    message: String,
    onDismiss: () -> Unit
) {
    AppleCard(
        backgroundColor = AppleRed.copy(alpha = 0.1f),
        cornerRadius = 12
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppleSpacing.medium)
        ) {
            Text(
                text = message,
                style = AppleBody,
                color = AppleRed,
                modifier = Modifier.weight(1f)
            )
            
            TextButton(onClick = onDismiss) {
                Text("✕", color = AppleSecondaryLabel)
            }
        }
    }
} 