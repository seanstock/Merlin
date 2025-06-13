package com.example.merlin.ui.game

/**
 * Static registry of available games, eliminating runtime asset discovery.
 * This provides instant game list loading by pre-defining all game metadata.
 */
object GameRegistry {
    
    /**
     * All available games with their static metadata.
     * Games are defined at compile time for optimal performance.
     */
    val AVAILABLE_GAMES = listOf(
        GameMetadata(
            id = "sample-game",
            name = "Merlin's Memory",
            description = "Test your memory with magical sequences and patterns",
            maxLevel = 10,
            estimatedLoadTime = 1500L,
            requiresNetwork = false,
            supportedFeatures = listOf("touch", "timer", "scoring", "levels")
        ),
        GameMetadata(
            id = "color-match",
            name = "Color Match",
            description = "Learn colors and improve recognition skills",
            maxLevel = 8,
            estimatedLoadTime = 1200L,
            requiresNetwork = false,
            supportedFeatures = listOf("touch", "timer", "scoring", "colors")
        ),
        GameMetadata(
            id = "shape-match", 
            name = "Shape Match",
            description = "Identify and match geometric shapes",
            maxLevel = 10,
            estimatedLoadTime = 1300L,
            requiresNetwork = false,
            supportedFeatures = listOf("touch", "timer", "scoring", "shapes")
        ),
        GameMetadata(
            id = "number-match",
            name = "Number Match", 
            description = "Practice numbers and counting skills",
            maxLevel = 12,
            estimatedLoadTime = 1400L,
            requiresNetwork = false,
            supportedFeatures = listOf("touch", "timer", "scoring", "numbers")
        ),
        GameMetadata(
            id = "shape-drop",
            name = "Shape Drop Adventure",
            description = "Drag and drop shapes into matching holes - perfect for toddlers!",
            maxLevel = 10,
            estimatedLoadTime = 800L,
            requiresNetwork = false,
            supportedFeatures = listOf("touch", "drag-drop", "shapes", "toddler-friendly", "haptic")
        )
    )
    
    /**
     * Get game metadata by ID.
     */
    fun getGameById(gameId: String): GameMetadata? {
        return AVAILABLE_GAMES.find { it.id == gameId }
    }
    
    /**
     * Get all available games.
     */
    fun getAllGames(): List<GameMetadata> = AVAILABLE_GAMES
    
    /**
     * Check if a game exists.
     */
    fun hasGame(gameId: String): Boolean {
        return AVAILABLE_GAMES.any { it.id == gameId }
    }
    
    /**
     * Get games by supported feature.
     */
    fun getGamesByFeature(feature: String): List<GameMetadata> {
        return AVAILABLE_GAMES.filter { it.supportedFeatures.contains(feature) }
    }
} 