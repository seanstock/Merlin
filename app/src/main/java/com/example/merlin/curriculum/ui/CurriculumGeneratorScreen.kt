package com.example.merlin.curriculum.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.merlin.curriculum.model.GenerationStage
import androidx.compose.ui.platform.LocalContext
import com.example.merlin.config.ServiceLocator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurriculumGeneratorScreen(
    onNavigateBack: () -> Unit = {},
    onCurriculumGenerated: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val syllabusGeneratorService = remember { ServiceLocator.getSyllabusGeneratorService(context) }
    val curriculumService = remember { ServiceLocator.getCurriculumService(context) }
    val factory = remember {
        CurriculumGeneratorViewModelFactory(syllabusGeneratorService, curriculumService)
    }
    val viewModel: CurriculumGeneratorViewModel = viewModel(factory = factory)

    var syllabusText by remember { mutableStateOf("") }
    var curriculumTitle by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var targetWeeks by remember { mutableStateOf("16") }
    var sessionsPerWeek by remember { mutableStateOf("3") }
    var sessionDuration by remember { mutableStateOf("35") }
    var showTemplateDialog by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val availableTemplates by viewModel.availableTemplates.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAvailableTemplates()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Curriculum Generator",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header description
            Text(
                text = "Generate AI-powered curricula from syllabi or templates",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Generation Options
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Generation Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showTemplateDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("From Template")
                        }
                        
                        OutlinedButton(
                            onClick = { /* Custom syllabus mode - already default */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("From Syllabus")
                        }
                    }
                }
            }

            // Basic Information
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Basic Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = curriculumTitle,
                        onValueChange = { curriculumTitle = it },
                        label = { Text("Curriculum Title") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., Advanced Biology") }
                    )

                    // Category Selection
                    ExposedDropdownMenuBox(
                        expanded = false,
                        onExpandedChange = { }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = { selectedCategory = it },
                            label = { Text("Category") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g., high_school_science") }
                        )
                    }
                }
            }

            // Syllabus Input
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Syllabus Content",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = syllabusText,
                        onValueChange = { syllabusText = it },
                        label = { Text("Paste your syllabus here") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        placeholder = { Text("Enter the complete syllabus text, including learning objectives, topics, and any specific requirements...") },
                        maxLines = 10
                    )

                    // Validation feedback
                    if (syllabusText.isNotEmpty()) {
                        val validationResult = viewModel.validateSyllabusText(syllabusText)
                        if (validationResult.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Validation Issues:",
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    validationResult.forEach { issue ->
                                        Text(
                                            "• $issue",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Syllabus looks good!",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Generation Parameters
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Generation Parameters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = targetWeeks,
                            onValueChange = { targetWeeks = it },
                            label = { Text("Weeks") },
                            modifier = Modifier.weight(1f)
                        )
                        
                        OutlinedTextField(
                            value = sessionsPerWeek,
                            onValueChange = { sessionsPerWeek = it },
                            label = { Text("Sessions/Week") },
                            modifier = Modifier.weight(1f)
                        )
                        
                        OutlinedTextField(
                            value = sessionDuration,
                            onValueChange = { sessionDuration = it },
                            label = { Text("Minutes") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Generation Progress
            if (uiState.isGenerating) {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Generating Curriculum...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        LinearProgressIndicator(
                            progress = uiState.generationProgress.progress / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = uiState.generationProgress.message,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "Stage: ${uiState.generationProgress.stage.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Generate Button
            Button(
                onClick = {
                    viewModel.generateCurriculum(
                        syllabusText = syllabusText,
                        title = curriculumTitle,
                        category = selectedCategory,
                        targetWeeks = targetWeeks.toIntOrNull() ?: 16,
                        sessionsPerWeek = sessionsPerWeek.toIntOrNull() ?: 3,
                        sessionDuration = sessionDuration.toIntOrNull() ?: 35
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isGenerating && 
                         curriculumTitle.isNotBlank() && 
                         selectedCategory.isNotBlank() && 
                         syllabusText.isNotBlank() &&
                         viewModel.validateSyllabusText(syllabusText).isEmpty()
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Generate Curriculum")
            }

            // Success/Error Messages
            uiState.generatedCurriculumId?.let { curriculumId ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "✅ Curriculum Generated Successfully!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Your new curriculum has been created and is ready to use.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onCurriculumGenerated(curriculumId) }
                        ) {
                            Text("View Curriculum")
                        }
                    }
                }
            }

            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "❌ Generation Failed",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    // Template Selection Dialog
    if (showTemplateDialog) {
        TemplateSelectionDialog(
            templates = availableTemplates,
            onTemplateSelected = { templateName, category ->
                curriculumTitle = templateName
                selectedCategory = category
                syllabusText = generateDetailedTemplateText(templateName, category)
                showTemplateDialog = false
            },
            onDismiss = { showTemplateDialog = false }
        )
    }
}

@Composable
fun TemplateSelectionDialog(
    templates: Map<String, List<String>>,
    onTemplateSelected: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Template") },
        text = {
            LazyColumn {
                templates.forEach { (category, templateList) ->
                    item {
                        Text(
                            text = category.replace('_', ' ').replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(templateList) { template ->
                        TextButton(
                            onClick = { onTemplateSelected(template, category) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = template,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun generateDetailedTemplateText(templateName: String, category: String): String {
    return when (templateName) {
        "Addition and Subtraction (Grades K-2)" -> """
            Elementary Mathematics: Addition and Subtraction Fundamentals
            
            Course Description:
            This comprehensive mathematics curriculum introduces young learners to the fundamental concepts of addition and subtraction through hands-on activities, visual representations, and real-world applications.
            
            Learning Objectives:
            Students will be able to:
            • Understand the concept of addition as combining groups of objects
            • Master addition facts within 20 with fluency and accuracy
            • Understand subtraction as taking away or finding the difference
            • Solve subtraction problems within 20 using various strategies
            • Apply addition and subtraction to solve real-world word problems
            • Use manipulatives and visual models to represent mathematical thinking
            • Develop number sense and mathematical reasoning skills
            • Recognize patterns in addition and subtraction relationships
            
            Topics and Learning Progression:
            
            Unit 1: Introduction to Addition (3 weeks)
            - Counting and combining objects
            - Addition as "putting together"
            - Using manipulatives (blocks, counters, toys)
            - Number line introduction
            - Addition facts 0-5
            
            Unit 2: Addition Strategies (4 weeks)
            - Counting on strategy
            - Doubles facts (1+1, 2+2, etc.)
            - Near doubles (2+3, 4+5)
            - Making 10 strategy
            - Addition facts 6-10
            
            Unit 3: Introduction to Subtraction (3 weeks)
            - Subtraction as "taking away"
            - Using manipulatives for subtraction
            - Subtraction on number line
            - Subtraction facts 0-5
            - Connection between addition and subtraction
            
            Unit 4: Subtraction Strategies (4 weeks)
            - Counting back strategy
            - Think addition to subtract
            - Subtraction facts 6-10
            - Fact families
            - Missing addend problems
            
            Unit 5: Word Problems and Applications (2 weeks)
            - Addition word problems
            - Subtraction word problems
            - Mixed operation problems
            - Real-world applications
            - Mathematical communication
            
            Assessment Methods:
            - Daily practice activities
            - Weekly fact fluency checks
            - Unit assessments with manipulatives
            - Portfolio of student work
            - Observation of problem-solving strategies
            
            Materials and Resources:
            - Counting manipulatives (blocks, bears, etc.)
            - Number lines and hundred charts
            - Interactive math games
            - Story books with math themes
            - Worksheets and practice pages
        """.trimIndent()
        
        "Multiplication and Division (Grades 2-4)" -> """
            Elementary Mathematics: Multiplication and Division Mastery
            
            Course Description:
            This curriculum builds upon addition and subtraction skills to introduce multiplication and division concepts through visual models, repeated addition, and real-world problem solving.
            
            Learning Objectives:
            Students will be able to:
            • Understand multiplication as repeated addition and equal groups
            • Master multiplication facts through 12x12 with fluency
            • Understand division as sharing equally and repeated subtraction
            • Solve division problems with and without remainders
            • Apply multiplication and division to solve multi-step word problems
            • Use arrays, area models, and other visual representations
            • Develop computational fluency and mathematical reasoning
            • Recognize the relationship between multiplication and division
            
            Topics and Learning Progression:
            
            Unit 1: Introduction to Multiplication (4 weeks)
            - Multiplication as repeated addition
            - Equal groups and arrays
            - Skip counting patterns
            - Multiplication facts 0-5
            - Commutative property
            
            Unit 2: Multiplication Strategies (5 weeks)
            - Doubling and halving
            - Break apart strategy
            - Area models
            - Multiplication facts 6-12
            - Two-digit multiplication introduction
            
            Unit 3: Introduction to Division (4 weeks)
            - Division as sharing equally
            - Division as repeated subtraction
            - Relationship to multiplication
            - Division facts 0-5
            - Remainders introduction
            
            Unit 4: Division Strategies (3 weeks)
            - Think multiplication to divide
            - Division with remainders
            - Division facts 6-12
            - Long division introduction
            - Checking division with multiplication
            
            Assessment and Practice:
            - Fact fluency assessments
            - Problem-solving portfolios
            - Unit tests with multiple representations
            - Real-world application projects
        """.trimIndent()
        
        "Introduction to Biology" -> """
            Middle School Biology: Foundations of Life Science
            
            Course Description:
            This comprehensive biology course introduces students to the fundamental concepts of life science, including cell structure, genetics, ecology, and human body systems through hands-on investigations and scientific inquiry.
            
            Learning Objectives:
            Students will be able to:
            • Understand the characteristics that define living organisms
            • Explain the structure and function of plant and animal cells
            • Describe the process of photosynthesis and cellular respiration
            • Analyze patterns of inheritance and genetic variation
            • Investigate ecosystems and environmental interactions
            • Examine human body systems and their functions
            • Apply scientific methods to biological investigations
            • Communicate scientific findings using appropriate vocabulary
            
            Topics and Learning Progression:
            
            Unit 1: What is Life? (2 weeks)
            - Characteristics of living things
            - Levels of organization in biology
            - Scientific method in biology
            - Laboratory safety and procedures
            - Introduction to microscopy
            
            Unit 2: Cell Biology (4 weeks)
            - Cell theory and history
            - Plant vs. animal cell structures
            - Cell organelles and their functions
            - Cell membrane and transport
            - Mitosis and cell division
            
            Unit 3: Genetics and Heredity (3 weeks)
            - DNA structure and function
            - Genes and chromosomes
            - Patterns of inheritance
            - Punnett squares and probability
            - Genetic disorders and mutations
            
            Unit 4: Ecology and Environment (4 weeks)
            - Ecosystems and biomes
            - Food chains and energy flow
            - Population dynamics
            - Human impact on environment
            - Conservation and sustainability
            
            Unit 5: Human Body Systems (3 weeks)
            - Circulatory and respiratory systems
            - Digestive and excretory systems
            - Nervous and endocrine systems
            - Immune system and disease
            - Health and wellness
            
            Laboratory Activities:
            - Microscope investigations
            - Cell observation and drawing
            - Genetics probability experiments
            - Ecosystem modeling
            - Human body system demonstrations
            
            Assessment Methods:
            - Laboratory reports and data analysis
            - Unit tests with multiple choice and short answer
            - Research projects on current biological topics
            - Scientific poster presentations
            - Practical laboratory skills assessments
        """.trimIndent()
        
        "Reading Comprehension (Elementary)" -> """
            Elementary Language Arts: Reading Comprehension Mastery
            
            Course Description:
            This comprehensive reading program develops students' ability to understand, analyze, and respond to various types of texts through systematic instruction in comprehension strategies and critical thinking skills.
            
            Learning Objectives:
            Students will be able to:
            • Apply reading strategies before, during, and after reading
            • Identify main ideas and supporting details in texts
            • Make inferences and draw conclusions from textual evidence
            • Compare and contrast characters, settings, and events
            • Analyze author's purpose and point of view
            • Expand vocabulary through context clues and word analysis
            • Respond to literature through discussion and writing
            • Read fluently with appropriate expression and pacing
            
            Topics and Learning Progression:
            
            Unit 1: Reading Strategies Foundation (3 weeks)
            - Pre-reading strategies (previewing, predicting)
            - During-reading strategies (questioning, visualizing)
            - Post-reading strategies (summarizing, reflecting)
            - Text features and organization
            - Reading for different purposes
            
            Unit 2: Fiction Comprehension (4 weeks)
            - Story elements (character, setting, plot)
            - Character analysis and motivation
            - Plot structure and sequence
            - Theme identification
            - Making connections (text-to-self, text-to-text, text-to-world)
            
            Unit 3: Nonfiction Comprehension (4 weeks)
            - Main idea and supporting details
            - Text structures (cause/effect, compare/contrast, sequence)
            - Fact vs. opinion
            - Author's purpose and bias
            - Using text features (headings, captions, diagrams)
            
            Unit 4: Vocabulary Development (3 weeks)
            - Context clues strategies
            - Word parts (prefixes, suffixes, roots)
            - Multiple meaning words
            - Figurative language
            - Academic vocabulary
            
            Unit 5: Critical Reading Skills (2 weeks)
            - Making inferences
            - Drawing conclusions
            - Evaluating information
            - Comparing multiple texts
            - Forming opinions with evidence
            
            Reading Materials:
            - Leveled fiction and nonfiction texts
            - Poetry and drama selections
            - Informational articles and biographies
            - Student choice independent reading
            - Digital texts and multimedia resources
            
            Assessment Strategies:
            - Running records and fluency assessments
            - Comprehension quizzes and discussions
            - Reading response journals
            - Book reports and projects
            - Portfolio of reading growth
        """.trimIndent()
        
        else -> """
            $templateName Curriculum
            
            Course Description:
            This comprehensive curriculum provides students with a thorough understanding of $templateName concepts and skills through engaging activities, hands-on learning, and real-world applications.
            
            Learning Objectives:
            Students will be able to:
            • Demonstrate mastery of fundamental concepts in $templateName
            • Apply knowledge to solve complex problems and real-world situations
            • Develop critical thinking and analytical skills
            • Communicate effectively using subject-specific vocabulary
            • Work collaboratively on projects and investigations
            • Make connections between concepts and other subject areas
            • Develop independent learning and study skills
            • Demonstrate creativity and innovation in problem-solving
            
            Topics and Learning Progression:
            
            Unit 1: Foundations and Basic Concepts (3 weeks)
            - Introduction to key terminology and vocabulary
            - Fundamental principles and theories
            - Historical context and development
            - Basic skills and techniques
            - Safety procedures and best practices
            
            Unit 2: Core Knowledge and Skills (5 weeks)
            - Essential concepts and processes
            - Skill development through practice
            - Application of knowledge to new situations
            - Problem-solving strategies
            - Critical analysis and evaluation
            
            Unit 3: Advanced Applications (4 weeks)
            - Complex problem-solving scenarios
            - Integration of multiple concepts
            - Real-world applications and case studies
            - Independent research and investigation
            - Creative projects and presentations
            
            Unit 4: Synthesis and Assessment (4 weeks)
            - Review and reinforcement of key concepts
            - Comprehensive projects and portfolios
            - Peer collaboration and feedback
            - Self-reflection and goal setting
            - Preparation for advanced study
            
            Assessment Methods:
            - Formative assessments and check-ins
            - Unit tests and quizzes
            - Project-based assessments
            - Portfolio development
            - Peer and self-evaluation
            - Performance-based demonstrations
            
            Resources and Materials:
            - Textbooks and reference materials
            - Digital resources and online tools
            - Hands-on manipulatives and equipment
            - Multimedia presentations and videos
            - Community resources and field trip opportunities
        """.trimIndent()
    }
} 