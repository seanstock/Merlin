package com.example.merlin

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.merlin.ui.UIVariant
import com.example.merlin.ui.UiVariantState
import com.example.merlin.ui.SimpleMainMenuScreen
import com.example.merlin.ui.MainViewModel
import com.example.merlin.security.SecurityManager
import com.example.merlin.security.SecurityResponseManager
import com.example.merlin.security.SecurityThreat
import com.example.merlin.security.RuntimeSecurityMonitor
import com.example.merlin.security.SecurityEventInterceptor
import com.example.merlin.ui.chat.ChatScreen
import com.example.merlin.ui.game.GameScreen
import com.example.merlin.ui.settings.SettingsScreen
import com.example.merlin.ui.onboarding.OnboardingFlow
import com.example.merlin.ui.onboarding.OnboardingViewModel
import com.example.merlin.ui.theme.MerlinTheme
import com.example.merlin.ui.theme.*
import com.example.merlin.data.database.DatabaseProvider
import com.example.merlin.data.repository.ChildProfileRepository
import com.example.merlin.data.repository.ParentSettingsRepository
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.*
import com.example.merlin.ui.accessibility.AccessibilityConstants
import com.example.merlin.ui.chat.PinExitDialog
import com.example.merlin.utils.PinAuthenticationService
import kotlin.system.exitProcess
import com.example.merlin.utils.UserSessionRepository
import com.example.merlin.config.ServiceLocator
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import com.example.merlin.ui.parent.ParentDashboardScreen
import com.example.merlin.ui.settings.ChildProfileScreen

