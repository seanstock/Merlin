package com.example.merlin.economy.service

/**
 * App launch service interface for managing external app access through coin purchases.
 * Pure business logic contracts with no Android dependencies - ready for local or remote implementation.
 */
interface AppLaunchService {
    
    /**
     * Launch an external app by package name
     */
    suspend fun launchApp(packageName: String): Result<Boolean>
    
    /**
     * Check if an app is installed on the device
     */
    suspend fun isAppInstalled(packageName: String): Result<Boolean>
    
    /**
     * Get list of available purchasable apps
     */
    suspend fun getAvailableApps(): Result<List<PurchasableAppDto>>
    
    /**
     * Calculate cost for app access based on duration
     */
    suspend fun calculateAppAccessCost(
        appPackage: String,
        durationMinutes: Int
    ): Result<Int>
    
    /**
     * Purchase app access with coins
     */
    suspend fun purchaseAppAccess(
        childId: String,
        appPackage: String,
        durationMinutes: Int
    ): Result<AppAccessPurchaseDto>
}

/**
 * Data class for purchasable apps
 */
data class PurchasableAppDto(
    val packageName: String,
    val displayName: String,
    val description: String,
    val icon: String? = null,
    val category: String,
    val costPerMinute: Int,  // Merlin Coins per minute
    val maxDuration: Int,    // Maximum allowed session in minutes
    val isInstalled: Boolean = false
)

/**
 * Result of app access purchase
 */
data class AppAccessPurchaseDto(
    val success: Boolean,
    val appPackage: String,
    val durationMinutes: Int,
    val totalCost: Int,
    val remainingBalance: Int,
    val sessionId: String,
    val expiresAt: String,  // ISO 8601 timestamp
    val errorMessage: String = ""
) 