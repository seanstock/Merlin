# Merlin Curriculum Management System - Comprehensive Implementation Plan

## Executive Summary

This document outlines the complete implementation plan for transforming the Merlin Android tutoring app into a curriculum-driven educational platform. The system adapts Task Master concepts to create a structured learning environment where:

1. **Syllabi Input**: Users (educators/parents) submit course syllabi
2. **AI Parsing**: System automatically converts syllabi into structured curricula
3. **Curriculum Following**: Merlin guides students through lessons systematically
4. **Progress Tracking**: App monitors completion and adapts guidance accordingly

## System Architecture Overview

### Core Components

1. **Curriculum Generation Engine** (Node.js)
   - AI-powered syllabus parsing
   - Bulk curriculum creation
   - Template-based generation system

2. **Android Integration Layer** (Kotlin)
   - Data models and storage
   - UI components
   - Progress tracking
   - Asset management

3. **Asset Management System**
   - JSON curriculum files
   - Kotlin data class generation
   - Resource optimization

## Current Implementation Status

### ✅ Completed Components

#### 1. Curriculum Generation System
- **Location**: `/c/Users/seang/Tutor/Curricula/`
- **Status**: Fully functional, tested with real API calls
- **Cost**: ~$1 for 5 complete curricula (highly cost-effective)

**Files Implemented:**
```
package.json                    # Node.js project configuration
generate-curriculum.js          # Main curriculum generator
convert-for-android.js         # Android asset converter
curriculum-templates.js        # 35 predefined templates
generated-curricula/           # Output directory for JSON files
android-assets/               # Android-compatible outputs
docs/                         # Implementation documentation
```

**Templates Available (35 total across 7 categories):**
- Elementary Math (6 templates)
- Middle School Science (5 templates)  
- High School Core (5 templates)
- Language Arts (5 templates)
- STEM Advanced (5 templates)
- Life Skills (5 templates)
- Arts & Humanities (4 templates)

#### 2. Android Data Models
**Generated Kotlin Classes:**
```kotlin
data class Curriculum(
    val id: String,
    val title: String,
    val description: String,
    val gradeLevel: String,
    val subject: String,
    val estimatedDuration: String,
    val lessons: List<Lesson>,
    val metadata: CurriculumMetadata
)

data class Lesson(
    val id: String,
    val title: String,
    val description: String,
    val objectives: List<String>,
    val content: String,
    val activities: List<Activity>,
    val assessments: List<Assessment>,
    val resources: List<Resource>,
    val estimatedTime: String,
    val prerequisites: List<String>
)

// Additional supporting classes: Activity, Assessment, Resource, CurriculumMetadata
```

#### 3. Testing Results
- **Generated**: 5 test curricula successfully
- **Cost**: $0.96 total for comprehensive curricula
- **Android Conversion**: Successfully created all Kotlin files
- **Quality**: High-quality, detailed lesson plans with activities and assessments

### 🔄 In Progress

#### Common Core Grade 3 Math Implementation
**Provided Standards:**
- 3.OA.A.1: Multiplication as repeated addition
- 3.OA.A.2: Division concepts
- 3.OA.A.3: Word problems with four operations
- 3.OA.A.4: Unknown factor problems
- 3.OA.B.5: Distributive property
- 3.OA.B.6: Division as unknown factor
- 3.OA.C.7: Multiplication and division fluency
- 3.OA.D.8: Two-step word problems
- 3.NBT.A.1: Rounding to nearest 10/100
- 3.NBT.A.2: Addition and subtraction fluency
- 3.NBT.A.3: Multiplication by multiples of 10
- 3.NF.A.1: Fraction concepts
- 3.NF.A.2: Fractions on number line
- 3.NF.A.3: Equivalent fractions and comparison
- 3.MD.A.1: Time intervals
- 3.MD.A.2: Volume and mass measurement
- 3.MD.B.3: Data interpretation
- 3.MD.B.4: Measurement data in line plots
- 3.MD.C.5: Area concepts
- 3.MD.C.6: Area measurement
- 3.MD.C.7: Area and perimeter
- 3.MD.D.8: Perimeter problem solving
- 3.G.A.1: Shape categories and attributes
- 3.G.A.2: Shape partitioning and fractions

### 🎯 Next Implementation Phase

## Detailed Implementation Plan

### Phase 1: Common Core Integration (Immediate - Next 2 weeks)

