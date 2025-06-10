package com.example.merlin.economy.model

/**
 * Sealed class representing the result of an operation that can either succeed or fail.
 * This provides a consistent way to handle success/failure states across all economy services.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
    }
    
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onError(action: (Exception) -> Unit): Result<T> {
        if (this is Error) action(exception)
        return this
    }
    
    inline fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (Exception) -> R
    ): R = when (this) {
        is Success -> onSuccess(data)
        is Error -> onFailure(exception)
    }
    
    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun <T> error(exception: Exception): Result<T> = Error(exception)
        fun <T> error(message: String): Result<T> = Error(Exception(message))
    }
} 