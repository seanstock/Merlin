package com.example.merlin.curriculum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlin.curriculum.model.CurriculumDto
import com.example.merlin.ui.theme.AppleSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurriculumDashboard(
    curricula: List<CurriculumDto>,
    isLoading: Boolean,
    onCurriculumSelected: (String) -> Unit,
    onBackPressed: () -> Unit,
    onGenerateNewCurriculum: () -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    fabPosition: FabPosition = FabPosition.End
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Learning Path",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBackPressed) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onGenerateNewCurriculum,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Generate New Curriculum",
                    tint = Color.White
                )
            }
        },
        floatingActionButtonPosition = fabPosition,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (curricula.isEmpty()) {
            // Empty state with call to action
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(AppleSpacing.large),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppleSpacing.medium)
                ) {
                    Text(
                        text = "📚",
                        fontSize = 64.sp
                    )
                    Text(
                        text = "No Curricula Yet",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Create your first AI-powered curriculum by tapping the + button below",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(AppleSpacing.medium))
                    Button(
                        onClick = onGenerateNewCurriculum,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Curriculum")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(AppleSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(AppleSpacing.medium)
            ) {
                item {
                    // Header with generation info
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
                                text = "🤖 AI-Generated Curricula",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "These learning paths were created using advanced AI to provide structured, engaging education tailored to your needs.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                items(curricula) { curriculum ->
                    CurriculumCard(
                        curriculum = curriculum,
                        onClick = { onCurriculumSelected(curriculum.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CurriculumCard(
    curriculum: CurriculumDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppleSpacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subject emoji/icon
            Text(
                text = getSubjectEmoji(curriculum.subject),
                fontSize = 32.sp,
                modifier = Modifier.padding(end = AppleSpacing.medium)
            )
            
            // Curriculum info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = curriculum.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = curriculum.description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 2
                )
                Text(
                    text = "Grade ${curriculum.gradeLevel} • ${curriculum.lessons.size} lessons",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Start button
            IconButton(onClick = onClick) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Start",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun getSubjectEmoji(subject: String): String {
    return when (subject.lowercase()) {
        "math", "mathematics" -> "🔢"
        "science" -> "🔬"
        "language", "english", "reading" -> "📚"
        "social studies", "history" -> "🌍"
        "art" -> "🎨"
        "music" -> "🎵"
        else -> "📖"
    }
} 