#### Step 1.1: Generate Common Core Grade 3 Math Curriculum
**Action Items:**
1. Create specific template for Common Core Grade 3 Math
2. Map each standard to detailed lessons
3. Generate comprehensive curriculum with all 24 standards
4. Ensure alignment with provided topics and sequence

**Files to Create/Modify:**
```javascript
// In curriculum-templates.js
const commonCoreGrade3Math = {
  id: 'common-core-grade3-math',
  title: 'Common Core Grade 3 Mathematics',
  gradeLevel: 'Grade 3',
  subject: 'Mathematics',
  standards: [/* all 24 standards */],
  estimatedDuration: '36 weeks',
  // ... detailed template
};
```

**Expected Output:**
- Complete 36-week curriculum
- 150+ individual lessons
- Standards-aligned activities
- Built-in assessments
- Progress tracking markers

#### Step 1.2: Android Integration Setup
**Files to Create in Android Project:**

```kotlin
// app/src/main/java/com/example/merlin/curriculum/
CurriculumManager.kt           # Main curriculum controller
CurriculumRepository.kt        # Data access layer
CurriculumDao.kt              # Room database interface
CurriculumDatabase.kt         # Database setup
CurriculumViewModel.kt        # UI state management

// app/src/main/java/com/example/merlin/curriculum/ui/
CurriculumDashboard.kt        # Main curriculum UI
LessonDetailScreen.kt         # Individual lesson display
ProgressTracker.kt            # Progress visualization
SyllabusInputScreen.kt        # Syllabus upload interface
```

**Database Schema:**
```kotlin
@Entity(tableName = "curricula")
data class CurriculumEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val gradeLevel: String,
    val subject: String,
    val jsonData: String,           // Serialized curriculum data
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val id: String,
    val curriculumId: String,
    val lessonId: String,
    val status: String,             // "not_started", "in_progress", "completed"
    val completionPercentage: Int,
    val timeSpent: Long,
    val completedAt: Long?
)
```

### Phase 2: Core Android Implementation (Weeks 3-6)

#### Step 2.1: Database Integration
**Implementation Details:**

```kotlin
@Dao
interface CurriculumDao {
    @Query("SELECT * FROM curricula WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveCurriculum(): CurriculumEntity?
    
    @Query("SELECT * FROM curricula")
    suspend fun getAllCurricula(): List<CurriculumEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurriculum(curriculum: CurriculumEntity)
    
    @Query("UPDATE curricula SET isActive = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setActiveCurriculum(id: String)
    
    @Query("SELECT * FROM lesson_progress WHERE curriculumId = :curriculumId")
    suspend fun getLessonProgress(curriculumId: String): List<LessonProgressEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateLessonProgress(progress: LessonProgressEntity)
}
```

#### Step 2.2: Curriculum Manager Implementation
**Core Functionality:**

```kotlin
class CurriculumManager @Inject constructor(
    private val repository: CurriculumRepository,
    private val gson: Gson
) {
    suspend fun loadActiveCurriculum(): Curriculum? {
        return repository.getActiveCurriculum()?.let { entity ->
            gson.fromJson(entity.jsonData, Curriculum::class.java)
        }
    }
    
    suspend fun getNextLesson(): Lesson? {
        val curriculum = loadActiveCurriculum() ?: return null
        val progress = repository.getLessonProgress(curriculum.id)
        
        // Find first incomplete lesson
        return curriculum.lessons.firstOrNull { lesson ->
            val lessonProgress = progress.find { it.lessonId == lesson.id }
            lessonProgress?.status != "completed"
        }
    }
    
    suspend fun markLessonComplete(lessonId: String) {
        val curriculum = loadActiveCurriculum() ?: return
        repository.updateLessonProgress(
            LessonProgressEntity(
                id = "${curriculum.id}_$lessonId",
                curriculumId = curriculum.id,
                lessonId = lessonId,
                status = "completed",
                completionPercentage = 100,
                timeSpent = 0, // Track actual time
                completedAt = System.currentTimeMillis()
            )
        )
    }
    
    suspend fun getCurriculumProgress(): CurriculumProgress {
        val curriculum = loadActiveCurriculum() ?: return CurriculumProgress.empty()
        val progress = repository.getLessonProgress(curriculum.id)
        
        val totalLessons = curriculum.lessons.size
        val completedLessons = progress.count { it.status == "completed" }
        val inProgressLessons = progress.count { it.status == "in_progress" }
        
        return CurriculumProgress(
            totalLessons = totalLessons,
            completedLessons = completedLessons,
            inProgressLessons = inProgressLessons,
            completionPercentage = (completedLessons * 100) / totalLessons
        )
    }
}
```

