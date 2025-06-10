package com.example.merlin.economy.model

/**
 * Data transfer object for badge definitions (available badges that can be earned).
 * Pure Kotlin data class with no Android dependencies, fully serializable for API transport.
 */
data class BadgeDefinitionDto(
    val id: String,
    val name: String,
    val description: String,
    val category: String,           // From BadgeCategory constants
    val imageUrl: String,
    val rarity: String,             // From BadgeRarity constants
    val requirements: Map<String, String>, // Requirements to earn this badge
    val rewards: Map<String, String> = emptyMap() // Rewards for earning this badge
) 