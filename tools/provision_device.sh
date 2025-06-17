#!/usr/bin/env bash
# Merlin Device Provisioning Tool
# Usage: ./provision_device.sh path/to/merlin.apk
set -e

echo "Merlin Device Provisioning Tool"
echo "--------------------------------"

if ! command -v adb &> /dev/null; then
  echo "Error: adb not found. Please install Android Platform Tools." >&2
  exit 1
fi

# Check connected device
if ! adb get-state 1>/dev/null 2>&1; then
  echo "Error: No device detected. Plug in a device with USB-debugging enabled." >&2
  exit 1
fi

echo "Installing APK..."
APK_PATH="$1"
if [[ -z "$APK_PATH" ]]; then
  echo "Usage: $0 path/to/merlin.apk" >&2
  exit 1
fi
adb install -r "$APK_PATH"

echo "Preparing device (marking setup complete)..."
adb shell settings put global device_provisioned 1 || true
adb shell settings put secure user_setup_complete 1 || true

echo "Setting Merlin as device owner..."
if ! adb shell dpm set-device-owner com.example.merlin/.security.MerlinDeviceAdminReceiver; then
  echo "Failed to set device owner. Ensure the device is freshly reset." >&2
  exit 1
fi

echo "Launching Merlin..."
adb shell am start -n com.example.merlin/.MainActivity

echo "Device successfully provisioned for kiosk mode." 