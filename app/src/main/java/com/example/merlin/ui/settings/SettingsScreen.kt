package com.example.merlin.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlin.ui.accessibility.AccessibilityConstants
import com.example.merlin.ui.theme.*

data class SettingsItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isWip: Boolean = true,
    val onClick: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onExitApp: () -> Unit,
    onNavigateToParentDashboard: () -> Unit = {}
) {
    // Create settings items - all are now accessible since we verified PIN to enter
    val settingsItems = listOf(
        SettingsItem(
            title = "Profile",
            description = "Manage parent profile and settings",
            icon = Icons.Default.Person,
            isWip = true
        ),
        SettingsItem(
            title = "Child Profile", 
            description = "Configure child's learning preferences",
            icon = Icons.Default.ChildCare,
            isWip = true
        ),
        SettingsItem(
            title = "Child Performance",
            description = "View learning progress and achievements",
            icon = Icons.Default.School,
            isWip = false,
            onClick = onNavigateToParentDashboard
        ),
        SettingsItem(
            title = "Time Economy",
            description = "Set screen time limits and schedules",
            icon = Icons.Default.Schedule,
            isWip = true
        ),
        SettingsItem(
            title = "Exit",
            description = "Exit Merlin and return to device",
            icon = Icons.Default.ExitToApp,
            isWip = false,
            onClick = onExitApp // Direct exit since PIN was verified at entry
        )
    )
    
    // Main settings content
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MistyBlue.copy(alpha = 0.3f),
                            LavenderMist.copy(alpha = 0.2f),
                            IceBlue.copy(alpha = 0.4f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // Header with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .background(
                            color = SeafoamMist.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = AccessibilityConstants.ContentDescriptions.BACK_BUTTON,
                        tint = DeepOcean
                    )
                }
                
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = DeepOcean
                    ),
                    textAlign = TextAlign.Center
                )
                
                // Spacer to balance the layout
                Spacer(modifier = Modifier.size(48.dp))
            }
            
            // Settings items list
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(settingsItems) { item ->
                    SettingsItemCard(
                        item = item,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItemCard(
    item: SettingsItem,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { 
            if (!item.isWip) {
                item.onClick()
            }
        },
        modifier = modifier
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isWip) {
                Color.White.copy(alpha = 0.4f)
            } else {
                Color.White.copy(alpha = 0.7f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (item.isWip) 2.dp else 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = if (item.isWip) {
                            LavenderMist.copy(alpha = 0.6f)
                        } else {
                            SeafoamMist.copy(alpha = 0.8f)
                        },
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = if (item.isWip) {
                        DeepOcean.copy(alpha = 0.6f)
                    } else {
                        DeepOcean
                    },
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = if (item.isWip) {
                                DeepOcean.copy(alpha = 0.6f)
                            } else {
                                DeepOcean
                            }
                        )
                    )
                    
                    if (item.isWip) {
                        Surface(
                            color = LavenderMist.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = "WIP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = DeepOcean.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp,
                        color = if (item.isWip) {
                            DeepOcean.copy(alpha = 0.5f)
                        } else {
                            DeepOcean.copy(alpha = 0.8f)
                        }
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Arrow icon for active items
            if (!item.isWip) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Navigate to ${item.title}",
                    tint = DeepOcean.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
} 