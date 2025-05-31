package com.example.merlin.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.merlin.services.MerlinAccessibilityService

class ScreenStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ScreenStateReceiver", "Received action: ${intent.action}")

        val serviceIntent = Intent(context, MerlinAccessibilityService::class.java)

        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                Log.d("ScreenStateReceiver", "SCREEN_ON received, commanding service to bring app to foreground.")
                serviceIntent.action = MerlinAccessibilityService.ACTION_BRING_APP_TO_FOREGROUND
                // Consider using ContextCompat.startForegroundService if service will be foreground
                context.startService(serviceIntent)
            }
            Intent.ACTION_SCREEN_OFF -> {
                Log.d("ScreenStateReceiver", "SCREEN_OFF received.")
                // No action needed for screen off in the new sticky app approach
            }
            Intent.ACTION_USER_PRESENT -> {
                Log.d("ScreenStateReceiver", "USER_PRESENT received. Device unlocked.")
                // Bring app to foreground when device is unlocked
                serviceIntent.action = MerlinAccessibilityService.ACTION_BRING_APP_TO_FOREGROUND
                context.startService(serviceIntent)
            }
        }
    }
} 