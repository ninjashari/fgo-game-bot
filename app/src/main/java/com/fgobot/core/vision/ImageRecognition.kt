/*
 * FGO Bot - Image Recognition System
 * 
 * This file implements the core image recognition functionality using OpenCV.
 * Provides template matching, UI element detection, and battle state recognition.
 * 
 * Phase 4 Update: Integrated with OpenCV-based TemplateMatchingEngine for real functionality.
 */

package com.fgobot.core.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.fgobot.core.logging.FGOBotLogger
import kotlinx.coroutines.runBlocking
import kotlin.math.max
import kotlin.math.min

/**
 * Template matching result
 */
data class MatchResult(
    val found: Boolean,
    val confidence: Double,
    val location: android.graphics.Point,
    val boundingRect: Rect,
    val templateName: String,
    val processingTime: Long = 0
)

/**
 * Recognition region for optimized searching
 */
data class RecognitionRegion(
    val name: String,
    val rect: Rect,
    val description: String
)

/**
 * Battle state enumeration
 */
enum class BattleState {
    UNKNOWN,
    QUEST_SELECTION,
    SUPPORT_SELECTION,
    BATTLE_START,
    COMMAND_SELECTION,
    SKILL_SELECTION,
    NP_SELECTION,
    BATTLE_RESULT,
    AP_RECOVERY,
    ERROR_STATE
}

/**
 * Image recognition system using OpenCV
 * 
 * Provides comprehensive image analysis capabilities for FGO automation.
 * Optimized for performance with template caching and region-based searching.
 * 
 * Phase 4 Update: Now uses actual OpenCV template matching engine.
 */
