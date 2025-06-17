package com.example.merlin.economy.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import com.example.merlin.economy.model.TransactionCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Local Android implementation of AppLaunchService using Android Intents.
 * This will be swapped for RemoteAppLaunchService in the LaaS architecture.
 */
class LocalAppLaunchService(
    private val context: Context,
    private val economyService: EconomyService
) : AppLaunchService {

    companion object {
        // Predefined apps available for purchase
        private val AVAILABLE_APPS = listOf(
            PurchasableAppDto(
                packageName = "com.google.android.apps.youtube.kids",
                displayName = "YouTube Kids",
                description = "Safe videos for children",
                category = "entertainment",
                costPerMinute = 10, // 10 Merlin Coins per minute
                maxDuration = 30    // Max 30 minutes
            )
        )
    }

    override suspend fun launchApp(packageName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val intent = when (packageName) {
                "com.google.android.apps.youtube.kids" -> {
                    context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }
                else -> {
                    context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }
            }

            if (intent != null && intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Result.success(true)
            } else {
                Result.failure(Exception("App not found or cannot be launched: $packageName"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isAppInstalled(packageName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            context.packageManager.getApplicationInfo(packageName, 0)
            Result.success(true)
        } catch (e: PackageManager.NameNotFoundException) {
            Result.success(false)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get the app icon from the system
     */
    suspend fun getAppIcon(packageName: String): Result<android.graphics.drawable.Drawable?> = withContext(Dispatchers.IO) {
        try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val icon = context.packageManager.getApplicationIcon(appInfo)
            Result.success(icon)
        } catch (e: PackageManager.NameNotFoundException) {
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAvailableApps(): Result<List<PurchasableAppDto>> = withContext(Dispatchers.IO) {
        try {
            // Only return apps that are actually installed
            val installedApps = AVAILABLE_APPS.mapNotNull { app ->
                val isInstalled = isAppInstalled(app.packageName).getOrElse { false }
                if (isInstalled) {
                    app.copy(isInstalled = true)
                } else {
                    null // Filter out non-installed apps
                }
            }
            Result.success(installedApps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun calculateAppAccessCost(
        appPackage: String,
        durationMinutes: Int
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val app = AVAILABLE_APPS.find { it.packageName == appPackage }
                ?: return@withContext Result.failure(Exception("App not found: $appPackage"))

            if (durationMinutes > app.maxDuration) {
                return@withContext Result.failure(
                    Exception("Duration exceeds maximum allowed: ${app.maxDuration} minutes")
                )
            }

            val baseCost = app.costPerMinute * durationMinutes
            
            // Apply category discount from existing economy system
            val discount = TransactionCategory.CATEGORY_DISCOUNTS[TransactionCategory.SPENDING_APP_ACCESS] ?: 1.0f
            val finalCost = (baseCost * discount).toInt()

            Result.success(finalCost)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun purchaseAppAccess(
        childId: String,
        appPackage: String,
        durationMinutes: Int
    ): Result<AppAccessPurchaseDto> = withContext(Dispatchers.IO) {
        try {
            // 1. Calculate cost
            val costResult = calculateAppAccessCost(appPackage, durationMinutes)
            if (costResult.isFailure) {
                return@withContext Result.success(AppAccessPurchaseDto(
                    success = false,
                    appPackage = appPackage,
                    durationMinutes = durationMinutes,
                    totalCost = 0,
                    remainingBalance = 0,
                    sessionId = "",
                    expiresAt = "",
                    errorMessage = costResult.exceptionOrNull()?.message ?: "Cost calculation failed"
                ))
            }

            val totalCost = costResult.getOrThrow()

            // 2. Check if child has sufficient coins
            val balanceResult = economyService.getBalance(childId)
            if (balanceResult.isFailure) {
                return@withContext Result.success(AppAccessPurchaseDto(
                    success = false,
                    appPackage = appPackage,
                    durationMinutes = durationMinutes,
                    totalCost = totalCost,
                    remainingBalance = 0,
                    sessionId = "",
                    expiresAt = "",
                    errorMessage = "Could not check balance"
                ))
            }

            val currentBalance = balanceResult.getOrThrow().balance
            if (currentBalance < totalCost) {
                return@withContext Result.success(AppAccessPurchaseDto(
                    success = false,
                    appPackage = appPackage,
                    durationMinutes = durationMinutes,
                    totalCost = totalCost,
                    remainingBalance = currentBalance,
                    sessionId = "",
                    expiresAt = "",
                    errorMessage = "Insufficient coins. Need $totalCost, have $currentBalance"
                ))
            }

            // 3. Launch the app (coin spending handled by WalletViewModel)
            val launchResult = launchApp(appPackage)
            if (launchResult.isFailure) {
                return@withContext Result.success(AppAccessPurchaseDto(
                    success = false,
                    appPackage = appPackage,
                    durationMinutes = durationMinutes,
                    totalCost = totalCost,
                    remainingBalance = currentBalance,
                    sessionId = "",
                    expiresAt = "",
                    errorMessage = "App launch failed: ${launchResult.exceptionOrNull()?.message}"
                ))
            }

            // 4. Success! (Coins will be spent by WalletViewModel)
            val sessionId = UUID.randomUUID().toString()
            val expiresAt = Instant.now().plus(durationMinutes.toLong(), ChronoUnit.MINUTES).toString()

            Result.success(AppAccessPurchaseDto(
                success = true,
                appPackage = appPackage,
                durationMinutes = durationMinutes,
                totalCost = totalCost,
                remainingBalance = currentBalance, // Balance unchanged here - WalletViewModel will update it
                sessionId = sessionId,
                expiresAt = expiresAt,
                errorMessage = ""
            ))

        } catch (e: Exception) {
            Result.success(AppAccessPurchaseDto(
                success = false,
                appPackage = appPackage,
                durationMinutes = durationMinutes,
                totalCost = 0,
                remainingBalance = 0,
                sessionId = "",
                expiresAt = "",
                errorMessage = "Unexpected error: ${e.message}"
            ))
        }
    }
} 