package com.example.merlin.curriculum.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlin.curriculum.model.CurriculumDto
import com.example.merlin.curriculum.model.LessonDto
import com.example.merlin.ui.theme.AppleSpacing
import androidx.compose.ui.platform.LocalContext
import com.example.merlin.config.ServiceLocator
import com.example.merlin.utils.UserSessionRepository
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonListScreen(
    curriculumId: String,
    onBackPressed: () -> Unit,
    onLessonSelected: (LessonDto) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val curriculumManager = remember { ServiceLocator.getCurriculumManager(context) }
    val factory = remember { LessonListViewModelFactory(curriculumId, curriculumManager) }
    val viewModel: LessonListViewModel = viewModel(factory = factory)

    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
        }
        uiState.curriculum != null -> {
            LessonListContent(
                curriculum = uiState.curriculum!!,
                onBackPressed = onBackPressed,
                onLessonSelected = onLessonSelected,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonListContent(
    curriculum: CurriculumDto,
    onBackPressed: () -> Unit,
    onLessonSelected: (LessonDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val curriculumService = remember { ServiceLocator.getCurriculumService(context) }
    val childId = remember { UserSessionRepository.getInstance(context).getActiveChildId() }

    // Map lessonId -> status
    var progressMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    // State for expanded lesson card and coroutine scope for suspend calls
    val coroutineScope = rememberCoroutineScope()
    var expandedId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(childId) {
        if (childId != null) {
            val statuses = mutableMapOf<String, String>()
            curriculum.lessons.forEach { lesson ->
                curriculumService.getLessonProgress(lesson.id, childId).onSuccess { lp ->
                    val status = lp?.status ?: "not_started"
                    statuses[lesson.id] = status
                }
            }
            progressMap = statuses
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = curriculum.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Grade ${curriculum.gradeLevel} • ${curriculum.lessons.size} lessons",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(AppleSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppleSpacing.medium)
        ) {
            item {
                // Curriculum description card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(AppleSpacing.medium)
                    ) {
                        Text(
                            text = curriculum.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            items(curriculum.lessons.sortedBy { it.order }) { lesson ->
                LessonCard(
                    lesson = lesson,
                    status = progressMap[lesson.id] ?: "not_started",
                    expanded = expandedId == lesson.id,
                    onToggle = {
                        expandedId = if (expandedId == lesson.id) null else lesson.id
                    },
                    onSaveProgress = { status, percent, grade ->
                        if (childId != null) {
                            coroutineScope.launch {
                                curriculumService.recordLessonProgress(
                                    lessonId = lesson.id,
                                    childId = childId,
                                    progress = com.example.merlin.curriculum.model.LessonProgressDto(
                                        lessonId = lesson.id,
                                        childId = childId,
                                        status = status,
                                        percentComplete = percent,
                                        grade = grade,
                                        tutorNotes = ""
                                    )
                                )
                            }
                            progressMap = progressMap.toMutableMap().apply { put(lesson.id, status) }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LessonCard(
    lesson: LessonDto,
    status: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSaveProgress: (String, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppleSpacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lesson status indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = AppleSpacing.medium),
                contentAlignment = Alignment.Center
            ) {
                when (status) {
                    "completed" -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                    "in_progress" -> Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFFFA000))
                    else -> Text(lesson.order.toString())
                }
            }
            
            // Lesson info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = lesson.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = lesson.description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 2
                )
                if (lesson.activities.isNotEmpty()) {
                    Text(
                        text = "${lesson.activities.size} activities • ${lesson.activities.sumOf { it.estimatedMinutes }} min",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Action button
            IconButton(onClick = onToggle) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Start Lesson",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (expanded) {
            Divider()
            var selStatus by remember { mutableStateOf(status) }
            var percent by remember { mutableStateOf(0) }
            var grade by remember { mutableStateOf(0) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppleSpacing.medium),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Description:",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = lesson.description,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(AppleSpacing.medium))

                // Status dropdown with proper z-order
                Text(
                    text = "Status:",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    var expandedDD by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { expandedDD = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(selStatus.replace('_', ' ').replaceFirstChar { it.uppercase() })
                        }
                    }
                    DropdownMenu(
                        expanded = expandedDD,
                        onDismissRequest = { expandedDD = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("not_started", "in_progress", "completed").forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.replace('_', ' ').replaceFirstChar { it.uppercase() }) },
                                onClick = { selStatus = s; expandedDD = false },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(AppleSpacing.medium))
                
                Text(
                    text = "Completion: $percent%",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Slider(
                    value = percent.toFloat(),
                    onValueChange = { percent = it.toInt() },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(AppleSpacing.medium))
                
                Text(
                    text = "Score: $grade",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Slider(
                    value = grade.toFloat(),
                    onValueChange = { grade = it.toInt() },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(AppleSpacing.medium))
                
                Button(
                    onClick = { onSaveProgress(selStatus, percent, grade); onToggle() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Progress")
                }
            }
        }
    }
} 