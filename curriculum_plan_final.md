# Merlin Curriculum Integration - LaaS-Compliant Implementation Plan

## **Implementation Steps**

### **Step 1: Generate Curriculum Data**
```bash
cd tasks
npm run generate-sample
npm run convert-for-android
```

### **Step 2: Create LaaS-Compliant Curriculum Package**
Create `app/src/main/java/com/example/merlin/curriculum/` with:

```
curriculum/
├── model/
│   ├── Curriculum.kt              # DTO for service boundaries
│   ├── Lesson.kt                  # DTO for service boundaries
│   ├── Activity.kt                # DTO for service boundaries
│   └── CurriculumProgressDto.kt   # DTO for progress data
├── service/
│   ├── CurriculumService.kt       # 🎯 LaaS Interface
│   ├── LocalCurriculumService.kt  # Local implementation
│   └── RemoteCurriculumService.kt # Future remote implementation
├── data/
│   ├── CurriculumEntity.kt        # Local storage only
│   ├── CurriculumDao.kt           # Local storage only
│   └── CurriculumRepository.kt    # Local storage only
├── domain/
│   └── CurriculumManager.kt       # Business logic (service-agnostic)
└── ui/
    ├── CurriculumDashboard.kt
    └── CurriculumViewModel.kt
```

### **Step 3: Create LaaS Service Interface**
**Primary interface for externalization:**
```kotlin
interface CurriculumService {
    suspend fun getAvailableCurricula(): Result<List<CurriculumDto>>
    suspend fun getCurriculumById(id: String): Result<CurriculumDto>
    suspend fun getChildProgress(curriculumId: String, childId: String): Result<CurriculumProgressDto>
    suspend fun recordLessonProgress(lessonId: String, childId: String, progress: LessonProgressDto): Result<Unit>
    suspend fun recordTaskProgress(taskId: String, childId: String, progress: TaskProgressDto): Result<Unit>
    suspend fun getNextLesson(curriculumId: String, childId: String): Result<LessonDto?>
    suspend fun getTaskProgress(lessonId: String, childId: String): Result<List<TaskProgressDto>>
}

// Simple Task Tracking DTOs
data class TaskProgressDto(
    val taskId: String,
    val lessonId: String,
    val childId: String,
    val status: String,           // "not_started", "in_progress", "completed"
    val percentComplete: Int,     // 0-100
    val grade: Int,              // 0-100
    val tutorNotes: String       // AI-generated or manual notes
)

data class LessonProgressDto(
    val lessonId: String,
    val childId: String,
    val status: String,           // "not_started", "in_progress", "completed"
    val percentComplete: Int,     // 0-100
    val grade: Int,              // 0-100  
    val tutorNotes: String       // Overall lesson notes
)
```

### **Step 4: Implement Local Service**
```kotlin
@Singleton
class LocalCurriculumService @Inject constructor(
    private val repository: CurriculumRepository  // Only local implementation touches Room
) : CurriculumService {
    override suspend fun getCurriculumById(id: String): Result<CurriculumDto> {
        return try {
            val entity = repository.getCurriculumById(id)
            Result.success(entity.toDto())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // ... other methods
}
```

### **Step 5: Create Service-Agnostic Manager**
```kotlin
@Singleton
class CurriculumManager @Inject constructor(
    private val curriculumService: CurriculumService,  // 🎯 Interface dependency
    private val economyService: EconomyService
) {
    suspend fun getCurrentLessonContext(childId: String): CurriculumContext? {
        // Platform-agnostic business logic
        val progress = curriculumService.getChildProgress(curriculumId, childId).getOrNull()
        return progress?.let { buildContext(it) }
    }
    
    suspend fun markLessonComplete(lessonId: String, childId: String, grade: Int, notes: String): Result<Unit> {
        val progress = LessonProgressDto(
            lessonId = lessonId,
            childId = childId,
                status = "completed",
            percentComplete = 100,
            grade = grade,
            tutorNotes = notes
        )
        
        // Award coins based on grade
        val coinReward = when {
            grade >= 90 -> 20
            grade >= 80 -> 15
            grade >= 70 -> 10
            grade >= 60 -> 5
            else -> 2
        }
        economyService.awardCoins(childId, coinReward, "lesson_completed")
        
        return curriculumService.recordLessonProgress(lessonId, childId, progress)
    }
    
    suspend fun updateTaskProgress(taskId: String, childId: String, status: String, percentComplete: Int, grade: Int, notes: String): Result<Unit> {
        val progress = TaskProgressDto(
            taskId = taskId,
            lessonId = "", // Get from context
            childId = childId,
            status = status,
            percentComplete = percentComplete,
            grade = grade,
            tutorNotes = notes
        )
        return curriculumService.recordTaskProgress(taskId, childId, progress)
    }
    
    suspend fun getTaskProgress(lessonId: String, childId: String): Result<List<TaskProgressDto>> {
        return curriculumService.getTaskProgress(lessonId, childId)
    }
}
```

### **Step 6: Configure Service Selection**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object CurriculumServiceModule {
    
    @Provides
    @Singleton
    fun provideCurriculumService(
        localService: LocalCurriculumService,
        config: ServiceConfiguration
    ): CurriculumService {
        return when (config.curriculumMode) {
            ServiceMode.LOCAL -> localService
            ServiceMode.REMOTE -> RemoteCurriculumService() // Future
            ServiceMode.MOCK -> MockCurriculumService()    // Testing
        }
    }
}
```

### **Step 7: Add Assets & Database (Local Only)**
- Create `app/src/main/assets/curricula/` for local data
- Extend `MerlinDatabase.kt` for local storage only
- Repository pattern remains local implementation detail

### **Step 8: Enhance AI Service (LaaS-Compliant)**
```kotlin
class AIService @Inject constructor(
    private val curriculumService: CurriculumService,  // 🎯 Interface
    private val openAiClient: OpenAIClient
) {
    suspend fun generateResponse(message: String, childId: String): String {
        val curriculumContext = curriculumService.getChildProgress(curriculumId, childId)
        // ... build curriculum-aware prompt
    }
}
```

## **LaaS Compliance Features**

### **✅ Service Abstraction**
- `CurriculumService` interface enables local/remote swapping
- Business logic depends on interfaces, not implementations
- Clean boundaries for externalization

### **✅ Data Transfer Objects**
- `CurriculumDto`, `LessonDto`, `CurriculumProgressDto` for service boundaries
- JSON-serializable for API compatibility
- Platform-agnostic data structures

### **✅ Context Independence**
- No Android `Context` in business logic
- Service implementations handle platform specifics
- Manager classes are framework-agnostic

### **✅ Configuration-Driven**
- Easy switching between local/remote services
- Environment-based service selection
- Prepared for LaaS migration

## **Future LaaS Migration Path**

### **Phase 1: Local with Interfaces** (Current)
```kotlin
val curriculumService: CurriculumService = LocalCurriculumService()
```

### **Phase 2: Hybrid Implementation**
```kotlin
val curriculumService: CurriculumService = HybridCurriculumService(
    local = LocalCurriculumService(),
    remote = RemoteCurriculumService()
)
```

### **Phase 3: Full LaaS Platform**
```kotlin
val learningService = LearningAsAService.configure {
    apiKey = "dev_key"
    curriculum = CurriculumConfiguration.remote()
}
val curriculumService = learningService.getCurriculumService()
```

This creates a curriculum system designed for eventual externalization while maintaining current local functionality.