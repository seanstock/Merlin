package com.example.merlin.curriculum.ui

import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.merlin.config.ServiceLocator

@Composable
fun CurriculumScreen(
    navController: NavController,
    onBackPressed: () -> Unit,
    onNavigateToGenerator: () -> Unit = {},
    onCurriculumSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    viewModelOverride: CurriculumViewModel? = null
) {
    val context = LocalContext.current
    
    // Get CurriculumManager from ServiceLocator (LaaS-compliant)
    val curriculumManager = remember { ServiceLocator.getCurriculumManager(context) }
    
    // Create ViewModel with factory
    val viewModel: CurriculumViewModel = viewModelOverride ?: viewModel(
        factory = CurriculumViewModelFactory(curriculumManager)
    )
    
    // Listen for the result from the generator screen
    val navResult = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Boolean>("curriculum_generated")
        ?.observeAsState()

    // When the result is observed, refresh the data and remove the signal
    LaunchedEffect(navResult) {
        if (navResult?.value == true) {
            viewModel.refreshCurricula()
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.remove<Boolean>("curriculum_generated")
        }
    }
    
    // Collect state
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle error state
    uiState.error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            println("Curriculum error: $errorMessage")
            // In a real app, you might show a snackbar or error dialog
        }
    }
    
    CurriculumDashboard(
        uiState = uiState,
        onCurriculumSelected = onCurriculumSelected,
        onDeleteClicked = { curriculumId ->
            viewModel.deleteCurriculum(curriculumId)
        },
        onBackPressed = onBackPressed,
        onGenerateNewCurriculum = onNavigateToGenerator,
        showBackButton = showBackButton,
        modifier = modifier
    )
} 