class MainActivity : ComponentActivity(), 
    SecurityResponseManager.SecurityLockoutCallback,
    RuntimeSecurityMonitor.SecurityEventCallback {

    companion object {
        private const val TAG = "MainActivity"
        private const val PREF_PROPER_EXIT = "proper_exit_flag"
    }

    private val REQUEST_CODE_SYSTEM_ALERT_WINDOW = 101

    private lateinit var securityManager: SecurityManager
    private lateinit var securityResponseManager: SecurityResponseManager
    private lateinit var runtimeSecurityMonitor: RuntimeSecurityMonitor
    private lateinit var securityEventInterceptor: SecurityEventInterceptor
    private lateinit var userSessionRepository: UserSessionRepository
    
    // Screen time tracking
    
    private val screenTimeService by lazy { ServiceLocator.getScreenTimeService(this) }
    
    // Flag to track if we need to enable immersive mode when window is ready
    private var shouldEnableImmersiveMode = false
    
    // Flag to track if we're in the middle of a proper exit sequence
    private var isExitingProperly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if we're coming back from a proper exit - if so, don't auto-reinitialize protections
        val prefs = getSharedPreferences("merlin_state", Context.MODE_PRIVATE)
        val wasProperExit = prefs.getBoolean(PREF_PROPER_EXIT, false)
        
        if (wasProperExit) {
            // Clear the flag and use minimal initialization
            prefs.edit().putBoolean(PREF_PROPER_EXIT, false).apply()
            Log.d(TAG, "Returning from proper exit - minimal initialization")
            
            // Still do basic setup but without aggressive protections
            enableEdgeToEdge()
            setContent {
                MerlinTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        MerlinApp(
                            modifier = Modifier.padding(innerPadding),
                            showReEnableProtection = true
                        )
                    }
                }
            }
            return
        }
        
        // Normal startup - make the app sticky by setting window flags
        setupStickyWindow()
        
        // Initialize security components
        initializeSecurity()
        
        // Check for security threats before proceeding
        if (checkSecurityThreats()) {
            // Security threat detected, SecurityResponseManager will handle the response
            return
        }
        
        enableEdgeToEdge()
        setContent {
            MerlinTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MerlinApp(
                        modifier = Modifier.padding(innerPadding),
                        showReEnableProtection = false
                    )
                }
            }
        }

        // Delay lock task to avoid immediate "App is Pinned" popup issues
        window.decorView.post {
            // Give the UI time to fully initialize before enabling lock task
            window.decorView.postDelayed({
                attemptLockTaskIfNeeded()
            }, 2000) // 2 second delay
        }

        if (!checkSystemAlertWindowPermission(this)) {
            requestSystemAlertWindowPermission(this, REQUEST_CODE_SYSTEM_ALERT_WINDOW)
        }
        
        // Trigger critical event for app launch
        securityEventInterceptor.triggerSensitiveOperationCheck("app_launch", "MainActivity onCreate")
    }

    /**
     * Set up window flags to make the app harder to dismiss and more sticky
     */
    private fun setupStickyWindow() {
        // Enhanced window flags for maximum stickiness
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        
        // Prevent the app from being easily dismissed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        
        // Mark that we want immersive mode, but don't enable it yet
        // (window decorView might not be ready in onCreate)
        shouldEnableImmersiveMode = true
        
        // Set up listener to re-enable immersive mode if system UI appears
        setupSystemUiVisibilityListener()
        
        Log.d(TAG, "Enhanced sticky window flags applied - immersive mode will be enabled when window is ready")
    }

    /**
     * Enable immersive mode to hide system navigation and prevent gesture navigation
     */
    private fun enableImmersiveMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ approach
                window.insetsController?.let { controller ->
                    window.setDecorFitsSystemWindows(false)
                    controller.hide(
                        android.view.WindowInsets.Type.statusBars() or
                        android.view.WindowInsets.Type.navigationBars() or
                        android.view.WindowInsets.Type.systemGestures()
                    )
                    controller.systemBarsBehavior = 
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    Log.d(TAG, "Immersive mode enabled using Android 11+ API")
                } ?: run {
                    Log.w(TAG, "Window insetsController not available yet, will retry later")
                    return
                }
            } else {
                // Legacy approach for older Android versions
                val decorView = window.decorView
                if (decorView != null) {
                    @Suppress("DEPRECATION")
                    decorView.systemUiVisibility = (
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                        android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )
                    Log.d(TAG, "Immersive mode enabled using legacy API")
                } else {
                    Log.w(TAG, "Window decorView not available yet, will retry later")
                    return
                }
            }
            
            // Mark that immersive mode has been successfully enabled
            shouldEnableImmersiveMode = false
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable immersive mode: ${e.message}", e)
        }
    }

    /**
     * Re-enable immersive mode when system UI becomes visible
     */
    private fun setupSystemUiVisibilityListener() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            try {
                window.decorView?.setOnSystemUiVisibilityChangeListener { visibility ->
                    // If any system UI becomes visible, hide it again
                    if (visibility and android.view.View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                        enableImmersiveMode()
                        Log.d(TAG, "System UI became visible - re-enabling immersive mode")
                    }
                } ?: run {
                    Log.w(TAG, "DecorView not available yet, system UI visibility listener will be set later")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set system UI visibility listener: ${e.message}", e)
            }
        }
    }

    /**
     * Override back button to prevent easy exit from the app
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        // Custom back press handling can be added here if needed
    }

    override fun onResume() {
        super.onResume()
        
        // Start screen time tracking
                        screenTimeService.startSession()
        
        // Don't do aggressive reinitialization if we're exiting properly
        if (isExitingProperly) {
            Log.d(TAG, "In proper exit sequence - skipping aggressive resume actions")
            return
        }
        
        // Check for security threats when app resumes
        if (checkSecurityThreats()) {
            // Security threat detected, SecurityResponseManager will handle the response
            return
        }
        
        // Start comprehensive security monitoring
        startSecurityMonitoring()
        
        // Enable immersive mode if we haven't done so yet, or re-enable it
        if (shouldEnableImmersiveMode && !isExitingProperly) {
            enableImmersiveMode()
        } else if (!isExitingProperly) {
            // Re-enable immersive mode in case it was disabled
            enableImmersiveMode()
        }
        
        // Trigger critical event for app resume
        securityEventInterceptor.triggerSensitiveOperationCheck("app_resume", "MainActivity onResume")
        
        Log.d(TAG, "MainActivity resumed with comprehensive security monitoring active")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        
        // Don't be aggressive about regaining focus if we're exiting properly
        if (isExitingProperly) {
            Log.d(TAG, "In proper exit sequence - not forcing focus")
            return
        }
        
        if (hasFocus) {
            // Enable immersive mode if we haven't done so yet or if we need to re-enable it
            if (shouldEnableImmersiveMode) {
                enableImmersiveMode()
            } else {
                // Re-enable immersive mode when we regain focus, but with a small delay
                // to avoid conflicts with system dialogs like "App is Pinned"
                window.decorView.postDelayed({
                    if (!isExitingProperly) {
                        enableImmersiveMode()
                    }
                }, 500)
            }
            Log.d(TAG, "Window focus regained - immersive mode enabled/re-enabled")
        } else {
            Log.d(TAG, "Window focus lost - user may be trying to navigate away")
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        
        // Don't try to prevent leaving if we're exiting properly
        if (isExitingProperly) {
            Log.d(TAG, "Proper exit in progress - allowing user to leave")
            return
        }
        
        // User pressed home or used recent apps - try to bring back to foreground
        Log.d(TAG, "User leave hint detected - user trying to navigate away")
        
        // Re-enable immersive mode aggressively (but only if not exiting properly)
        enableImmersiveMode()
    }

    override fun onPause() {
        super.onPause()
        
        // Stop screen time tracking and save session
        lifecycleScope.launch {
            val childId = userSessionRepository.getActiveChildId()
            if (childId != null) {
                screenTimeService.stopSession(childId)
            }
        }
        
        // Stop security monitoring when app is paused (but keep basic monitoring)
        stopSecurityMonitoring()
        
        Log.d(TAG, "MainActivity paused, active security monitoring stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up all security components
        cleanupSecurity()
        
        Log.d(TAG, "MainActivity destroyed, all security components cleaned up")
    }

    /**
     * Initialize all security components and register callbacks.
     */
    private fun initializeSecurity() {
        Log.d(TAG, "Initializing comprehensive security system")
        
        securityManager = SecurityManager(this)
        securityResponseManager = SecurityResponseManager(this)
        runtimeSecurityMonitor = RuntimeSecurityMonitor(this)
        securityEventInterceptor = SecurityEventInterceptor(this, runtimeSecurityMonitor)
        userSessionRepository = UserSessionRepository.getInstance(applicationContext)
        
        // Register callbacks
        securityResponseManager.registerLockoutCallback("MainActivity", this)
        runtimeSecurityMonitor.registerCallback("MainActivity", this)
        
        Log.d(TAG, "Comprehensive security system initialized successfully")
    }

    /**
     * Start comprehensive security monitoring.
     */
    private fun startSecurityMonitoring() {
        Log.d(TAG, "Starting comprehensive security monitoring")
        
        // Start runtime security monitoring
        runtimeSecurityMonitor.startMonitoring()
        
        // Start security event interception
        securityEventInterceptor.startIntercepting()
        
        // Start the existing security response monitoring
        securityResponseManager.startSecurityMonitoring(securityManager)
        
        Log.d(TAG, "Comprehensive security monitoring started")
    }

    /**
     * Stop active security monitoring (but keep basic monitoring).
     */
    private fun stopSecurityMonitoring() {
        Log.d(TAG, "Stopping active security monitoring")
        
        // Stop the existing security response monitoring
        securityResponseManager.stopSecurityMonitoring()
        
        // Note: We keep runtime monitoring and event interception active
        // as they provide background protection
        
        Log.d(TAG, "Active security monitoring stopped")
    }

    /**
     * Clean up all security components.
     */
    private fun cleanupSecurity() {
        Log.d(TAG, "Cleaning up security components")
        
        // Unregister callbacks
        securityResponseManager.unregisterLockoutCallback("MainActivity")
        runtimeSecurityMonitor.unregisterCallback("MainActivity")
        
        // Stop all monitoring
        securityResponseManager.stopSecurityMonitoring()
        runtimeSecurityMonitor.stopMonitoring()
        securityEventInterceptor.stopIntercepting()
        
        Log.d(TAG, "Security components cleanup completed")
    }

    /**
     * Check for security threats and respond appropriately.
     * @return true if a security threat was detected and handled, false otherwise
     */
    private fun checkSecurityThreats(): Boolean {
        Log.d(TAG, "Performing security threat check")
        
        // Check if already in security lockout
        if (securityResponseManager.isInSecurityLockout()) {
            Log.w(TAG, "Already in security lockout, redirecting to lockout activity")
            val lockoutInfo = securityResponseManager.getCurrentLockoutInfo()
            if (lockoutInfo != null) {
                securityResponseManager.launchSecureLockoutActivity(lockoutInfo)
            }
            finish()
            return true
        }
        
        // Perform security checks
        val threat = securityManager.enforceSecurityMeasures()
        if (threat != null) {
            Log.w(TAG, "Security threat detected: $threat")
            
            // Respond to the threat
            val lockoutInfo = securityResponseManager.respondToThreat(threat)
            Log.w(TAG, "Security lockout activated: ${lockoutInfo.reason}")
            
            // The SecurityResponseManager will launch the lockout activity
            finish()
            return true
        }
        
        Log.d(TAG, "No security threats detected")
        return false
    }

    // SecurityLockoutCallback implementation
    override fun onSecurityLockoutTriggered(threat: SecurityThreat, lockoutInfo: SecurityResponseManager.LockoutInfo) {
        Log.w(TAG, "Security lockout triggered: $threat")
        
        runOnUiThread {
            Toast.makeText(
                this,
                "Security threat detected: ${threat.name}. App will be locked.",
                Toast.LENGTH_LONG
            ).show()
            
            // Finish this activity as the lockout activity will be launched
            finish()
        }
    }

    override fun onSecurityLockoutCleared() {
        Log.i(TAG, "Security lockout cleared")
        
        runOnUiThread {
            Toast.makeText(
                this,
                "Security lockout cleared. Welcome back!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // RuntimeSecurityMonitor.SecurityEventCallback implementation
    override fun onSecurityCheckCompleted(result: RuntimeSecurityMonitor.SecurityCheckResult) {
        Log.d(TAG, "Runtime security check completed: ${result.checkType} - Threat: ${result.threat}")
        
        if (result.threat != null) {
            Log.w(TAG, "Runtime security check detected threat: ${result.threat}")
            // The RuntimeSecurityMonitor will handle the threat response
        }
    }

    override fun onCriticalEventDetected(event: RuntimeSecurityMonitor.CriticalSecurityEvent) {
        Log.d(TAG, "Critical security event detected: ${event.eventType} - Level: ${event.securityLevel}")
        
        // Log critical events for monitoring
        if (event.securityLevel == RuntimeSecurityMonitor.SecurityLevel.CRITICAL) {
            Log.w(TAG, "CRITICAL security event: ${event.eventType} - ${event.context}")
        }
    }

    override fun onTamperAttemptDetected(attempt: RuntimeSecurityMonitor.TamperAttempt) {
        Log.w(TAG, "Tamper attempt detected: ${attempt.attemptType} - Severity: ${attempt.severity}")
        
        runOnUiThread {
            if (attempt.severity == RuntimeSecurityMonitor.TamperSeverity.HIGH || 
                attempt.severity == RuntimeSecurityMonitor.TamperSeverity.CRITICAL) {
                Toast.makeText(
                    this,
                    "Security violation detected: ${attempt.attemptType}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun checkSystemAlertWindowPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            // On versions older than M, if the permission is in the manifest, it's granted at install time.
            // However, SYSTEM_ALERT_WINDOW behavior was significantly different and less controlled pre-M.
            // For simplicity and focus on modern Android, we assume it's effectively available if declared.
            true
        }
    }

    private fun requestSystemAlertWindowPermission(activity: Activity, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(activity)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + activity.packageName)
                )
                activity.startActivityForResult(intent, requestCode)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SYSTEM_ALERT_WINDOW) {
            // It's important to re-check the permission status here, as resultCode is not reliable for this permission.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // Permission granted
                    Toast.makeText(this, "SYSTEM_ALERT_WINDOW permission granted.", Toast.LENGTH_SHORT).show()
                    // Trigger security check for permission granted
                    securityEventInterceptor.triggerSensitiveOperationCheck("permission", "SYSTEM_ALERT_WINDOW granted")
                } else {
                    // Permission denied
                    Toast.makeText(this, "SYSTEM_ALERT_WINDOW permission denied. Overlay functionality will be limited.", Toast.LENGTH_LONG).show()
                    // Trigger security check for permission denied
                    securityEventInterceptor.triggerSensitiveOperationCheck("permission", "SYSTEM_ALERT_WINDOW denied")
                }
            }
        }
    }

    private fun attemptLockTaskIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                // Don't enable lock task if we're in the middle of exiting
                if (isExitingProperly) {
                    Log.d(TAG, "Skipping lock task - proper exit in progress")
                    return
                }
                
                val activeChildId = userSessionRepository.getActiveChildId()
                if (activeChildId != null) {
                    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    if (activityManager.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                        startLockTask()
                        Log.d(TAG, "Task lock enabled - onboarding complete, app cannot be swiped away")
                    }
                } else {
                    // Onboarding incomplete – do NOT lock task to allow permissions screens
                    Log.d(TAG, "Onboarding not complete - skipping task lock")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enable task lock: ${e.message}")
            }
        }
    }
    
    /**
     * Properly exit the app by cleaning up lock task mode and setting exit flag
     */
    fun exitAppProperly() {
        Log.d(TAG, "Starting proper app exit sequence")
        isExitingProperly = true
        
        // Set flag to prevent aggressive reinitialization on next startup
        val prefs = getSharedPreferences("merlin_state", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_PROPER_EXIT, true).apply()
        
        // Stop lock task mode if active
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                if (activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
                    stopLockTask()
                    Log.d(TAG, "Lock task mode stopped")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop lock task: ${e.message}")
            }
        }
        
        // Signal accessibility service to stop aggressive behavior
        try {
            val serviceIntent = Intent(this, com.example.merlin.services.MerlinAccessibilityService::class.java)
            serviceIntent.action = "DISABLE_AGGRESSIVE_MODE"
            startService(serviceIntent)
            Log.d(TAG, "Signaled accessibility service to disable aggressive mode")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to signal accessibility service: ${e.message}")
        }
        
        // Clean up security components
        try {
            cleanupSecurity()
        } catch (e: Exception) {
            Log.w(TAG, "Error during security cleanup: ${e.message}")
        }
        
        // Exit the app
        finishAndRemoveTask()
        exitProcess(0)
    }

    /**
     * Re-enable sticky behavior after proper exit (for parents who want to use the app again)
     */
    fun reEnableStickyBehavior() {
        Log.d(TAG, "Re-enabling sticky behavior at parent request")
        
        // Clear the proper exit flag
        val prefs = getSharedPreferences("merlin_state", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_PROPER_EXIT, false).apply()
        
        // Signal accessibility service to re-enable aggressive mode
        try {
            val serviceIntent = Intent(this, com.example.merlin.services.MerlinAccessibilityService::class.java)
            serviceIntent.action = "ENABLE_AGGRESSIVE_MODE"
            startService(serviceIntent)
            Log.d(TAG, "Signaled accessibility service to enable aggressive mode")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to signal accessibility service: ${e.message}")
        }
        
        // Set up sticky window behavior
        setupStickyWindow()
        
        // Initialize security components
        initializeSecurity()
        
        // Start security monitoring
        startSecurityMonitoring()
        
        // Enable lock task if needed
        attemptLockTaskIfNeeded()
        
        Log.d(TAG, "Sticky behavior re-enabled successfully")
    }

    private fun updateScreenTime() {
        // ... existing code ...
    }
}

