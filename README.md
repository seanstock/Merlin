# Merlin: AI-Powered Kiosk Learning App for Kids

![License: MIT+Common Clause](https://img.shields.io/badge/License-MIT-blue.svg)
![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Kotlin Version](https://img.shields.io/badge/Kotlin-1.9.0-7F52FF)
![Compose Version](https://img.shields.io/badge/Compose-1.6.8-4285F4)

Merlin is a comprehensive, AI-powered educational application designed to run in a secure kiosk mode on Android devices. It provides a safe, distraction-free environment where children can learn through conversation, play educational games, and be rewarded through a built-in economy system.

*(Suggestion: Add a high-quality GIF or screenshot of the app in action here. Visuals are incredibly important for an open-source UI project.)*



## Core Features

*   **Conversational AI Tutor**: A friendly AI (powered by OpenAI) that can answer questions, provide explanations, and even launch in-app activities using function calling.
*   **Gamified Learning & Economy**: Children earn "Merlin Coins" for completing lessons and games, which they can spend on app access or other rewards.
*   **Secure Kiosk Mode**: Utilizes Android's Device Owner features to lock the device into the Merlin app, preventing children from accessing other apps or system settings without a parent PIN.
*   **Dynamic UI Variants**: The user interface adapts based on the child's age, offering a `SIMPLE` icon-based grid for younger children (3-4) and an `ADVANCED` card-based menu for older children.
*   **Modular Curriculum System**: A robust system for defining educational curricula. Includes an AI-powered syllabus generator to automatically create structured lesson plans from text.
*   **Parental Controls & Analytics**: PIN-gated settings and a comprehensive dashboard for parents to monitor progress, manage screen time, and view analytics.

## Architecture

This project follows a modern, multi-module Clean Architecture approach designed for scalability and maintainability.

*   **Multi-Module Structure**: The app is divided into `app`, `core`, `data`, `domain`, and `ui` modules to enforce separation of concerns.
*   **MVVM & Unidirectional Data Flow (UDF)**: UI state is managed in ViewModels and exposed to the Compose UI via `StateFlow`, ensuring a predictable and reactive user interface.
*   **Repository Pattern**: Data sources (local Room database, remote services) are abstracted away from the rest of the app by repositories.
*   **Learning-as-a-Service (LaaS) Ready**: The architecture is built around service interfaces (e.g., `EconomyService`, `CurriculumService`) with a `ServiceLocator` that can provide either local or remote implementations. **Note:** Currently, only local implementations are provided, but the groundwork is laid for future cloud-based services.

## Tech Stack

*   **Language**: 100% [Kotlin](https://kotlinlang.org/)
*   **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) for the entire UI.
*   **Architecture**: MVVM, Clean Architecture, Repository Pattern
*   **Asynchronous**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html) for all async operations.
*   **Database**: [Room](https://developer.android.com/training/data-storage/room) for local data persistence.
*   **Navigation**: [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation) for screen transitions.
*   **AI**: [openai-kotlin](https://github.com/aallam/openai-kotlin) client for interacting with the OpenAI API.
*   **Security**: [Jetpack Security (EncryptedSharedPreferences)](https://developer.android.com/topic/security/data), Android `DevicePolicyManager` for Kiosk Mode.
*   **Background Processing**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) for reliable background security checks.

## Getting Started

To build and run this project, you will need Android Studio Hedgehog or newer.

### 1. API Keys

The project uses the OpenAI API for its AI tutor functionality. You will need to provide your own API key.

1.  Create a file named `local.properties` in the root directory of the project.
2.  Add your OpenAI API key to this file:
    ```properties
    OPENAI_API_KEY="your_openai_api_key_here"
    ```
3.  The `local.properties` file is included in `.gitignore` to prevent your key from being committed to version control.

### 2. Kiosk Mode Setup (Critical Step)

Merlin is designed to be a **Device Owner** app to enable its secure kiosk mode. This gives it special privileges to lock the device. Setting this up requires `adb`.

**WARNING:** Setting a device owner can only be done on a device that has **no accounts** (e.g., a factory-reset device). If a Google account is present, this command will fail.

1.  **Enable USB Debugging** on your target Android device.
2.  **Connect the device** to your computer.
3.  **Build and install the debug APK** onto the device. You can do this via Android Studio or with the command:
    ```bash
    ./gradlew installDebug
    ```
4.  **Set Merlin as the Device Owner** using the following `adb` command. (Ensure `adb` is in your system's PATH).

    ```bash
    adb shell dpm set-device-owner com.example.merlin.debug/com.example.merlin.security.MerlinDeviceAdminReceiver
    ```
    *Note the `.debug` suffix for the debug build.*

5.  If the command is successful, you will see a `Success` message. If it fails, ensure there are no user accounts on the device.

### 3. Build and Run

Once the API key is in place and the device owner is set, you can build and run the app from Android Studio as usual. The app will launch into an onboarding flow, after which it will enter Kiosk (Lock Task) mode.

## Project Modules

*   `:app` - The main application module, containing the UI, Activities, ViewModels, and DI setup.
*   `:core` - Common utilities, constants, and extensions shared across modules. (Currently minimal).
*   `:data` - Contains data-layer components: repositories, Room database definitions, and remote data sources.
*   `:domain` - Intended for business logic and use cases, but currently light as most logic resides in ViewModels or services.
*   `:ui` - Common Jetpack Compose UI components, themes, and design system elements. (Currently part of `:app`, a candidate for refactoring).

## Contributing

Contributions are welcome! If you'd like to help improve Merlin, please feel free to:
*   Open an issue to report bugs or suggest new features.
*   Submit a pull request with your improvements.
*   Help improve the documentation.
*   Chat with me on LinkedIN
