@echo off
echo Building MerlinKiosk...
call gradlew assembleDebug -x lint -x test

if %ERRORLEVEL% neq 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo Build successful! Installing app...

:: Try to find ADB in common locations
set ADB_PATH=
where adb >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set ADB_PATH=adb
) else (
    if exist "%ANDROID_HOME%\platform-tools\adb.exe" (
        set ADB_PATH="%ANDROID_HOME%\platform-tools\adb.exe"
    ) else if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
        set ADB_PATH="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
    ) else if exist "C:\Users\%USERNAME%\AppData\Local\Android\Sdk\platform-tools\adb.exe" (
        set ADB_PATH="C:\Users\%USERNAME%\AppData\Local\Android\Sdk\platform-tools\adb.exe"
    ) else (
        echo ADB not found! Please ensure Android SDK is installed and ADB is in your PATH.
        echo You can manually install the APK from: app\build\outputs\apk\debug\app-debug.apk
        pause
        exit /b 1
    )
)

echo Using ADB at: %ADB_PATH%

%ADB_PATH% install -r app\build\outputs\apk\debug\app-debug.apk

if %ERRORLEVEL% neq 0 (
    echo Installation failed! Make sure your device is connected and USB debugging is enabled.
    pause
    exit /b 1
)

echo Starting app...
%ADB_PATH% shell am start -n com.example.merlin/.MainActivity

echo Done! App should be running on your device.
pause