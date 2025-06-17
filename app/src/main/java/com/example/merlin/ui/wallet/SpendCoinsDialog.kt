package com.example.merlin.ui.wallet

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.merlin.ui.theme.*
import com.example.merlin.economy.service.AppLaunchService
import com.example.merlin.economy.service.PurchasableAppDto
import com.example.merlin.config.ServiceLocator
import kotlinx.coroutines.launch

/**
 * Simplified spending dialog for 3-year-olds
 * Only shows apps and emergency calling - no free time to OS
 */
@Composable
fun SpendCoinsDialog(
    currentBalance: Int,
    appLaunchService: AppLaunchService,
    childId: String,
    onSpendCoins: (timeInSeconds: Int, category: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    walletViewModel: WalletViewModel? = null
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // App access state
    var availableApps by remember { mutableStateOf<List<PurchasableAppDto>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }
    var selectedApp by remember { mutableStateOf<PurchasableAppDto?>(null) }
    var selectedTime by remember { mutableStateOf(10) } // Default 10 minutes
    
    // Load available apps
    LaunchedEffect(Unit) {
        scope.launch {
            val result = appLaunchService.getAvailableApps()
            if (result.isSuccess) {
                availableApps = result.getOrThrow()
                if (availableApps.isNotEmpty()) {
                    selectedApp = availableApps.first()
                }
            }
            isLoadingApps = false
        }
    }
    
    // Calculate cost for selected app
    val totalCost = selectedApp?.let { app ->
        (app.costPerMinute * selectedTime * 0.9f).toInt() // 10% discount for apps
    } ?: 0
    val canAfford = currentBalance >= totalCost

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
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = CloudWhite
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with big coin icon
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🪙",
                        fontSize = 48.sp
                    )
                    Text(
                        text = "I have $currentBalance coins!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AmberGlow,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Call Daddy Button - Costs coins
                CallDaddyButton(
                    currentBalance = currentBalance,
                    onCallPurchase = {
                        if (walletViewModel != null) {
                            // Spend 20 coins to call daddy
                            walletViewModel.spendCoins(20, "call_daddy")
                            
                            // Determine actual dialer package that will handle ACTION_DIAL
                            val pm = context.packageManager
                            val dialerResolve = pm.resolveActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:")), 0)
                            val resolvedPkg = dialerResolve?.activityInfo?.packageName
                            val packagesToAllow = mutableSetOf<String>()
                            resolvedPkg?.let { packagesToAllow.add(it) }
                            packagesToAllow.add("com.android.dialer")
                            packagesToAllow.add("com.google.android.dialer")
                            packagesToAllow.forEach { pkg ->
                                com.example.merlin.config.ServiceLocator.getKioskManager(context).addAllowedPackage(pkg)
                                android.util.Log.d("SpendCoinsDialog", "Whitelisted $pkg for lock-task")
                            }

                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:8586108633")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            android.util.Log.d("SpendCoinsDialog", "Launching dialer intent with resolvedPkg=$resolvedPkg")
                            context.startActivity(dialIntent)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Divider
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
                
                if (isLoadingApps) {
                    CircularProgressIndicator(
                        color = AmberGlow,
                        modifier = Modifier.size(48.dp)
                    )
                } else if (availableApps.isEmpty()) {
                    Text(
                        text = "No apps available right now",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                } else {
                    // App selection section
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Pick an app to play with:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = WisdomBlue
                        )
                        
                        // App options - big, colorful buttons
                        availableApps.forEach { app ->
                            AppOptionButton(
                                app = app,
                                isSelected = selectedApp == app,
                                onClick = { selectedApp = app },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        // Time selection - simple buttons
                        Text(
                            text = "How long do you want to play?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = WisdomBlue
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(5, 10, 15, 20).forEach { minutes ->
                                TimeButton(
                                    minutes = minutes,
                                    isSelected = selectedTime == minutes,
                                    onClick = { selectedTime = minutes },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        // Cost display
                        if (selectedApp != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (canAfford) SageGreen.copy(alpha = 0.1f) 
                                                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (canAfford) "This costs $totalCost coins" else "Need $totalCost coins",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (canAfford) SageGreen else MaterialTheme.colorScheme.error
                                    )
                                    
                                    if (!canAfford) {
                                        Text(
                                            text = "🎯 Play learning games to earn more coins!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = WisdomBlue,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cancel button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = WisdomBlue
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Maybe later",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Play button
                    if (selectedApp != null) {
                        Button(
                            onClick = {
                                if (canAfford && walletViewModel != null) {
                                    walletViewModel.purchaseAppAccess(
                                        appPackage = selectedApp!!.packageName,
                                        durationMinutes = selectedTime
                                    )
                                }
                                onDismiss()
                            },
                            enabled = canAfford && selectedApp != null,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canAfford) SageGreen else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (canAfford) CloudWhite else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = if (canAfford) "Let's play! 🎮" else "Need more coins",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallDaddyButton(
    currentBalance: Int,
    onCallPurchase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val callCost = 20
    val canAfford = currentBalance >= callCost
    
    Button(
        onClick = onCallPurchase,
        enabled = canAfford,
        modifier = modifier.height(72.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (canAfford) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant, // Green when affordable
            contentColor = if (canAfford) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📞",
                fontSize = 32.sp
            )
            Column {
                Text(
                    text = if (canAfford) "Call Daddy" else "Call Daddy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (canAfford) "Costs $callCost coins" else "Need $callCost coins",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (canAfford) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun AppOptionButton(
    app: PurchasableAppDto,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "app_button_scale"
    )
    
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                onClickLabel = "Select ${app.displayName}",
                role = Role.RadioButton
            ) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                AmberGlow.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(AmberGlow),
                width = 3.dp
            )
        } else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App icon/emoji
            Text(
                text = when (app.packageName) {
                    "com.google.android.apps.youtube.kids" -> "📺"
                    else -> "📱"
                },
                fontSize = 40.sp
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AmberGlow
                )
                Text(
                    text = "${app.costPerMinute} coins per minute",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = AmberGlow,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun TimeButton(
    minutes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) WisdomBlue else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) CloudWhite else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$minutes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "min",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Preview function for SpendCoinsDialog
 */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SpendCoinsDialogPreview() {
    MaterialTheme {
        SpendCoinsDialog(
            currentBalance = 150,
            appLaunchService = object : AppLaunchService {
                override suspend fun launchApp(packageName: String) = Result.success(true)
                override suspend fun isAppInstalled(packageName: String) = Result.success(true)
                override suspend fun getAvailableApps() = Result.success(listOf(
                    PurchasableAppDto(
                        packageName = "com.google.android.apps.youtube.kids",
                        displayName = "YouTube Kids",
                        description = "Safe videos for children",
                        category = "entertainment",
                        costPerMinute = 10,
                        maxDuration = 30,
                        isInstalled = true
                    )
                ))
                override suspend fun calculateAppAccessCost(appPackage: String, durationMinutes: Int) = Result.success(60)
                override suspend fun purchaseAppAccess(childId: String, appPackage: String, durationMinutes: Int) = 
                    Result.success(com.example.merlin.economy.service.AppAccessPurchaseDto(
                        success = true, appPackage = appPackage, durationMinutes = durationMinutes,
                        totalCost = 60, remainingBalance = 90, sessionId = "test", expiresAt = ""
                    ))
            },
            childId = "preview_child",
            onSpendCoins = { _, _ -> },
            onDismiss = {}
        )
    }
} 