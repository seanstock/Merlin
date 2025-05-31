package com.example.merlin.data.model.openaidl

/**
 * A simplified data class to represent the relevant parts of an AI response,
 * especially when function calling is involved.
 */
data class MerlinAIResponse(
    val content: String?, // Assistant's textual response content, if any
    val functionCallName: String?, // Name of the function the model wants to call
    val functionCallArguments: String? // JSON string representation of the arguments for the function call
) 