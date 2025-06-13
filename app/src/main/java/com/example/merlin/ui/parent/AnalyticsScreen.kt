package com.example.merlin.ui.parent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.merlin.economy.model.*
import com.example.merlin.economy.service.LearningRecommendationDto
import com.example.merlin.config.ServiceLocator
import kotlinx.coroutines.launch
import kotlin.math.*
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.tooling.preview.Preview
import com.example.merlin.ui.theme.*

@Composable
fun EmptyState(
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    AppleCard(
        modifier = modifier.fillMaxWidth(),
        elevation = 1,
        cornerRadius = 16
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppleSpacing.xl)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(AppleSpacing.medium))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Analytics screen showing child progress analytics visualization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    childId: String = "demo_child", // Default for now, should be passed from parent
    onNavigateToSettings: () -> Unit = {} // Navigation callback for settings
) {
    val context = LocalContext.current
    val factory = remember(childId) {
        AnalyticsViewModelFactory(
            adaptiveDifficultyService = ServiceLocator.getAdaptiveDifficultyService(),
            economyService = ServiceLocator.getEconomyService(context),
            badgeService = ServiceLocator.getBadgeService(),
            experienceService = ServiceLocator.getExperienceService()
        )
    }

    val viewModel: AnalyticsViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()

    LaunchedEffect(key1 = childId) {
        viewModel.loadAnalytics(childId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Apple-style large title header
        AppleSectionHeader(
            title = "Analytics",
            subtitle = "Track your child's learning progress",
            modifier = Modifier.padding(top = AppleSpacing.medium)
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppleSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppleSpacing.medium),
            contentPadding = PaddingValues(vertical = AppleSpacing.medium)
        ) {
            item {
                SubjectMasteryCard(masteryLevels = state.subjectMastery)
            }
            item {
                BadgeCard(badges = state.earnedBadges)
            }
            item {
                ExperiencePointsCard(experience = state.experience)
            }
        }
    }
}

@Composable
fun AnalyticsContent(
    state: AnalyticsState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = AppleSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(AppleSpacing.medium),
        contentPadding = PaddingValues(vertical = AppleSpacing.medium)
    ) {
        // Overall performance summary
        item {
            PerformanceSummaryCard(
                overallStats = state.overallPerformance
            )
        }
        
        // Economy overview section
        item {
            EconomyOverviewSection(
                balance = state.balance,
                recentTransactions = state.recentTransactions
            )
        }
        
        // Subject mastery levels
        item {
            SubjectMasterySection(
                masteryLevels = state.subjectMastery
            )
        }
        
        // Performance trends chart
        item {
            PerformanceTrendsChart(
                trends = state.performanceTrends
            )
        }
        
        // Badge progression
        item {
            BadgeProgressionSection(
                earnedBadges = state.earnedBadges,
                availableBadges = state.availableBadges
            )
        }
        
        // XP progression
        item {
            XpProgressionCard(
                experience = state.experience
            )
        }
        
        // Learning recommendations
        item {
            LearningRecommendationsSection(
                recommendations = state.recommendations
            )
        }
    }
}

@Composable
fun PerformanceSummaryCard(
    overallStats: PerformanceStatsDto?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = WisdomBlue.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = WisdomBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Overall Performance",
                    style = MaterialTheme.typography.titleLarge,
                    color = WisdomBlue,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (overallStats != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PerformanceMetric(
                        label = "Success Rate",
                        value = "${(overallStats.successRate * 100).toInt()}%",
                        color = when {
                            overallStats.successRate >= 0.8f -> Color(0xFF4CAF50) // Green
                            overallStats.successRate >= 0.6f -> Color(0xFFFF9800) // Orange
                            else -> Color(0xFFF44336) // Red
                        }
                    )
                    PerformanceMetric(
                        label = "Tasks Done",
                        value = overallStats.tasksCompleted.toString(),
                        color = Color(0xFF9C27B0) // Purple
                    )
                    PerformanceMetric(
                        label = "Current Streak",
                        value = overallStats.currentStreak.toString(),
                        color = Color(0xFF4CAF50) // Green
                    )
                    PerformanceMetric(
                        label = "Avg Difficulty",
                        value = String.format("%.1f", overallStats.averageDifficulty),
                        color = Color(0xFF2196F3) // Blue
                    )
                }
            } else {
                EmptyState(
                    message = "No performance data available yet. Start a lesson to see progress!",
                    icon = Icons.Default.BarChart
                )
            }
        }
    }
}

