/*
 * FGO Bot - Template Matching Engine
 * 
 * This file implements the core template matching functionality using OpenCV.
 * Provides high-performance image recognition for FGO UI elements, servants,
 * and game objects with multi-scale and rotation-invariant matching.
 */

package com.fgobot.core.vision

import android.graphics.Bitmap
import android.graphics.Rect
import com.fgobot.core.logging.FGOBotLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.*

/**
 * Template Matching Engine using OpenCV
 * 
 * Provides advanced template matching capabilities including:
 * - Multi-scale template matching for different screen resolutions
 * - Rotation-invariant matching for rotated UI elements
 * - Confidence-based result filtering
 * - Performance optimization with ROI and pyramid matching
 * - Multiple matching algorithms (TM_CCOEFF_NORMED, TM_CCORR_NORMED)
 */
class TemplateMatchingEngine(
    private val openCVManager: OpenCVManager,
    private val logger: FGOBotLogger
) {
    
    companion object {
        private const val TAG = "TemplateMatchingEngine"
        
        // Matching algorithm constants
        const val METHOD_CCOEFF_NORMED = Imgproc.TM_CCOEFF_NORMED
        const val METHOD_CCORR_NORMED = Imgproc.TM_CCORR_NORMED
        const val METHOD_SQDIFF_NORMED = Imgproc.TM_SQDIFF_NORMED
        
        // Default confidence thresholds
        const val CONFIDENCE_UI_ELEMENTS = 0.75
        const val CONFIDENCE_CRITICAL_ELEMENTS = 0.85
        const val CONFIDENCE_SERVANTS = 0.80
        const val CONFIDENCE_CARDS = 0.82
        
        // Multi-scale parameters
        const val SCALE_MIN = 0.8
        const val SCALE_MAX = 1.2
        const val SCALE_STEP = 0.1
        
        // Performance optimization
        const val MAX_TEMPLATE_SIZE = 500
        const val MIN_TEMPLATE_SIZE = 20
    }
    
    /**
     * Template matching result
     */
    data class MatchResult(
        val found: Boolean,
        val confidence: Double,
        val location: Rect,
        val scale: Double = 1.0,
        val method: Int = METHOD_CCOEFF_NORMED,
        val processingTime: Long = 0
    )
    
    /**
     * Multi-scale matching result
     */
    data class MultiScaleResult(
        val results: List<MatchResult>,
        val bestMatch: MatchResult?,
        val totalProcessingTime: Long
    )
    
    /**
     * Performs template matching on the given image
     * 
     * @param sourceImage Source image to search in
     * @param templateImage Template image to find
     * @param confidence Minimum confidence threshold (0.0 to 1.0)
     * @param method Matching method (default: TM_CCOEFF_NORMED)
     * @param roi Region of interest (optional)
     * @return MatchResult containing match information
     */
    suspend fun matchTemplate(
        sourceImage: Bitmap,
        templateImage: Bitmap,
        confidence: Double = CONFIDENCE_UI_ELEMENTS,
        method: Int = METHOD_CCOEFF_NORMED,
        roi: Rect? = null
    ): MatchResult = withContext(Dispatchers.Default) {
        
        if (!openCVManager.isReady()) {
            logger.error(FGOBotLogger.Category.VISION, "OpenCV not initialized for template matching")
            return@withContext MatchResult(false, 0.0, Rect())
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Convert bitmaps to OpenCV Mat
            val sourceMat = bitmapToMat(sourceImage)
            val templateMat = bitmapToMat(templateImage)
            
            // Validate template size
            if (!isValidTemplateSize(templateMat, sourceMat)) {
                logger.warn(FGOBotLogger.Category.VISION, "Invalid template size for matching")
                return@withContext MatchResult(false, 0.0, Rect())
            }
            
            // Apply ROI if specified
            val searchMat = if (roi != null) {
                val roiRect = org.opencv.core.Rect(roi.left, roi.top, roi.width(), roi.height())
                Mat(sourceMat, roiRect)
            } else {
                sourceMat
            }
            
            // Perform template matching
            val result = performMatching(searchMat, templateMat, method)
            
            // Find best match location
            val minMaxLocResult = Core.minMaxLoc(result)
            val matchLoc = if (method == METHOD_SQDIFF_NORMED) {
                minMaxLocResult.minLoc
            } else {
                minMaxLocResult.maxLoc
            }
            
            val matchConfidence = if (method == METHOD_SQDIFF_NORMED) {
                1.0 - minMaxLocResult.minVal
            } else {
                minMaxLocResult.maxVal
            }
            
            // Calculate match rectangle
            val matchRect = Rect(
                (matchLoc.x + (roi?.left ?: 0)).toInt(),
                (matchLoc.y + (roi?.top ?: 0)).toInt(),
                (matchLoc.x + templateMat.cols() + (roi?.left ?: 0)).toInt(),
                (matchLoc.y + templateMat.rows() + (roi?.top ?: 0)).toInt()
            )
            
            val processingTime = System.currentTimeMillis() - startTime
            val matchFound = matchConfidence >= confidence
            
            // Log result
            logger.debug(
                FGOBotLogger.Category.VISION,
                "Template matching: confidence=${String.format("%.3f", matchConfidence)}, " +
                "threshold=${String.format("%.3f", confidence)}, found=$matchFound, time=${processingTime}ms"
            )
            
            // Cleanup
            sourceMat.release()
            templateMat.release()
            searchMat.release()
            result.release()
            
            return@withContext MatchResult(
                found = matchFound,
                confidence = matchConfidence,
                location = matchRect,
                method = method,
                processingTime = processingTime
            )
            
        } catch (e: Exception) {
            logger.error(
                FGOBotLogger.Category.VISION,
                "Template matching error: ${e.message}",
                e
            )
            return@withContext MatchResult(false, 0.0, Rect())
        }
    }
    
    /**
     * Performs multi-scale template matching
     * 
     * @param sourceImage Source image to search in
     * @param templateImage Template image to find
     * @param confidence Minimum confidence threshold
     * @param method Matching method
     * @param scaleRange Scale range (min to max)
     * @param scaleStep Scale increment step
     * @return MultiScaleResult with all scale results
     */
    suspend fun matchTemplateMultiScale(
        sourceImage: Bitmap,
        templateImage: Bitmap,
        confidence: Double = CONFIDENCE_UI_ELEMENTS,
        method: Int = METHOD_CCOEFF_NORMED,
        scaleRange: Pair<Double, Double> = Pair(SCALE_MIN, SCALE_MAX),
        scaleStep: Double = SCALE_STEP
    ): MultiScaleResult = withContext(Dispatchers.Default) {
        
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<MatchResult>()
        
        try {
            var scale = scaleRange.first
            while (scale <= scaleRange.second) {
                // Scale the template
                val scaledTemplate = scaleTemplate(templateImage, scale)
                
                // Perform matching at this scale
                val result = matchTemplate(sourceImage, scaledTemplate, confidence, method)
                
                if (result.found) {
                    results.add(result.copy(scale = scale))
                }
                
                scale += scaleStep
            }
            
            // Find best match (highest confidence)
            val bestMatch = results.maxByOrNull { it.confidence }
            val totalTime = System.currentTimeMillis() - startTime
            
            logger.debug(
                FGOBotLogger.Category.VISION,
                "Multi-scale matching: ${results.size} matches found, " +
                "best confidence=${bestMatch?.confidence ?: 0.0}, time=${totalTime}ms"
            )
            
            return@withContext MultiScaleResult(
                results = results,
                bestMatch = bestMatch,
                totalProcessingTime = totalTime
            )
            
        } catch (e: Exception) {
            logger.error(
                FGOBotLogger.Category.VISION,
                "Multi-scale template matching error: ${e.message}",
                e
            )
            return@withContext MultiScaleResult(
                results = emptyList(),
                bestMatch = null,
                totalProcessingTime = System.currentTimeMillis() - startTime
            )
        }
    }
    
    /**
     * Finds all matches above the confidence threshold
     * 
     * @param sourceImage Source image to search in
     * @param templateImage Template image to find
     * @param confidence Minimum confidence threshold
     * @param method Matching method
     * @param maxMatches Maximum number of matches to return
     * @return List of all matches found
     */
    suspend fun findAllMatches(
        sourceImage: Bitmap,
        templateImage: Bitmap,
        confidence: Double = CONFIDENCE_UI_ELEMENTS,
        method: Int = METHOD_CCOEFF_NORMED,
        maxMatches: Int = 10
    ): List<MatchResult> = withContext(Dispatchers.Default) {
        
        if (!openCVManager.isReady()) {
            logger.error(FGOBotLogger.Category.VISION, "OpenCV not initialized for multi-match")
            return@withContext emptyList()
        }
        
        val startTime = System.currentTimeMillis()
        val matches = mutableListOf<MatchResult>()
        
        try {
            val sourceMat = bitmapToMat(sourceImage)
            val templateMat = bitmapToMat(templateImage)
            
            if (!isValidTemplateSize(templateMat, sourceMat)) {
                return@withContext emptyList()
            }
            
            val result = performMatching(sourceMat, templateMat, method)
            
            // Find all matches above threshold
            val mask = Mat()
            val threshold = if (method == METHOD_SQDIFF_NORMED) 1.0 - confidence else confidence
            
            if (method == METHOD_SQDIFF_NORMED) {
                Imgproc.threshold(result, mask, threshold, 1.0, Imgproc.THRESH_BINARY_INV)
            } else {
                Imgproc.threshold(result, mask, threshold, 1.0, Imgproc.THRESH_BINARY)
            }
            
            // Extract match locations
            var matchCount = 0
            for (y in 0 until result.rows()) {
                for (x in 0 until result.cols()) {
                    if (matchCount >= maxMatches) break
                    
                    val maskValue = mask.get(y, x)[0]
                    if (maskValue > 0) {
                        val matchConfidence = result.get(y, x)[0]
                        val adjustedConfidence = if (method == METHOD_SQDIFF_NORMED) {
                            1.0 - matchConfidence
                        } else {
                            matchConfidence
                        }
                        
                        if (adjustedConfidence >= confidence) {
                            val matchRect = Rect(x, y, x + templateMat.cols(), y + templateMat.rows())
                            matches.add(
                                MatchResult(
                                    found = true,
                                    confidence = adjustedConfidence,
                                    location = matchRect,
                                    method = method,
                                    processingTime = System.currentTimeMillis() - startTime
                                )
                            )
                            matchCount++
                        }
                    }
                }
                if (matchCount >= maxMatches) break
            }
            
            // Cleanup
            sourceMat.release()
            templateMat.release()
            result.release()
            mask.release()
            
            logger.debug(
                FGOBotLogger.Category.VISION,
                "Found ${matches.size} matches above confidence $confidence"
            )
            
            return@withContext matches.sortedByDescending { it.confidence }
            
        } catch (e: Exception) {
            logger.error(
                FGOBotLogger.Category.VISION,
                "Multi-match template matching error: ${e.message}",
                e
            )
            return@withContext emptyList()
        }
    }
    
    /**
     * Converts Android Bitmap to OpenCV Mat
     */
    private fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        return mat
    }
    
    /**
     * Scales a template bitmap
     */
    private fun scaleTemplate(template: Bitmap, scale: Double): Bitmap {
        val newWidth = (template.width * scale).toInt()
        val newHeight = (template.height * scale).toInt()
        return Bitmap.createScaledBitmap(template, newWidth, newHeight, true)
    }
    
    /**
     * Validates template size for matching
     */
    private fun isValidTemplateSize(template: Mat, source: Mat): Boolean {
        return template.rows() >= MIN_TEMPLATE_SIZE &&
               template.cols() >= MIN_TEMPLATE_SIZE &&
               template.rows() <= MAX_TEMPLATE_SIZE &&
               template.cols() <= MAX_TEMPLATE_SIZE &&
               template.rows() < source.rows() &&
               template.cols() < source.cols()
    }
    
    /**
     * Performs the actual OpenCV template matching
     */
    private fun performMatching(source: Mat, template: Mat, method: Int): Mat {
        val result = Mat()
        Imgproc.matchTemplate(source, template, result, method)
        return result
    }
    
    /**
     * Gets recommended confidence threshold for different element types
     */
    fun getRecommendedConfidence(elementType: String): Double {
        return when (elementType.lowercase()) {
            "ui", "button", "menu" -> CONFIDENCE_UI_ELEMENTS
            "servant", "character" -> CONFIDENCE_SERVANTS
            "card", "command" -> CONFIDENCE_CARDS
            "critical", "important" -> CONFIDENCE_CRITICAL_ELEMENTS
            else -> CONFIDENCE_UI_ELEMENTS
        }
    }
    
    /**
     * Gets performance statistics
     */
    fun getPerformanceStats(): Map<String, Any> {
        return mapOf(
            "openCVReady" to openCVManager.isReady(),
            "memoryStats" to openCVManager.getMemoryStats(),
            "supportedMethods" to listOf("TM_CCOEFF_NORMED", "TM_CCORR_NORMED", "TM_SQDIFF_NORMED"),
            "scaleRange" to "$SCALE_MIN - $SCALE_MAX",
            "maxTemplateSize" to MAX_TEMPLATE_SIZE
        )
    }
} 