#### Step 2.3: UI Components

**Curriculum Dashboard:**
```kotlin
@Composable
fun CurriculumDashboard(
    viewModel: CurriculumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Progress Overview Card
        CurriculumProgressCard(
            progress = uiState.progress,
            onViewDetailsClick = { /* Navigate to detailed progress */ }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Next Lesson Card
        uiState.nextLesson?.let { lesson ->
            NextLessonCard(
                lesson = lesson,
                onStartLessonClick = { viewModel.startLesson(lesson.id) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Recent Lessons
        LazyColumn {
            items(uiState.recentLessons) { lesson ->
                LessonProgressItem(
                    lesson = lesson,
                    progress = uiState.lessonProgress[lesson.id],
                    onLessonClick = { viewModel.navigateToLesson(lesson.id) }
                )
            }
        }
    }
}
```

**Lesson Detail Screen:**
```kotlin
@Composable
fun LessonDetailScreen(
    lessonId: String,
    viewModel: LessonViewModel = hiltViewModel()
) {
    val lesson by viewModel.lesson.collectAsState()
    val progress by viewModel.progress.collectAsState()
    
    lesson?.let { lessonData ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                LessonHeader(
                    title = lessonData.title,
                    description = lessonData.description,
                    estimatedTime = lessonData.estimatedTime,
                    progress = progress
                )
            }
            
            item {
                LessonObjectives(objectives = lessonData.objectives)
            }
            
            item {
                LessonContent(content = lessonData.content)
            }
            
            items(lessonData.activities) { activity ->
                ActivityCard(
                    activity = activity,
                    onCompleteActivity = { viewModel.completeActivity(it) }
                )
            }
            
            items(lessonData.assessments) { assessment ->
                AssessmentCard(
                    assessment = assessment,
                    onStartAssessment = { viewModel.startAssessment(it) }
                )
            }
            
            item {
                LessonActions(
                    onMarkComplete = { viewModel.markLessonComplete() },
                    onSaveProgress = { viewModel.saveProgress() }
                )
            }
        }
    }
}
```

### Phase 3: Advanced Features (Weeks 7-10)

#### Step 3.1: Syllabus Parser Integration
**AI Integration with Existing OpenAI Setup:**

```kotlin
class SyllabusParser @Inject constructor(
    private val openAiService: OpenAiService, // Existing service
    private val curriculumRepository: CurriculumRepository
) {
    suspend fun parseSyllabus(syllabusText: String): Result<Curriculum> {
        return try {
            val prompt = buildSyllabusParsingPrompt(syllabusText)
            val response = openAiService.generateCompletion(prompt)
            val curriculum = parseCurriculumFromResponse(response)
            
            // Save to database
            curriculumRepository.saveCurriculum(curriculum)
            
            Result.success(curriculum)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun buildSyllabusParsingPrompt(syllabusText: String): String {
        return """
        Parse the following syllabus into a structured curriculum format.
        
        Requirements:
        - Create detailed lessons for each topic
        - Include learning objectives for each lesson
        - Suggest appropriate activities and assessments
        - Estimate time requirements
        - Identify prerequisites and dependencies
        
        Syllabus:
        $syllabusText
        
        Return the curriculum in JSON format matching the Curriculum data class structure.
        """.trimIndent()
    }
}
```

#### Step 3.2: Bulk Curriculum Loading
**Asset Management System:**

```kotlin
class CurriculumAssetManager @Inject constructor(
    private val context: Context,
    private val curriculumRepository: CurriculumRepository
) {
    suspend fun loadPrebuiltCurricula() {
        val assetManager = context.assets
        val curriculumFiles = assetManager.list("curricula") ?: return
        
        curriculumFiles.forEach { fileName ->
            if (fileName.endsWith(".json")) {
                val jsonContent = assetManager.open("curricula/$fileName")
                    .bufferedReader()
                    .use { it.readText() }
                
                val curriculum = gson.fromJson(jsonContent, Curriculum::class.java)
                curriculumRepository.saveCurriculum(curriculum)
            }
        }
    }
    
    suspend fun checkForCurriculumUpdates() {
        // Future: Check for curriculum updates from server
        // Download and integrate new curricula
    }
}
```

