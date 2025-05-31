package com.example.merlin.config

import com.example.merlin.BuildConfig

/**
 * Service configuration for Learning-as-a-Service architecture.
 * Supports seamless migration from local services to remote VPS services.
 */
object ServiceConfiguration {
    
    /**
     * Service discovery configuration based on build variant
     */
    data class ServiceConfig(
        val aiServiceUrl: String,
        val economyServiceUrl: String,
        val contentServiceUrl: String,
        val analyticsServiceUrl: String,
        val useLocalServices: Boolean,
        val enableServiceMocking: Boolean,
        val enableDebugLogging: Boolean,
        val apiVersion: String,
        val buildVariant: String
    )
    
    /**
     * Get service configuration for current build variant
     */
    fun getServiceConfig(): ServiceConfig {
        return ServiceConfig(
            aiServiceUrl = BuildConfig.AI_SERVICE_URL,
            economyServiceUrl = BuildConfig.ECONOMY_SERVICE_URL,
            contentServiceUrl = BuildConfig.CONTENT_SERVICE_URL,
            analyticsServiceUrl = BuildConfig.ANALYTICS_SERVICE_URL,
            useLocalServices = BuildConfig.USE_LOCAL_SERVICES,
            enableServiceMocking = BuildConfig.ENABLE_SERVICE_MOCKING,
            enableDebugLogging = BuildConfig.ENABLE_DEBUG_LOGGING,
            apiVersion = BuildConfig.API_VERSION,
            buildVariant = BuildConfig.BUILD_TYPE
        )
    }
    
    /**
     * Service migration helper - determines which implementation to use
     */
    inline fun <reified T> getServiceImplementation(
        localImpl: () -> T,
        remoteImpl: () -> T,
        mockImpl: () -> T
    ): T {
        val config = getServiceConfig()
        
        return when {
            config.enableServiceMocking -> mockImpl()
            config.useLocalServices -> localImpl()
            else -> remoteImpl()
        }
    }
    
    /**
     * Check if current build is development-oriented (debug or staging)
     */
    fun isDevelopmentBuild(): Boolean {
        val config = getServiceConfig()
        return config.buildVariant == "debug" || config.buildVariant == "staging"
    }
    
    /**
     * Check if current build is production
     */
    fun isProductionBuild(): Boolean {
        return getServiceConfig().buildVariant == "release"
    }
    
    /**
     * Service health check endpoints
     */
    object HealthCheck {
        fun getAiServiceHealth() = "${getServiceConfig().aiServiceUrl}/health"
        fun getEconomyServiceHealth() = "${getServiceConfig().economyServiceUrl}/health"
        fun getContentServiceHealth() = "${getServiceConfig().contentServiceUrl}/health"
        fun getAnalyticsServiceHealth() = "${getServiceConfig().analyticsServiceUrl}/health"
    }
} 