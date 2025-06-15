package com.example.merlin.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.merlin.ui.accessibility.AccessibilityConstants
import com.example.merlin.ui.theme.*
import com.example.merlin.ui.parent.AnalyticsScreen
import com.example.merlin.ui.parent.ScreenTimeScreen
import com.example.merlin.curriculum.ui.CurriculumScreen
import com.example.merlin.curriculum.ui.CurriculumNavigationWrapper
import com.example.merlin.ui.settings.ChildProfileScreen

/**
 * Settings navigation destinations
 */
sealed class SettingsScreen(val route: String, val label: String, val icon: ImageVector) {
    object Profile : SettingsScreen("profile", "Profile", Icons.Default.Face)
    object Curriculum : SettingsScreen("curriculum", "Curriculum", Icons.Default.School)
    object Analytics : SettingsScreen("analytics", "Analytics", Icons.Default.Analytics)
    object ScreenTime : SettingsScreen("screentime", "Screen Time", Icons.Default.Schedule)
    object Security : SettingsScreen("security", "Security", Icons.Default.Security)
    object Exit : SettingsScreen("exit", "Exit", Icons.AutoMirrored.Filled.ExitToApp)
}

val settingsScreens = listOf(
    SettingsScreen.Profile,
    SettingsScreen.Curriculum,
    SettingsScreen.Analytics,
    SettingsScreen.ScreenTime,
    SettingsScreen.Security,
    SettingsScreen.Exit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onExitApp: () -> Unit,
    onNavigateToParentDashboard: () -> Unit = {}, // Deprecated - no longer used
    onNavigateToChildProfile: () -> Unit = {} // Deprecated - no longer used
) {
    val navController = rememberNavController()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings",
                        style = AppleLargeTitle,
                        color = ApplePrimaryLabel
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = AccessibilityConstants.ContentDescriptions.BACK_BUTTON,
                            tint = AppleBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppleSystemBackground
                )
            )
        },
        bottomBar = { 
            SettingsBottomNavBar(
                navController = navController,
                onExitApp = onExitApp
            )
        },
        containerColor = AppleSystemBackground
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SettingsScreen.Profile.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(SettingsScreen.Profile.route) {
                ChildProfileScreen(
                    onNavigateBack = { navController.navigateUp() },
                    showTopBar = false
                )
            }
            composable(SettingsScreen.Curriculum.route) {
                CurriculumSettingsScreen()
            }
            composable(SettingsScreen.Analytics.route) {
                AnalyticsScreen()
            }
            composable(SettingsScreen.ScreenTime.route) {
                ScreenTimeScreen()
            }
            composable(SettingsScreen.Security.route) {
                SecurityScreen()
            }
            composable(SettingsScreen.Exit.route) {
                ExitConfirmationScreen(
                    onConfirmExit = onExitApp,
                    onCancel = { navController.navigate(SettingsScreen.Profile.route) }
                )
            }
        }
    }
}

@Composable
fun SettingsBottomNavBar(
    navController: NavController,
    onExitApp: () -> Unit
) {
    NavigationBar(
        containerColor = AppleSystemBackground,
        contentColor = AppleBlue
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        
        settingsScreens.forEach { screen ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        screen.icon, 
                        contentDescription = screen.label,
                        tint = if (currentRoute == screen.route) AppleBlue else AppleSecondaryLabel
                    )
                },
                label = { 
                    Text(
                        screen.label,
                        color = if (currentRoute == screen.route) AppleBlue else AppleSecondaryLabel
                    )
                },
                selected = currentRoute == screen.route,
                onClick = {
                    if (screen.route == SettingsScreen.Exit.route) {
                        // Navigate to exit confirmation
                        navController.navigate(screen.route)
                    } else {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppleBlue,
                    selectedTextColor = AppleBlue,
                    unselectedIconColor = AppleSecondaryLabel,
                    unselectedTextColor = AppleSecondaryLabel,
                    indicatorColor = AppleBlue.copy(alpha = 0.1f)
                )
            )
        }
    }
}

@Composable
fun SecurityScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleSystemBackground)
            .padding(AppleSpacing.large),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = AppleSecondaryLabel,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(AppleSpacing.large))
            Text(
                text = "PIN & Security",
                style = AppleHeadline,
                color = ApplePrimaryLabel
            )
            Spacer(modifier = Modifier.height(AppleSpacing.small))
            Text(
                text = "Change PIN and manage security settings",
                style = AppleBody,
                color = AppleSecondaryLabel,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ExitConfirmationScreen(
    onConfirmExit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleSystemBackground)
            .padding(AppleSpacing.large),
        contentAlignment = Alignment.Center
    ) {
        AppleCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(AppleSpacing.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🚪",
                    style = AppleLargeTitle.copy(fontSize = 64.sp)
                )
                Spacer(modifier = Modifier.height(AppleSpacing.medium))
                Text(
                    text = "Exit Merlin?",
                    style = AppleHeadline,
                    color = ApplePrimaryLabel
                )
                Spacer(modifier = Modifier.height(AppleSpacing.small))
                Text(
                    text = "Are you sure you want to close Merlin and return to your device?",
                    style = AppleBody,
                    color = AppleSecondaryLabel,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(AppleSpacing.large))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppleSpacing.medium)
                ) {
                    AppleButton(
                        text = "Cancel",
                        onClick = onCancel,
                        style = AppleButtonStyle.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                    AppleButton(
                        text = "Exit",
                        onClick = onConfirmExit,
                        style = AppleButtonStyle.Primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun CurriculumSettingsScreen(
    modifier: Modifier = Modifier
) {
    // Use the navigation wrapper that includes the generator
    CurriculumNavigationWrapper(
        onBackPressed = { /* No-op since we're in settings tabs */ },
        showBackButton = false, // Hide back button in settings context
        modifier = modifier
    )
} 