@Composable
fun MerlinApp(modifier: Modifier = Modifier, showReEnableProtection: Boolean = false) {
    val context = LocalContext.current
    val onboardingViewModel: OnboardingViewModel = viewModel()
    var showOnboarding by remember { mutableStateOf(true) }
    var showReEnableDialog by remember { mutableStateOf(showReEnableProtection) }

    // Setup Repositories for OnboardingViewModel
    LaunchedEffect(onboardingViewModel) {
        val database = DatabaseProvider.getInstance(context.applicationContext)
        val childProfileRepo = ChildProfileRepository(database.childProfileDao())
        val parentSettingsRepo = ParentSettingsRepository(database.parentSettingsDao())
        onboardingViewModel.setRepositories(childProfileRepo, parentSettingsRepo)
        
        // Now check onboarding status after repositories are set
        showOnboarding = !onboardingViewModel.isOnboardingCompleted()
    }
    
    if (showOnboarding) {
        OnboardingFlow(
            onComplete = {
                showOnboarding = false
            },
            modifier = modifier
        )
    } else {
        MerlinMainScreen(modifier = modifier)
    }
    
    // Show re-enable protection dialog if needed
    if (showReEnableDialog) {
        ReEnableProtectionDialog(
            onReEnable = {
                showReEnableDialog = false
                (context as? MainActivity)?.reEnableStickyBehavior()
            },
            onDismiss = {
                showReEnableDialog = false
            }
        )
    }
}

