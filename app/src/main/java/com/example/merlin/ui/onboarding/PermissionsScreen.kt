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
import com.example.merlin.ui.theme.MerlinTheme

/**
 * Permissions screen that requests necessary system permissions for the app to function.
 * Provides clear explanations for why each permission is needed.
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Header
        Text(
            text = "🔐",
            fontSize = 64.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "App Permissions",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Merlin needs these permissions to keep your child safe and provide the best learning experience.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Security note
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔒",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Privacy & Security",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "These permissions are used solely to provide a safe learning environment. Merlin does not access personal data from other apps or share information with third parties.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    lineHeight = 20.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            
            Button(
                onClick = onContinue,
                enabled = accessibilityGranted && overlayGranted,
                modifier = Modifier.weight(1f)
            ) {
                Text("Continue")
            }
        }
        
        if (!accessibilityGranted || !overlayGranted) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Please grant all required permissions to continue",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Individual permission card component.
 */
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
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = icon,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    
                    Column {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGranted) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        
                        Text(
                            text = importance,
                            fontSize = 12.sp,
                            color = if (importance == "Required") {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Status icon
                Icon(
                    imageVector = if (isGranted) Icons.Filled.Check else Icons.Filled.Warning,
                    contentDescription = if (isGranted) "Granted" else "Not granted",
                    tint = if (isGranted) {
                        Color(0xFF4CAF50)
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = description,
                fontSize = 14.sp,
                color = if (isGranted) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                },
                lineHeight = 20.sp
            )
            
            if (!isGranted) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

/**
 * Check if accessibility service is enabled.
 */
private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val serviceName = "${context.packageName}/com.example.merlin.services.MerlinAccessibilityService"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    return enabledServices?.contains(serviceName) == true
}

/**
 * Check if overlay permission is granted.
 */
private fun canDrawOverlays(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionsScreenPreview() {
    MerlinTheme {
        PermissionsScreen(
            onPermissionUpdate = { _, _ -> },
            onContinue = { },
            onBack = { },
            permissionsGranted = mapOf(
                "accessibility" to false,
                "overlay" to true
            )
        )
    }
} 