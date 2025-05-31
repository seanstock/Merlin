# Enhanced System UI Protection for Merlin

## Overview
This document describes the comprehensive system UI protection implemented to prevent users from escaping the Merlin app through gesture navigation, recent apps, or other system UI interactions.

## Protection Layers

### 1. **Immersive Mode Protection**
- **Full Screen Mode**: Hides status bar and navigation bar
- **Android 11+ Approach**: Uses WindowInsetsController to hide system gestures
- **Legacy Support**: Uses system UI visibility flags for older Android versions
- **Sticky Behavior**: Automatically re-enables when system UI appears

### 2. **Enhanced Window Flags**
```kotlin
FLAG_SHOW_WHEN_LOCKED       // Show over lock screen
FLAG_DISMISS_KEYGUARD        // Bypass lock screen
FLAG_KEEP_SCREEN_ON          // Prevent screen timeout
FLAG_TURN_SCREEN_ON          // Turn screen on when app starts
FLAG_FULLSCREEN              // Full screen mode
FLAG_LAYOUT_IN_SCREEN        // Layout within screen boundaries
FLAG_LAYOUT_NO_LIMITS        // Allow layout beyond screen
```

### 3. **Task Lock Protection**
- **startLockTask()**: Prevents app from being swiped away in recent apps
- **Fallback Handling**: Graceful degradation if task lock fails
- **Recent Apps Management**: Ensures app stays visible in recent apps

### 4. **Aggressive Accessibility Monitoring**
The accessibility service now monitors multiple event types:

#### Window State Changes
- Detects when external apps come to foreground
- Immediately brings Merlin back to foreground
- Allows system UI and IME briefly

#### Window Changes
- Monitors for recent apps/task switcher activation
- Aggressively returns focus to Merlin

#### System UI Interactions
- Detects notification panel interactions
- Prevents escape through system UI

#### Periodic Checking
- Every 2 seconds, ensures Merlin is in foreground
- Only active after onboarding completion

### 5. **Activity Lifecycle Protection**
```kotlin
onResume()           // Re-enables immersive mode
onWindowFocusChanged() // Detects focus loss and regains
onUserLeaveHint()    // Detects home button/recent apps
```

## System Packages Allowed
The following system packages are temporarily allowed to prevent interference:
- `com.android.systemui` (System UI)
- `android` (Core Android)
- `com.android.launcher*` (Launchers)
- `com.google.android.apps.nexuslauncher` (Pixel Launcher)
- `com.google.android.inputmethod*` (Gboard)
- `com.android.inputmethod*` (Other IMEs)

## Implementation Details

### MainActivity Protection
1. **setupStickyWindow()**: Applies all window flags and enables immersive mode
2. **enableImmersiveMode()**: Hides system navigation (Android 11+ and legacy)
3. **setupSystemUiVisibilityListener()**: Re-enables immersive mode when system UI appears
4. **Task Locking**: Prevents swipe-to-dismiss in recent apps

### MerlinAccessibilityService Protection
1. **Enhanced Event Monitoring**: Multiple accessibility event types
2. **Aggressive Response**: Fast response to navigation attempts
3. **Periodic Checking**: Regular foreground verification
4. **Smart Filtering**: Allows necessary system interactions

## User Experience Impact

### For Parents
- PIN protection still works normally
- Settings access remains secure
- Exit functionality preserved

### For Children
- Gesture navigation blocked
- Recent apps button ineffective
- Home button returns to Merlin
- Swipe up gestures blocked
- Status bar interactions blocked

## Security Notes
- All protection respects onboarding completion status
- System IMEs and essential UI allowed temporarily
- Accessibility service permissions required
- No impact on legitimate system operations

## Testing
Test the following scenarios to verify protection:
1. ✅ Swipe up from bottom (recent apps)
2. ✅ Swipe down from top (notifications)
3. ✅ Home button press
4. ✅ Recent apps button
5. ✅ Long press home (Google Assistant)
6. ✅ Volume buttons (should work)
7. ✅ Power button (should work)
8. ✅ PIN-protected settings access

## Logs
Monitor these log tags for debugging:
- `MainActivity`: Window flags and immersive mode
- `MerlinAccessibilityService`: Accessibility events and foreground management

## Fallback Behavior
If any protection layer fails:
1. Other layers continue working
2. Accessibility service provides backup
3. Periodic checking ensures recovery
4. Graceful degradation maintained 