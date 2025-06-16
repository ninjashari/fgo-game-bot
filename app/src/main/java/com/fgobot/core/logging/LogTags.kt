/*
 * FGO Bot - Logging Tags Constants
 * 
 * This file defines standardized logging tags for different components of the FGO Bot system.
 * Provides consistent tag naming across all modules for better log filtering and debugging.
 */

package com.fgobot.core.logging

/**
 * Centralized logging tags for FGO Bot components
 * 
 * These constants provide standardized tag names for logging across different modules.
 * Each tag is prefixed with "FGOBot-" for easy identification in system logs.
 */
object LogTags {
    
    // Core System Tags
    const val VISION = "FGOBot-Vision"
    const val INPUT = "FGOBot-Input"
    const val STRATEGY = "FGOBot-Strategy"
    const val AUTOMATION = "FGOBot-Automation"
    
    // Vision System Tags
    const val SCREEN_CAPTURE = "FGOBot-ScreenCapture"
    const val IMAGE_RECOGNITION = "FGOBot-ImageRecognition"
    const val TEMPLATE_MATCHING = "FGOBot-TemplateMatching"
    
    // Input System Tags
    const val GESTURE_CONTROLLER = "FGOBot-GestureController"
    const val ACCESSIBILITY_SERVICE = "FGOBot-AccessibilityService"
    const val INPUT_SIMULATION = "FGOBot-InputSimulation"
    
    // Decision System Tags
    const val DECISION_ENGINE = "FGOBot-DecisionEngine"
    const val BATTLE_STRATEGY = "FGOBot-BattleStrategy"
    const val CARD_SELECTION = "FGOBot-CardSelection"
    
    // Data System Tags
    const val DATABASE = "FGOBot-Database"
    const val API_CLIENT = "FGOBot-ApiClient"
    const val CACHE_MANAGER = "FGOBot-CacheManager"
    const val SYNC_MANAGER = "FGOBot-SyncManager"
    
    // Performance Tags
    const val PERFORMANCE = "FGOBot-Performance"
    const val MEMORY = "FGOBot-Memory"
    const val NETWORK = "FGOBot-Network"
    
    // Error and Debug Tags
    const val ERROR = "FGOBot-Error"
    const val DEBUG = "FGOBot-Debug"
    const val TRACE = "FGOBot-Trace"
    
    // UI Tags
    const val UI = "FGOBot-UI"
    const val COMPOSE = "FGOBot-Compose"
    const val NAVIGATION = "FGOBot-Navigation"
} 