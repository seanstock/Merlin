package com.example.merlin.ui.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlin.ui.theme.*

/**
 * Permissions screen that requests necessary system permissions for the app to function.
 * Uses Apple's clean design language with clear explanations.
 */
@Composable
fun PermissionsScreen(
    onPermissionUpdate: (String, Boolean) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    permissionsGranted: Map<String, Boolean>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Permission states
    var accessibilityGranted by remember { 
        mutableStateOf(permissionsGranted["accessibility"] ?: false) 
    }
    var overlayGranted by remember { 
        mutableStateOf(permissionsGranted["overlay"] ?: false) 
    }
    
    // Check permissions on composition
    LaunchedEffect(Unit) {
        accessibilityGranted = isAccessibilityServiceEnabled(context)
        overlayGranted = canDrawOverlays(context)
        
        onPermissionUpdate("accessibility", accessibilityGranted)
        onPermissionUpdate("overlay", overlayGranted)
    }
    
    // Launcher for overlay permission
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        overlayGranted = canDrawOverlays(context)
        onPermissionUpdate("overlay", overlayGranted)
    }
    
    // Launcher for accessibility settings
    val accessibilityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        accessibilityGranted = isAccessibilityServiceEnabled(context)
        onPermissionUpdate("accessibility", accessibilityGranted)
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppleSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(AppleSpacing.large))
        
        // Header with Apple design
        Text(
            text = "🔐",
            fontSize = 64.sp,
            modifier = Modifier.padding(bottom = AppleSpacing.medium)
        )
        
        Text(
            text = "App Permissions",
            style = AppleNavigationTitle,
            color = ApplePrimaryLabel,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(AppleSpacing.small))
        
        Text(
            text = "Merlin needs these permissions to provide a safe and secure learning environment for your child.",
            style = AppleBody,
            color = AppleSecondaryLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = AppleSpacing.medium)
        )
        
        Spacer(modifier = Modifier.height(AppleSpacing.extraLarge))
        
        // Accessibility Permission
        PermissionCard(
            title = "Accessibility Service",
            description = "Allows Merlin to create a secure lock screen that protects your child from accessing other apps without permission.",
            icon = "🛡️",
            isGranted = accessibilityGranted,
            onRequestPermission = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                accessibilityLauncher.launch(intent)
            },
            importance = "Required"
        )
        
        Spacer(modifier = Modifier.height(AppleSpacing.medium))
        
        // Overlay Permission
        PermissionCard(
            title = "Display Over Other Apps",
            description = "Enables Merlin to show the learning interface on top of other apps, ensuring your child stays in the safe learning environment.",
            icon = "📱",
            isGranted = overlayGranted,
            onRequestPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    overlayPermissionLauncher.launch(intent)
                }
            },
            importance = "Required"
        )
        
        Spacer(modifier = Modifier.height(AppleSpacing.extraLarge))
        
        // Security note with Apple styling
        AppleCard(
            backgroundColor = AppleSystemGray6
        ) {
            Column(
                modifier = Modifier.padding(AppleSpacing.medium)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = AppleSpacing.small)
                ) {
                    Text(
                        text = "🔒",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = AppleSpacing.small)
                    )
                    Text(
                        text = "Privacy & Security",
                        style = AppleCallout.copy(fontWeight = FontWeight.SemiBold),
                        color = ApplePrimaryLabel
                    )
                }
                
                Text(
                    text = "These permissions are used solely to provide a safe learning environment. Merlin does not access personal data from other apps or share information with third parties.",
                    style = AppleFootnote,
                    color = AppleSecondaryLabel
                )
            }
        }
        
        Spacer(modifier = Modifier.height(AppleSpacing.extraLarge))
        
        // Navigation buttons with Apple styling
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppleSpacing.medium)
        ) {
            AppleButton(
                text = "Back",
                onClick = onBack,
                style = AppleButtonStyle.Secondary,
                modifier = Modifier.weight(1f)
            )
            
            AppleButton(
                text = "Continue",
                onClick = onContinue,
                style = AppleButtonStyle.Primary,
                enabled = accessibilityGranted && overlayGranted,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(AppleSpacing.large))
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    icon: String,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    importance: String,
    modifier: Modifier = Modifier
) {
    AppleCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppleSpacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = AppleSpacing.small)
            ) {
                Text(
                    text = icon,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = AppleSpacing.small)
                )
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = AppleSubheadline.copy(fontWeight = FontWeight.SemiBold),
                        color = ApplePrimaryLabel
                    )
                    Text(
                        text = importance,
                        style = AppleCaption,
                        color = if (isGranted) AppleGreen else AppleOrange
                    )
                }
                
                // Status indicator
                if (isGranted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Granted",
                        tint = AppleGreen,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Not granted",
                        tint = AppleOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Text(
                text = description,
                style = AppleFootnote,
                color = AppleSecondaryLabel,
                modifier = Modifier.padding(bottom = AppleSpacing.medium)
            )
            
            if (!isGranted) {
                AppleButton(
                    text = "Grant Permission",
                    onClick = onRequestPermission,
                    style = AppleButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Helper functions (these would typically be in a utility class)
private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    // Simplified check - in real implementation, you'd check for your specific service
    return try {
        val accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED
        )
        accessibilityEnabled == 1
    } catch (e: Settings.SettingNotFoundException) {
        false
    }
}

private fun canDrawOverlays(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true // Permission not required on older versions
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionsScreenPreview() {
    MerlinTheme {
        PermissionsScreen(
            onPermissionUpdate = { _, _ -> },
            onContinue = {},
            onBack = {},
            permissionsGranted = mapOf(
                "accessibility" to false,
                "overlay" to true
            )
        )
    }
} 