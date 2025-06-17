# Merlin Device Provisioning Guide

## Prerequisites
- Android Debug Bridge (ADB) installed on your computer
- A factory-reset Android device (or emulator)
- USB debugging enabled on the device

---

## Provisioning Steps

### 1. Connect the Device
```bash
adb devices
```
Ensure your device appears in the list.

### 2. Install the Merlin APK
```bash
adb install -r path/to/merlin.apk
```

### 3. Set Merlin as Device Owner
```bash
adb shell dpm set-device-owner com.example.merlin/.security.MerlinDeviceAdminReceiver
```

### 4. Verify Device-Owner Status
```bash
adb shell dumpsys device_policy | grep -i "device owner"
```
You should see `com.example.merlin` listed as the device owner.

### 5. Launch Merlin
```bash
adb shell am start -n com.example.merlin/.MainActivity
```
Merlin should automatically enter kiosk mode.

---

## Troubleshooting

• **"Not allowed to set the given component as device owner"**
  - The device must be factory-reset or a fresh emulator.

• **Provisioning on existing setup wizard**
  - Some devices need the setup wizard disabled first:
```bash
adb shell settings put global device_provisioned 1
adb shell settings put secure user_setup_complete 1
```
Reboot and run the `dpm set-device-owner` command again.

• **Samsung Knox / Xiaomi MIUI quirks**
  - Refer to manufacturer documentation for additional steps.

---

## Removing Device Owner (development only)
```bash
adb shell dpm remove-active-admin com.example.merlin/.security.MerlinDeviceAdminReceiver
```

> **Warning:** Removing device owner will break kiosk mode and policies. 