class ImageRecognition(
    private val context: Context,
    private val logger: FGOBotLogger
) {
    
    companion object {
        private const val TAG = "ImageRecognition"
        private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.8
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.9
        private const val LOW_CONFIDENCE_THRESHOLD = 0.7
        
        // Pre-defined recognition regions for FGO UI
        val REGIONS = mapOf(
            "attack_button" to RecognitionRegion(
                "attack_button",
                Rect(800, 1600, 1080, 1720),
                "Attack button area"
            ),
            "command_cards" to RecognitionRegion(
                "command_cards",
                Rect(0, 1200, 1080, 1600),
                "Command card selection area"
            ),
            "servant_skills" to RecognitionRegion(
                "servant_skills",
                Rect(0, 1000, 1080, 1200),
                "Servant skills area"
            ),
            "master_skills" to RecognitionRegion(
                "master_skills",
                Rect(900, 100, 180, 200),
                "Master skills area"
            ),
            "support_list" to RecognitionRegion(
                "support_list",
                Rect(0, 300, 1080, 1200),
                "Support servant list area"
            ),
            "battle_menu" to RecognitionRegion(
                "battle_menu",
                Rect(0, 0, 1080, 300),
                "Battle menu and status area"
            )
        )
    }
    
    // OpenCV components
    private lateinit var openCVManager: OpenCVManager
    private lateinit var templateMatchingEngine: TemplateMatchingEngine
    private lateinit var templateAssetManager: TemplateAssetManager
    
    private var isInitialized = false
    private val recognitionStats = mutableMapOf<String, RecognitionStats>()
    
    /**
     * Initializes the image recognition system
     * 
     * @return True if initialization successful
     */
    suspend fun initialize(): Boolean {
        return try {
            logger.info(FGOBotLogger.Category.VISION, "Initializing image recognition system with OpenCV")
            
            // Initialize OpenCV manager
            openCVManager = OpenCVManager.getInstance(context, logger)
            if (!openCVManager.initialize()) {
                logger.error(FGOBotLogger.Category.VISION, "Failed to initialize OpenCV")
                return false
            }
            
            // Initialize template matching engine
            templateMatchingEngine = TemplateMatchingEngine(openCVManager, logger)
            
            // Initialize template asset manager
            templateAssetManager = TemplateAssetManager(context, logger)
            templateAssetManager.initialize()
            
            isInitialized = true
            
            logger.info(FGOBotLogger.Category.VISION, "Image recognition system initialized successfully")
            logger.info(FGOBotLogger.Category.VISION, "OpenCV Version: ${openCVManager.getVersion()}")
            
            true
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.VISION, "Failed to initialize image recognition", e)
            false
        }
    }
    
    /**
     * Detects the current battle state from screenshot
     * 
     * @param screenshot Current screen bitmap
     * @return Detected battle state
     */
    suspend fun detectBattleState(screenshot: Bitmap): BattleState {
        if (!isInitialized) {
            return BattleState.UNKNOWN
        }
        
        return try {
            val startTime = System.currentTimeMillis()
            
            // Check for different battle states using template matching
            val stateTemplates = mapOf(
                BattleState.QUEST_SELECTION to "quest_selection_screen",
                BattleState.SUPPORT_SELECTION to "support_selection_screen",
                BattleState.BATTLE_START to "battle_start_screen",
                BattleState.COMMAND_SELECTION to "command_selection_screen",
                BattleState.SKILL_SELECTION to "skill_selection_screen",
                BattleState.NP_SELECTION to "np_selection_screen",
                BattleState.BATTLE_RESULT to "battle_result_screen",
                BattleState.AP_RECOVERY to "ap_recovery_screen"
            )
            
            var detectedState = BattleState.UNKNOWN
            var highestConfidence = 0.0
            
            for ((state, templateName) in stateTemplates) {
                val template = templateAssetManager.getTemplate(templateName)
                if (template != null) {
                    val result = templateMatchingEngine.matchTemplate(
                        screenshot,
                        template,
                        confidence = 0.7 // Lower threshold for state detection
                    )
                    
                    if (result.found && result.confidence > highestConfidence) {
                        detectedState = state
                        highestConfidence = result.confidence
                    }
                }
            }
            
            val processingTime = System.currentTimeMillis() - startTime
            logger.debug(
                FGOBotLogger.Category.VISION,
                "Battle state detection completed in ${processingTime}ms: $detectedState (confidence: ${"%.3f".format(highestConfidence)})"
            )
            
            detectedState
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.VISION, "Error detecting battle state", e)
            BattleState.ERROR_STATE
        }
    }
    
    /**
     * Finds a template in the screenshot
     * 
     * @param screenshot Screenshot to search in
     * @param templateName Name of template to find
     * @param region Optional region to limit search area
     * @param confidenceThreshold Minimum confidence for match
     * @return Match result
     */
    suspend fun findTemplate(
        screenshot: Bitmap,
        templateName: String,
        region: RecognitionRegion? = null,
        confidenceThreshold: Double = DEFAULT_CONFIDENCE_THRESHOLD
    ): MatchResult {
        if (!isInitialized) {
            return MatchResult(false, 0.0, android.graphics.Point(), Rect(), templateName)
        }
        
        return try {
            val startTime = System.currentTimeMillis()
            
            // Get template from asset manager
            val template = templateAssetManager.getTemplate(templateName)
            if (template == null) {
                logger.warn(FGOBotLogger.Category.VISION, "Template not found: $templateName")
                return MatchResult(false, 0.0, android.graphics.Point(), Rect(), templateName)
            }
            
            // Convert region to Rect for OpenCV
            val roi = region?.rect
            
            // Perform template matching
            val result = templateMatchingEngine.matchTemplate(
                screenshot,
                template,
                confidenceThreshold,
                roi = roi
            )
            
            val processingTime = System.currentTimeMillis() - startTime
            
            // Convert result to our format
            val matchResult = MatchResult(
                found = result.found,
                confidence = result.confidence,
                location = android.graphics.Point(result.location.left, result.location.top),
                boundingRect = result.location,
                templateName = templateName,
                processingTime = processingTime
            )
            
            // Update statistics
            updateRecognitionStats(templateName, result.found, result.confidence, processingTime)
            
            logger.debug(
                FGOBotLogger.Category.VISION,
                "Template matching '$templateName': found=${result.found}, confidence=${"%.3f".format(result.confidence)}, time=${processingTime}ms"
            )
            
            matchResult
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.VISION, "Error in template matching for $templateName", e)
            MatchResult(false, 0.0, android.graphics.Point(), Rect(), templateName)
        }
    }
    
    /**
     * Finds multiple instances of a template
     * 
     * @param screenshot Screenshot to search in
     * @param templateName Name of template to find
     * @param region Optional region to limit search area
     * @param confidenceThreshold Minimum confidence for matches
     * @param maxMatches Maximum number of matches to return
     * @return List of match results
     */
    suspend fun findMultipleTemplates(
        screenshot: Bitmap,
        templateName: String,
        region: RecognitionRegion? = null,
        confidenceThreshold: Double = DEFAULT_CONFIDENCE_THRESHOLD,
        maxMatches: Int = 10
    ): List<MatchResult> {
        if (!isInitialized) {
            return emptyList()
        }
        
        return try {
            val template = templateAssetManager.getTemplate(templateName)
            if (template == null) {
                logger.warn(FGOBotLogger.Category.VISION, "Template not found: $templateName")
                return emptyList()
            }
            
            // Use the template matching engine to find all matches
            val results = templateMatchingEngine.findAllMatches(
                screenshot,
                template,
                confidenceThreshold,
                maxMatches = maxMatches
            )
            
            // Convert results to our format
            results.map { result ->
                MatchResult(
                    found = result.found,
                    confidence = result.confidence,
                    location = android.graphics.Point(result.location.left, result.location.top),
                    boundingRect = result.location,
                    templateName = templateName,
                    processingTime = result.processingTime
                )
            }
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.VISION, "Error in multiple template matching for $templateName", e)
            emptyList()
        }
    }
    
    /**
     * Performs multi-scale template matching for better accuracy
     * 
     * @param screenshot Screenshot to search in
     * @param templateName Name of template to find
     * @param confidenceThreshold Minimum confidence for match
     * @return Best match result across all scales
     */
    suspend fun findTemplateMultiScale(
        screenshot: Bitmap,
        templateName: String,
        confidenceThreshold: Double = DEFAULT_CONFIDENCE_THRESHOLD
    ): MatchResult {
        if (!isInitialized) {
            return MatchResult(false, 0.0, android.graphics.Point(), Rect(), templateName)
        }
        
        return try {
            val template = templateAssetManager.getTemplate(templateName)
            if (template == null) {
                logger.warn(FGOBotLogger.Category.VISION, "Template not found: $templateName")
                return MatchResult(false, 0.0, android.graphics.Point(), Rect(), templateName)
            }
            
            val result = templateMatchingEngine.matchTemplateMultiScale(
                screenshot,
                template,
                confidenceThreshold
            )
            
            val bestMatch = result.bestMatch
            if (bestMatch != null) {
                MatchResult(
                    found = bestMatch.found,
                    confidence = bestMatch.confidence,
                    location = android.graphics.Point(bestMatch.location.left, bestMatch.location.top),
                    boundingRect = bestMatch.location,
                    templateName = templateName,
                    processingTime = result.totalProcessingTime
                )
            } else {
                MatchResult(false, 0.0, android.graphics.Point(), Rect(), templateName)
            }
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.VISION, "Error in multi-scale template matching for $templateName", e)
            MatchResult(false, 0.0, android.graphics.Point(), Rect(), templateName)
        }
    }
    
    /**
     * Gets recognition statistics
     * 
     * @return Map of template names to their statistics
     */
    fun getRecognitionStats(): Map<String, RecognitionStats> {
        return recognitionStats.toMap()
    }
    
    /**
     * Gets performance statistics from all components
     */
    fun getPerformanceStats(): Map<String, Any> {
        val stats = mutableMapOf<String, Any>()
        
        if (isInitialized) {
            stats["openCV"] = openCVManager.getInitializationMetrics()
            stats["templateMatching"] = templateMatchingEngine.getPerformanceStats()
            stats["templateAssets"] = templateAssetManager.getStats()
            stats["recognition"] = recognitionStats.mapValues { (_, stats) ->
                mapOf(
                    "successRate" to stats.successRate,
                    "averageProcessingTime" to stats.averageProcessingTime,
                    "totalAttempts" to stats.totalAttempts
                )
            }
        }
        
        return stats
    }
    
    /**
     * Updates recognition statistics
     */
    private fun updateRecognitionStats(
        templateName: String,
        found: Boolean,
        confidence: Double,
        processingTime: Long
    ) {
        val stats = recognitionStats.getOrPut(templateName) { RecognitionStats(templateName) }
        
        stats.totalAttempts++
        if (found) stats.successfulMatches++
        stats.totalProcessingTime += processingTime
        stats.averageConfidence = (stats.averageConfidence * (stats.totalAttempts - 1) + confidence) / stats.totalAttempts
        stats.lastProcessingTime = processingTime
        stats.lastConfidence = confidence
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        logger.info(FGOBotLogger.Category.VISION, "Cleaning up image recognition resources")
        
        if (isInitialized) {
            templateAssetManager.cleanup()
            openCVManager.cleanup()
        }
        
        recognitionStats.clear()
        isInitialized = false
        
        logger.info(FGOBotLogger.Category.VISION, "Image recognition cleanup completed")
    }
    
    /**
     * Performs a health check on all components
     */
    suspend fun healthCheck(): Boolean {
        return if (isInitialized) {
            openCVManager.healthCheck() && templateAssetManager.isReady()
        } else {
            false
        }
    }
}

/**
 * Recognition statistics for performance monitoring
 */
data class RecognitionStats(
    val templateName: String,
    var totalAttempts: Long = 0,
    var successfulMatches: Long = 0,
    var totalProcessingTime: Long = 0,
    var averageConfidence: Double = 0.0,
    var lastProcessingTime: Long = 0,
    var lastConfidence: Double = 0.0
) {
    val successRate: Double
        get() = if (totalAttempts > 0) successfulMatches.toDouble() / totalAttempts else 0.0
    
    val averageProcessingTime: Double
        get() = if (totalAttempts > 0) totalProcessingTime.toDouble() / totalAttempts else 0.0
} 