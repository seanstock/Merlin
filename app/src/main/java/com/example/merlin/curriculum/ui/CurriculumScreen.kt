package com.example.merlin.curriculum.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.merlin.config.ServiceLocator

@Composable
fun CurriculumScreen(
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
    
    // Collect state
    val curricula by viewModel.curricula.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // Handle error state
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            println("Curriculum error: $errorMessage")
            // In a real app, you might show a snackbar or error dialog
        }
    }
    
    CurriculumDashboard(
        curricula = curricula,
        isLoading = isLoading,
        onCurriculumSelected = onCurriculumSelected,
        onBackPressed = onBackPressed,
        onGenerateNewCurriculum = onNavigateToGenerator,
        showBackButton = showBackButton,
        modifier = modifier
    )
} 