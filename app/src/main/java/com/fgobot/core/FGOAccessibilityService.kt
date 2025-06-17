/*
 * FGO Bot - Accessibility Service
 * 
 * This file defines the main accessibility service for FGO Bot automation.
 * Provides screen interaction capabilities and automation functionality.
 * 
 * FGA-Inspired Update: Added floating overlay system that shows play button
 * when FGO is detected, with on-demand screen capture initialization.
 */

package com.fgobot.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.PixelFormat
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import com.fgobot.R
import com.fgobot.core.automation.AutomationController
import com.fgobot.core.logging.FGOBotLogger
import com.fgobot.data.database.entities.Team
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Main accessibility service for FGO Bot automation
 * 
 * FGA-Inspired Features:
 * - Floating overlay that appears when FGO is detected
 * - On-demand screen capture permission handling
 * - Persistent service that stays active
 * - Automatic FGO app detection
 */
class FGOAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "FGOAccessibilityService"
        
        // FGO package names (like FGA)
        private val FGO_PACKAGES = setOf(
            "com.aniplex.fategrandorder",      // JP version
            "com.aniplex.fategrandorder.en"    // EN version
        )
        
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
        fun isServiceRunning(): Boolean = instance != null && !instance!!.isServiceStopped
        
        /**
         * Check if service is currently stopped (even if accessibility service is enabled)
         * 
         * @return True if service is stopped by user, false otherwise
         */
        fun isServiceStopped(): Boolean = instance?.isServiceStopped ?: true
        
        /**
         * Request screen capture permission from the service
         */
        fun requestScreenCapture() {
            instance?.requestScreenCapturePermission()
        }
    }
    
    // Logger instance for this service
    private val logger = FGOBotLogger
    
    // FGA-inspired floating overlay components
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var playButton: ImageButton? = null
    private var isOverlayVisible = false
    
    // FGA-inspired: Draggable button state
    private var windowParams: WindowManager.LayoutParams? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    
    // FGO detection state
    private var isFGOActive = false
    private var currentPackageName: String? = null
    
    // Automation components
    private var automationController: AutomationController? = null
    private var isAutomationRunning = false
    
    // Service control state
    private var isServiceStopped = false
    
    // Coroutine scope for async operations
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    
    // Screen capture permission handling
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var pendingScreenCaptureRequest = false
    
    /**
     * Called when the accessibility service is connected
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        
        logger.info(FGOBotLogger.Category.AUTOMATION, "FGO Accessibility Service connected successfully")
        
        // Initialize FGA-inspired components
        initializeFloatingOverlay()
        initializeScreenCaptureManager()
        
        logger.info(FGOBotLogger.Category.AUTOMATION, "FGA-inspired floating overlay system initialized")
    }
    
    /**
     * Called when the accessibility service is disconnected
     */
    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        
        // Clean up floating overlay
        hideFloatingOverlay()
        
        // Clean up automation controller
        automationController?.cleanup()
        automationController = null
        
        logger.info(FGOBotLogger.Category.AUTOMATION, "FGO Accessibility Service disconnected")
        
        return super.onUnbind(intent)
    }
    
    /**
     * Called when an accessibility event occurs
     * 
     * FGA-inspired: Monitor for FGO app detection
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let { accessibilityEvent ->
            // FGA-inspired: Detect FGO app state changes
            when (accessibilityEvent.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    val packageName = accessibilityEvent.packageName?.toString()
                    handleWindowStateChange(packageName)
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
        logger.warn(FGOBotLogger.Category.AUTOMATION, "FGO Accessibility Service interrupted")
        
        // Hide overlay during interruption
        hideFloatingOverlay()
    }
    
    /**
     * FGA-inspired: Handle window state changes to detect FGO
     */
    private fun handleWindowStateChange(packageName: String?) {
        val wasFGOActive = isFGOActive
        isFGOActive = packageName in FGO_PACKAGES
        currentPackageName = packageName
        
        logger.debug(
            FGOBotLogger.Category.AUTOMATION,
            "Window state changed: $packageName, FGO active: $isFGOActive"
        )
        
        // Show/hide overlay based on FGO state
        if (isFGOActive && !wasFGOActive) {
            // FGO became active - show floating overlay
            showFloatingOverlay()
        } else if (!isFGOActive && wasFGOActive) {
            // FGO became inactive - hide floating overlay
            hideFloatingOverlay()
            
            // Stop automation if running
            if (isAutomationRunning) {
                stopAutomation()
            }
        }
    }
    
    /**
     * FGA-inspired: Initialize floating overlay system with draggable support
     */
    private fun initializeFloatingOverlay() {
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            // Create floating view
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            floatingView = inflater.inflate(R.layout.floating_overlay, null)
            
            // Setup play button with click and drag support
            playButton = floatingView?.findViewById(R.id.play_button)
            playButton?.setOnClickListener {
                if (!isDragging) {
                    onPlayButtonClicked()
                }
            }
            
            // FGA-inspired: Add touch listener for dragging
            floatingView?.setOnTouchListener { view, event ->
                handleTouchEvent(event)
            }
            
            logger.info(FGOBotLogger.Category.AUTOMATION, "Floating overlay initialized with drag support")
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error initializing floating overlay", e)
        }
    }
    
    /**
     * FGA-inspired: Initialize screen capture manager
     */
    private fun initializeScreenCaptureManager() {
        try {
            mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            logger.info(FGOBotLogger.Category.AUTOMATION, "Screen capture manager initialized")
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error initializing screen capture manager", e)
        }
    }
    
    /**
     * FGA-inspired: Show floating overlay when FGO is active
     */
    private fun showFloatingOverlay() {
        if (isOverlayVisible || floatingView == null || windowManager == null || isServiceStopped) return
        
        // Check if overlay permission is granted
        if (!Settings.canDrawOverlays(this)) {
            logger.warn(FGOBotLogger.Category.AUTOMATION, "Overlay permission not granted, cannot show floating overlay")
            return
        }
        
        try {
            windowParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                // Use TYPE_APPLICATION_OVERLAY for better full-screen compatibility
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                // Enhanced flags for full-screen mode compatibility
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 20
                y = 100
            }
            
            windowManager?.addView(floatingView, windowParams)
            isOverlayVisible = true
            
            // Update button state
            updatePlayButtonState()
            
            logger.info(FGOBotLogger.Category.AUTOMATION, "Floating overlay shown with full-screen compatibility")
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error showing floating overlay", e)
        }
    }
    
    /**
     * FGA-inspired: Hide floating overlay
     */
    private fun hideFloatingOverlay() {
        if (!isOverlayVisible || floatingView == null || windowManager == null) return
        
        try {
            windowManager?.removeView(floatingView)
            isOverlayVisible = false
            
            logger.info(FGOBotLogger.Category.AUTOMATION, "Floating overlay hidden")
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error hiding floating overlay", e)
        }
    }
    
    /**
     * FGA-inspired: Handle play button click
     */
    private fun onPlayButtonClicked() {
        logger.info(FGOBotLogger.Category.AUTOMATION, "Play button clicked")
        
        if (isAutomationRunning) {
            // Stop automation
            stopAutomation()
        } else {
            // Start automation
            startAutomation()
        }
    }
    
    /**
     * FGA-inspired: Start automation with on-demand screen capture
     */
    private fun startAutomation() {
        serviceScope.launch {
            try {
                logger.info(FGOBotLogger.Category.AUTOMATION, "Starting automation...")
                
                // Check if we need screen capture permission
                if (automationController == null) {
                    logger.info(FGOBotLogger.Category.AUTOMATION, "Requesting screen capture permission...")
                    requestScreenCapturePermission()
                    return@launch
                }
                
                // Start automation with existing controller
                // For now, we'll use a default team - in the future this should be configurable
                val defaultTeam = createDefaultTeam()
                val startSuccess = automationController?.startAutomation(defaultTeam) ?: false
                
                if (startSuccess) {
                    isAutomationRunning = true
                    updatePlayButtonState()
                    logger.info(FGOBotLogger.Category.AUTOMATION, "Automation started successfully")
                } else {
                    logger.error(FGOBotLogger.Category.AUTOMATION, "Failed to start automation")
                }
                
            } catch (e: Exception) {
                logger.error(FGOBotLogger.Category.AUTOMATION, "Error starting automation", e)
            }
        }
    }
    
    /**
     * FGA-inspired: Stop automation
     */
    private fun stopAutomation() {
        serviceScope.launch {
            try {
                logger.info(FGOBotLogger.Category.AUTOMATION, "Stopping automation...")
                
                automationController?.stopAutomation()
                isAutomationRunning = false
                updatePlayButtonState()
                
                logger.info(FGOBotLogger.Category.AUTOMATION, "Automation stopped")
                
            } catch (e: Exception) {
                logger.error(FGOBotLogger.Category.AUTOMATION, "Error stopping automation", e)
            }
        }
    }
    
    /**
     * FGA-inspired: Update play button appearance based on state
     */
    private fun updatePlayButtonState() {
        playButton?.let { button ->
            if (isAutomationRunning) {
                button.setImageResource(R.drawable.ic_stop)
                button.contentDescription = "Stop Automation"
            } else {
                button.setImageResource(R.drawable.ic_play_arrow)
                button.contentDescription = "Start Automation"
            }
        }
    }
    
    /**
     * FGA-inspired: Request screen capture permission
     */
    fun requestScreenCapturePermission() {
        try {
            val intent = mediaProjectionManager?.createScreenCaptureIntent()
            if (intent != null) {
                // Send broadcast to MainActivity to handle permission request
                val permissionIntent = Intent("com.fgobot.REQUEST_SCREEN_CAPTURE")
                permissionIntent.putExtra("screen_capture_intent", intent)
                sendBroadcast(permissionIntent)
                
                pendingScreenCaptureRequest = true
                logger.info(FGOBotLogger.Category.AUTOMATION, "Screen capture permission requested")
            }
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error requesting screen capture permission", e)
        }
    }
    
    /**
     * FGA-inspired: Handle screen capture permission result
     */
    fun handleScreenCapturePermission(resultCode: Int, data: Intent?) {
        serviceScope.launch {
            try {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    logger.info(FGOBotLogger.Category.AUTOMATION, "Screen capture permission granted")
                    
                    // Initialize automation controller
                    if (automationController == null) {
                        automationController = AutomationController(this@FGOAccessibilityService, this@FGOAccessibilityService, logger)
                    }
                    
                    // Initialize with screen capture permission
                    val success = automationController?.initialize(resultCode, data) ?: false
                    
                    if (success) {
                        logger.info(FGOBotLogger.Category.AUTOMATION, "Automation controller initialized successfully")
                        
                        // Auto-start automation if permission was requested from play button
                        if (pendingScreenCaptureRequest) {
                            startAutomation()
                        }
                    } else {
                        logger.error(FGOBotLogger.Category.AUTOMATION, "Failed to initialize automation controller")
                    }
                } else {
                    logger.warn(FGOBotLogger.Category.AUTOMATION, "Screen capture permission denied")
                }
                
                pendingScreenCaptureRequest = false
                
            } catch (e: Exception) {
                logger.error(FGOBotLogger.Category.AUTOMATION, "Error handling screen capture permission", e)
                pendingScreenCaptureRequest = false
            }
        }
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
    
    /**
     * FGA-inspired: Handle touch events for draggable floating button
     */
    private fun handleTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = false
                initialX = windowParams?.x ?: 0
                initialY = windowParams?.y ?: 0
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val deltaX = (event.rawX - initialTouchX).toInt()
                val deltaY = (event.rawY - initialTouchY).toInt()
                
                // If moved more than threshold, consider it dragging
                if (kotlin.math.abs(deltaX) > 10 || kotlin.math.abs(deltaY) > 10) {
                    isDragging = true
                    
                    windowParams?.let { params ->
                        params.x = initialX + deltaX
                        params.y = initialY + deltaY
                        
                        try {
                            windowManager?.updateViewLayout(floatingView, params)
                        } catch (e: Exception) {
                            logger.error(FGOBotLogger.Category.AUTOMATION, "Error updating floating button position", e)
                        }
                    }
                }
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                // Reset dragging flag after a short delay to prevent accidental clicks
                Handler(Looper.getMainLooper()).postDelayed({
                    isDragging = false
                }, 100)
                return true
            }
        }
        return false
    }
    
    /**
     * FGA-inspired: Stop service and hide floating overlay
     */
    fun stopService() {
        try {
            logger.info(FGOBotLogger.Category.AUTOMATION, "Stopping service and hiding floating overlay")
            
            // Set service stopped flag
            isServiceStopped = true
            
            // Stop any running automation
            serviceScope.launch {
                automationController?.stopAutomation()
            }
            isAutomationRunning = false
            
            // Hide floating overlay
            hideFloatingOverlay()
            
            logger.info(FGOBotLogger.Category.AUTOMATION, "Service stopped successfully")
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error stopping service", e)
        }
    }
    
    /**
     * FGA-inspired: Restart service (used when accessibility service is re-enabled)
     */
    fun restartService() {
        try {
            logger.info(FGOBotLogger.Category.AUTOMATION, "Restarting service")
            
            // Reset service stopped flag
            isServiceStopped = false
            
            // Stop any running automation first
            if (isAutomationRunning) {
                stopAutomation()
            }
            
            // Show overlay if FGO is active
            if (isFGOActive) {
                showFloatingOverlay()
            }
            
            logger.info(FGOBotLogger.Category.AUTOMATION, "Service restarted successfully")
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error restarting service", e)
        }
    }
    
    /**
     * FGA-inspired: Create a default team for automation
     * TODO: This should be configurable in the future
     */
    private fun createDefaultTeam(): Team {
        return Team(
            id = 0,
            name = "Default FGA Team",
            description = "Auto-generated team for FGA mode",
            servantIds = listOf(1, 2, 3, 0, 0, 0), // Main 3 servants, empty subs
            craftEssenceIds = listOf(1, 2, 3, 0, 0, 0), // Default CE IDs
            isDefault = true,
            createdAt = System.currentTimeMillis(),
            lastUpdated = System.currentTimeMillis()
        )
    }
} 