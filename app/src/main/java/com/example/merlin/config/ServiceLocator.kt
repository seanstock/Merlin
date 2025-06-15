package com.example.merlin.config

import android.content.Context
import com.example.merlin.data.repository.ChildProfileRepository
import com.example.merlin.data.repository.EconomyStateRepository
import com.example.merlin.data.database.DatabaseProvider
import com.example.merlin.economy.service.*
import com.example.merlin.economy.model.*
import com.example.merlin.ui.theme.ThemeService
import com.example.merlin.curriculum.service.CurriculumService
import com.example.merlin.curriculum.service.LocalCurriculumService
import com.example.merlin.curriculum.service.SyllabusGeneratorService
import com.example.merlin.curriculum.service.LocalSyllabusGeneratorService
import com.example.merlin.curriculum.data.CurriculumRepository
import com.example.merlin.curriculum.domain.CurriculumManager

/**
 * Service locator for Learning-as-a-Service architecture.
 * Provides service implementations based on configuration (local vs remote).
 */
object ServiceLocator {
    
    @Volatile
    private var adaptiveDifficultyService: AdaptiveDifficultyService? = null
    
    @Volatile
    private var economyService: EconomyService? = null
    
    @Volatile
    private var badgeService: BadgeService? = null
    
    @Volatile
    private var experienceService: ExperienceService? = null
    
    @Volatile
    private var screenTimeService: ScreenTimeService? = null
    
    @Volatile
    private var uiConfigurationService: UIConfigurationService? = null
    
    @Volatile
    private var themeService: ThemeService? = null
    
    @Volatile
    private var curriculumService: CurriculumService? = null
    
    @Volatile
    private var syllabusGeneratorService: SyllabusGeneratorService? = null
    
    @Volatile
    private var curriculumManager: CurriculumManager? = null
    
    /**
     * Get AdaptiveDifficultyService implementation based on configuration
     */
    fun getAdaptiveDifficultyService(): AdaptiveDifficultyService {
        return adaptiveDifficultyService ?: synchronized(this) {
            adaptiveDifficultyService ?: ServiceConfiguration.getServiceImplementation(
                localImpl = { LocalAdaptiveDifficultyService() },
                remoteImpl = { 
                    // TODO: Implement RemoteAdaptiveDifficultyService for LaaS
                    LocalAdaptiveDifficultyService() // Fallback to local for now
                },
                mockImpl = { LocalAdaptiveDifficultyService() } // Use local for mocking too
            ).also { adaptiveDifficultyService = it }
        }
    }
    
    /**
     * Get EconomyService implementation based on configuration
     */
    fun getEconomyService(context: Context): EconomyService {
        return economyService ?: synchronized(this) {
            economyService ?: ServiceConfiguration.getServiceImplementation(
                localImpl = {
                    val db = DatabaseProvider.getInstance(context)
                    val economyRepository = EconomyStateRepository(db.economyStateDao())
                    val childProfileRepository = ChildProfileRepository(db.childProfileDao())
                    LocalEconomyService(economyRepository, childProfileRepository)
                },
                remoteImpl = {
                    // TODO: Implement RemoteEconomyService for LaaS
                    val db = DatabaseProvider.getInstance(context)
                    val economyRepository = EconomyStateRepository(db.economyStateDao())
                    val childProfileRepository = ChildProfileRepository(db.childProfileDao())
                    LocalEconomyService(economyRepository, childProfileRepository) // Fallback
                },
                mockImpl = { 
                    // Use local for mocking too
                    val db = DatabaseProvider.getInstance(context)
                    val economyRepository = EconomyStateRepository(db.economyStateDao())
                    val childProfileRepository = ChildProfileRepository(db.childProfileDao())
                    LocalEconomyService(economyRepository, childProfileRepository)
                }
            ).also { economyService = it }
        }
    }
    
    /**
     * Get BadgeService implementation based on configuration
     */
    fun getBadgeService(): BadgeService {
        return badgeService ?: synchronized(this) {
            badgeService ?: ServiceConfiguration.getServiceImplementation(
                localImpl = { LocalBadgeService() },
                remoteImpl = {
                    // TODO: Implement RemoteBadgeService for LaaS
                    LocalBadgeService() // Fallback to local for now
                },
                mockImpl = { LocalBadgeService() } // Use local for mocking too
            ).also { badgeService = it }
        }
    }
    
    /**
     * Get ExperienceService implementation based on configuration
     */
    fun getExperienceService(): ExperienceService {
        return experienceService ?: synchronized(this) {
            experienceService ?: ServiceConfiguration.getServiceImplementation(
                localImpl = { LocalExperienceService() },
                remoteImpl = {
                    // TODO: Implement RemoteExperienceService for LaaS
                    LocalExperienceService() // Fallback to local for now
                },
                mockImpl = { LocalExperienceService() } // Use local for mocking too
            ).also { experienceService = it }
        }
    }
    