@Composable
fun PerformanceMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SubjectMasterySection(
    masteryLevels: List<SubjectMasteryDto>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = RoyalPurple,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Subject Mastery Levels",
                    style = MaterialTheme.typography.titleLarge,
                    color = RoyalPurple,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (masteryLevels.isNotEmpty()) {
                masteryLevels.forEach { mastery ->
                    SubjectMasteryItem(mastery = mastery)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                EmptyState(
                    message = "Subject mastery will be shown here after a few lessons.",
                    icon = Icons.Default.School
                )
            }
        }
    }
}

@Composable
fun SubjectMasteryItem(
    mastery: SubjectMasteryDto,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mastery.subject.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Grade ${mastery.gradeLevel} - ${(mastery.masteryScore * 100).toInt()}%",
                style = MaterialTheme.typography.bodyLarge,
                color = when {
                    mastery.masteryScore >= 0.8f -> Color(0xFF4CAF50) // Green
                    mastery.masteryScore >= 0.6f -> Color(0xFFFF9800) // Orange  
                    else -> Color(0xFFF44336) // Red
                },
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = { mastery.masteryScore },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = when {
                mastery.masteryScore >= 0.8f -> Color(0xFF4CAF50) // Green
                mastery.masteryScore >= 0.6f -> Color(0xFFFF9800) // Orange
                else -> Color(0xFFF44336) // Red
            },
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun PerformanceTrendsChart(
    trends: List<PerformanceTrendDto>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50), // Green
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Performance Trends",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF4CAF50), // Green
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (trends.isNotEmpty()) {
                // Simple line chart visualization
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    drawPerformanceChart(trends)
                }
            } else {
                EmptyState(
                    message = "Performance trends will appear here after a week of use.",
                    icon = Icons.Default.ShowChart,
                    modifier = Modifier.height(200.dp)
                )
            }
        }
    }
}

@Composable
fun BadgeProgressionSection(
    earnedBadges: List<BadgeDto>,
    availableBadges: List<BadgeDefinitionDto>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = AmberGlow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Achievement Badges",
                    style = MaterialTheme.typography.titleLarge,
                    color = AmberGlow,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (earnedBadges.isNotEmpty() || availableBadges.isNotEmpty()) {
                Text(
                    text = "Earned: ${earnedBadges.size} of ${availableBadges.size} available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(earnedBadges.take(5)) { badge ->
                        BadgeItem(badge = badge, isEarned = true)
                    }
                    items(availableBadges.filter { avail -> earnedBadges.none { it.id == avail.id } }.take(5)) { badgeDef ->
                        BadgeItem(badge = null, badgeDef = badgeDef, isEarned = false)
                    }
                }
            } else {
                EmptyState(
                    message = "Badges for achievements will be displayed here.",
                    icon = Icons.Default.EmojiEvents
                )
            }
        }
    }
}

@Composable
fun BadgeItem(
    badge: BadgeDto? = null,
    badgeDef: BadgeDefinitionDto? = null,
    isEarned: Boolean,
    modifier: Modifier = Modifier
) {
    val name = badge?.name ?: badgeDef?.name ?: ""
    val imageUrl = badge?.imageUrl ?: badgeDef?.imageUrl ?: ""
    val rarity = badge?.rarity ?: badgeDef?.rarity ?: BadgeRarity.COMMON

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isEarned) AmberGlow.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🏆", // Default badge emoji
                fontSize = 24.sp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = if (isEarned) MaterialTheme.colorScheme.onSurface
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
fun XpProgressionCard(
    experience: ExperienceDto?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = RoyalPurple,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Experience Progress",
                    style = MaterialTheme.typography.titleLarge,
                    color = RoyalPurple,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (experience != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Level ${experience.level}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = RoyalPurple,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${experience.currentXp} / ${experience.nextLevelXp} XP",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val progress = if (experience.nextLevelXp > 0) {
                    experience.currentXp.toFloat() / experience.nextLevelXp.toFloat()
                } else 1f
                
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = RoyalPurple,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            } else {
                EmptyState(
                    message = "XP and level progression will appear here after the first task.",
                    icon = Icons.Default.MilitaryTech
                )
            }
        }
    }
}

