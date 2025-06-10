package com.example.merlin.ui.parent

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlin.ui.theme.*

/**
 * Profiles screen for managing multiple child profiles
 * TODO: Implement full profile management functionality
 */
@Composable
fun ProfilesScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = RoyalPurple.copy(alpha = 0.6f),
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Profile Management",
                style = MaterialTheme.typography.headlineMedium,
                color = RoyalPurple,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Manage profiles for each child.",
                style = MaterialTheme.typography.bodyLarge,
                color = RoyalPurple.copy(alpha = 0.8f)
            )
        }
    }
} 