### Phase 4: Integration with Existing Merlin Features (Weeks 11-12)

#### Step 4.1: Chat Integration
**Curriculum-Aware Conversations:**

```kotlin
class CurriculumAwareChatService @Inject constructor(
    private val curriculumManager: CurriculumManager,
    private val existingChatService: ChatService
) {
    suspend fun generateContextualResponse(userMessage: String): String {
        val currentLesson = curriculumManager.getCurrentLesson()
        val context = buildCurriculumContext(currentLesson)
        
        return existingChatService.generateResponse(
            message = userMessage,
            context = context
        )
    }
    
    private fun buildCurriculumContext(lesson: Lesson?): String {
        return lesson?.let {
            """
            Current Context:
            - Student is working on: ${it.title}
            - Lesson objectives: ${it.objectives.joinToString(", ")}
            - Current topic: ${it.content.take(200)}...
            
            Please provide responses that align with the current lesson context.
            """.trimIndent()
        } ?: ""
    }
}
```

#### Step 4.2: Progress-Based Guidance
**Adaptive App Behavior:**

```kotlin
class AdaptiveGuidanceSystem @Inject constructor(
    private val curriculumManager: CurriculumManager,
    private val progressTracker: ProgressTracker
) {
    suspend fun getNextGuidance(): GuidanceAction {
        val progress = curriculumManager.getCurriculumProgress()
        val nextLesson = curriculumManager.getNextLesson()
        val strugglingAreas = progressTracker.identifyStrugglingAreas()
        
        return when {
            strugglingAreas.isNotEmpty() -> {
                GuidanceAction.ReviewMode(strugglingAreas.first())
            }
            nextLesson != null -> {
                GuidanceAction.StartLesson(nextLesson)
            }
            progress.isComplete() -> {
                GuidanceAction.CurriculumComplete
            }
            else -> {
                GuidanceAction.FreeStudy
            }
        }
    }
}

sealed class GuidanceAction {
    data class StartLesson(val lesson: Lesson) : GuidanceAction()
    data class ReviewMode(val topic: String) : GuidanceAction()
    object CurriculumComplete : GuidanceAction()
    object FreeStudy : GuidanceAction()
}
```

## File Structure

### Current Project Structure
```
/c/Users/seang/Tutor/Curricula/           # Curriculum Generation System
├── package.json                          # Node.js configuration
├── generate-curriculum.js                # Main generator
├── convert-for-android.js               # Android converter
├── curriculum-templates.js              # Template definitions
├── generated-curricula/                 # Generated JSON files
├── android-assets/                      # Android-compatible outputs
└── docs/                               # Documentation

/c/Users/seang/Tutor/Merlinv3/          # Android Project
├── app/src/main/java/com/example/merlin/
│   ├── curriculum/                      # New curriculum package
│   │   ├── data/                       # Data models and repository
│   │   ├── ui/                         # UI components
│   │   └── domain/                     # Business logic
│   └── [existing packages]             # Existing Merlin code
├── tasks/                              # Task files (converted to lessons)
└── curriculum_plan.md                  # This document
```

### Target Android Structure
```
app/src/main/java/com/example/merlin/curriculum/
├── data/
│   ├── CurriculumDao.kt
│   ├── CurriculumDatabase.kt
│   ├── CurriculumEntity.kt
│   ├── CurriculumRepository.kt
│   └── LessonProgressEntity.kt
├── domain/
│   ├── model/
│   │   ├── Curriculum.kt
│   │   ├── Lesson.kt
│   │   ├── Activity.kt
│   │   ├── Assessment.kt
│   │   └── Resource.kt
│   ├── CurriculumManager.kt
│   ├── SyllabusParser.kt
│   └── ProgressTracker.kt
├── ui/
│   ├── CurriculumDashboard.kt
│   ├── LessonDetailScreen.kt
│   ├── ProgressVisualization.kt
│   ├── SyllabusInputScreen.kt
│   └── components/
│       ├── LessonCard.kt
│       ├── ProgressCard.kt
│       └── ActivityCard.kt
└── CurriculumAssetManager.kt

app/src/main/assets/curricula/            # Prebuilt curricula
├── common-core-grade3-math.json
├── elementary-math-basics.json
├── middle-school-science.json
└── [other curriculum files]
```

## Technical Specifications

### Dependencies to Add