    /**
     * Get ScreenTimeService implementation based on configuration
     */
    fun getScreenTimeService(context: Context): ScreenTimeService {
        return screenTimeService ?: synchronized(this) {
            screenTimeService ?: ServiceConfiguration.getServiceImplementation(
                localImpl = { LocalScreenTimeService(context) },
                remoteImpl = {
                    // TODO: Implement RemoteScreenTimeService for LaaS
                    LocalScreenTimeService(context) // Fallback to local for now
                },
                mockImpl = { LocalScreenTimeService(context) } // Use local for mocking too
            ).also { screenTimeService = it }
        }
    }
    
    /**
     * Get UIConfigurationService implementation based on configuration
     */
    fun getUIConfigurationService(context: Context): UIConfigurationService {
        return LocalUIConfigurationService(context)
    }
    
    /**
     * Get ThemeService implementation based on configuration
     */
    fun getThemeService(context: Context): ThemeService {
        return themeService ?: synchronized(this) {
            themeService ?: ServiceConfiguration.getServiceImplementation(
                localImpl = {
                    val db = DatabaseProvider.getInstance(context)
                    val childRepo = ChildProfileRepository(db.childProfileDao())
                    ThemeService(childRepo)
                },
                remoteImpl = {
                    // TODO: Remote implementation once LaaS backend supports it
                    val db = DatabaseProvider.getInstance(context)
                    val childRepo = ChildProfileRepository(db.childProfileDao())
                    ThemeService(childRepo) // Fallback
                },
                mockImpl = {
                    val db = DatabaseProvider.getInstance(context)
                    val childRepo = ChildProfileRepository(db.childProfileDao())
                    ThemeService(childRepo)
                }
            ).also { themeService = it }
        }
    }
    
    /**
     * Get CurriculumService implementation based on configuration
     */
    fun getCurriculumService(context: Context): CurriculumService {
        return curriculumService ?: synchronized(this) {
            curriculumService ?: ServiceConfiguration.getServiceImplementation(
                localImpl = {
                    val db = DatabaseProvider.getInstance(context)
                    val repository = CurriculumRepository(db.curriculumDao())
                    LocalCurriculumService(repository)
                },
                remoteImpl = {
                    // TODO: Implement RemoteCurriculumService for LaaS
                    val db = DatabaseProvider.getInstance(context)
                    val repository = CurriculumRepository(db.curriculumDao())
                    LocalCurriculumService(repository) // Fallback
                },
                mockImpl = {
                    val db = DatabaseProvider.getInstance(context)
                    val repository = CurriculumRepository(db.curriculumDao())
                    LocalCurriculumService(repository)
                }
            ).also { curriculumService = it }
        }
    }
    
    /**
     * Get SyllabusGeneratorService implementation based on configuration
     */
    fun getSyllabusGeneratorService(context: Context): SyllabusGeneratorService {
        return syllabusGeneratorService ?: synchronized(this) {
            syllabusGeneratorService ?: ServiceConfiguration.getServiceImplementation(
                localImpl = { LocalSyllabusGeneratorService(context) },
                remoteImpl = {
                    // TODO: Implement RemoteSyllabusGeneratorService for LaaS
                    LocalSyllabusGeneratorService(context) // Fallback to local for now
                },
                mockImpl = { LocalSyllabusGeneratorService(context) } // Use local for mocking too
            ).also { syllabusGeneratorService = it }
        }
    }
    
    /**
     * Get CurriculumManager implementation (service-agnostic business logic)
     */
    fun getCurriculumManager(context: Context): CurriculumManager {
        return curriculumManager ?: synchronized(this) {
            curriculumManager ?: CurriculumManager(
                curriculumService = getCurriculumService(context),
                economyService = getEconomyService(context)
            ).also { curriculumManager = it }
        }
    }
    
    /**
     * Clear all service instances (useful for testing and service configuration changes)
     */
    fun clearAllServices() {
        synchronized(this) {
            adaptiveDifficultyService = null
            economyService = null
            badgeService = null
            experienceService = null
            screenTimeService = null
            uiConfigurationService = null
            themeService = null
            curriculumService = null
            syllabusGeneratorService = null
            curriculumManager = null
        }
    }
    
    /**
     * Get service health status for monitoring
     */
    suspend fun getServiceHealthStatus(): Map<String, String> {
        return mapOf(
            "adaptive_difficulty" to if (adaptiveDifficultyService != null) "initialized" else "not_initialized",
            "economy" to if (economyService != null) "initialized" else "not_initialized",
            "badge" to if (badgeService != null) "initialized" else "not_initialized",
            "experience" to if (experienceService != null) "initialized" else "not_initialized",
            "screen_time" to if (screenTimeService != null) "initialized" else "not_initialized",
            "ui_configuration" to if (uiConfigurationService != null) "initialized" else "not_initialized",
            "theme" to if (themeService != null) "initialized" else "not_initialized",
            "curriculum" to if (curriculumService != null) "initialized" else "not_initialized",
            "syllabus_generator" to if (syllabusGeneratorService != null) "initialized" else "not_initialized",
            "curriculum_manager" to if (curriculumManager != null) "initialized" else "not_initialized",
            "configuration" to ServiceConfiguration.getServiceConfig().buildVariant
        )
    }
} 