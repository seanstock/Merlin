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
import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke

/**
 * Simplified spending dialog for 3-year-olds
 * Only shows apps and emergency calling - no free time to OS
 */
@Composable
fun SpendCoinsDialog(
    currentBalance: Int,
    appLaunchService: AppLaunchService,
    childId: String,
    onSpendCoins: (Int, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var availableApps by remember { mutableStateOf(emptyList<PurchasableAppDto>()) }
    var isLoadingApps by remember { mutableStateOf(true) }
    var selectedApp by remember { mutableStateOf<PurchasableAppDto?>(null) }
    var selectedTime by remember { mutableStateOf(10) }
    var selectedCall by remember { mutableStateOf<CallOption?>(null) }

    val context = LocalContext.current
    val walletViewModel = remember {
        try {
            val factory = WalletViewModelFactory(
                application = context.applicationContext as Application,
                childId = childId
            )
            ViewModelProvider(context as ComponentActivity, factory)[WalletViewModel::class.java]
        } catch (e: Exception) {
            null
        }
    }

    val callOptions = listOf(
        CallOption("Dad", "👨", "8586108633"),
        CallOption("Mom", "👩", "8586108633"),
        CallOption("Grandpa", "👴", "8586108633"),
        CallOption("Grandma", "👵", "8586108633")
    )

    // Calculate costs
    val totalCost = when {
        selectedApp != null -> selectedApp!!.costPerMinute * selectedTime
        selectedCall != null -> selectedCall!!.cost
        else -> 0
    }
    val canAfford = currentBalance >= totalCost

    // Load available apps
    LaunchedEffect(Unit) {
        try {
            val result = appLaunchService.getAvailableApps()
            if (result.isSuccess) {
                availableApps = result.getOrNull() ?: emptyList()
            }
        } catch (e: Exception) {
            // Handle error silently
        } finally {
            isLoadingApps = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFAFAFA) // Clean off-white
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left side - Store Items (65%)
                Column(
                    modifier = Modifier.weight(0.65f),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🛒",
                            fontSize = 48.sp
                        )
                        Text(
                            text = "Store",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    // Call family section
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Call Family:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2C3E50)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            callOptions.take(2).forEach { call ->
                                CallStoreButton(
                                    call = call,
                                    isSelected = selectedCall == call,
                                    onClick = { 
                                        selectedCall = call
                                        selectedApp = null // Clear app selection
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            callOptions.drop(2).forEach { call ->
                                CallStoreButton(
                                    call = call,
                                    isSelected = selectedCall == call,
                                    onClick = { 
                                        selectedCall = call
                                        selectedApp = null // Clear app selection
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    // Divider
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                    
                    // Apps section
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
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Play Apps:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2C3E50)
                            )
                            
                            availableApps.forEach { app ->
                                AppOptionButton(
                                    app = app,
                                    isSelected = selectedApp == app,
                                    onClick = { 
                                        selectedApp = app
                                        selectedCall = null // Clear call selection
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                
                // Right side - Receipt (35%)
                Card(
                    modifier = Modifier.weight(0.35f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8F9FA)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Receipt header
                        Text(
                            text = "💰 Balance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                        
                        Text(
                            text = "$currentBalance coins",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                        
                        HorizontalDivider(
                            color = Color(0xFF2C3E50).copy(alpha = 0.2f),
                            thickness = 1.dp
                        )
                        
                        // Order details
                        when {
                            selectedCall != null -> {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Order:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF2C3E50).copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "Call ${selectedCall!!.name} ${selectedCall!!.emoji}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2C3E50)
                                    )
                                    Text(
                                        text = "Cost: ${selectedCall!!.cost} coins",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (canAfford) Color(0xFF2C3E50) else Color(0xFF8B4513)
                                    )
                                }
                            }
                            selectedApp != null -> {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Order:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF2C3E50).copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = selectedApp!!.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2C3E50)
                                    )
                                    Text(
                                        text = "Duration: 10 minutes",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF2C3E50).copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "Cost: $totalCost coins",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (canAfford) Color(0xFF2C3E50) else Color(0xFF8B4513)
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = "Select an item to see cost",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF2C3E50).copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        if (!canAfford && (selectedCall != null || selectedApp != null)) {
                            Text(
                                text = "🎯 Play learning games to earn more coins!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2C3E50).copy(alpha = 0.8f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Action buttons
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Purchase button
                            if (selectedCall != null || selectedApp != null) {
                                Button(
                                    onClick = {
                                        if (canAfford && walletViewModel != null) {
                                            when {
                                                selectedCall != null -> {
                                                    // Handle call purchase
                                                    walletViewModel.spendCoins(selectedCall!!.cost, "call_${selectedCall!!.name}")
                                                    
                                                                                                         // Setup dialer for kiosk mode
                                                     val pm = context.packageManager
                                                    val dialerResolve = pm.resolveActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:")), 0)
                                                    val resolvedPkg = dialerResolve?.activityInfo?.packageName
                                                    val packagesToAllow = mutableSetOf<String>()
                                                    resolvedPkg?.let { packagesToAllow.add(it) }
                                                    packagesToAllow.add("com.android.dialer")
                                                    packagesToAllow.add("com.google.android.dialer")
                                                    packagesToAllow.forEach { pkg ->
                                                        ServiceLocator.getKioskManager(context).addAllowedPackage(pkg)
                                                    }

                                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${selectedCall!!.phoneNumber}")).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(dialIntent)
                                                }
                                                selectedApp != null -> {
                                                    walletViewModel.purchaseAppAccess(
                                                        appPackage = selectedApp!!.packageName,
                                                        durationMinutes = selectedTime
                                                    )
                                                }
                                            }
                                        }
                                        onDismiss()
                                    },
                                    enabled = canAfford && (selectedCall != null || selectedApp != null),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (canAfford) Color(0xFFE8F4F8) else Color(0xFFF5F5F5),
                                        contentColor = if (canAfford) Color(0xFF2C3E50) else Color(0xFF9E9E9E)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = when {
                                            selectedCall != null -> if (canAfford) "Call! 📞" else "Need coins"
                                            selectedApp != null -> if (canAfford) "Play! 🎮" else "Need coins"
                                            else -> "Select item"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            // Exit button
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF2C3E50)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Exit",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class CallOption(
    val name: String,
    val emoji: String,
    val phoneNumber: String,
    val cost: Int = 20
)

@Composable
private fun CallStoreButton(
    call: CallOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canAfford = true // Will be handled by parent component
    
    Button(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFFE8F4F8) else Color(0xFFF5F5F5),
            contentColor = Color(0xFF2C3E50)
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, Color(0xFF2C3E50)) else null
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = call.emoji,
                fontSize = 24.sp
            )
            Text(
                text = call.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
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