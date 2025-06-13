package com.example.merlin.ui

/**
 * Represents the available UI layouts for the main menu.
 */
enum class UIVariant {
    /** Simple icon-based grid layout for ages 3-4. */
    SIMPLE,
    /** Advanced card-based list layout for ages 5+. */
    ADVANCED
}

/**
 * Determines the appropriate UI variant based on the child's age.
 * @param age The child's age.
 * @return The UIVariant to be displayed.
 */
fun getUIVariantForAge(age: Int): UIVariant {
    return if (age <= 4) UIVariant.SIMPLE else UIVariant.ADVANCED
} 