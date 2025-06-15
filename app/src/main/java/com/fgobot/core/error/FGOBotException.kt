package com.fgobot.core.error

sealed class FGOBotException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    
    // Network related errors
    class NetworkException(message: String, cause: Throwable? = null) : FGOBotException(message, cause)
    class ApiException(message: String, val code: Int, cause: Throwable? = null) : FGOBotException(message, cause)
    class TimeoutException(message: String, cause: Throwable? = null) : FGOBotException(message, cause)
    
    // Database related errors
    class DatabaseException(message: String, cause: Throwable? = null) : FGOBotException(message, cause)
    class DataNotFoundException(message: String) : FGOBotException(message)
    
    // Automation related errors
    class ScreenCaptureException(message: String, cause: Throwable? = null) : FGOBotException(message, cause)
    class ImageRecognitionException(message: String, cause: Throwable? = null) : FGOBotException(message, cause)
    class InputException(message: String, cause: Throwable? = null) : FGOBotException(message, cause)
    class BattleStateException(message: String) : FGOBotException(message)
    
    // Team building related errors
    class TeamBuildingException(message: String) : FGOBotException(message)
    class InsufficientResourcesException(message: String) : FGOBotException(message)
    
    // Permission related errors
    class PermissionException(message: String) : FGOBotException(message)
    class AccessibilityServiceException(message: String) : FGOBotException(message)
    
    // Configuration errors
    class ConfigurationException(message: String) : FGOBotException(message)
    class InvalidSettingsException(message: String) : FGOBotException(message)
    
    // Unknown errors
    class UnknownException(message: String, cause: Throwable? = null) : FGOBotException(message, cause)
} 