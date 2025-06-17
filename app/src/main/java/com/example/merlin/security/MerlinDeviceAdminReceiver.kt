package com.example.merlin.security

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Device Owner receiver to enable kiosk-mode policies.
 * Provision the APK as device owner with:
 *   adb shell dpm set-device-owner com.example.merlin/.security.MerlinDeviceAdminReceiver
 */
class MerlinDeviceAdminReceiver : DeviceAdminReceiver() {

    private val tag = "MerlinDeviceAdminReceiver"

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(tag, "Merlin set as Device Owner (onEnabled)")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence? {
        Log.w(tag, "Disable requested, ignoring (kiosk mode)")
        // Return a warning string – device owner cannot actually be removed by user.
        return "Merlin is running in kiosk mode and cannot be disabled."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.i(tag, "Device Admin disabled – this should not happen in kiosk mode")
    }
} 