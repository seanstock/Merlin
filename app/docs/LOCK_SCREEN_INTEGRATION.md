# Sticky Main App Integration and UI Implementation Documentation

## Overview

This document provides comprehensive documentation for the Merlin AI Tutor's sticky main app integration and magical UI implementation. The system has been completely refactored from the previous translucent overlay approach to a simpler, more secure sticky main app architecture that provides seamless educational interaction while maintaining robust security and usability.

## Architecture Overview

### Major Architectural Refactor

**Previous Approach (Removed):**
- ❌ Translucent chat overlay via MerlinAccessibilityService
- ❌ Complex WindowManager and ComposeView lifecycle management
- ❌ Dual chat interfaces causing conversation sync issues

**Current Approach (Implemented):**
- ✅ Sticky main app with enhanced window flags
- ✅ Simplified MerlinAccessibilityService for app switching monitoring
- ✅ Single unified chat interface
- ✅ PIN-gated settings system for secure exit

### Core Integration Components

1. **MainActivity** - Enhanced with sticky window flags and lifecycle protection
2. **MerlinAccessibilityService** - Simplified to monitor app switches and bring app to foreground
3. **ChatScreen** - Unified chat interface with PIN-gated settings access
4. **SettingsScreen** - Material 3 design with PIN-protected exit functionality
5. **ScreenStateReceiver** - Handles screen state changes for app foregrounding

### Key Files Structure

```
app/src/main/java/com/example/merlin/
├── MainActivity.kt                            # Sticky app implementation with window flags
├── service/
│   └── MerlinAccessibilityService.kt          # App switching monitoring (simplified)
├── receiver/
│   └── ScreenStateReceiver.kt                 # Screen state handling
├── ui/
│   ├── chat/
│   │   ├── ChatScreen.kt                      # Unified chat interface with settings access
│   │   ├── ChatViewModel.kt                   # State management
│   │   ├── ChatScreenPerformance.kt           # Performance optimizations
│   │   └── SpeechToTextManager.kt             # Voice input handling
│   ├── settings/
│   │   └── SettingsScreen.kt                  # PIN-gated settings with Material 3 design
│   ├── theme/
│   │   └── Theme.kt                           # Magical color palette
│   ├── accessibility/
│   │   └── AccessibilityConstants.kt          # Accessibility guidelines
│   ├── safety/
│   │   └── ContentFilter.kt                   # Child safety filtering
│   └── chat/
│       ├── PinExitDialog.kt                   # PIN authentication dialog
│       └── PinAuthenticationService.kt        # Secure PIN verification
└── utils/
    └── UserSessionRepository.kt               # Session and onboarding state management
```

## Technical Implementation Details

### 1. Sticky Main App Implementation

#### Window Flags Configuration

The main app is made sticky using enhanced window flags in `MainActivity.onCreate()`:

```kotlin
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
    
    // Modern Android APIs for lock screen behavior
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    }
}
```

#### Immersive Mode Implementation

Full-screen immersive mode with system UI hiding:

```kotlin
private fun enableImmersiveMode() {
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
        }
    } else {
        // Legacy approach for older Android versions
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }
}
```

#### Back Button Protection

Back button disabled with user-friendly feedback:

```kotlin
@Deprecated("Deprecated in Java")
override fun onBackPressed() {
    // Don't call super.onBackPressed() to prevent default back behavior
    Log.d(TAG, "Back button pressed - ignoring to maintain stickiness")
    
    // Show helpful toast to guide users
    Toast.makeText(this, "Use settings to exit Merlin safely", Toast.LENGTH_SHORT).show()
}
```

#### Task Lock Management

Lock task mode prevents app from being swiped away in recent apps:

```kotlin
private fun attemptLockTaskIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val activeChildId = userSessionRepository.getActiveChildId()
        if (activeChildId != null) { // Only after onboarding completion
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (activityManager.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                startLockTask()
                Log.d(TAG, "Task lock enabled - app cannot be swiped away")
            }
        }
    }
}
```

### 2. Simplified Accessibility Service

#### App Switching Monitoring

The `MerlinAccessibilityService` has been simplified to only monitor app switches:

```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
        // Only bring app to foreground after onboarding completion
        val activeChildId = userSessionRepository.getActiveChildId()
        if (activeChildId != null) {
            val packageName = event.packageName?.toString()
            if (packageName != null && packageName != "com.example.merlin") {
                bringAppToForeground()
            }
        }
    }
}

private fun bringAppToForeground() {
    val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    startActivity(intent)
}
```

#### Onboarding-Aware Behavior

Service only activates after onboarding completion to avoid interfering with permission screens:

```kotlin
override fun onServiceConnected() {
    super.onServiceConnected()
    // No automatic overlay creation - only monitor app switches
    Log.d(TAG, "Accessibility service connected - monitoring app switches")
}
```

### 3. PIN-Gated Settings System

#### Settings Screen Implementation

Material 3 design with magical color palette integration:

