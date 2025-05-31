plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    // Add any specific dependencies for your domain layer here
    // For example, if you use Kotlin coroutines:
    // implementation(libs.kotlinx.coroutines.core) // Ensure libs.kotlinx.coroutines.core is defined in your version catalog

    // If it depends on your :core module (if :core is also a pure Kotlin/Java module)
    // implementation(project(":core"))
} 