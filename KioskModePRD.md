# PRD: Merlin Kiosk Mode Device Conversion

| **Document Info** | |
| :--- | :--- |
| **Title** | Merlin App to Kiosk Mode Device Conversion |
| **Author** | AI Assistant |
| **Status** | Draft |
| **Version** | 1.0 |
| **Date** | October 27, 2023 |

---

## 1. Introduction & Overview

### 1.1. Problem Statement
The current Merlin application uses a sophisticated but fragile combination of an Accessibility Service, WindowManager flags, and a persistent `SharedPreferences` flag to simulate a "kiosk" or "child-lock" mode. This implementation is complex, potentially unstable across different Android OS versions, and can be bypassed by tech-savvy users (e.g., via Safe Mode). This "hack" is not suitable for a professional, pre-configured hardware device offering.

### 1.2. Proposed Solution
This project will convert the Merlin application into a true kiosk-mode app by leveraging the official Android Enterprise **Device Owner** APIs. The app will be re-architected to function as a Device Policy Controller (DPC), giving it administrative control over the device. This will replace the current complex lockdown mechanism with a simpler, more robust, and virtually escape-proof system enforced at the OS level.

### 1.3. Goals
- **Increase Stability & Reliability:** Eliminate dependencies on the Accessibility Service and other workarounds, which are prone to breaking with OS updates.
- **Enhance Security:** Create a truly secure environment by programmatically disabling escape hatches like Safe Mode and the status bar.
- **Simplify the Codebase:** Remove large amounts of complex code related to the current lockdown simulation.
- **Professionalize the Product:** Establish a technical foundation suitable for selling Merlin as a dedicated hardware device.

## 2. Phased Rollout & Scope

This project is divided into two distinct phases to manage complexity and risk.

### 2.1. Phase 1: Kiosk Mode Migration (MVP)
The sole focus of Phase 1 is to replace the underlying lockdown technology without changing the end-user functionality.

#### **In Scope:**
- Implementing the app as a Device Policy Controller (DPC).
- Replacing the existing lockdown mechanism with `DevicePolicyManager` and `startLockTask()`.
- Replicating the current PIN-gated exit flow using `stopLockTask()`.
- Ensuring the device is fully locked down and inescapable for the child.
- Removing all now-redundant code (Accessibility Service, Screen State Receiver, custom flags).

#### **Out of Scope:**
- Any new user-facing features.
- The "MerlinOS" walled garden concept.
- Allowing any apps other than the main Merlin app to run.

**The goal of Phase 1 is functional parity with the current app, but with superior underlying technology.**

### 2.2. Phase 2: "MerlinOS" Walled Garden
This phase builds upon the new kiosk foundation to introduce a multi-app, parent-managed environment.

#### **In Scope:**
- Creating a new "Manage Allowed Apps" section in the parent settings.
- Allowing parents to select installed apps to add to a "safe" whitelist.
- Displaying these whitelisted apps to the child as launchable options.
- Using the DPC to enforce that only whitelisted apps can be launched.

## 3. User Personas & Stories

### 3.1. Personas
- **Parent (Administrator):** Needs a simple, secure way to control the device and exit lockdown mode.
- **Child (End-User):** Interacts with the learning content within the locked-down environment.

### 3.2. User Stories

#### **Phase 1: Kiosk Mode Migration (MVP)**
- **As a Parent,** I want the device to be securely locked to the Merlin app when my child is using it, so that I have peace of mind they cannot access anything else.
- **As a Parent,** I want to be able to enter my PIN in the settings to temporarily disable the lockdown, so that I can use the device as a standard tablet.
- **As a Parent,** I want the device to automatically re-lock itself to Merlin when I re-launch the app, so that it's always ready and safe for my child.

#### **Phase 2: "MerlinOS" Walled Garden**
- **As a Parent,** I want a settings screen where I can see all apps installed on the device, so that I can choose which ones are safe for my child.
- **As a Parent,** I want to add and remove apps from a "safe list," so that I can customize my child's available activities.
- **As a Child,** I want to see icons for all the apps my parent has approved (like YouTube Kids or a drawing app), so that I can launch them directly from the Merlin interface.

## 4. Requirements

### 4.1. Phase 1: Kiosk Mode Migration (MVP)

#### **Functional Requirements**
- **FR1.1: Automatic Lockdown:** On launch (after initial provisioning), the app must enter Lock Task Mode, making it the foreground app.
- **FR1.2: UI Lockdown:** The Android status bar (with notifications and quick settings) and navigation bar (home/back/recents buttons) must be completely disabled and inaccessible to the user.
- **FR1.3: PIN-Gated Exit:** The existing PIN-gated settings screen must contain an "Exit Kiosk Mode" button. Tapping this button will call `stopLockTask()` and return the device to a standard, usable Android tablet state.
- **FR1.4: Automatic Re-Lock:** When the Merlin app is launched while not in kiosk mode, it should automatically re-initiate kiosk mode via `startLockTask()`.

#### **Technical Requirements**
- **TR1.1: Create DeviceAdminReceiver:** A new `MerlinDeviceAdminReceiver` class extending `DeviceAdminReceiver` must be created.
- **TR1.2: Update Manifest:** The `AndroidManifest.xml` must be updated to declare the `MerlinDeviceAdminReceiver`, including the `BIND_DEVICE_ADMIN` permission and the device admin policy XML resource.
- **TR1.3: Implement DevicePolicyManager:** `MainActivity` will get an instance of `DevicePolicyManager` and the `ComponentName` for the receiver.
- **TR1.4: Set Kiosk Policies:** The app, acting as Device Owner, must set the following policies:
    - `setLockTaskPackages()`: Whitelist only the Merlin app's own package name (`com.example.merlin`).
    - `addUserRestriction(UserManager.DISALLOW_SAFE_BOOT)`: Prevent escaping via Safe Mode.
    - `setKeyguardDisabled(true)`: Disable the system lock screen.
    - `setStatusBarDisabled(true)`: Permanently hide the status bar.
    - Optionally, `addUserRestriction(UserManager.DISALLOW_FACTORY_RESET)` for added security.
- **TR1.5: Implement `startKioskMode()`:** A function that applies the policies above and then calls `startLockTask()`.
- **TR1.6: Implement `exitKioskMode()`:** A function that calls `stopLockTask()`.

#### **Deprecation Requirements (Code to be Removed)**
- **DR1.1:** The entire `MerlinAccessibilityService.kt` class and its manifest entry must be deleted.
- **DR1.2:** The `ScreenStateReceiver.kt` class and its manifest entry must be deleted.
- **DR1.3:** In `MainActivity.kt`, the `setupStickyWindow()` method and all `WindowManager.LayoutParams` flags are to be removed.
- **DR1.4:** The `onBackPressed()` override in `MainActivity.kt` must be removed.
- **DR1.5:** The entire "proper exit" flag system (`PREF_PROPER_EXIT`, `.commit()` logic, and checks in `onCreate`) must be removed.

### 4.2. Phase 2: "MerlinOS" Walled Garden

#### **Functional Requirements**
- **FR2.1: App Management UI:** A new screen within Parent Settings titled "Manage Apps" will display two lists: "Allowed Apps" and "All Other Apps."
