# Merlin Kiosk Mode – Provisioning Guide

Follow these steps **in order** to turn an Android device (real or emulator) into a locked-down Merlin kiosk.

---
## 1 – Prerequisites on the PC

```powershell
# 1. Install Android SDK (if not already)
winget install --id Google.AndroidSDK --source winget

# 2. Ensure adb is on PATH (one-time)
$env:Path += ';C:\Users\<username>\AppData\Local\Android\Sdk\platform-tools'
```

Verify:
```powershell
adb version
```
> **Note on Shells:** This guide uses commands for Windows PowerShell/CMD. If you are using a Linux-style shell (like Git Bash or WSL), use `grep` instead of `findstr`.

---
## 2 – Build the APK
From the project root:
```powershell
./gradlew :app:assembleDebug   # or :app:assembleRelease for production
```
Output: `app/build/outputs/apk/debug/app-debug.apk`.

---
## 3 – Prepare the Device
1. **Factory-reset** (Settings → General management → Reset → Factory data reset).  
   This is the most reliable way to ensure no other account or app can be a device owner.
2. Complete the setup wizard **without adding Google / Samsung accounts**.
3. Enable **Developer options** → turn **USB debugging** ON.
4. (Samsung) Also enable *USB debugging (Security settings)* if the option exists.

---
## 4 – Connect & Authorise
```powershell
adb kill-server
adb start-server
adb devices           # accept RSA prompt on the tablet
```
Device should show as `device`, not `unauthorised`.

---
## 5A – (Crucial) Check & Remove Existing Admins
On devices that weren't just factory-reset, another app (like Samsung Knox) might already be the device owner, which will block Merlin.

1.  **Check for active admins:**
    ```powershell
    adb shell dumpsys device_policy | findstr "Active admin"
    ```
    If you see **any** output, you must remove the listed admin before proceeding.

2.  **Remove the unwanted admin:**
    ```powershell
    # Template: adb shell dpm remove-active-admin --user 0 <package>/<receiver>
    # Example for Knox:
    adb shell dpm remove-active-admin --user 0 com.samsung.knox/.KnoxDeviceAdminReceiver
    ```
    > **Note:** `--user 0` specifies the device's primary user profile. You must remove all other admins before you can set a new one.

---
## 5B – Install Merlin
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```
If reinstalling, use `-r` to replace the existing build.

---
## 6 – Become Device-Owner (Kiosk prerequisite)
```powershell
# This command must be run as a single line in CMD/PowerShell.
# The backslash (\) is a line-continuation character for Linux/macOS shells, not for Windows.
adb shell dpm set-device-owner com.example.merlin.debug/com.example.merlin.security.MerlinDeviceAdminReceiver
```
Success message ⇒ device owner set.  
Failure *"Not allowed to set the device owner"* ⇒ you missed a step. Go back to **Step 3** (factory-reset) or **Step 5A** (remove existing admins).

---
## 7 – Launch Merlin
```powershell
adb shell monkey -p com.example.merlin.debug -c android.intent.category.LAUNCHER 1
```
Complete onboarding (overlay permission). Merlin should auto-enter kiosk.

---
## 8 – Verify Kiosk Lock
```powershell
adb shell dumpsys activity | findstr mLockTaskModeState
```
`mLockTaskModeState=LOCKED` ⇒ kiosk active.

To double-check device owner:
```powershell
adb shell dpm get-device-owner
```

---
## 9 – Optional Hardening Flags
Run as the device owner (Merlin) to tighten escape paths:
```powershell
adb shell cmd device_policy add-user-restriction DISALLOW_FACTORY_RESET
adb shell cmd device_policy add-user-restriction DISALLOW_SAFE_BOOT
adb shell cmd device_policy add-user-restriction DISALLOW_USB_FILE_TRANSFER
adb shell cmd device_policy set-status-bar-disabled true
```

---
## 10 – Exiting Kiosk (for developers)
```powershell
# Temporarily exit lock-task
adb shell am stop-locktask

# Remove Merlin as device owner for the primary user (user 0)
adb shell dpm remove-active-admin --user 0 com.example.merlin.debug/com.example.merlin.security.MerlinDeviceAdminReceiver
```

---
### Emulator Quick Start (skip factory-reset)
```powershell
# Start fresh emulator
emulator -avd Pixel_Tablet_API_36 -wipe-data -no-snapshot-save &

adb wait-for-device
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell dpm set-device-owner com.example.merlin.debug/com.example.merlin.security.MerlinDeviceAdminReceiver
adb shell monkey -p com.example.merlin.debug -c android.intent.category.LAUNCHER 1
```
You should immediately land in Merlin locked to kiosk mode.

