/*
 * FGO Bot - Accessibility Service
 * 
 * This file defines the main accessibility service for FGO Bot automation.
 * Provides screen interaction capabilities and automation functionality.
 */

package com.fgobot.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.fgobot.core.logging.FGOBotLogger

/**
 * Main accessibility service for FGO Bot automation
 * 
 * This service provides the core automation capabilities including:
 * - Screen interaction through accessibility APIs
 * - UI element detection and interaction
 * - Gesture simulation for taps, swipes, etc.
 * - Game state monitoring through accessibility events
 */
class FGOAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "FGOAccessibilityService"
        
        // Static reference to the service instance
        @Volatile
        private var instance: FGOAccessibilityService? = null
        
        /**
         * Get the current service instance
         * 
         * @return Service instance or null if not running
         */
        fun getInstance(): FGOAccessibilityService? = instance
        
        /**
         * Check if the accessibility service is running
         * 
         * @return True if service is active, false otherwise
         */
        fun isServiceRunning(): Boolean = instance != null
    }
    
    // Logger instance for this service
    private val logger = FGOBotLogger
    
    /**
     * Called when the accessibility service is connected
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        
        logger.info(
            FGOBotLogger.Category.AUTOMATION,
            "FGO Accessibility Service connected successfully"
        )
    }
    
    /**
     * Called when the accessibility service is disconnected
     */
    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        
        logger.info(
            FGOBotLogger.Category.AUTOMATION,
            "FGO Accessibility Service disconnected"
        )
        
        return super.onUnbind(intent)
    }
    
    /**
     * Called when an accessibility event occurs
     * 
     * @param event The accessibility event
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let { accessibilityEvent ->
            // Log significant events for debugging
            when (accessibilityEvent.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    logger.debug(
                        FGOBotLogger.Category.AUTOMATION,
                        "Window state changed: ${accessibilityEvent.packageName}"
                    )
                }
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    logger.debug(
                        FGOBotLogger.Category.AUTOMATION,
                        "View clicked: ${accessibilityEvent.contentDescription}"
                    )
                }
            }
        }
    }
    
    /**
     * Called when the accessibility service is interrupted
     */
    override fun onInterrupt() {
        logger.warn(
            FGOBotLogger.Category.AUTOMATION,
            "FGO Accessibility Service interrupted"
        )
    }
    
    /**
     * Perform a tap gesture at the specified coordinates
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param duration Tap duration in milliseconds (default: 100ms)
     * @return True if gesture was dispatched successfully
     */
    fun performTap(x: Float, y: Float, duration: Long = 100L): Boolean {
        return try {
            val path = Path().apply {
                moveTo(x, y)
            }
            
            val gestureBuilder = GestureDescription.Builder()
            val strokeDescription = GestureDescription.StrokeDescription(path, 0, duration)
            gestureBuilder.addStroke(strokeDescription)
            
            val result = dispatchGesture(gestureBuilder.build(), null, null)
            
            if (result) {
                logger.debug(
                    FGOBotLogger.Category.AUTOMATION,
                    "Tap gesture performed at ($x, $y) with duration ${duration}ms"
                )
            }
            
            result
        } catch (e: Exception) {
            logger.error(
                FGOBotLogger.Category.AUTOMATION,
                "Error performing tap gesture at ($x, $y)",
                e
            )
            false
        }
    }
    
    /**
     * Find nodes by text content
     * 
     * @param text Text to search for
     * @return List of matching accessibility nodes
     */
    fun findNodesByText(text: String): List<AccessibilityNodeInfo> {
        val rootNode = rootInActiveWindow ?: return emptyList()
        
        return try {
            val nodes = rootNode.findAccessibilityNodeInfosByText(text)
            
            logger.debug(
                FGOBotLogger.Category.AUTOMATION,
                "Found ${nodes.size} nodes matching text: '$text'"
            )
            
            nodes
        } catch (e: Exception) {
            logger.error(
                FGOBotLogger.Category.AUTOMATION,
                "Error finding nodes by text: '$text'",
                e
            )
            emptyList()
        }
    }
    
    /**
     * Click on an accessibility node
     * 
     * @param node Node to click
     * @return True if click was successful
     */
    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        return try {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            
            if (result) {
                logger.debug(
                    FGOBotLogger.Category.AUTOMATION,
                    "Successfully clicked node: ${node.text ?: node.contentDescription ?: "Unknown"}"
                )
            }
            
            result
        } catch (e: Exception) {
            logger.error(
                FGOBotLogger.Category.AUTOMATION,
                "Error clicking node",
                e
            )
            false
        }
    }
} 