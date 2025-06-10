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
import com.example.merlin.ui.accessibility.AccessibilityConstants
import com.example.merlin.ui.theme.*
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Parent Dashboard Screen - Entry point for parent analytics and controls
 */
sealed class ParentScreen(val route: String, val label: String, val icon: ImageVector) {
    object Analytics : ParentScreen("analytics", "Analytics", Icons.Default.Analytics)
    object ScreenTime : ParentScreen("screentime", "Screen Time", Icons.Default.Schedule)
    object Profiles : ParentScreen("profiles", "Profiles", Icons.Default.Group)
    object Settings : ParentScreen("settings", "Settings", Icons.Default.Settings)
}

val parentScreens = listOf(
    ParentScreen.Analytics,
    ParentScreen.ScreenTime,
    ParentScreen.Profiles,
    ParentScreen.Settings,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { ParentBottomNavBar(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ParentScreen.Analytics.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ParentScreen.Analytics.route) {
                AnalyticsScreen()
            }
            composable(ParentScreen.ScreenTime.route) {
                ScreenTimeScreen()
            }
            composable(ParentScreen.Profiles.route) {
                ProfilesScreen()
            }
            composable(ParentScreen.Settings.route) {
                // Placeholder for SettingsScreen
                Text("Settings Screen (Not Implemented)")
            }
        }
    }
}

@Composable
fun ParentBottomNavBar(navController: NavController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        parentScreens.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
} 