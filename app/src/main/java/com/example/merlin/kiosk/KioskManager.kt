package com.example.merlin.kiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import android.util.Log
import com.example.merlin.security.MerlinDeviceAdminReceiver

/**
 * Helper wrapper around [DevicePolicyManager] for enabling and disabling Android kiosk (lock-task) mode.
 */
class KioskManager(private val context: Context) {

    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val adminComponent = ComponentName(context, MerlinDeviceAdminReceiver::class.java)

    /** Returns true if this app is registered as device owner. */
    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(context.packageName)

    /** Apply all policies required for kiosk. */
    fun setKioskPolicies() {
        if (!isDeviceOwner()) {
            throw SecurityException("App is not device owner; cannot set kiosk policies")
        }
        // Allow only Merlin to run in lock-task mode.
        dpm.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
        // Disable safe boot so child cannot reboot to escape.
        dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
        // Disable keyguard and status bar for full lockdown.
        dpm.setKeyguardDisabled(adminComponent, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            dpm.setStatusBarDisabled(adminComponent, true)
        }
        // Android 14 (API 34)+ : disable swipe-to-reveal system bars
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                val m = DevicePolicyManager::class.java.getMethod(
                    "setUserControlDisabled",
                    ComponentName::class.java,
                    Boolean::class.javaPrimitiveType
                )
                m.invoke(dpm, adminComponent, true)
            } catch (e: Exception) {
                Log.w(TAG, "setUserControlDisabled reflection failed: ${e.message}")
            }
        }
    }

    /** Clear kiosk policies so device returns to normal. */
    fun clearKioskPolicies() {
        if (!isDeviceOwner()) return
        dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
        dpm.setKeyguardDisabled(adminComponent, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            dpm.setStatusBarDisabled(adminComponent, false)
        }
        // Remove lock-task package whitelist
        dpm.setLockTaskPackages(adminComponent, emptyArray())
    }

    /** Enters kiosk (lock-task) mode if possible. Safe-guards onboarding check left to caller. */
    fun startKioskMode(activity: Activity) {
        try {
            if (!isDeviceOwner()) {
                Log.w(TAG, "startKioskMode: app is not device owner")
                return
            }
            setKioskPolicies()
            activity.startLockTask()
            Log.i(TAG, "Kiosk mode started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start kiosk mode", e)
        }
    }

    /** Exits kiosk (lock-task) mode and clears policies. */
    fun exitKioskMode(activity: Activity) {
        try {
            activity.stopLockTask()
            clearKioskPolicies()
            Log.i(TAG, "Kiosk mode exited")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to exit kiosk mode", e)
        }
    }

    /**
     * Dynamically allow an external package to run inside lock-task while kiosk is active.
     */
    fun addAllowedPackage(pkg: String) {
        if (!isDeviceOwner()) return
        try {
            val current = dpm.getLockTaskPackages(adminComponent).toMutableSet()
            if (current.add(pkg)) {
                dpm.setLockTaskPackages(adminComponent, current.toTypedArray())
                Log.i(TAG, "Added $pkg to lock-task whitelist")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add allowed package $pkg", e)
        }
    }

    /**
     * Remove package from lock-task whitelist (call when session expires)
     */
    fun removeAllowedPackage(pkg: String) {
        if (!isDeviceOwner()) return
        try {
            val current = dpm.getLockTaskPackages(adminComponent).toMutableSet()
            if (current.remove(pkg)) {
                dpm.setLockTaskPackages(adminComponent, current.toTypedArray())
                Log.i(TAG, "Removed $pkg from lock-task whitelist")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove allowed package $pkg", e)
        }
    }

    companion object {
        private const val TAG = "KioskManager"
    }
} 