@Composable
fun MerlinMainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf("main") }
    var selectedGameId by remember { mutableStateOf<String?>(null) }
    var selectedGameLevel by remember { mutableIntStateOf(1) }

    // UI variant via MainViewModel
    val mainViewModel: MainViewModel = viewModel()
    // Observe the new state holder
    val uiVariantState by mainViewModel.uiVariantState.collectAsState()
    
    // PIN authentication state for settings access
    var showSettingsPinDialog by remember { mutableStateOf(false) }
    val pinAuthService = remember { PinAuthenticationService(context) }
    
    // Handle settings access request - show PIN dialog
    val handleSettingsRequest = {
        showSettingsPinDialog = true
    }
    
    // Handle successful PIN verification - go to settings
    val handleSettingsPinVerified = {
        showSettingsPinDialog = false
        currentScreen = "settings"
    }
    
    // Handle PIN dialog dismissal or failed attempt - stay on main screen
    val handleSettingsPinDismiss = {
        showSettingsPinDialog = false
        // Stay on main screen - this helps toddlers find their way home
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiVariantState) {
            is UiVariantState.Loading -> {
                // Show a full-screen loading indicator
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiVariantState.Error -> {
                // Show an error message
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}")
                }
            }
            is UiVariantState.Success -> {
                val uiVariant = state.variant
                // The existing when block for showing the correct screen
        when (currentScreen) {
            "main" -> {
                        when (uiVariant) {
                            UIVariant.SIMPLE -> {
                                SimpleMainMenuScreen(
                                    onNavigateToGames = { gameId -> 
                                        selectedGameId = gameId
                                        selectedGameLevel = 1
                                        currentScreen = "games" 
                                    },
                                    onNavigateToChat = { currentScreen = "chat" },
                                    onSpendCoins = { /* TODO: Handle spend coins */ },
                                    onNavigateToSettings = handleSettingsRequest,
                                    modifier = modifier
                                )
                            }
                            UIVariant.ADVANCED -> {
                                AdvancedMainMenuScreen(
                    onNavigateToChat = { currentScreen = "chat" },
                    onNavigateToGames = { currentScreen = "games" },
                    onNavigateToSettings = handleSettingsRequest,
                    modifier = modifier
                )
                            }
                        }
            }
            "chat" -> {
                ChatScreen(
                    onNavigateBack = { currentScreen = "main" },
                    onNavigateToSettings = handleSettingsRequest,
                    onLaunchGame = { gameId, level ->
                        selectedGameId = gameId
                        selectedGameLevel = level
                        currentScreen = "games"
                    },
                    modifier = modifier
                )
            }
            "games" -> {
                GameScreen(
                    onNavigateBack = { currentScreen = "main" },
                            modifier = modifier,
                            gameId = selectedGameId
                )
            }

            "settings" -> {
                SettingsScreen(
                    onNavigateBack = { currentScreen = "main" },
                    onExitApp = {
                        // Use proper exit method instead of direct exitProcess
                        (context as? MainActivity)?.exitAppProperly()
                            ?: exitProcess(0) // Fallback if context is not MainActivity
                    }
                )
            }
                }
            }
        }
        
        // PIN authentication dialog for settings access
        if (showSettingsPinDialog) {
            PinExitDialog(
                isVisible = showSettingsPinDialog,
                onDismiss = handleSettingsPinDismiss,
                onPinVerified = handleSettingsPinVerified,
                onPinVerification = { enteredPin ->
                    pinAuthService.verifyPin(enteredPin)
                },
                maxAttempts = 1 // Close after one failed attempt to help toddlers
            )
        }
    }
}