**Android (build.gradle)**:
```kotlin
dependencies {
    // Room database
    implementation "androidx.room:room-runtime:2.5.0"
    implementation "androidx.room:room-ktx:2.5.0"
    kapt "androidx.room:room-compiler:2.5.0"
    
    // Navigation
    implementation "androidx.navigation:navigation-compose:2.7.2"
    
    // Gson for JSON parsing
    implementation "com.google.code.gson:gson:2.10.1"
    
    // Existing dependencies remain
    // ...
}
```

**Node.js (package.json)** - Already implemented:
```json
{
  "dependencies": {
    "openai": "^4.20.1",
    "fs-extra": "^11.1.1",
    "dotenv": "^16.3.1"
  }
}
```

### Environment Configuration

**Required Environment Variables:**
```
OPENAI_API_KEY=your_openai_api_key_here
```

**Android Configuration:**
- Minimum SDK: 24 (existing requirement)
- Target SDK: 34 (existing)
- Kotlin version: 1.9.0+ (existing)

## Testing Strategy

### Unit Testing Approach
```kotlin
// Example test structure
@RunWith(JUnit4::class)
class CurriculumManagerTest {
    
    @Mock
    private lateinit var repository: CurriculumRepository
    
    @InjectMocks
    private lateinit var curriculumManager: CurriculumManager
    
    @Test
    fun `getNextLesson returns first incomplete lesson`() = runTest {
        // Given
        val curriculum = createTestCurriculum()
        val progress = listOf(
            createCompletedProgress("lesson1"),
            createInProgressProgress("lesson2")
        )
        
        // When
        val nextLesson = curriculumManager.getNextLesson()
        
        // Then
        assertEquals("lesson2", nextLesson?.id)
    }
}
```

### Integration Testing
- Test complete curriculum loading flow
- Verify AI parsing accuracy with known syllabi
- Test progress persistence across app restarts
- Validate Android asset conversion process

### Performance Considerations
- Curriculum data caching strategies
- Lazy loading of lesson content
- Background progress synchronization
- Memory management for large curricula

## Cost Analysis

### Current Costs (Tested)
- **5 Complete Curricula**: $0.96
- **Average per Curriculum**: ~$0.19
- **Estimated 35 Full Curricula**: ~$6.65

### Production Estimates
- **Monthly AI Usage**: $50-100 (including parsing + generation)
- **Storage Requirements**: 50-100MB per complete curriculum set
- **Development Time**: 12 weeks for full implementation

## Risk Assessment & Mitigation

### Technical Risks
1. **AI API Rate Limits**
   - Mitigation: Implement exponential backoff, queue system
   
2. **Large Curriculum Storage**
   - Mitigation: Compress JSON, lazy loading, cloud storage

3. **Android Performance**
   - Mitigation: Background processing, progress caching

### Educational Risks
1. **Curriculum Quality Consistency**
   - Mitigation: Template validation, educational review process
   
2. **Standards Alignment**
   - Mitigation: Expert review, standards mapping verification

## Success Metrics

### Technical Metrics
- Curriculum generation success rate: >95%
- Android app performance: <2s lesson loading
- Data persistence reliability: 100%

### Educational Metrics
- User engagement with curriculum-guided lessons
- Completion rates compared to free-form study
- Learning outcome improvements

## Implementation Timeline Summary

**Week 1-2**: Common Core Grade 3 Math curriculum generation and Android data model integration

**Week 3-6**: Core Android implementation (database, UI, basic navigation)

**Week 7-10**: Advanced features (syllabus parser, bulk loading, progress tracking)

**Week 11-12**: Integration with existing Merlin features and testing

**Week 13+**: Production deployment, user testing, iteration

## Next Immediate Actions

1. **Generate Common Core Grade 3 Math Curriculum**
   - Run curriculum generator with specific CC standards
   - Validate output quality and completeness
   - Convert to Android assets

2. **Setup Android Database Schema**
   - Implement Room database entities
   - Create DAOs and repository pattern
   - Add migration strategies

3. **Create Basic UI Components**
   - Curriculum dashboard mockup
   - Lesson detail screen prototype
   - Progress visualization components

4. **Integration Testing**
   - Test curriculum loading in Android app
   - Verify data persistence
   - Validate navigation flow

---

*This plan serves as the canonical reference for the Merlin Curriculum Management System. All implementation should follow this specification, with updates made to this document as the system evolves.*