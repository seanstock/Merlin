package com.example.merlin.ui.wallet

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlin.ui.theme.*
import com.example.merlin.ui.accessibility.AccessibilityConstants

/**
 * Wallet display component as a simple gold coin showing Merlin Coin balance.
 * Designed to be child-friendly with coin-like appearance and clear visual feedback.
 */
@Composable
fun WalletDisplay(
    balance: Int,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate time equivalent (1 MC = 1 second)
    val totalSeconds = balance
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    
    // Animation for balance changes
    val animatedBalance by animateFloatAsState(
        targetValue = balance.toFloat(),
        animationSpec = tween(durationMillis = 500),
        label = "balance_animation"
    )
    
    // Scale animation for interaction feedback
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "scale_animation"
    )
    
    // Subtle gold gradient for the coin
    val coinGradient = Brush.radialGradient(
        colors = listOf(
            AmberGlow.copy(alpha = 0.9f),
            AmberGlow.copy(alpha = 0.7f),
            AmberGlow.copy(alpha = 0.6f)
        ),
        radius = 60f
    )
    
    // Coin design
    Box(
        modifier = modifier
            .size(48.dp) // Same size as chat avatar
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 2.dp else 4.dp,
                shape = CircleShape
            )
            .clip(CircleShape)
            .background(coinGradient)
            .clickable(
                onClickLabel = "Spend Merlin Coins",
                role = Role.Button
            ) {
                isPressed = true
                onClick()
            }
            .semantics {
                contentDescription = "Wallet: ${animatedBalance.toInt()} Merlin Coins, equivalent to $minutes minutes and $seconds seconds of screen time"
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = CloudWhite
            )
        } else {
            // Display the coin amount
            Text(
                text = when {
                    animatedBalance.toInt() >= 1000 -> "${(animatedBalance.toInt() / 1000).toInt()}k"
                    animatedBalance.toInt() >= 100 -> "${animatedBalance.toInt()}"
                    else -> "${animatedBalance.toInt()}"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = CloudWhite,
                fontSize = when {
                    animatedBalance.toInt() >= 1000 -> 11.sp
                    animatedBalance.toInt() >= 100 -> 10.sp
                    animatedBalance.toInt() >= 10 -> 12.sp
                    else -> 14.sp
                }
            )
        }
        
        // Subtle inner ring for coin effect
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            AmberGlow.copy(alpha = 0.1f)
                        ),
                        radius = 30f
                    )
                )
        )
    }
    
    // Reset pressed state
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(150)
            isPressed = false
        }
    }
}

/**
 * Preview function for WalletDisplay
 */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun WalletDisplayPreview() {
    MaterialTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WalletDisplay(
                balance = 5,
                onClick = {}
            )
            WalletDisplay(
                balance = 150,
                onClick = {}
            )
            WalletDisplay(
                balance = 1250,
                onClick = {}
            )
            WalletDisplay(
                balance = 0,
                isLoading = true,
                onClick = {}
            )
        }
    }
} 