@Composable
fun ReEnableProtectionDialog(
    onReEnable: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🔒 Re-enable Protection?",
                style = AppleHeadline,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                Text(
                    text = "Merlin was safely exited. Would you like to re-enable child protection mode?",
                    style = AppleBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(AppleSpacing.medium))
                Text(
                    text = "• App will become sticky again\n• Screen protection will be active\n• Child-safe environment will be enforced",
                    style = AppleFootnote,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            AppleButton(
                text = "Enable Protection",
                onClick = onReEnable,
                style = AppleButtonStyle.Primary
            )
        },
        dismissButton = {
            AppleButton(
                text = "Stay Normal",
                onClick = onDismiss,
                style = AppleButtonStyle.Secondary
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedMainMenuScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToGames: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ✨ REFINED MAGICAL ANIMATIONS ✨
    val infiniteTransition = rememberInfiniteTransition(label = "subtle_animation")
    
    // Gentle floating animation instead of pulsing
    val gentleFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gentle_float"
    )
    
    // Subtle gradient rotation
    val gradientRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_rotation"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleSystemBackground)
    ) {
        // Apple-style settings icon in top right
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(AppleSpacing.medium)
                .size(44.dp) // Apple's recommended touch target
                .background(
                    color = AppleGray5,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = AccessibilityConstants.ContentDescriptions.SETTINGS,
                tint = AppleBlue,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppleSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppleSpacing.large)
        ) {
            Spacer(modifier = Modifier.height(AppleSpacing.xxl))
            
            // Apple-style welcome section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(AppleSpacing.medium)
            ) {
                Text(
                    text = "🧙‍♂️",
                    fontSize = 72.sp,
                    modifier = Modifier.padding(bottom = AppleSpacing.medium)
                )
                Text(
                    text = "Merlin AI",
                    style = AppleLargeTitle,
                    color = ApplePrimaryLabel,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Your intelligent learning companion",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppleSecondaryLabel,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = AppleSpacing.xs)
                )
            }
            
            Spacer(modifier = Modifier.height(AppleSpacing.large))
            
            // Apple-style navigation buttons
            AppleCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToChat() }
                    .semantics {
                        contentDescription = "Chat with Merlin AI"
                        role = Role.Button
                    },
                elevation = 1,
                cornerRadius = 16
            ) {
                AppleListItem(
                    title = "Chat with Merlin",
                    subtitle = "Ask questions and explore ideas",
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = AppleBlue.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(AppleCornerRadius.medium)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "💬",
                                fontSize = 24.sp
                            )
                        }
                    },
                    trailing = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = AppleGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
            
            AppleCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToGames() }
                    .semantics {
                        contentDescription = "Educational Games"
                        role = Role.Button
                    },
                elevation = 1,
                cornerRadius = 16
            ) {
                AppleListItem(
                    title = "Educational Games",
                    subtitle = "Interactive learning experiences",
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = ApplePurple.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(AppleCornerRadius.medium)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🎮",
                                fontSize = 24.sp
                            )
                        }
                    },
                    trailing = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = AppleGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MerlinTheme {
        MerlinApp()
    }
}