```kotlin
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onExitApp: () -> Unit
) {
    // Settings categories with beautiful card-based layout
    val categories = listOf(
        SettingsCategory("Profile", "👤", RoyalPurple),
        SettingsCategory("Child Profile", "👶", MistyBlue),
        SettingsCategory("Child Learning Preferences", "🎓", SageGreen),
        SettingsCategory("Child Performance", "📊", AmberGlow),
        SettingsCategory("Time Economy", "⏰", LavenderMist),
        SettingsCategory("Exit", "🚪", WarmTerracotta, onExitApp)
    )
    
    // Material 3 card-based layout with magical gradients
}
```

#### PIN Authentication Integration

Secure PIN verification using existing onboarding system:

```kotlin
// PIN dialog for settings access
PinExitDialog(
    isVisible = showSettingsPinDialog,
    onDismiss = { showSettingsPinDialog = false },
    onPinVerified = { navigateToSettings() },
    onPinVerification = { enteredPin ->
        pinAuthService.verifyPin(enteredPin)
    },
    maxAttempts = 1 // Toddler-friendly: return home after one failed attempt
)
```

#### Secure Exit Implementation

Proper cleanup of lock task mode and system resources:

```kotlin
fun exitAppProperly() {
    Log.d(TAG, "Starting proper app exit sequence")
    isExitingProperly = true
    
    // Set flag to prevent aggressive reinitialization on next startup
    val prefs = getSharedPreferences("merlin_state", Context.MODE_PRIVATE)
    prefs.edit().putBoolean(PREF_PROPER_EXIT, true).apply()
    
    // Stop lock task mode if active
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
            stopLockTask()
        }
    }
    
    // Clean up security components and exit
    cleanupSecurity()
    finishAndRemoveTask()
    exitProcess(0)
}
```

### 4. Enhanced Lifecycle Protection

#### Reinitialization Prevention

