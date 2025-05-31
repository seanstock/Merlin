package com.example.merlin.ui.wallet

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.merlin.ui.theme.*
import com.example.merlin.ui.accessibility.AccessibilityConstants

/**
 * Screen time category with associated discount rate and icon
 */
data class ScreenTimeCategory(
    val name: String,
    val displayName: String,
    val rate: Float, // spending multiplier (lower = better discount)
    val icon: ImageVector,
    val color: Color,
    val description: String
)

/**
 * Time option for spending coins
 */
data class TimeOption(
    val minutes: Int,
    val displayText: String
)

/**
 * Dialog for spending Merlin Coins on screen time.
 * Shows categories, time options, and cost calculations.
 */
@Composable
fun SpendCoinsDialog(
    currentBalance: Int,
    onSpendCoins: (timeInSeconds: Int, category: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Available categories with their rates
    val categories = remember {
        listOf(
            ScreenTimeCategory(
                name = "entertainment",
                displayName = "Entertainment",
                rate = 1.0f,
                icon = Icons.Default.VideogameAsset,
                color = RoyalPurple,
                description = "Games, videos, fun apps"
            ),
            ScreenTimeCategory(
                name = "educational",
                displayName = "Educational",
                rate = 0.8f,
                icon = Icons.Default.School,
                color = WisdomBlue,
                description = "Learning apps, educational games (20% discount!)"
            ),
            ScreenTimeCategory(
                name = "creative",
                displayName = "Creative",
                rate = 0.7f,
                icon = Icons.Default.Brush,
                color = RoyalPurple,
                description = "Art, music, drawing apps (30% discount!)"
            ),
            ScreenTimeCategory(
                name = "physical",
                displayName = "Physical",
                rate = 0.5f,
                icon = Icons.Default.DirectionsRun,
                color = SageGreen,
                description = "Fitness, dance, movement apps (50% discount!)"
            )
        )
    }
    
    // Time options
    val timeOptions = remember {
        listOf(
            TimeOption(5, "5 minutes"),
            TimeOption(10, "10 minutes"),
            TimeOption(15, "15 minutes"),
            TimeOption(30, "30 minutes")
        )
    }
    
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var selectedTime by remember { mutableStateOf(timeOptions[0]) }
    
    // Calculate cost
    val costInSeconds = selectedTime.minutes * 60
    val actualCost = (costInSeconds * selectedCategory.rate).toInt()
    val canAfford = currentBalance >= actualCost
    
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
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = CloudWhite
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    // Header
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = AmberGlow,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Spend Merlin Coins",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = WisdomBlue
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Current Balance: $currentBalance MC",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AmberGlow,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                item {
                    // Category selection
                    Text(
                        text = "Choose Activity Type:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WisdomBlue,
                        modifier = Modifier.semantics {
                            heading()
                        }
                    )
                }
                
                items(categories.size) { index ->
                    val category = categories[index]
                    CategoryOption(
                        category = category,
                        isSelected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    // Time selection
                    Text(
                        text = "Choose Time Amount:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WisdomBlue,
                        modifier = Modifier.semantics {
                            heading()
                        }
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        timeOptions.forEach { timeOption ->
                            TimeOptionButton(
                                timeOption = timeOption,
                                isSelected = selectedTime == timeOption,
                                onClick = { selectedTime = timeOption },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                item {
                    // Cost calculation
                    CostCalculation(
                        selectedCategory = selectedCategory,
                        selectedTime = selectedTime,
                        actualCost = actualCost,
                        canAfford = canAfford
                    )
                }
                
                item {
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = WisdomBlue
                            )
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        Button(
                            onClick = {
                                onSpendCoins(costInSeconds, selectedCategory.name)
                                onDismiss()
                            },
                            enabled = canAfford,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canAfford) SageGreen else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (canAfford) CloudWhite else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(
                                text = if (canAfford) "Unlock Screen Time!" else "Not Enough Coins",
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
private fun CategoryOption(
    category: ScreenTimeCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "category_scale"
    )
    
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                onClickLabel = "Select ${category.displayName}",
                role = Role.RadioButton
            ) { onClick() }
            .semantics {
                contentDescription = "${category.displayName}: ${category.description}"
                role = Role.RadioButton
                stateDescription = if (isSelected) "Selected" else "Not selected"
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                category.color.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(category.color),
                width = 2.dp
            )
        } else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                category.icon,
                contentDescription = null,
                tint = category.color,
                modifier = Modifier.size(32.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = category.color
                )
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = category.color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun TimeOptionButton(
    timeOption: TimeOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "time_button_scale"
    )
    
    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) WisdomBlue else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) CloudWhite else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Text(
            text = timeOption.displayText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CostCalculation(
    selectedCategory: ScreenTimeCategory,
    selectedTime: TimeOption,
    actualCost: Int,
    canAfford: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (canAfford) SageGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Cost Breakdown:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WisdomBlue
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${selectedTime.displayText}:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${selectedTime.minutes * 60} MC",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (selectedCategory.rate < 1.0f) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${selectedCategory.displayName} discount:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SageGreen
                    )
                    Text(
                        text = "-${((1.0f - selectedCategory.rate) * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = SageGreen
                    )
                }
            }
            
            Divider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Cost:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$actualCost MC",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (canAfford) SageGreen else MaterialTheme.colorScheme.error
                )
            }
            
            if (!canAfford) {
                Text(
                    text = "💡 Complete more learning tasks to earn coins!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WisdomBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
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
            onSpendCoins = { _, _ -> },
            onDismiss = {}
        )
    }
} 