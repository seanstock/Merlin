package com.example.merlin.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlin.ui.theme.*
import com.example.merlin.config.ServiceLocator

@Composable
fun ThemeSelectionScreen(
    selectedThemeId: String?,
    onThemeSelected: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeService = remember { ServiceLocator.getThemeService(context) }
    val availableThemes = remember { themeService.getAllThemes() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppleSystemBackground)
            .padding(AppleSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(AppleSpacing.xl))
        
        // Title
        Text(
            text = "Choose Your Adventure!",
            style = AppleLargeTitle,
            color = ApplePrimaryLabel,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(AppleSpacing.small))
        
        Text(
            text = "Pick a magical world to explore with your tutor",
            style = MaterialTheme.typography.bodyLarge,
            color = AppleSecondaryLabel,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(AppleSpacing.xl))
        
        // Theme Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(AppleSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppleSpacing.large),
            horizontalArrangement = Arrangement.spacedBy(AppleSpacing.large),
            modifier = Modifier.weight(1f)
        ) {
            items(availableThemes) { theme ->
                ThemeCard(
                    theme = theme,
                    isSelected = selectedThemeId == theme.id,
                    onSelected = { onThemeSelected(theme.id) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(AppleSpacing.large))
        
        // Continue Button
        Button(
            onClick = onContinue,
            enabled = selectedThemeId != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppleBlue,
                disabledContainerColor = AppleGray4
            ),
            shape = RoundedCornerShape(AppleCornerRadius.large)
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(AppleSpacing.large))
    }
}

@Composable
fun ThemeCard(
    theme: AppTheme,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppleCard(
        modifier = modifier
            .aspectRatio(0.8f)
            .clickable { onSelected() }
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = AppleBlue,
                        shape = RoundedCornerShape(AppleCornerRadius.large)
                    )
                } else {
                    Modifier
                }
            ),
        elevation = if (isSelected) 4 else 2
    ) {
        Box {
            // Background preview
            Image(
                painter = painterResource(id = theme.backgroundImage), // Use portrait for theme selection
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(AppleCornerRadius.large)),
                contentScale = ContentScale.Crop
            )
            
            // Overlay with theme info
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(alpha = 0.4f),
                        RoundedCornerShape(AppleCornerRadius.large)
                    )
            )
            
            // Theme content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppleSpacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Theme name
                Text(
                    text = theme.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(AppleSpacing.small))
                
                // Tutor info
                Text(
                    text = "Meet ${theme.tutorName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(AppleSpacing.xs))
                
                Text(
                    text = theme.tutorDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
            
            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(AppleSpacing.small)
                        .size(24.dp)
                        .background(
                            AppleBlue,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
} 