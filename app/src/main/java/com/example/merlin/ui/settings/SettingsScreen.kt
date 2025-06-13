package com.example.merlin.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.merlin.ui.accessibility.AccessibilityConstants
import com.example.merlin.ui.theme.*

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onExitApp: () -> Unit,
    onNavigateToParentDashboard: () -> Unit,
    onNavigateToChildProfile: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppleSystemBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppleSpacing.large)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppleSpacing.large),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = AccessibilityConstants.ContentDescriptions.BACK_BUTTON,
                        tint = AppleBlue
                    )
                }
                Text(
                    text = "Settings",
                    style = AppleLargeTitle,
                    color = ApplePrimaryLabel,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(48.dp)) // To balance the back button
            }

            // Settings Items
            SettingsItem(
                title = "Child Profile",
                description = "Edit name, age, and theme",
                icon = Icons.Default.Face,
                onClick = onNavigateToChildProfile
            )
            SettingsItem(
                title = "Parent Dashboard",
                description = "View progress and manage settings",
                icon = Icons.Default.School,
                onClick = onNavigateToParentDashboard
            )
            SettingsItem(
                title = "Exit Merlin",
                description = "Sign out and exit the application",
                icon = Icons.Default.ExitToApp,
                onClick = onExitApp
            )
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    AppleCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AppleSpacing.medium)
            .clickable { onClick() },
        elevation = 1,
        cornerRadius = 16
    ) {
        AppleListItem(
            title = title,
            subtitle = description,
            leading = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = AppleGray5,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AppleBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            trailing = {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AppleGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    }
} 