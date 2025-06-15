package com.example.merlin.curriculum.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.merlin.config.ServiceLocator
import com.example.merlin.curriculum.model.CurriculumDto
import kotlinx.coroutines.launch

@Composable
fun CurriculumNavigationWrapper(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true
) {
    var currentScreen by remember { mutableStateOf(CurriculumScreenType.LIST) }
    var selectedCurriculum by remember { mutableStateOf<CurriculumDto?>(null) }
    
    val context = LocalContext.current
    val curriculumService = remember { ServiceLocator.getCurriculumService(context) }
    val scope = rememberCoroutineScope()
    
    // single ViewModel instance for curricula list
    val curriculaVM: CurriculumViewModel = viewModel(
        factory = CurriculumViewModelFactory(ServiceLocator.getCurriculumManager(context))
    )
    
    when (currentScreen) {
        CurriculumScreenType.LIST -> {
            CurriculumScreen(
                onBackPressed = onBackPressed,
                onNavigateToGenerator = { 
                    currentScreen = CurriculumScreenType.GENERATOR 
                },
                onCurriculumSelected = { curriculumId ->
                    // Find the curriculum and navigate to lessons
                    scope.launch {
                        curriculumService.getAvailableCurricula()
                            .onSuccess { curricula ->
                                val curriculum = curricula.find { it.id == curriculumId }
                                if (curriculum != null) {
                                    selectedCurriculum = curriculum
                                    currentScreen = CurriculumScreenType.LESSON_LIST
                                }
                            }
                    }
                },
                showBackButton = showBackButton,
                modifier = modifier,
                viewModelOverride = curriculaVM
            )
        }
        
        CurriculumScreenType.LESSON_LIST -> {
            selectedCurriculum?.let { curriculum ->
                LessonListScreen(
                    curriculum = curriculum,
                    onBackPressed = { 
                        currentScreen = CurriculumScreenType.LIST 
                    },
                    onLessonSelected = { lesson ->
                        // TODO: Navigate to lesson detail/activity screen
                        println("Selected lesson: ${lesson.title}")
                    },
                    modifier = modifier
                )
            }
        }
        
        CurriculumScreenType.GENERATOR -> {
            val syllabusGeneratorService = remember { 
                ServiceLocator.getSyllabusGeneratorService(context) 
            }
            
            val generatorViewModel: CurriculumGeneratorViewModel = viewModel(
                factory = CurriculumGeneratorViewModelFactory(
                    syllabusGeneratorService = syllabusGeneratorService,
                    curriculumService = curriculumService
                )
            )
            
            CurriculumGeneratorScreen(
                viewModel = generatorViewModel,
                onNavigateBack = { currentScreen = CurriculumScreenType.LIST },
                onCurriculumGenerated = { _ ->
                    curriculaVM.loadCurricula()
                    currentScreen = CurriculumScreenType.LIST
                }
            )
        }
    }
}

enum class CurriculumScreenType {
    LIST,
    LESSON_LIST,
    GENERATOR
} 