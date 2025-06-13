package com.example.merlin.config

import com.example.merlin.ui.UIVariant

/**
 * Service that determines which UI variant should be displayed for a given child.
 * This abstraction allows for swapping local logic with a remote LaaS endpoint in the future.
 */
interface UIConfigurationService {
    /**
     * Returns the UI variant that should be used for the provided child.
     * This may involve reading local data or calling a remote server.
     * @param childId The ID of the child for whom to determine the UI variant.
     * @return The UIVariant to be displayed.
     */
    suspend fun getUIVariant(childId: String): UIVariant
} 