import java.util.Properties // Added for Properties class

// Logic to load API key from local.properties - MOVED TO TOP
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties") // Assumes local.properties is at the Merlin project root
if (localPropertiesFile.exists()) {
    try {
        localPropertiesFile.inputStream().use { localProperties.load(it) }
    } catch (e: Exception) {
        println("Warning: Could not load local.properties file: ${e.message}")
    }
}
val openAIApiKeyFromProperties = localProperties.getProperty("OPENAI_API_KEY", "KEY_NOT_FOUND_IN_LOCAL_PROPERTIES")

// Debug: Print the API key value (first 10 chars only for security)
println("DEBUG: API key loaded: ${openAIApiKeyFromProperties.take(10)}...")

plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt") // For Room - keeping id reference as it worked before
}

android {
    namespace = "com.example.merlin.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug { // Ensure debug build type exists or is created
            buildConfigField("String", "OPENAI_API_KEY", "\"$openAIApiKeyFromProperties\"")
        }
        create("staging") {
            initWith(getByName("debug"))
            buildConfigField("String", "OPENAI_API_KEY", "\"$openAIApiKeyFromProperties\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "OPENAI_API_KEY", "\"$openAIApiKeyFromProperties\"")
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
        buildConfig = true
    }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas".toString())
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // Room and SQLCipher will be primary dependencies here
    implementation(libs.sqlcipher.android)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // OpenAI client might also be used here or in a specific repository
    implementation(libs.openai.client)
    // Required Ktor engine for OpenAI client
    implementation("io.ktor:ktor-client-okhttp:3.0.0")

    // Dependency on the core module
    implementation(project(":core"))

    // Testing
    testImplementation(libs.junit) // Or "junit:junit:4.13.2"
    androidTestImplementation(libs.androidx.junit) // Or "androidx.test.ext:junit:1.1.5"
    androidTestImplementation(libs.androidx.espresso.core) // Or "androidx.test.espresso:espresso-core:3.5.1"
    androidTestImplementation(libs.androidx.room.testing) // Or "androidx.room:room-testing:<room_version>"

    // Mockito for unit testing
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Optional: Robolectric for running Android framework code on JVM tests
    // testImplementation(libs.robolectric) // Or "org.robolectric:robolectric:4.10.3"
}

// REMOVED FROM BOTTOM - Logic to load API key from local.properties moved to top 