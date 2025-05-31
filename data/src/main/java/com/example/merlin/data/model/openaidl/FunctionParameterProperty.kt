package com.example.merlin.data.model.openaidl

/**
 * Represents the properties of a parameter for an OpenAI function tool.
 * Used to help construct the JSON schema for the function's parameters.
 */
data class FunctionParameterProperty(
    val type: String, // e.g., "string", "integer", "boolean", "number", "array", "object"
    val description: String,
    val enum: List<String>? = null, // Optional: for enum types, a list of allowed string values
    // For object type, properties would be Map<String, FunctionParameterProperty>
    // For array type, items would be FunctionParameterProperty
    // For simplicity, keeping it flat for now, can be extended if complex nested objects are needed for functions.
) 