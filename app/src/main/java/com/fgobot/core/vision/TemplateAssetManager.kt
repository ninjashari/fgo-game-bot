/*
 * FGO Bot - Template Asset Manager
 * 
 * This file manages template assets for image recognition, including loading,
 * caching, validation, and optimization of template images used for matching
 * FGO UI elements, servants, and game objects.
 */

package com.fgobot.core.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.fgobot.core.logging.FGOBotLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Template Asset Manager
 * 
 * Manages template assets for image recognition including:
 * - Loading templates from assets directory
 * - Template caching and memory management
 * - Template validation and quality control
 * - Asset versioning and updates
 * - Performance optimization
 */
class TemplateAssetManager(
    private val context: Context,
    private val logger: FGOBotLogger
) {
    
    companion object {
        private const val TAG = "TemplateAssetManager"
        private const val TEMPLATES_DIR = "templates"
        private const val MAX_CACHE_SIZE = 100
        private const val MAX_TEMPLATE_WIDTH = 1000
        private const val MAX_TEMPLATE_HEIGHT = 1000
        private const val MIN_TEMPLATE_SIZE = 10
    }
    
    // Template cache
    private val templateCache = ConcurrentHashMap<String, Bitmap>()
    private val templateMetadata = ConcurrentHashMap<String, TemplateMetadata>()
    private val loadingMutex = Mutex()
    
    // Statistics
    private var totalLoadTime: Long = 0
    private var loadedTemplates: Int = 0
    private var cacheHits: Long = 0
    private var cacheMisses: Long = 0
    
    private var isInitialized = false
    
    /**
     * Template metadata
     */
    data class TemplateMetadata(
        val name: String,
        val width: Int,
        val height: Int,
        val fileSize: Long,
        val loadTime: Long,
        val category: String,
        val confidence: Double = 0.8
    )
    
    /**
     * Template categories with their default confidence thresholds
     */
    private val templateCategories = mapOf(
        "ui_elements" to 0.75,
        "buttons" to 0.80,
        "screens" to 0.70,
        "servants" to 0.85,
        "cards" to 0.82,
        "enemies" to 0.80,
        "craft_essences" to 0.83,
        "status" to 0.78
    )
    
    /**
     * Initializes the template asset manager
     */
    suspend fun initialize() {
        loadingMutex.withLock {
            if (isInitialized) {
                logger.debug(FGOBotLogger.Category.VISION, "Template asset manager already initialized")
                return
            }
            
            logger.info(FGOBotLogger.Category.VISION, "Initializing template asset manager")
            
            try {
                // Load essential templates first
                loadEssentialTemplates()
                
                isInitialized = true
                
                logger.info(
                    FGOBotLogger.Category.VISION,
                    "Template asset manager initialized with ${templateCache.size} templates"
                )
                
            } catch (e: Exception) {
                logger.error(
                    FGOBotLogger.Category.VISION,
                    "Failed to initialize template asset manager: ${e.message}",
                    e
                )
                throw e
            }
        }
    }
    
    /**
     * Gets a template by name
     * 
     * @param templateName Name of the template
     * @return Template bitmap or null if not found
     */
    suspend fun getTemplate(templateName: String): Bitmap? {
        return if (templateCache.containsKey(templateName)) {
            cacheHits++
            templateCache[templateName]
        } else {
            cacheMisses++
            loadTemplate(templateName)
        }
    }
    
    /**
     * Loads a template from assets
     * 
     * @param templateName Name of the template to load
     * @return Loaded template bitmap or null if failed
     */
    private suspend fun loadTemplate(templateName: String): Bitmap? = withContext(Dispatchers.IO) {
        loadingMutex.withLock {
            // Check cache again in case it was loaded while waiting for lock
            if (templateCache.containsKey(templateName)) {
                return@withContext templateCache[templateName]
            }
            
            val startTime = System.currentTimeMillis()
            
            try {
                // Try different file extensions and paths
                val possiblePaths = generateTemplatePaths(templateName)
                
                for (path in possiblePaths) {
                    try {
                        val inputStream: InputStream = context.assets.open(path)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        
                        if (bitmap != null && validateTemplate(bitmap, templateName)) {
                            val optimizedBitmap = optimizeTemplate(bitmap)
                            val loadTime = System.currentTimeMillis() - startTime
                            
                            // Cache the template
                            templateCache[templateName] = optimizedBitmap
                            
                            // Store metadata
                            val category = extractCategory(path)
                            templateMetadata[templateName] = TemplateMetadata(
                                name = templateName,
                                width = optimizedBitmap.width,
                                height = optimizedBitmap.height,
                                fileSize = estimateFileSize(optimizedBitmap),
                                loadTime = loadTime,
                                category = category,
                                confidence = templateCategories[category] ?: 0.8
                            )
                            
                            totalLoadTime += loadTime
                            loadedTemplates++
                            
                            logger.debug(
                                FGOBotLogger.Category.VISION,
                                "Loaded template '$templateName' from '$path' in ${loadTime}ms"
                            )
                            
                            // Cleanup cache if needed
                            if (templateCache.size > MAX_CACHE_SIZE) {
                                cleanupCache()
                            }
                            
                            return@withContext optimizedBitmap
                        }
                    } catch (e: IOException) {
                        // Try next path
                        continue
                    }
                }
                
                logger.warn(FGOBotLogger.Category.VISION, "Template not found: $templateName")
                return@withContext null
                
            } catch (e: Exception) {
                logger.error(
                    FGOBotLogger.Category.VISION,
                    "Error loading template '$templateName': ${e.message}",
                    e
                )
                return@withContext null
            }
        }
    }
    
    /**
     * Loads essential templates that are needed for basic functionality
     */
    private suspend fun loadEssentialTemplates() {
        val essentialTemplates = listOf(
            // Screen state templates
            "quest_selection_screen",
            "support_selection_screen",
            "battle_start_screen",
            "command_selection_screen",
            "skill_selection_screen",
            "np_selection_screen",
            "battle_result_screen",
            "ap_recovery_screen",
            
            // UI elements
            "attack_button",
            "menu_button",
            "back_button",
            "confirm_button",
            "cancel_button",
            
            // Card types
            "card_arts",
            "card_buster",
            "card_quick",
            "card_np",
            
            // Basic UI indicators
            "hp_bar",
            "np_gauge",
            "skill_ready",
            "skill_cooldown"
        )
        
        logger.info(FGOBotLogger.Category.VISION, "Loading ${essentialTemplates.size} essential templates")
        
        for (templateName in essentialTemplates) {
            try {
                loadTemplate(templateName)
            } catch (e: Exception) {
                logger.warn(
                    FGOBotLogger.Category.VISION,
                    "Failed to load essential template '$templateName': ${e.message}"
                )
            }
        }
        
        logger.info(FGOBotLogger.Category.VISION, "Loaded ${templateCache.size} essential templates")
    }
    
    /**
     * Generates possible paths for a template
     */
    private fun generateTemplatePaths(templateName: String): List<String> {
        val paths = mutableListOf<String>()
        
        // Direct path
        paths.add("$TEMPLATES_DIR/$templateName.png")
        paths.add("$TEMPLATES_DIR/$templateName.jpg")
        
        // Category-based paths
        for (category in templateCategories.keys) {
            paths.add("$TEMPLATES_DIR/$category/$templateName.png")
            paths.add("$TEMPLATES_DIR/$category/$templateName.jpg")
        }
        
        // Resolution-specific paths
        val resolutions = listOf("1080p", "720p", "1440p")
        for (resolution in resolutions) {
            paths.add("$TEMPLATES_DIR/$resolution/$templateName.png")
            paths.add("$TEMPLATES_DIR/$resolution/$templateName.jpg")
        }
        
        return paths
    }
    
    /**
     * Validates a template bitmap
     */
    private fun validateTemplate(bitmap: Bitmap, templateName: String): Boolean {
        if (bitmap.width < MIN_TEMPLATE_SIZE || bitmap.height < MIN_TEMPLATE_SIZE) {
            logger.warn(
                FGOBotLogger.Category.VISION,
                "Template '$templateName' too small: ${bitmap.width}x${bitmap.height}"
            )
            return false
        }
        
        if (bitmap.width > MAX_TEMPLATE_WIDTH || bitmap.height > MAX_TEMPLATE_HEIGHT) {
            logger.warn(
                FGOBotLogger.Category.VISION,
                "Template '$templateName' too large: ${bitmap.width}x${bitmap.height}"
            )
            return false
        }
        
        if (bitmap.isRecycled) {
            logger.warn(FGOBotLogger.Category.VISION, "Template '$templateName' bitmap is recycled")
            return false
        }
        
        return true
    }
    
    /**
     * Optimizes a template bitmap for better performance
     */
    private fun optimizeTemplate(bitmap: Bitmap): Bitmap {
        // Convert to RGB_565 for better performance if it's not already
        if (bitmap.config != Bitmap.Config.RGB_565) {
            val optimized = bitmap.copy(Bitmap.Config.RGB_565, false)
            if (optimized != null) {
                return optimized
            }
        }
        
        return bitmap
    }
    
    /**
     * Extracts category from file path
     */
    private fun extractCategory(path: String): String {
        for (category in templateCategories.keys) {
            if (path.contains("/$category/")) {
                return category
            }
        }
        return "general"
    }
    
    /**
     * Estimates file size of a bitmap
     */
    private fun estimateFileSize(bitmap: Bitmap): Long {
        return (bitmap.width * bitmap.height * when (bitmap.config) {
            Bitmap.Config.ARGB_8888 -> 4
            Bitmap.Config.RGB_565 -> 2
            Bitmap.Config.ARGB_4444 -> 2
            Bitmap.Config.ALPHA_8 -> 1
            else -> 4
        }).toLong()
    }
    
    /**
     * Cleans up cache by removing least recently used templates
     */
    private fun cleanupCache() {
        if (templateCache.size <= MAX_CACHE_SIZE) return
        
        logger.debug(FGOBotLogger.Category.VISION, "Cleaning up template cache")
        
        // Simple cleanup - remove 20% of templates
        val toRemove = templateCache.size - (MAX_CACHE_SIZE * 0.8).toInt()
        val keysToRemove = templateCache.keys.take(toRemove)
        
        for (key in keysToRemove) {
            templateCache.remove(key)
            templateMetadata.remove(key)
        }
        
        logger.debug(
            FGOBotLogger.Category.VISION,
            "Removed $toRemove templates from cache, ${templateCache.size} remaining"
        )
    }
    
    /**
     * Gets template metadata
     */
    fun getTemplateMetadata(templateName: String): TemplateMetadata? {
        return templateMetadata[templateName]
    }
    
    /**
     * Gets all loaded template names
     */
    fun getLoadedTemplates(): Set<String> {
        return templateCache.keys.toSet()
    }
    
    /**
     * Gets cache statistics
     */
    fun getStats(): Map<String, Any> {
        val totalMemory = templateCache.values.sumOf { estimateFileSize(it) }
        
        return mapOf(
            "loadedTemplates" to loadedTemplates,
            "cachedTemplates" to templateCache.size,
            "totalLoadTime" to totalLoadTime,
            "averageLoadTime" to if (loadedTemplates > 0) totalLoadTime / loadedTemplates else 0,
            "cacheHits" to cacheHits,
            "cacheMisses" to cacheMisses,
            "cacheHitRate" to if (cacheHits + cacheMisses > 0) cacheHits.toDouble() / (cacheHits + cacheMisses) else 0.0,
            "totalMemoryUsage" to totalMemory,
            "averageTemplateSize" to if (templateCache.isNotEmpty()) totalMemory / templateCache.size else 0,
            "categories" to templateCategories.keys.toList()
        )
    }
    
    /**
     * Checks if the manager is ready
     */
    fun isReady(): Boolean {
        return isInitialized && templateCache.isNotEmpty()
    }
    
    /**
     * Preloads templates for a specific category
     */
    suspend fun preloadCategory(category: String) {
        logger.info(FGOBotLogger.Category.VISION, "Preloading templates for category: $category")
        
        // This would require scanning the assets directory
        // For now, we'll just log the intent
        logger.debug(FGOBotLogger.Category.VISION, "Category preloading not yet implemented")
    }
    
    /**
     * Clears the template cache
     */
    fun clearCache() {
        logger.info(FGOBotLogger.Category.VISION, "Clearing template cache")
        
        templateCache.clear()
        templateMetadata.clear()
        
        // Reset statistics
        cacheHits = 0
        cacheMisses = 0
        
        logger.debug(FGOBotLogger.Category.VISION, "Template cache cleared")
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        logger.info(FGOBotLogger.Category.VISION, "Cleaning up template asset manager")
        
        // Recycle bitmaps to free memory
        templateCache.values.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        
        clearCache()
        isInitialized = false
        
        logger.debug(FGOBotLogger.Category.VISION, "Template asset manager cleanup completed")
    }
}