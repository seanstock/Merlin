package com.example.merlin.security

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.merlin.R
import com.example.merlin.MainActivity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Secure lockout activity displayed when security threats are detected.
 * Requires PIN re-authentication and prevents bypassing security measures.
 */
class SecurityLockoutActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "SecurityLockoutActivity"
        private const val UPDATE_INTERVAL = 1000L // 1 second
    }
    
    private lateinit var threatTypeText: TextView
    private lateinit var lockoutReasonText: TextView
    private lateinit var countdownText: TextView
    private lateinit var lockoutCountText: TextView
    private lateinit var retryButton: Button
    private lateinit var exitButton: Button
    
    private var lockoutDuration: Long = 0
    private var lockoutStartTime: Long = 0
    private var lockoutCount: Int = 0
    private var threatType: SecurityThreat = SecurityThreat.ROOTED_DEVICE
    
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make this activity tamper-resistant
        setupSecureWindow()
        
        setContentView(R.layout.activity_security_lockout)
        
        initializeViews()
        extractIntentData()
        setupUI()
        startCountdownTimer()
        
        Log.w(TAG, "Security lockout activity started for threat: $threatType")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopCountdownTimer()
    }
    
    override fun onBackPressed() {
        // Prevent back button from bypassing security lockout
        Log.w(TAG, "Back button pressed - security lockout cannot be bypassed")
        // Do nothing - don't call super.onBackPressed()
    }
    
    override fun onPause() {
        super.onPause()
        // Prevent app from being paused to bypass security
        if (!isFinishing) {
            Log.w(TAG, "Attempt to pause security lockout activity - bringing back to front")
            val intent = Intent(this, SecurityLockoutActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
        }
    }
    
    private fun setupSecureWindow() {
        // Prevent screenshots and screen recording
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        // Keep screen on during lockout
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Make fullscreen and hide navigation
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        // Prevent task switching
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    }
    
    private fun initializeViews() {
        threatTypeText = findViewById(R.id.threat_type_text)
        lockoutReasonText = findViewById(R.id.lockout_reason_text)
        countdownText = findViewById(R.id.countdown_text)
        lockoutCountText = findViewById(R.id.lockout_count_text)
        retryButton = findViewById(R.id.retry_button)
        exitButton = findViewById(R.id.exit_button)
    }
    
    private fun extractIntentData() {
        threatType = SecurityThreat.valueOf(
            intent.getStringExtra("threat_type") ?: SecurityThreat.ROOTED_DEVICE.name
        )
        val lockoutReason = intent.getStringExtra("lockout_reason") ?: "Security threat detected"
        lockoutDuration = intent.getLongExtra("lockout_duration", 5 * 60 * 1000L)
        lockoutCount = intent.getIntExtra("lockout_count", 1)
        lockoutStartTime = System.currentTimeMillis()
        
        lockoutReasonText.text = lockoutReason
    }
    
    private fun setupUI() {
        // Set threat type display
        threatTypeText.text = when (threatType) {
            SecurityThreat.ROOTED_DEVICE -> "🔒 Root Access Detected"
            SecurityThreat.SAFE_MODE -> "⚠️ Safe Mode Active"
            SecurityThreat.ADB_ENABLED -> "🔧 USB Debugging Enabled"
            SecurityThreat.DEVELOPER_OPTIONS -> "⚙️ Developer Options Active"
            SecurityThreat.TAMPERED_APP -> "🚫 App Tampering Detected"
            SecurityThreat.DEBUGGER_ATTACHED -> "🐛 Debugger Attached"
        }
        
        // Set lockout count display
        lockoutCountText.text = "Security violation #$lockoutCount"
        
        // Setup button listeners
        retryButton.setOnClickListener {
            retrySecurityCheck()
        }
        
        exitButton.setOnClickListener {
            exitApplication()
        }
        
        // Initially disable retry button
        retryButton.isEnabled = false
    }
    
    private fun startCountdownTimer() {
        updateRunnable = object : Runnable {
            override fun run() {
                updateCountdown()
                handler.postDelayed(this, UPDATE_INTERVAL)
            }
        }
        handler.post(updateRunnable!!)
    }
    
    private fun stopCountdownTimer() {
        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = null
    }
    
    private fun updateCountdown() {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lockoutStartTime
        val remainingTime = lockoutDuration - elapsedTime
        
        if (remainingTime <= 0) {
            // Lockout period has ended
            onLockoutExpired()
        } else {
            // Update countdown display
            val minutes = (remainingTime / 1000) / 60
            val seconds = (remainingTime / 1000) % 60
            countdownText.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            
            // Enable retry button in the last 30 seconds
            retryButton.isEnabled = remainingTime <= 30000
        }
    }
    
    private fun onLockoutExpired() {
        Log.i(TAG, "Security lockout period expired")
        stopCountdownTimer()
        
        countdownText.text = "00:00"
        retryButton.isEnabled = true
        retryButton.text = "Security Check Complete - Retry"
        
        // Automatically retry security check
        handler.postDelayed({
            retrySecurityCheck()
        }, 2000)
    }
    
    private fun retrySecurityCheck() {
        Log.i(TAG, "Retrying security check")
        
        val securityManager = SecurityManager(this)
        val threat = securityManager.enforceSecurityMeasures()
        
        if (threat == null) {
            // Security check passed, return to main app
            Log.i(TAG, "Security check passed, returning to main app")
            returnToMainApp()
        } else {
            // Security threat still present
            Log.w(TAG, "Security threat still present: $threat")
            showSecurityStillActive(threat)
        }
    }
    
    private fun showSecurityStillActive(threat: SecurityThreat) {
        // Update UI to show that security threat is still active
        threatTypeText.text = "⚠️ Security Threat Still Active"
        lockoutReasonText.text = "The security issue has not been resolved. Please address the security concern and try again."
        
        // Reset countdown for another lockout period
        lockoutStartTime = System.currentTimeMillis()
        lockoutCount++
        lockoutCountText.text = "Security violation #$lockoutCount"
        
        retryButton.isEnabled = false
        retryButton.text = "Retry Security Check"
        
        startCountdownTimer()
    }
    
    private fun returnToMainApp() {
        Log.i(TAG, "Returning to main application")
        
        // Clear the security lockout state
        val securityResponseManager = SecurityResponseManager(this)
        securityResponseManager.clearSecurityLockout()
        
        // Return to main activity
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }
    
    private fun exitApplication() {
        Log.i(TAG, "User chose to exit application due to security lockout")
        
        // Close the application completely
        finishAffinity()
        System.exit(0)
    }
} 