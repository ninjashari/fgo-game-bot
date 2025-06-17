/*
 * FGO Bot - Input Controller System
 * 
 * This file implements the input control functionality using AccessibilityService.
 * Provides touch gestures, swipes, and human-like interaction patterns.
 */

package com.fgobot.core.input

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import com.fgobot.core.logging.FGOBotLogger
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Gesture types supported by the input controller
 */
enum class GestureType {
    TAP,
    LONG_PRESS,
    SWIPE,
    DRAG,
    MULTI_TAP
}

/**
 * Gesture execution result
 */
sealed class GestureResult {
    object Success : GestureResult()
    data class Error(val message: String, val cause: Throwable? = null) : GestureResult()
    object Timeout : GestureResult()
}

/**
 * Human-like timing configuration
 */
data class TimingConfig(
    val minDelay: Long = 100L,
    val maxDelay: Long = 300L,
    val tapDuration: Long = 50L,
    val longPressDuration: Long = 1000L,
    val swipeSpeed: Float = 1000f, // pixels per second
    val randomVariation: Float = 0.2f // 20% variation
)

/**
 * Input controller system using AccessibilityService
 * 
 * Provides human-like input simulation with randomization and timing variations.
 * Designed to avoid detection while maintaining reliable automation.
 */
class InputController(
    private val accessibilityService: AccessibilityService,
    private val logger: FGOBotLogger,
    private val timingConfig: TimingConfig = TimingConfig()
) {
    
    companion object {
        private const val TAG = "InputController"
        private const val GESTURE_TIMEOUT = 5000L // 5 seconds
        private const val MAX_RETRY_ATTEMPTS = 3
    }
    
    private var gestureCount = 0L
    private var lastGestureTime = 0L
    private val gestureStats = mutableMapOf<GestureType, GestureStats>()
    
    /**
     * Checks if the input controller is ready to perform gestures
     * 
     * @return True if ready to perform gestures
     */
    fun isReady(): Boolean {
        return try {
            // Check if accessibility service is available and connected
            accessibilityService.serviceInfo != null
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error checking input controller readiness", e)
            false
        }
    }
    
    /**
     * Performs a tap gesture at the specified location
     * 
     * @param point Target location for tap
     * @param randomOffset Apply random offset to make tap more human-like
     * @return Gesture execution result
     */
    suspend fun tap(point: Point, randomOffset: Boolean = true): GestureResult {
        return performGesture(GestureType.TAP) {
            val targetPoint = if (randomOffset) {
                applyRandomOffset(point)
            } else {
                point
            }
            
            logger.debug(FGOBotLogger.Category.AUTOMATION, "Performing tap at (${targetPoint.x}, ${targetPoint.y})")
            
            val path = Path().apply {
                moveTo(targetPoint.x.toFloat(), targetPoint.y.toFloat())
            }
            
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, timingConfig.tapDuration))
                .build()
            
            executeGesture(gesture)
        }
    }
    
    /**
     * Performs a long press gesture at the specified location
     * 
     * @param point Target location for long press
     * @param duration Duration of the long press
     * @return Gesture execution result
     */
    suspend fun longPress(point: Point, duration: Long = timingConfig.longPressDuration): GestureResult {
        return performGesture(GestureType.LONG_PRESS) {
            val targetPoint = applyRandomOffset(point)
            
            logger.debug(FGOBotLogger.Category.AUTOMATION, "Performing long press at (${targetPoint.x}, ${targetPoint.y}) for ${duration}ms")
            
            val path = Path().apply {
                moveTo(targetPoint.x.toFloat(), targetPoint.y.toFloat())
            }
            
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                .build()
            
            executeGesture(gesture)
        }
    }
    
    /**
     * Performs a swipe gesture between two points
     * 
     * @param startPoint Starting point of swipe
     * @param endPoint Ending point of swipe
     * @param duration Duration of swipe (calculated if not provided)
     * @return Gesture execution result
     */
    suspend fun swipe(startPoint: Point, endPoint: Point, duration: Long? = null): GestureResult {
        return performGesture(GestureType.SWIPE) {
            val start = applyRandomOffset(startPoint)
            val end = applyRandomOffset(endPoint)
            
            val distance = calculateDistance(start, end)
            val swipeDuration = duration ?: (distance / timingConfig.swipeSpeed * 1000).toLong()
            
            logger.debug(FGOBotLogger.Category.AUTOMATION, 
                "Performing swipe from (${start.x}, ${start.y}) to (${end.x}, ${end.y}) in ${swipeDuration}ms")
            
            val path = Path().apply {
                moveTo(start.x.toFloat(), start.y.toFloat())
                lineTo(end.x.toFloat(), end.y.toFloat())
            }
            
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, swipeDuration))
                .build()
            
            executeGesture(gesture)
        }
    }
    
    /**
     * Performs multiple taps in sequence
     * 
     * @param points List of points to tap
     * @param delayBetweenTaps Delay between each tap
     * @return Gesture execution result
     */
    suspend fun multiTap(points: List<Point>, delayBetweenTaps: Long = 200L): GestureResult {
        return performGesture(GestureType.MULTI_TAP) {
            logger.debug(FGOBotLogger.Category.AUTOMATION, "Performing multi-tap on ${points.size} points")
            
            for ((index, point) in points.withIndex()) {
                val result = tap(point)
                if (result is GestureResult.Error) {
                    return@performGesture result
                }
                
                // Add delay between taps except for the last one
                if (index < points.size - 1) {
                    val actualDelay = addRandomVariation(delayBetweenTaps)
                    delay(actualDelay)
                }
            }
            
            GestureResult.Success
        }
    }
    
    /**
     * Taps on a UI element within a bounding rectangle
     * 
     * @param rect Bounding rectangle of the UI element
     * @param centerBias Bias towards center (0.0 = random, 1.0 = always center)
     * @return Gesture execution result
     */
    suspend fun tapInRect(rect: Rect, centerBias: Float = 0.7f): GestureResult {
        val centerX = rect.centerX()
        val centerY = rect.centerY()
        
        val targetX = if (Random.nextFloat() < centerBias) {
            // Bias towards center
            centerX + Random.nextInt(-rect.width() / 6, rect.width() / 6)
        } else {
            // Random within bounds
            Random.nextInt(rect.left + 10, rect.right - 10)
        }
        
        val targetY = if (Random.nextFloat() < centerBias) {
            // Bias towards center
            centerY + Random.nextInt(-rect.height() / 6, rect.height() / 6)
        } else {
            // Random within bounds
            Random.nextInt(rect.top + 10, rect.bottom - 10)
        }
        
        return tap(Point(targetX, targetY), randomOffset = false)
    }
    
    /**
     * Scrolls in the specified direction
     * 
     * @param direction Scroll direction
     * @param distance Scroll distance in pixels
     * @param startPoint Starting point for scroll (center of screen if not provided)
     * @return Gesture execution result
     */
    suspend fun scroll(
        direction: ScrollDirection,
        distance: Int = 500,
        startPoint: Point? = null
    ): GestureResult {
        val start = startPoint ?: Point(540, 960) // Default to screen center
        
        val end = when (direction) {
            ScrollDirection.UP -> Point(start.x, start.y - distance)
            ScrollDirection.DOWN -> Point(start.x, start.y + distance)
            ScrollDirection.LEFT -> Point(start.x - distance, start.y)
            ScrollDirection.RIGHT -> Point(start.x + distance, start.y)
        }
        
        return swipe(start, end)
    }
    
    /**
     * Waits for a human-like delay before next action
     * 
     * @param baseDelay Base delay time
     * @param addRandomVariation Whether to add random variation
     */
    suspend fun humanDelay(baseDelay: Long = 500L, addRandomVariation: Boolean = true) {
        val actualDelay = if (addRandomVariation) {
            addRandomVariation(baseDelay)
        } else {
            baseDelay
        }
        
        logger.debug(FGOBotLogger.Category.AUTOMATION, "Human delay: ${actualDelay}ms")
        delay(actualDelay)
    }
    
    /**
     * Gets input controller statistics
     * 
     * @return Current gesture statistics
     */
    fun getInputStats(): Map<GestureType, GestureStats> {
        return gestureStats.toMap()
    }
    
    /**
     * Resets input statistics
     */
    fun resetStats() {
        gestureStats.clear()
        gestureCount = 0L
        logger.info(FGOBotLogger.Category.AUTOMATION, "Input statistics reset")
    }
    
    /**
     * Performs a gesture with error handling and statistics tracking
     */
    private suspend fun performGesture(
        gestureType: GestureType,
        gestureAction: suspend () -> GestureResult
    ): GestureResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            // Add human-like delay between gestures
            val timeSinceLastGesture = startTime - lastGestureTime
            if (timeSinceLastGesture < timingConfig.minDelay) {
                delay(timingConfig.minDelay - timeSinceLastGesture)
            }
            
            val result = gestureAction()
            
            gestureCount++
            lastGestureTime = System.currentTimeMillis()
            
            val executionTime = lastGestureTime - startTime
            updateGestureStats(gestureType, result is GestureResult.Success, executionTime)
            
            result
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error performing $gestureType gesture", e)
            updateGestureStats(gestureType, false, System.currentTimeMillis() - startTime)
            GestureResult.Error("Failed to perform $gestureType gesture", e)
        }
    }
    
    /**
     * Executes a gesture using AccessibilityService
     */
    private suspend fun executeGesture(gesture: GestureDescription): GestureResult {
        return try {
            var result: GestureResult = GestureResult.Timeout
            
            val callback = object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    result = GestureResult.Success
                }
                
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    result = GestureResult.Error("Gesture was cancelled")
                }
            }
            
            val success = accessibilityService.dispatchGesture(gesture, callback, null)
            if (!success) {
                return GestureResult.Error("Failed to dispatch gesture")
            }
            
            // Wait for gesture completion with timeout
            val timeoutTime = System.currentTimeMillis() + GESTURE_TIMEOUT
            while (result == GestureResult.Timeout && System.currentTimeMillis() < timeoutTime) {
                delay(10)
            }
            
            result
        } catch (e: Exception) {
            GestureResult.Error("Exception during gesture execution", e)
        }
    }
    
    /**
     * Applies random offset to a point for more human-like behavior
     */
    private fun applyRandomOffset(point: Point): Point {
        val offsetRange = 5 // pixels
        val offsetX = Random.nextInt(-offsetRange, offsetRange + 1)
        val offsetY = Random.nextInt(-offsetRange, offsetRange + 1)
        
        return Point(point.x + offsetX, point.y + offsetY)
    }
    
    /**
     * Adds random variation to a timing value
     */
    private fun addRandomVariation(baseValue: Long): Long {
        val variation = (baseValue * timingConfig.randomVariation).toLong()
        val offset = Random.nextLong(-variation, variation + 1)
        return (baseValue + offset).coerceAtLeast(10L) // Minimum 10ms
    }
    
    /**
     * Calculates distance between two points
     */
    private fun calculateDistance(start: Point, end: Point): Float {
        val dx = (end.x - start.x).toFloat()
        val dy = (end.y - start.y).toFloat()
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Updates gesture statistics
     */
    private fun updateGestureStats(gestureType: GestureType, success: Boolean, executionTime: Long) {
        val stats = gestureStats.getOrPut(gestureType) { GestureStats(gestureType) }
        
        stats.totalAttempts++
        if (success) stats.successfulGestures++
        stats.totalExecutionTime += executionTime
        stats.lastExecutionTime = executionTime
    }
}

/**
 * Scroll direction enumeration
 */
enum class ScrollDirection {
    UP, DOWN, LEFT, RIGHT
}

/**
 * Gesture statistics for performance monitoring
 */
data class GestureStats(
    val gestureType: GestureType,
    var totalAttempts: Long = 0,
    var successfulGestures: Long = 0,
    var totalExecutionTime: Long = 0,
    var lastExecutionTime: Long = 0
) {
    val successRate: Double
        get() = if (totalAttempts > 0) successfulGestures.toDouble() / totalAttempts else 0.0
    
    val averageExecutionTime: Double
        get() = if (totalAttempts > 0) totalExecutionTime.toDouble() / totalAttempts else 0.0
} 