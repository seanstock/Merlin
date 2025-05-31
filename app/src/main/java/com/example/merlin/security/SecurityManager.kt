package com.example.merlin.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.merlin.BuildConfig
import com.example.merlin.config.ServiceConfiguration
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Comprehensive security manager for detecting and responding to security threats.
 * Implements root detection, Safe Mode detection, ADB detection, and security enforcement.
 */
class SecurityManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SecurityManager"
        
        // Development flag - now using ServiceConfiguration to check build variant
        // This will be true for debug and staging, false for production
        private val DISABLE_ADB_CHECKS_FOR_DEVELOPMENT get() = ServiceConfiguration.isDevelopmentBuild()
        
        // Common root management apps
        private val ROOT_APPS = arrayOf(
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.koushikdutta.rommanager",
            "com.koushikdutta.rommanager.license",
            "com.dimonvideo.luckypatcher",
            "com.chelpus.lackypatch",
            "com.ramdroid.appquarantine",
            "com.ramdroid.appquarantinepro",
            "com.topjohnwu.magisk"
        )
        
        // Common root binaries
        private val ROOT_BINARIES = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/system/xbin/daemonsu",
            "/system/etc/init.d/99SuperSUDaemon",
            "/dev/com.koushikdutta.superuser.daemon/",
            "/system/app/SuperSU.apk"
        )
        
        // Dangerous system properties
        private val DANGEROUS_PROPS = arrayOf(
            "ro.debuggable" to "1",
            "ro.secure" to "0",
            "service.adb.root" to "1",
            "ro.build.selinux" to "0"
        )
        
        // Root cloaking apps
        private val ROOT_CLOAKING_APPS = arrayOf(
            "com.devadvance.rootcloak",
            "com.devadvance.rootcloakplus",
            "de.robv.android.xposed.installer",
            "com.saurik.substrate",
            "com.zachspong.temprootremovejb",
            "com.amphoras.hidemyroot",
            "com.amphoras.hidemyrootadfree",
            "com.formyhm.hiderootPremium",
            "com.formyhm.hideroot"
        )
    }
    
    /**
     * Comprehensive root detection using multiple methods.
     */
    fun isDeviceRooted(): Boolean {
        Log.d(TAG, "Starting comprehensive root detection")
        
        val rootIndicators = mutableListOf<String>()
        
        // Check for SU binary
        if (checkForSUBinary()) {
            rootIndicators.add("SU binary found")
        }
        
        // Check for test keys
        if (checkForTestKeys()) {
            rootIndicators.add("Test keys detected")
        }
        
        // Check for dangerous properties
        if (checkForDangerousProps()) {
            rootIndicators.add("Dangerous properties found")
        }
        
        // Check for RW system partition
        if (checkForRWSystem()) {
            rootIndicators.add("RW system partition detected")
        }
        
        // Check for root management apps
        if (checkForRootApps()) {
            rootIndicators.add("Root management apps found")
        }
        
        // Check for root cloaking apps
        if (checkForRootCloakingApps()) {
            rootIndicators.add("Root cloaking apps detected")
        }
        
        // Check for Magisk
        if (checkForMagisk()) {
            rootIndicators.add("Magisk detected")
        }
        
        // Check for Xposed framework
        if (checkForXposed()) {
            rootIndicators.add("Xposed framework detected")
        }
        
        // Check for BusyBox
        if (checkForBusyBox()) {
            rootIndicators.add("BusyBox detected")
        }
        
        val isRooted = rootIndicators.isNotEmpty()
        
        if (isRooted) {
            Log.w(TAG, "Device appears to be rooted. Indicators: ${rootIndicators.joinToString(", ")}")
        } else {
            Log.d(TAG, "No root indicators detected")
        }
        
        return isRooted
    }
    
    /**
     * Check if device is in Safe Mode.
     */
    fun isInSafeMode(): Boolean {
        val safeMode = context.packageManager.isSafeMode
        if (safeMode) {
            Log.w(TAG, "Device is in Safe Mode")
        }
        return safeMode
    }
    
    /**
     * Check if ADB is enabled.
     */
    fun isADBEnabled(): Boolean {
        return try {
            val adbEnabled = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) == 1
            
            if (adbEnabled) {
                Log.w(TAG, "ADB is enabled")
            }
            
            adbEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Error checking ADB status", e)
            false
        }
    }
    
    /**
     * Check if USB debugging is enabled.
     */
    fun isUSBDebuggingEnabled(): Boolean {
        return try {
            val usbDebugging = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) == 1
            
            if (usbDebugging) {
                Log.w(TAG, "USB debugging is enabled")
            }
            
            usbDebugging
        } catch (e: Exception) {
            Log.e(TAG, "Error checking USB debugging status", e)
            false
        }
    }
    
    /**
     * Check if developer options are enabled.
     */
    fun isDeveloperOptionsEnabled(): Boolean {
        return try {
            val devOptions = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) == 1
            
            if (devOptions) {
                Log.w(TAG, "Developer options are enabled")
            }
            
            devOptions
        } catch (e: Exception) {
            Log.e(TAG, "Error checking developer options status", e)
            false
        }
    }
    
    /**
     * Enforce security measures when threats are detected.
     */
    fun enforceSecurityMeasures(): SecurityThreat? {
        Log.d(TAG, "Running security enforcement checks")
        
        if (DISABLE_ADB_CHECKS_FOR_DEVELOPMENT) {
            Log.d(TAG, "Development mode: Skipping ADB/USB debugging checks")
        }
        
        return when {
            isDeviceRooted() -> {
                Log.w(TAG, "Security threat detected: Device is rooted")
                SecurityThreat.ROOTED_DEVICE
            }
            isInSafeMode() -> {
                Log.w(TAG, "Security threat detected: Device is in Safe Mode")
                SecurityThreat.SAFE_MODE
            }
            !DISABLE_ADB_CHECKS_FOR_DEVELOPMENT && isADBEnabled() -> {
                Log.w(TAG, "Security threat detected: ADB is enabled")
                SecurityThreat.ADB_ENABLED
            }
            !DISABLE_ADB_CHECKS_FOR_DEVELOPMENT && isDeveloperOptionsEnabled() -> {
                Log.w(TAG, "Security threat detected: Developer options enabled")
                SecurityThreat.DEVELOPER_OPTIONS
            }
            else -> {
                Log.d(TAG, "No security threats detected")
                null
            }
        }
    }
    
    /**
     * Get detailed security status report.
     */
    fun getSecurityReport(): SecurityReport {
        return SecurityReport(
            isRooted = isDeviceRooted(),
            isSafeMode = isInSafeMode(),
            isADBEnabled = isADBEnabled(),
            isDeveloperOptionsEnabled = isDeveloperOptionsEnabled(),
            buildInfo = getBuildInfo(),
            timestamp = System.currentTimeMillis()
        )
    }
    
    // Private helper methods for root detection
    
    private fun checkForSUBinary(): Boolean {
        return ROOT_BINARIES.any { path ->
            try {
                val file = File(path)
                file.exists() && file.canExecute()
            } catch (e: Exception) {
                false
            }
        }
    }
    
    private fun checkForTestKeys(): Boolean {
        return try {
            Build.TAGS?.contains("test-keys") == true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun checkForDangerousProps(): Boolean {
        return DANGEROUS_PROPS.any { (prop, dangerousValue) ->
            try {
                val value = getSystemProperty(prop)
                value == dangerousValue
            } catch (e: Exception) {
                false
            }
        }
    }
    
    private fun checkForRWSystem(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("mount")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            reader.useLines { lines ->
                lines.any { line ->
                    line.contains("/system") && (line.contains("rw,") || line.contains("rw "))
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun checkForRootApps(): Boolean {
        return ROOT_APPS.any { packageName ->
            try {
                context.packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            } catch (e: Exception) {
                false
            }
        }
    }
    
    private fun checkForRootCloakingApps(): Boolean {
        return ROOT_CLOAKING_APPS.any { packageName ->
            try {
                context.packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            } catch (e: Exception) {
                false
            }
        }
    }
    
    private fun checkForMagisk(): Boolean {
        return try {
            // Check for Magisk app
            context.packageManager.getPackageInfo("com.topjohnwu.magisk", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            // Check for Magisk files
            listOf(
                "/sbin/.magisk",
                "/cache/.disable_magisk",
                "/dev/.magisk.unblock",
                "/cache/magisk.log",
                "/data/adb/magisk",
                "/sbin/.core"
            ).any { path ->
                try {
                    File(path).exists()
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun checkForXposed(): Boolean {
        return try {
            // Check for Xposed installer
            context.packageManager.getPackageInfo("de.robv.android.xposed.installer", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            // Check for Xposed files
            listOf(
                "/system/framework/XposedBridge.jar",
                "/system/bin/app_process_xposed",
                "/system/xbin/xposed"
            ).any { path ->
                try {
                    File(path).exists()
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun checkForBusyBox(): Boolean {
        return listOf(
            "/system/bin/busybox",
            "/system/xbin/busybox",
            "/sbin/busybox",
            "/data/local/busybox",
            "/data/local/bin/busybox",
            "/data/local/xbin/busybox"
        ).any { path ->
            try {
                File(path).exists()
            } catch (e: Exception) {
                false
            }
        }
    }
    
    private fun getSystemProperty(key: String): String? {
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine()?.trim()
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getBuildInfo(): BuildInfo {
        return BuildInfo(
            brand = Build.BRAND,
            model = Build.MODEL,
            device = Build.DEVICE,
            product = Build.PRODUCT,
            hardware = Build.HARDWARE,
            bootloader = Build.BOOTLOADER,
            fingerprint = Build.FINGERPRINT,
            tags = Build.TAGS,
            type = Build.TYPE,
            isDebuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        )
    }
}

/**
 * Enum representing different types of security threats.
 */
enum class SecurityThreat {
    ROOTED_DEVICE,
    SAFE_MODE,
    ADB_ENABLED,
    DEVELOPER_OPTIONS,
    TAMPERED_APP,
    DEBUGGER_ATTACHED
}

/**
 * Data class representing a comprehensive security report.
 */
data class SecurityReport(
    val isRooted: Boolean,
    val isSafeMode: Boolean,
    val isADBEnabled: Boolean,
    val isDeveloperOptionsEnabled: Boolean,
    val buildInfo: BuildInfo,
    val timestamp: Long
)

/**
 * Data class containing device build information.
 */
data class BuildInfo(
    val brand: String,
    val model: String,
    val device: String,
    val product: String,
    val hardware: String,
    val bootloader: String,
    val fingerprint: String,
    val tags: String?,
    val type: String,
    val isDebuggable: Boolean
) 