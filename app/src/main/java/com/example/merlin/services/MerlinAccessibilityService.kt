package com.example.merlin.services

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.example.merlin.MainActivity
import com.example.merlin.utils.UserSessionRepository

class MerlinAccessibilityService : AccessibilityService() {

    private lateinit var userSessionRepository: UserSessionRepository

    companion object {
        const val ACTION_BRING_APP_TO_FOREGROUND = "com.example.merlin.ACTION_BRING_APP_TO_FOREGROUND"
        private const val TAG = "MerlinAccessibilityService"
        private const val FG_NOTIFICATION_CHANNEL_ID = "MERLIN_ACCESSIBILITY_SERVICE_CHANNEL"
        private const val FG_NOTIFICATION_CHANNEL_NAME = "Merlin Service"
        private const val ONGOING_NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        userSessionRepository = UserSessionRepository.getInstance(this)

        // Create notification channel for foreground service
        createNotificationChannelIfNeeded()
        startForeground(ONGOING_NOTIFICATION_ID, buildOngoingNotification())
        Log.d("MerlinAccessibilityService", "Service connected and running in foreground.")
        
        Log.d("MerlinAccessibilityService", "Service ready to monitor app switches and maintain Merlin app foreground.")
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existingChannel = notificationManager.getNotificationChannel(FG_NOTIFICATION_CHANNEL_ID)
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    FG_NOTIFICATION_CHANNEL_ID,
                    FG_NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notification channel for Merlin Accessibility Service."
                    enableLights(false)
                    enableVibration(false)
                    setShowBadge(false)
                }
                notificationManager.createNotificationChannel(channel)
                Log.d("MerlinAccessibilityService", "Notification channel created.")
            } else {
                Log.d("MerlinAccessibilityService", "Notification channel already exists.")
            }
        }
    }

    private fun buildOngoingNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)

        val smallIcon = android.R.drawable.ic_dialog_info

        return NotificationCompat.Builder(this, FG_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Merlin Active")
            .setContentText("Merlin is protecting your device and keeping the learning environment safe.")
            .setSmallIcon(smallIcon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val eventType = event?.eventType
        val eventPackageName = event?.packageName?.toString() ?: "Unknown"
        val eventClassName = event?.className?.toString() ?: "Unknown"

        Log.d("MerlinAccessibilityService", "onAccessibilityEvent: type=$eventType, pkg=$eventPackageName, class=$eventClassName")

        when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val currentActivityPackage = eventPackageName

                if (currentActivityPackage == this.packageName) {
                    // Our app is in the foreground - all good!
                    Log.d("MerlinAccessibilityService", "Merlin app is in foreground. No action needed.")
                } else {
                    // Another app is in the foreground
                    // Only bring Merlin to foreground if onboarding is complete and it's not an IME
                    val activeChildId = userSessionRepository.getActiveChildId()
                    if (activeChildId != null) {
                        // Onboarding is complete, bring Merlin app to foreground
                        if (!isImePackage(currentActivityPackage) && !isSystemUiPackage(currentActivityPackage)) {
                            Log.i("MerlinAccessibilityService", "External app [$currentActivityPackage] detected. Bringing Merlin to foreground.")
                            bringMerlinToForeground()
                        } else {
                            Log.d("MerlinAccessibilityService", "System UI or IME is active. Allowing usage.")
                        }
                    } else {
                        // Onboarding is not complete, allow other apps during setup
                        Log.d("MerlinAccessibilityService", "Onboarding not complete. Allowing external app usage.")
                    }
                }
            }
            
            // Monitor for recent apps/task switcher events
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                val activeChildId = userSessionRepository.getActiveChildId()
                if (activeChildId != null) {
                    // Aggressively bring app back to foreground on any window change
                    Log.d("MerlinAccessibilityService", "Window change detected - ensuring Merlin is foreground")
                    bringMerlinToForeground()
                }
            }
            
            // Monitor for notification panel or system UI interactions
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (eventPackageName.startsWith("com.android.systemui")) {
                    val activeChildId = userSessionRepository.getActiveChildId()
                    if (activeChildId != null) {
                        Log.d("MerlinAccessibilityService", "System UI interaction detected - bringing Merlin to foreground")
                        bringMerlinToForeground()
                    }
                }
            }
        }
    }

    // Helper function to check for common IME package names
    private fun isImePackage(packageName: String?): Boolean {
        return packageName != null && 
               (packageName.startsWith("com.google.android.inputmethod") || // Gboard
                packageName.startsWith("com.android.inputmethod")) // Common prefix for other IMEs
    }
    
    // Helper function to check for system UI packages that should be allowed briefly
    private fun isSystemUiPackage(packageName: String?): Boolean {
        return packageName != null && 
               (packageName == "com.android.systemui" ||
                packageName == "android" ||
                packageName.startsWith("com.android.launcher") ||
                packageName.startsWith("com.google.android.apps.nexuslauncher"))
    }

    private fun bringMerlinToForeground() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
            }
            startActivity(intent)
            Log.d("MerlinAccessibilityService", "Successfully brought Merlin app to foreground.")
        } catch (e: Exception) {
            Log.e("MerlinAccessibilityService", "Error bringing Merlin to foreground: ${e.message}", e)
        }
    }

    override fun onInterrupt() {
        Log.d("MerlinAccessibilityService", "onInterrupt called.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MerlinAccessibilityService", "onDestroy called. Stopping foreground service.")
        
        stopForeground(STOP_FOREGROUND_REMOVE) // Use the new constant for clarity
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_BRING_APP_TO_FOREGROUND) {
            bringMerlinToForeground()
        }
        return START_STICKY
    }
} 