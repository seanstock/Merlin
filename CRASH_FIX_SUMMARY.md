# MainActivity Crash Fix - WindowInsetsController NullPointerException

## Problem
The app was crashing on launch with the following error:
```
java.lang.NullPointerException: Attempt to invoke virtual method 'android.view.WindowInsetsController com.android.internal.policy.DecorView.getWindowInsetsController()' on a null object reference
    at com.example.merlin.MainActivity.enableImmersiveMode(MainActivity.kt:162)
    at com.example.merlin.MainActivity.setupStickyWindow(MainActivity.kt:147)
    at com.example.merlin.MainActivity.onCreate(MainActivity.kt:80)
```

## Root Cause
The crash occurred because we were trying to access `window.insetsController` and `window.decorView` too early in the activity lifecycle - specifically in `onCreate()` before the window's decorView was fully initialized.

## Solution Implemented

### 1. **Deferred Immersive Mode Setup**
- Added `shouldEnableImmersiveMode` flag to track when immersive mode needs to be enabled
- Moved immersive mode setup from `onCreate()` to later lifecycle methods
- Window flags are still applied in `onCreate()`, but immersive mode waits for window readiness

### 2. **Null Safety Checks**
Added comprehensive null checks in `enableImmersiveMode()`:
```kotlin
// Android 11+ approach
window.insetsController?.let { controller ->
    // Safe to use controller
} ?: run {
    Log.w(TAG, "Window insetsController not available yet, will retry later")
    return
}

// Legacy approach
val decorView = window.decorView
if (decorView != null) {
    // Safe to use decorView
} else {
    Log.w(TAG, "Window decorView not available yet, will retry later")
    return
}
```

### 3. **Multiple Setup Opportunities**
Immersive mode is now attempted at multiple lifecycle points:
- `onResume()`: Primary setup opportunity
- `onWindowFocusChanged(hasFocus=true)`: When window gains focus
- `onUserLeaveHint()`: When user tries to leave the app

### 4. **Exception Handling**
Wrapped all immersive mode operations in try-catch blocks to gracefully handle any window-related exceptions.

### 5. **System UI Visibility Listener**
Added null checks to the system UI visibility listener setup to prevent similar issues on older Android versions.

## Key Changes Made

### MainActivity.kt
1. **Added Flag**: `shouldEnableImmersiveMode` to track deferred setup
2. **Modified setupStickyWindow()**: Removed immediate immersive mode call
3. **Enhanced enableImmersiveMode()**: Added null checks and exception handling
4. **Updated Lifecycle Methods**: Added immersive mode setup to onResume() and onWindowFocusChanged()
5. **Fixed System UI Listener**: Added null checks for decorView access

## Testing
The fix ensures that:
- ✅ App launches without crashing
- ✅ Immersive mode is enabled when window is ready
- ✅ System UI protection works as intended
- ✅ Graceful fallback if window components aren't available
- ✅ Multiple retry opportunities ensure eventual success

## Logs to Monitor
Watch for these log messages to verify proper operation:
- `"Enhanced sticky window flags applied - immersive mode will be enabled when window is ready"`
- `"Immersive mode enabled using Android 11+ API"` / `"Immersive mode enabled using legacy API"`
- `"Window insetsController not available yet, will retry later"`
- `"Window decorView not available yet, will retry later"`

## Backward Compatibility
The fix maintains full backward compatibility:
- Android 11+ uses modern WindowInsetsController API
- Older versions use legacy system UI visibility flags
- Graceful degradation if any API is unavailable
- No functional impact on system UI protection effectiveness 