Exit handling prevents aggressive reinitialization after proper exit:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    // Check if returning from proper exit
    val prefs = getSharedPreferences("merlin_state", Context.MODE_PRIVATE)
    val wasProperExit = prefs.getBoolean(PREF_PROPER_EXIT, false)
    
    if (wasProperExit) {
        // Clear flag and use minimal initialization
        prefs.edit().putBoolean(PREF_PROPER_EXIT, false).apply()
        // Basic UI setup without aggressive protections
        return
    }
    
    // Normal startup with full protections
    setupStickyWindow()
    // ... rest of normal initialization
}
```

#### System Dialog Handling

Graceful handling of system dialogs to prevent "App is Pinned" popup loops:

```kotlin
override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    
    if (hasFocus && !isExitingProperly) {
        // Re-enable immersive mode with delay to avoid system dialog conflicts
        window.decorView.postDelayed({
            if (!isExitingProperly) {
                enableImmersiveMode()
            }
        }, 500) // 500ms delay prevents conflicts
    }
}
```

#### Delayed Lock Task Activation

Prevents immediate "App is Pinned" popup by delaying lock task activation:

```kotlin
// Delay lock task to avoid immediate popup issues
window.decorView.post {
    window.decorView.postDelayed({
        attemptLockTaskIfNeeded()
    }, 2000) // 2 second delay allows UI to fully initialize
}
```

### 5. Screen State Management

#### ScreenStateReceiver Integration

Updated to work with sticky main app approach:

```kotlin
class ScreenStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                // Bring main app to foreground instead of showing overlay
                val appIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(appIntent)
            }
            Intent.ACTION_SCREEN_OFF -> {
                // No specific action needed - app remains sticky
            }
        }
    }
}
```

## UI Component Specifications

### 1. Navigation Integration

#### Settings Access from Chat

Small, unobtrusive gear icon in ChatScreen TopAppBar:

```kotlin
// Small gear icon in top-right corner with PIN protection
IconButton(
    onClick = onNavigateToSettings,
    modifier = Modifier
        .size(32.dp)
        .background(
            color = CloudWhite.copy(alpha = 0.3f),
            shape = CircleShape
        )
) {
    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = AccessibilityConstants.ContentDescriptions.SETTINGS,
        tint = WisdomBlue.copy(alpha = 0.7f),
        modifier = Modifier.size(18.dp)
    )
}
```

### 2. Settings Screen Design

#### Category Cards

Beautiful Material 3 cards with magical gradients:

```kotlin
Card(
    onClick = category.action,
    modifier = Modifier
        .fillMaxWidth()
        .height(100.dp),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
        containerColor = category.color.copy(alpha = 0.9f)
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
) {
    // Category content with icon, title, and description
}
```

## Security Considerations

### 1. Enhanced App Security

- **Sticky Window Flags**: App appears over lock screen and dismisses keyguard automatically
- **Lock Task Mode**: Prevents app from being swiped away in recent apps
- **Back Button Protection**: Disabled with user-friendly feedback
- **Immersive Mode**: Hides system navigation to prevent easy escape

### 2. PIN-Gated Security

- **Settings Access**: All settings access requires PIN verification
- **Secure Exit**: PIN-protected exit process with proper cleanup
- **Toddler-Friendly**: One failed PIN attempt returns to main screen
- **SHA-256 Hashing**: Secure PIN storage with salt encryption

### 3. Data Protection

- **Proper Cleanup**: Exit process cleans up all system resources
- **Session Management**: Onboarding-aware behavior protects permission screens
- **Content Filtering**: Continues to protect all interactions
- **Secure Storage**: User data remains encrypted and protected

## Error Handling and Fallbacks

### 1. Lock Task Mode Failures

```kotlin
private fun attemptLockTaskIfNeeded() {
    try {
        if (conditions_met) {
            startLockTask()
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to enable lock task: ${e.message}")
        // Continue without lock task - other protections remain active
    }
}
```

### 2. Exit Sequence Protection

All lifecycle methods include exit sequence checks:

```kotlin
if (isExitingProperly) {
    Log.d(TAG, "In proper exit sequence - skipping aggressive actions")
    return
}
```

### 3. System UI Conflicts

Graceful handling of system dialogs and popups:

```kotlin
// Re-enable immersive mode with delay to avoid conflicts
window.decorView.postDelayed({
    if (!isExitingProperly) {
        enableImmersiveMode()
    }
}, 500)
```

## Performance Optimizations

### 1. Reduced Complexity

- **Single Chat Interface**: Eliminates dual interface synchronization overhead
- **Simplified Service**: Accessibility service only monitors app switches
- **Efficient Navigation**: Direct navigation without overlay management

### 2. Resource Management

- **Proper Cleanup**: Exit sequence ensures all resources are released
- **Delayed Activation**: Prevents immediate system conflicts
- **Conditional Protection**: Only applies protections when needed

### 3. Memory Efficiency

All existing performance optimizations from `ChatScreenPerformance.kt` continue to apply:
- Gradient caching system
- Animation frame management
- Message virtualization
- TTS optimization
- Content filtering cache

## Maintenance Guidelines

### 1. Adding New Protections

1. Always include `isExitingProperly` checks in aggressive lifecycle methods
2. Use delays for system-level operations to avoid conflicts
3. Ensure proper cleanup in `exitAppProperly()` method
4. Test exit and reentry scenarios thoroughly

### 2. Settings Screen Extensions

1. Follow Material 3 design patterns established in `SettingsScreen.kt`
2. Use magical color palette for consistency
3. Implement proper PIN protection for sensitive settings
4. Maintain card-based layout for accessibility

### 3. System Integration

- Test window flags behavior across Android versions
- Validate lock task mode on different devices
- Ensure proper handling of system dialogs
- Monitor for Android API changes affecting sticky behavior

## Testing Recommendations

### 1. Sticky App Behavior Testing

- **Window Flags**: Verify app appears over lock screen and dismisses keyguard
- **Back Button**: Confirm back button is disabled with appropriate feedback
- **Lock Task**: Test that app cannot be swiped away in recent apps
- **System UI**: Validate immersive mode and navigation hiding

### 2. Exit Sequence Testing

- **Proper Exit**: Test PIN-gated exit through settings
- **Cleanup**: Verify lock task mode is stopped and resources are released
- **Reinitialization**: Confirm app doesn't aggressively reinitialize after proper exit
- **Popup Prevention**: Ensure "App is Pinned" loops are eliminated

### 3. PIN-Gated Settings Testing

- **Settings Access**: Test gear icon navigation and PIN verification
- **Settings Display**: Verify all categories display correctly
- **Exit Functionality**: Test complete exit flow with proper cleanup
- **Accessibility**: Confirm PIN dialog meets accessibility requirements

### 4. Accessibility Service Testing

- **App Switching**: Verify service brings app to foreground when needed
- **Onboarding Protection**: Ensure service doesn't interfere during onboarding
- **Permission Screens**: Test that service allows proper permissions flow
- **System Apps**: Validate behavior with various system apps

## Future Enhancement Opportunities

### 1. Adaptive Stickiness

- Time-based stickiness levels
- Context-aware protection (home vs. public spaces)
- Parental controls for stickiness intensity
- Learning-based adaptation to user behavior

### 2. Enhanced Settings

- Granular PIN protection for different settings categories
- Biometric authentication integration
- Time-limited exit permissions
- Emergency exit scenarios

### 3. Improved User Experience

- Onboarding tutorials for sticky behavior
- Visual feedback for protection status
- Customizable exit gestures for accessibility
- Voice-controlled settings access

## Conclusion

The sticky main app architecture represents a significant simplification and improvement over the previous translucent overlay approach. By eliminating the complexity of dual chat interfaces and overlay management, the system provides:

- **Enhanced Security**: Multiple layers of protection prevent easy exit
- **Simplified Maintenance**: Single unified interface reduces complexity
- **Better User Experience**: Seamless interaction without overlay conflicts
- **Robust Exit Handling**: Proper cleanup prevents system issues
- **Toddler-Friendly Design**: PIN protection with helpful feedback

The implementation leverages modern Android APIs, comprehensive security measures, and performance optimizations to deliver a safe, engaging, and maintainable educational environment for children.

Regular testing and monitoring ensure the integration remains effective across different devices and Android versions while maintaining the high standards of security and usability required for a child-focused application. 