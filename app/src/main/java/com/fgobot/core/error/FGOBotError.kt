/*
 * FGO Bot - Error Handling System
 * 
 * This file defines the error handling system for the FGO Bot application.
 * Uses sealed classes to represent different types of errors that can occur.
 */

package com.fgobot.core.error

/**
 * Sealed class representing all possible errors in the FGO Bot application
 * 
 * This provides a type-safe way to handle different error scenarios
 * and ensures comprehensive error handling throughout the application.
 */
sealed class FGOBotError : Exception() {
    
    /**
     * Network-related errors
     */
    sealed class NetworkError : FGOBotError() {
        
        /**
         * No internet connection available
         */
        object NoConnection : NetworkError() {
            override val message: String = "No internet connection available"
        }
        
        /**
         * Request timeout
         */
        object Timeout : NetworkError() {
            override val message: String = "Request timed out"
        }
        
        /**
         * Server error (5xx status codes)
         */
        data class ServerError(val statusCode: Int, override val message: String) : NetworkError()
        
        /**
         * Client error (4xx status codes)
         */
        data class ClientError(val statusCode: Int, override val message: String) : NetworkError()
        
        /**
         * Unknown network error
         */
        data class Unknown(override val message: String, override val cause: Throwable? = null) : NetworkError()
    }
    
    /**
     * Database-related errors
     */
    sealed class DatabaseError : FGOBotError() {
        
        /**
         * Database constraint violation
         */
        data class ConstraintViolation(override val message: String) : DatabaseError()
        
        /**
         * Database corruption detected
         */
        object Corruption : DatabaseError() {
            override val message: String = "Database corruption detected"
        }
        
        /**
         * Database disk full
         */
        object DiskFull : DatabaseError() {
            override val message: String = "Not enough storage space"
        }
        
        /**
         * Database access denied
         */
        object AccessDenied : DatabaseError() {
            override val message: String = "Database access denied"
        }
        
        /**
         * Unknown database error
         */
        data class Unknown(override val message: String, override val cause: Throwable? = null) : DatabaseError()
    }
    
    /**
     * Application-specific errors
     */
    sealed class ApplicationError : FGOBotError() {
        
        /**
         * Invalid application state
         */
        data class InvalidState(override val message: String) : ApplicationError()
        
        /**
         * Permission denied
         */
        data class PermissionDenied(val permission: String) : ApplicationError() {
            override val message: String = "Permission denied: $permission"
        }
        
        /**
         * Configuration error
         */
        data class ConfigurationError(override val message: String) : ApplicationError()
        
        /**
         * Data validation error
         */
        data class ValidationError(val field: String, override val message: String) : ApplicationError()
        
        /**
         * Unknown application error
         */
        data class Unknown(override val message: String, override val cause: Throwable? = null) : ApplicationError()
    }
    
    /**
     * Automation-specific errors
     */
    sealed class AutomationError : FGOBotError() {
        
        /**
         * Screen capture failed
         */
        object ScreenCaptureFailed : AutomationError() {
            override val message: String = "Failed to capture screen"
        }
        
        /**
         * Image recognition failed
         */
        data class ImageRecognitionFailed(val target: String) : AutomationError() {
            override val message: String = "Failed to recognize: $target"
        }
        
        /**
         * Input injection failed
         */
        data class InputFailed(val action: String) : AutomationError() {
            override val message: String = "Failed to perform input: $action"
        }
        
        /**
         * Battle state detection failed
         */
        object StateDetectionFailed : AutomationError() {
            override val message: String = "Failed to detect battle state"
        }
        
        /**
         * Automation timeout
         */
        data class Timeout(val operation: String) : AutomationError() {
            override val message: String = "Automation timeout: $operation"
        }
        
        /**
         * Unknown automation error
         */
        data class Unknown(override val message: String, override val cause: Throwable? = null) : AutomationError()
    }
}

/**
 * Extension function to convert throwables to FGOBotError
 * 
 * @return Appropriate FGOBotError based on the throwable type
 */
fun Throwable.toFGOBotError(): FGOBotError {
    return when (this) {
        is FGOBotError -> this
        is java.net.UnknownHostException -> FGOBotError.NetworkError.NoConnection
        is java.net.SocketTimeoutException -> FGOBotError.NetworkError.Timeout
        is java.io.IOException -> FGOBotError.NetworkError.Unknown(message ?: "Network error", this)
        is android.database.sqlite.SQLiteConstraintException -> 
            FGOBotError.DatabaseError.ConstraintViolation(message ?: "Database constraint violation")
        is android.database.sqlite.SQLiteDiskIOException -> FGOBotError.DatabaseError.DiskFull
        is android.database.sqlite.SQLiteException -> 
            FGOBotError.DatabaseError.Unknown(message ?: "Database error", this)
        is SecurityException -> 
            FGOBotError.ApplicationError.PermissionDenied(message ?: "Unknown permission")
        is IllegalStateException -> 
            FGOBotError.ApplicationError.InvalidState(message ?: "Invalid application state")
        is IllegalArgumentException -> 
            FGOBotError.ApplicationError.ValidationError("argument", message ?: "Invalid argument")
        else -> FGOBotError.ApplicationError.Unknown(message ?: "Unknown error", this)
    }
} 