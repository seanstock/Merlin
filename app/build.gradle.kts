plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.merlin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.merlin"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            this.applicationIdSuffix = ".debug"
            this.versionNameSuffix = "-debug"
            this.isDebuggable = true
            
            // Local development with local services and mocking
            buildConfigField("String", "AI_SERVICE_URL", "\"http://localhost:8080/api/v1\"")
            buildConfigField("String", "ECONOMY_SERVICE_URL", "\"http://localhost:8081/api/v1\"")
            buildConfigField("String", "CONTENT_SERVICE_URL", "\"http://localhost:8082/api/v1\"")
            buildConfigField("String", "ANALYTICS_SERVICE_URL", "\"http://localhost:8083/api/v1\"")
            buildConfigField("Boolean", "USE_LOCAL_SERVICES", "true")
            buildConfigField("Boolean", "ENABLE_SERVICE_MOCKING", "true")
            buildConfigField("Boolean", "ENABLE_DEBUG_LOGGING", "true")
            buildConfigField("String", "API_VERSION", "\"v1\"")
        }
        create("staging") {
            initWith(getByName("debug"))
            this.applicationIdSuffix = ".staging"
            this.versionNameSuffix = "-staging"
            this.isDebuggable = true
            
            // Staging environment for service integration testing
            buildConfigField("String", "AI_SERVICE_URL", "\"https://staging-ai.merlin.dev/api/v1\"")
            buildConfigField("String", "ECONOMY_SERVICE_URL", "\"https://staging-economy.merlin.dev/api/v1\"")
            buildConfigField("String", "CONTENT_SERVICE_URL", "\"https://staging-content.merlin.dev/api/v1\"")
            buildConfigField("String", "ANALYTICS_SERVICE_URL", "\"https://staging-analytics.merlin.dev/api/v1\"")
            buildConfigField("Boolean", "USE_LOCAL_SERVICES", "false")
            buildConfigField("Boolean", "ENABLE_SERVICE_MOCKING", "false")
            buildConfigField("Boolean", "ENABLE_DEBUG_LOGGING", "true")
            buildConfigField("String", "API_VERSION", "\"v1\"")
        }
        release {
            this.isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Production Learning-as-a-Service endpoints
            buildConfigField("String", "AI_SERVICE_URL", "\"https://api.merlin.ai/v1\"")
            buildConfigField("String", "ECONOMY_SERVICE_URL", "\"https://economy.merlin.ai/v1\"")
            buildConfigField("String", "CONTENT_SERVICE_URL", "\"https://content.merlin.ai/v1\"")
            buildConfigField("String", "ANALYTICS_SERVICE_URL", "\"https://analytics.merlin.ai/v1\"")
            buildConfigField("Boolean", "USE_LOCAL_SERVICES", "false")
            buildConfigField("Boolean", "ENABLE_SERVICE_MOCKING", "false")
            buildConfigField("Boolean", "ENABLE_DEBUG_LOGGING", "false")
            buildConfigField("String", "API_VERSION", "\"v1\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended:1.6.7")
    
    // AppCompat for SecurityLockoutActivity
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // Security Crypto for EncryptedSharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Lifecycle Process for ProcessLifecycleOwner
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    
    // Compose Lifecycle for lifecycle integration
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Room dependencies for database access
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":ui"))
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.pdfbox.android)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}