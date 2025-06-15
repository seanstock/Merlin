package com.example.merlin.curriculum.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun CurriculumNavigationWrapper(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
    showBackButton: Boolean = true
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "curriculum_dashboard",
        modifier = modifier
    ) {
        // The main dashboard screen showing the list of curricula
        composable("curriculum_dashboard") {
            CurriculumScreen(
                navController = navController,
                onBackPressed = onBackPressed,
                onNavigateToGenerator = {
                    navController.navigate("curriculum_generator")
                },
                onCurriculumSelected = { curriculumId ->
                    // Navigate to the lesson list, passing the ID
                    navController.navigate("lesson_list/$curriculumId")
                },
                showBackButton = showBackButton
            )
        }

        // The screen for generating a new curriculum
        composable("curriculum_generator") {
            CurriculumGeneratorScreen(
                onNavigateBack = { navController.popBackStack() },
                onCurriculumGenerated = {
                    // Set the result and pop back
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("curriculum_generated", true)
                    navController.popBackStack()
                }
            )
        }

        // The screen showing the lessons for a selected curriculum
        composable(
            route = "lesson_list/{curriculumId}",
            arguments = listOf(navArgument("curriculumId") { type = NavType.StringType })
        ) { backStackEntry ->
            val curriculumId = backStackEntry.arguments?.getString("curriculumId")
            if (curriculumId != null) {
                // This now correctly uses the refactored LessonListScreen
                LessonListScreen(
                    curriculumId = curriculumId,
                    onBackPressed = { navController.popBackStack() }
                )
            }
        }
    }
}