package com.example.merlin.security

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Security event interceptor that monitors critical app events and triggers
 * appropriate security checks to detect runtime security changes.
 */
class SecurityEventInterceptor(
    private val context: Context,
    private val runtimeSecurityMonitor: RuntimeSecurityMonitor
) : DefaultLifecycleObserver {
    
    companion object {
        private const val TAG = "SecurityEventInterceptor"
    }
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var systemEventReceiver: SystemEventReceiver? = null
    private var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null
    
    private var isInterceptorActive = false
    
    /**
     * Start intercepting security-relevant events.
     */
    fun startIntercepting() {
        if (isInterceptorActive) {
            Log.w(TAG, "Security event interceptor is already active")
            return
        }
        
        Log.i(TAG, "Starting security event interception")
        isInterceptorActive = true
        
        // Register lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        
        // Register network state monitoring
        registerNetworkCallback()
        
        // Register system event receiver
        registerSystemEventReceiver()
        
        // Register activity lifecycle callbacks
        registerActivityLifecycleCallbacks()
        
        // Trigger initial app launch security check
        runtimeSecurityMonitor.triggerCriticalEventCheck(
            RuntimeSecurityMonitor.CriticalEventType.APP_LAUNCH,
            "Security event interceptor started"
        )
    }
    
    /**
     * Stop intercepting security-relevant events.
     */
    fun stopIntercepting() {
        if (!isInterceptorActive) {
            Log.w(TAG, "Security event interceptor is not active")
            return
        }
        
        Log.i(TAG, "Stopping security event interception")
        isInterceptorActive = false
        
        // Unregister all callbacks and receivers
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        unregisterNetworkCallback()
        unregisterSystemEventReceiver()
        unregisterActivityLifecycleCallbacks()
    }
    
    // Lifecycle observer methods
    
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(TAG, "App moved to foreground")
        
        runtimeSecurityMonitor.triggerCriticalEventCheck(
            RuntimeSecurityMonitor.CriticalEventType.APP_LAUNCH,
            "App moved to foreground",
            mapOf("lifecycle_event" to "onStart")
        )
    }
    
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "App moved to background")
        
        // Perform security check before going to background
        runtimeSecurityMonitor.triggerCriticalEventCheck(
            RuntimeSecurityMonitor.CriticalEventType.SETTINGS_CHANGE,
            "App moved to background - checking for security changes",
            mapOf("lifecycle_event" to "onStop")
        )
    }
    
    // Network monitoring
    
    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(TAG, "Network became available")
                
                runtimeSecurityMonitor.triggerCriticalEventCheck(
                    RuntimeSecurityMonitor.CriticalEventType.NETWORK_STATE_CHANGE,
                    "Network connection established",
                    mapOf("network_event" to "available", "network_id" to network.toString())
                )
            }
            
            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d(TAG, "Network connection lost")
                
                runtimeSecurityMonitor.triggerCriticalEventCheck(
                    RuntimeSecurityMonitor.CriticalEventType.NETWORK_STATE_CHANGE,
                    "Network connection lost",
                    mapOf("network_event" to "lost", "network_id" to network.toString())
                )
            }
            
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                Log.d(TAG, "Network capabilities changed")
                
                runtimeSecurityMonitor.triggerCriticalEventCheck(
                    RuntimeSecurityMonitor.CriticalEventType.NETWORK_STATE_CHANGE,
                    "Network capabilities changed",
                    mapOf(
                        "network_event" to "capabilities_changed",
                        "network_id" to network.toString(),
                        "has_internet" to networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    )
                )
            }
        }
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
    }
    
    private fun unregisterNetworkCallback() {
        networkCallback?.let { callback ->
            connectivityManager.unregisterNetworkCallback(callback)
            networkCallback = null
        }
    }
    
    // System event monitoring
    
    private fun registerSystemEventReceiver() {
        systemEventReceiver = SystemEventReceiver()
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        
        context.registerReceiver(systemEventReceiver, intentFilter)
    }
    
    private fun unregisterSystemEventReceiver() {
        systemEventReceiver?.let { receiver ->
            context.unregisterReceiver(receiver)
            systemEventReceiver = null
        }
    }
    
    // Activity lifecycle monitoring
    
    private fun registerActivityLifecycleCallbacks() {
        if (context is Application) {
            activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    Log.d(TAG, "Activity created: ${activity.javaClass.simpleName}")
                }
                
                override fun onActivityStarted(activity: Activity) {
                    Log.d(TAG, "Activity started: ${activity.javaClass.simpleName}")
                }
                
                override fun onActivityResumed(activity: Activity) {
                    Log.d(TAG, "Activity resumed: ${activity.javaClass.simpleName}")
                    
                    // Check for security threats when activities resume
                    runtimeSecurityMonitor.triggerCriticalEventCheck(
                        RuntimeSecurityMonitor.CriticalEventType.SETTINGS_CHANGE,
                        "Activity resumed - checking for security changes",
                        mapOf("activity" to activity.javaClass.simpleName)
                    )
                }
                
                override fun onActivityPaused(activity: Activity) {
                    Log.d(TAG, "Activity paused: ${activity.javaClass.simpleName}")
                }
                
                override fun onActivityStopped(activity: Activity) {
                    Log.d(TAG, "Activity stopped: ${activity.javaClass.simpleName}")
                }
                
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                    // No action needed
                }
                
                override fun onActivityDestroyed(activity: Activity) {
                    Log.d(TAG, "Activity destroyed: ${activity.javaClass.simpleName}")
                }
            }
            
            context.registerActivityLifecycleCallbacks(activityLifecycleCallbacks!!)
        }
    }
    
    private fun unregisterActivityLifecycleCallbacks() {
        if (context is Application) {
            activityLifecycleCallbacks?.let { callbacks ->
                context.unregisterActivityLifecycleCallbacks(callbacks)
                activityLifecycleCallbacks = null
            }
        }
    }
    
    /**
     * Manually trigger security check for sensitive operations.
     */
    fun triggerSensitiveOperationCheck(operationType: String, context: String = "") {
        Log.d(TAG, "Triggering security check for sensitive operation: $operationType")
        
        val eventType = when (operationType.lowercase()) {
            "login", "authentication" -> RuntimeSecurityMonitor.CriticalEventType.USER_LOGIN
            "payment", "purchase", "transaction" -> RuntimeSecurityMonitor.CriticalEventType.PAYMENT_INITIATED
            "data_access", "sensitive_data" -> RuntimeSecurityMonitor.CriticalEventType.SENSITIVE_DATA_ACCESS
            "permission" -> RuntimeSecurityMonitor.CriticalEventType.PERMISSION_GRANTED
            "storage" -> RuntimeSecurityMonitor.CriticalEventType.EXTERNAL_STORAGE_ACCESS
            else -> RuntimeSecurityMonitor.CriticalEventType.SETTINGS_CHANGE
        }
        
        runtimeSecurityMonitor.triggerCriticalEventCheck(
            eventType,
            "Manual trigger for sensitive operation: $operationType - $context",
            mapOf("operation_type" to operationType, "manual_trigger" to true)
        )
    }
    
    /**
     * Check if interceptor is currently active.
     */
    fun isActive(): Boolean = isInterceptorActive
    
    // Inner class for system event receiver
    
    private inner class SystemEventReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!isInterceptorActive) return
            
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Screen turned on")
                    runtimeSecurityMonitor.triggerCriticalEventCheck(
                        RuntimeSecurityMonitor.CriticalEventType.SCREEN_UNLOCK,
                        "Screen turned on"
                    )
                }
                
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen turned off")
                    runtimeSecurityMonitor.triggerCriticalEventCheck(
                        RuntimeSecurityMonitor.CriticalEventType.SETTINGS_CHANGE,
                        "Screen turned off - checking for security changes"
                    )
                }
                
                Intent.ACTION_USER_PRESENT -> {
                    Log.d(TAG, "User unlocked device")
                    runtimeSecurityMonitor.triggerCriticalEventCheck(
                        RuntimeSecurityMonitor.CriticalEventType.SCREEN_UNLOCK,
                        "User unlocked device"
                    )
                }
                
                Intent.ACTION_PACKAGE_ADDED -> {
                    val packageName = intent.data?.schemeSpecificPart
                    Log.d(TAG, "Package installed: $packageName")
                    runtimeSecurityMonitor.triggerCriticalEventCheck(
                        RuntimeSecurityMonitor.CriticalEventType.APP_INSTALL_DETECTED,
                        "New package installed: $packageName",
                        mapOf("package_name" to (packageName ?: "unknown"))
                    )
                }
                
                Intent.ACTION_PACKAGE_REMOVED -> {
                    val packageName = intent.data?.schemeSpecificPart
                    Log.d(TAG, "Package removed: $packageName")
                    runtimeSecurityMonitor.triggerCriticalEventCheck(
                        RuntimeSecurityMonitor.CriticalEventType.SETTINGS_CHANGE,
                        "Package removed: $packageName",
                        mapOf("package_name" to (packageName ?: "unknown"))
                    )
                }
                
                Intent.ACTION_PACKAGE_REPLACED -> {
                    val packageName = intent.data?.schemeSpecificPart
                    Log.d(TAG, "Package replaced: $packageName")
                    runtimeSecurityMonitor.triggerCriticalEventCheck(
                        RuntimeSecurityMonitor.CriticalEventType.APP_INSTALL_DETECTED,
                        "Package replaced: $packageName",
                        mapOf("package_name" to (packageName ?: "unknown"))
                    )
                }
            }
        }
    }
} 