@Composable
fun EconomyOverviewSection(
    balance: BalanceDto?,
    recentTransactions: List<TransactionDto>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9800).copy(alpha = 0.1f) // Orange tint
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color(0xFFFF9800), // Orange
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Screen Time Economy",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFFF9800), // Orange
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (balance != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${balance.balance} MC",
                            style = MaterialTheme.typography.headlineMedium,
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Current Balance",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${balance.todayEarned} MC",
                            style = MaterialTheme.typography.headlineMedium,
                            color = WisdomBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Earned Today",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            } else {
                EmptyState(
                    message = "Merlin Coin balance and transactions will be shown here.",
                    icon = Icons.Default.AccountBalanceWallet
                )
            }

            if (recentTransactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                recentTransactions.forEach { transaction ->
                    TransactionItem(transaction = transaction)
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: TransactionDto,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = transaction.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = transaction.category.replace("_", " ").replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        Text(
            text = if (transaction.amount > 0) "+${transaction.amount} MC" else "${transaction.amount} MC",
            style = MaterialTheme.typography.bodyLarge,
            color = if (transaction.amount > 0) Color(0xFF4CAF50) else Color(0xFF9C27B0), // Green or Purple
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LearningRecommendationsSection(
    recommendations: List<LearningRecommendationDto>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50), // Green
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Learning Recommendations",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF4CAF50), // Green
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (recommendations.isNotEmpty()) {
                recommendations.forEach { recommendation ->
                    RecommendationItem(recommendation = recommendation)
                    if (recommendation != recommendations.last()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            } else {
                EmptyState(
                    message = "Personalized learning recommendations will appear here.",
                    icon = Icons.Default.Lightbulb
                )
            }
        }
    }
}

@Composable
fun RecommendationItem(
    recommendation: LearningRecommendationDto,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f) // Green tint
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = recommendation.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF4CAF50), // Green
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = recommendation.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            if (recommendation.actionItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                recommendation.actionItems.forEach { action ->
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4CAF50), // Green
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = action,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun ErrorDisplay(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = Color(0xFFF44336), // Red
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Oops! Something went wrong",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFFF44336), // Red
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3) // Blue
            )
        ) {
            Text(
                text = "Try Again",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun DrawScope.drawPerformanceChart(trends: List<PerformanceTrendDto>) {
    if (trends.isEmpty()) return
    
    val width = size.width
    val height = size.height
    val padding = 40f
    
    val maxSuccessRate = 1.0f
    val minSuccessRate = 0.0f
    
    val stepX = (width - 2 * padding) / (trends.size - 1).coerceAtLeast(1)
    
    val path = Path()
    
    trends.forEachIndexed { index, trend ->
        val x = padding + index * stepX
        val y = height - padding - ((trend.weeklySuccessRate - minSuccessRate) / (maxSuccessRate - minSuccessRate)) * (height - 2 * padding)
        
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
        
        // Draw data points
        drawCircle(
            color = androidx.compose.ui.graphics.Color(0xFF2563EB), // WisdomBlue equivalent
            radius = 6f,
            center = Offset(x, y)
        )
    }
    
    // Draw the line
    drawPath(
        path = path,
        color = androidx.compose.ui.graphics.Color(0xFF2563EB), // WisdomBlue equivalent
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
    )
}

@Composable
fun SubjectMasteryCard(masteryLevels: List<SubjectMasteryDto>) {
    AppleCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1,
        cornerRadius = 16
    ) {
        Text(
            text = "Subject Mastery",
            style = AppleNavigationTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(AppleSpacing.medium))
        
        if (masteryLevels.isEmpty()) {
            EmptyState(
                message = "No mastery data available yet.",
                icon = Icons.Default.School
            )
        } else {
            masteryLevels.forEach { mastery ->
                SubjectMasteryItem(mastery = mastery)
                if (mastery != masteryLevels.last()) {
                    Spacer(modifier = Modifier.height(AppleSpacing.small))
                }
            }
        }
    }
}

@Composable
fun ExperiencePointsCard(experience: ExperienceDto?) {
    AppleCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1,
        cornerRadius = 16
    ) {
        Text(
            text = "Experience Points",
            style = AppleNavigationTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(AppleSpacing.medium))
        
        if (experience == null) {
            EmptyState(
                message = "No experience data available.",
                icon = Icons.Default.TrendingUp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Level ${experience.level}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(AppleSpacing.medium))
                LinearProgressIndicator(
                    progress = experience.progressToNextLevel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(AppleSpacing.small))
            Text(
                text = "${experience.totalXpEarned} / ${experience.nextLevelXp} XP",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun BadgeCard(badges: List<BadgeDto>) {
    AppleCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1,
        cornerRadius = 16
    ) {
        Text(
            text = "Earned Badges",
            style = AppleNavigationTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(AppleSpacing.medium))
        
        if (badges.isEmpty()) {
            EmptyState(
                message = "No badges earned yet. Keep learning!",
                icon = Icons.Default.EmojiEvents
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(AppleSpacing.small),
                contentPadding = PaddingValues(AppleSpacing.xs)
            ) {
                items(badges.size) { index ->
                    val badge = badges[index]
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = AppleYellow.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(AppleCornerRadius.medium)
                            )
                            .padding(AppleSpacing.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = badge.name,
                            tint = AppleYellow,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
} 