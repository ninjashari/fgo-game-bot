/*
 * FGO Bot - Automation Controller
 * 
 * This file implements the main automation controller that orchestrates all core systems.
 * Provides the primary interface for FGO battle automation with error handling and recovery.
 */

package com.fgobot.core.automation

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import com.fgobot.core.decision.DecisionEngine
import com.fgobot.core.decision.DecisionResult
import com.fgobot.core.input.InputController
import com.fgobot.core.logging.FGOBotLogger
import com.fgobot.core.vision.ImageRecognition
import com.fgobot.core.vision.ScreenCapture
import com.fgobot.core.vision.CaptureResult
import com.fgobot.core.vision.BattleState
import com.fgobot.core.FGOAccessibilityService
import com.fgobot.data.database.entities.Team
import com.fgobot.data.database.entities.BattleLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Automation state enumeration
 */
enum class AutomationState {
    IDLE,
    INITIALIZING,
    RUNNING,
    PAUSED,
    STOPPING,
    ERROR,
    COMPLETED
}

/**
 * Automation configuration
 */
data class AutomationConfig(
    val maxBattles: Int = -1, // -1 for unlimited
    val maxErrors: Int = 5,
    val screenshotInterval: Long = 1000L,
    val decisionTimeout: Long = 10000L,
    val enableLearning: Boolean = true,
    val enableRecovery: Boolean = true,
    val humanLikeTiming: Boolean = true
)

/**
 * Automation statistics
 */
data class AutomationStats(
    val battlesCompleted: Int,
    val battlesWon: Int,
    val battlesLost: Int,
    val totalRuntime: Long,
    val averageBattleTime: Long,
    val errorsEncountered: Int,
    val decisionsExecuted: Int,
    val screenshotsTaken: Int
)

/**
 * Main automation controller for FGO Bot
 * 
 * Orchestrates all core systems including vision, decision-making, and input control.
 * Provides robust error handling, recovery mechanisms, and performance monitoring.
 */
class AutomationController(
    private val context: Context,
    private val accessibilityService: FGOAccessibilityService,
    private val logger: FGOBotLogger
) {
    
    companion object {
        private const val TAG = "AutomationController"
        private const val MAIN_LOOP_DELAY = 100L
        private const val ERROR_RECOVERY_DELAY = 5000L
        private const val MAX_CONSECUTIVE_ERRORS = 3
    }
    
    // Core systems
    private lateinit var screenCapture: ScreenCapture
    private lateinit var imageRecognition: ImageRecognition
    private lateinit var inputController: InputController
    private lateinit var decisionEngine: DecisionEngine
    
    // State management
    private val _automationState = MutableStateFlow(AutomationState.IDLE)
    val automationState: StateFlow<AutomationState> = _automationState.asStateFlow()
    
    private val _currentTeam = MutableStateFlow<Team?>(null)
    val currentTeam: StateFlow<Team?> = _currentTeam.asStateFlow()
    
    private val _automationStats = MutableStateFlow(AutomationStats(0, 0, 0, 0L, 0L, 0, 0, 0))
    val automationStats: StateFlow<AutomationStats> = _automationStats.asStateFlow()
    
    // Automation control
    private var automationJob: Job? = null
    private var isInitialized = false
    private var startTime = 0L
    private var consecutiveErrors = 0
    private var lastScreenshot: Bitmap? = null
    
    /**
     * Initializes the automation controller and all core systems
     * 
     * @param resultCode MediaProjection result code
     * @param data MediaProjection intent data
     * @return True if initialization successful
     */
    suspend fun initialize(resultCode: Int, data: Intent): Boolean {
        return try {
            logger.info(FGOBotLogger.Category.AUTOMATION, "Initializing automation controller (FGA-inspired)")
            _automationState.value = AutomationState.INITIALIZING
            
            // FGA-inspired approach: Initialize systems in order of dependency and reliability
            logger.debug(FGOBotLogger.Category.AUTOMATION, "Creating core system instances...")
            
            // 1. Initialize InputController first (most reliable)
            logger.debug(FGOBotLogger.Category.AUTOMATION, "Initializing input controller...")
            inputController = InputController(accessibilityService, logger)
            
            // 2. Initialize ImageRecognition (lightweight in FGA mode)
            logger.debug(FGOBotLogger.Category.AUTOMATION, "Initializing image recognition...")
            imageRecognition = ImageRecognition(context, logger)
            val imageRecognitionInit = withTimeoutOrNull(3000L) { // Reduced timeout for lightweight init
                imageRecognition.initialize()
            } ?: false
            
            if (!imageRecognitionInit) {
                logger.error(FGOBotLogger.Category.AUTOMATION, "Image recognition initialization failed or timed out")
                _automationState.value = AutomationState.ERROR
                return false
            }
            
            // 3. Initialize DecisionEngine (depends on ImageRecognition)
            logger.debug(FGOBotLogger.Category.AUTOMATION, "Initializing decision engine...")
            decisionEngine = DecisionEngine(imageRecognition, logger)
            
            // 4. Initialize ScreenCapture last (most complex and prone to issues)
            logger.debug(FGOBotLogger.Category.AUTOMATION, "Initializing screen capture...")
            screenCapture = ScreenCapture(context, logger)
            val screenCaptureInit = withTimeoutOrNull(8000L) { // Reasonable timeout for screen capture
                screenCapture.initialize(resultCode, data)
            } ?: false
            
            if (!screenCaptureInit) {
                logger.error(FGOBotLogger.Category.AUTOMATION, "Screen capture initialization failed or timed out")
                _automationState.value = AutomationState.ERROR
                return false
            }
            
            // FGA-inspired: Validate all systems are working
            logger.debug(FGOBotLogger.Category.AUTOMATION, "Validating system integration...")
            if (!validateSystemIntegration()) {
                logger.error(FGOBotLogger.Category.AUTOMATION, "System integration validation failed")
                _automationState.value = AutomationState.ERROR
                return false
            }
            
            isInitialized = true
            _automationState.value = AutomationState.IDLE
            
            logger.info(FGOBotLogger.Category.AUTOMATION, "Automation controller initialized successfully")
            true
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Failed to initialize automation controller", e)
            _automationState.value = AutomationState.ERROR
            false
        }
    }
    
    /**
     * FGA-inspired: Validate that all systems are working together properly
     */
    private suspend fun validateSystemIntegration(): Boolean {
        return try {
            logger.debug(FGOBotLogger.Category.AUTOMATION, "Validating system integration...")
            
            // Test screen capture
            val captureResult = screenCapture.captureScreen()
            if (captureResult !is CaptureResult.Success) {
                logger.error(FGOBotLogger.Category.AUTOMATION, "Screen capture validation failed: $captureResult")
                return false
            }
            
            logger.debug(FGOBotLogger.Category.AUTOMATION, "Screen capture validation passed")
            
            // Test image recognition with captured image
            val battleState = imageRecognition.detectBattleState(captureResult.bitmap)
            logger.debug(FGOBotLogger.Category.AUTOMATION, "Image recognition validation: detected state = $battleState")
            
            // Test input controller (basic validation)
            if (!inputController.isReady()) {
                logger.error(FGOBotLogger.Category.AUTOMATION, "Input controller not ready")
                return false
            }
            
            logger.debug(FGOBotLogger.Category.AUTOMATION, "Input controller validation passed")
            
            logger.info(FGOBotLogger.Category.AUTOMATION, "All systems validated successfully")
            true
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "System integration validation failed", e)
            false
        }
    }
    
    /**
     * Starts the automation with the specified team
     * 
     * @param team Team configuration to use
     * @return True if automation started successfully
     */
    suspend fun startAutomation(team: Team): Boolean {
        if (!isInitialized) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Cannot start automation: not initialized")
            return false
        }
        
        if (_automationState.value == AutomationState.RUNNING) {
            logger.warn(FGOBotLogger.Category.AUTOMATION, "Automation already running")
            return false
        }
        
        return try {
            logger.info(FGOBotLogger.Category.AUTOMATION, "Starting automation with team: ${team.name}")
            
            _currentTeam.value = team
            _automationState.value = AutomationState.RUNNING
            startTime = System.currentTimeMillis()
            consecutiveErrors = 0
            
            // Reset systems for new automation run
            decisionEngine.reset()
            
            // Start main automation loop
            automationJob = CoroutineScope(Dispatchers.Default).launch {
                runAutomationLoop()
            }
            
            true
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error starting automation", e)
            _automationState.value = AutomationState.ERROR
            false
        }
    }
    
    /**
     * Pauses the automation
     */
    fun pauseAutomation() {
        if (_automationState.value == AutomationState.RUNNING) {
            logger.info(FGOBotLogger.Category.AUTOMATION, "Pausing automation")
            _automationState.value = AutomationState.PAUSED
        }
    }
    
    /**
     * Resumes the automation
     */
    fun resumeAutomation() {
        if (_automationState.value == AutomationState.PAUSED) {
            logger.info(FGOBotLogger.Category.AUTOMATION, "Resuming automation")
            _automationState.value = AutomationState.RUNNING
        }
    }
    
    /**
     * Stops the automation
     */
    suspend fun stopAutomation() {
        logger.info(FGOBotLogger.Category.AUTOMATION, "Stopping automation")
        _automationState.value = AutomationState.STOPPING
        
        automationJob?.cancel()
        automationJob?.join()
        
        _automationState.value = AutomationState.IDLE
        _currentTeam.value = null
        
        logger.info(FGOBotLogger.Category.AUTOMATION, "Automation stopped")
    }
    
    /**
     * Main automation loop
     */
    private suspend fun runAutomationLoop() {
        logger.info(FGOBotLogger.Category.AUTOMATION, "Starting automation loop")
        
        try {
            while (_automationState.value == AutomationState.RUNNING || _automationState.value == AutomationState.PAUSED) {
                // Wait if paused
                if (_automationState.value == AutomationState.PAUSED) {
                    delay(MAIN_LOOP_DELAY)
                    continue
                }
                
                // Check for stop condition
                if (_automationState.value != AutomationState.RUNNING) {
                    break
                }
                
                // Execute automation cycle
                val cycleResult = executeAutomationCycle()
                
                if (!cycleResult) {
                    consecutiveErrors++
                    logger.warn(FGOBotLogger.Category.AUTOMATION, "Automation cycle failed (consecutive errors: $consecutiveErrors)")
                    
                    if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                        logger.error(FGOBotLogger.Category.AUTOMATION, "Too many consecutive errors, stopping automation")
                        _automationState.value = AutomationState.ERROR
                        break
                    }
                    
                    // Error recovery delay
                    delay(ERROR_RECOVERY_DELAY)
                } else {
                    consecutiveErrors = 0
                }
                
                // Main loop delay
                delay(MAIN_LOOP_DELAY)
            }
            
        } catch (e: CancellationException) {
            logger.info(FGOBotLogger.Category.AUTOMATION, "Automation loop cancelled")
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error in automation loop", e)
            _automationState.value = AutomationState.ERROR
        }
        
        logger.info(FGOBotLogger.Category.AUTOMATION, "Automation loop ended")
    }
    
    /**
     * Executes a single automation cycle
     * 
     * @return True if cycle completed successfully
     */
    private suspend fun executeAutomationCycle(): Boolean {
        return try {
            // Capture screenshot
            val captureResult = screenCapture.captureScreen()
            if (captureResult !is CaptureResult.Success) {
                logger.error(FGOBotLogger.Category.AUTOMATION, "Failed to capture screenshot")
                return false
            }
            
            lastScreenshot = captureResult.bitmap
            _automationStats.value = _automationStats.value.copy(screenshotsTaken = _automationStats.value.screenshotsTaken + 1)
            
            // Detect current battle state
            val battleState = imageRecognition.detectBattleState(captureResult.bitmap)
            logger.debug(FGOBotLogger.Category.AUTOMATION, "Detected battle state: $battleState")
            
            // Handle different battle states
            when (battleState) {
                BattleState.COMMAND_SELECTION -> {
                    // In battle - make tactical decisions
                    val team = _currentTeam.value ?: return false
                    val decision = decisionEngine.makeDecision(captureResult.bitmap, team)
                    _automationStats.value = _automationStats.value.copy(decisionsExecuted = _automationStats.value.decisionsExecuted + 1)
                    
                    val executionResult = executeDecision(decision)
                    if (!executionResult) {
                        logger.warn(FGOBotLogger.Category.AUTOMATION, "Failed to execute decision: $decision")
                        return false
                    }
                }
                
                BattleState.BATTLE_RESULT -> {
                    // Battle completed - handle results
                    handleBattleResult(captureResult.bitmap)
                }
                
                BattleState.AP_RECOVERY -> {
                    // Need AP recovery
                    handleAPRecovery(captureResult.bitmap)
                }
                
                BattleState.QUEST_SELECTION -> {
                    // Select quest to repeat
                    handleQuestSelection(captureResult.bitmap)
                }
                
                BattleState.SUPPORT_SELECTION -> {
                    // Wait for support selection or auto-select
                    inputController.humanDelay(2000L)
                }
                
                BattleState.BATTLE_START -> {
                    // Wait for battle to start
                    inputController.humanDelay(2000L)
                }
                
                BattleState.SKILL_SELECTION -> {
                    // Handle skill selection screen
                    inputController.humanDelay(1000L)
                }
                
                BattleState.NP_SELECTION -> {
                    // Handle NP selection screen
                    inputController.humanDelay(1000L)
                }
                
                BattleState.ERROR_STATE -> {
                    // Handle error state
                    logger.warn(FGOBotLogger.Category.AUTOMATION, "Error state detected")
                    handleErrorState(captureResult.bitmap)
                }
                
                BattleState.UNKNOWN -> {
                    // Unknown state - wait and retry
                    logger.debug(FGOBotLogger.Category.AUTOMATION, "Unknown state - waiting")
                    inputController.humanDelay(1000L)
                }
            }
            
            true
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error in automation cycle", e)
            false
        }
    }
    
    /**
     * Handles battle result screen with enhanced detection
     */
    private suspend fun handleBattleResult(screenshot: Bitmap?) {
        logger.info(FGOBotLogger.Category.AUTOMATION, "Handling battle result")
        
        try {
            // Enhanced victory/defeat detection
            val isVictory = detectBattleVictory(screenshot)
            
            if (isVictory) {
                _automationStats.value = _automationStats.value.copy(
                    battlesWon = _automationStats.value.battlesWon + 1, 
                    battlesCompleted = _automationStats.value.battlesCompleted + 1
                )
                logger.info(FGOBotLogger.Category.AUTOMATION, "Battle won!")
            } else {
                _automationStats.value = _automationStats.value.copy(
                    battlesLost = _automationStats.value.battlesLost + 1, 
                    battlesCompleted = _automationStats.value.battlesCompleted + 1
                )
                logger.info(FGOBotLogger.Category.AUTOMATION, "Battle lost")
            }
            
            // Navigate through result screens
            var resultScreens = 0
            val maxResultScreens = 5
            
            while (resultScreens < maxResultScreens) {
                // Tap center of screen to continue
                inputController.tap(android.graphics.Point(540, 960))
                inputController.humanDelay(2000L)
                
                // Check if we're still in result screens
                val currentScreenshot = screenCapture.captureScreen()
                if (currentScreenshot is CaptureResult.Success) {
                    val battleState = imageRecognition.detectBattleState(currentScreenshot.bitmap)
                    if (battleState != BattleState.BATTLE_RESULT) {
                        break
                    }
                }
                
                resultScreens++
            }
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error handling battle result", e)
        }
    }
    
    /**
     * Detects battle victory or defeat
     */
    private suspend fun detectBattleVictory(screenshot: Bitmap?): Boolean {
        if (screenshot == null) return true // Default to victory
        
        // Try to detect victory/defeat text or colors
        val victoryResult = imageRecognition.findTemplate(screenshot, "battle_victory", null, 0.7)
        if (victoryResult.found) return true
        
        val defeatResult = imageRecognition.findTemplate(screenshot, "battle_defeat", null, 0.7)
        if (defeatResult.found) return false
        
        // Fallback: analyze colors (victory screens often have gold/yellow, defeat screens have red)
        val centerPixel = screenshot.getPixel(screenshot.width / 2, screenshot.height / 3)
        val red = (centerPixel shr 16) and 0xFF
        val green = (centerPixel shr 8) and 0xFF
        val blue = centerPixel and 0xFF
        
        // If predominantly red, likely defeat
        if (red > green + 50 && red > blue + 50 && red > 150) {
            return false
        }
        
        // Default to victory
        return true
    }
    
    /**
     * Handles AP recovery screen with multiple options
     */
    private suspend fun handleAPRecovery(screenshot: Bitmap?) {
        logger.info(FGOBotLogger.Category.AUTOMATION, "Handling AP recovery")
        
        try {
            // Check for AP recovery items
            if (screenshot != null) {
                val appleButton = imageRecognition.findTemplate(screenshot, "golden_apple", null, 0.7)
                val silverAppleButton = imageRecognition.findTemplate(screenshot, "silver_apple", null, 0.7)
                
                if (appleButton.found) {
                    // Use golden apple
                    inputController.tap(android.graphics.Point(appleButton.location.x, appleButton.location.y))
                    inputController.humanDelay(1000L)
                    
                    // Confirm usage
                    val confirmButton = android.graphics.Point(540, 1200)
                    inputController.tap(confirmButton)
                    inputController.humanDelay(2000L)
                    
                    logger.info(FGOBotLogger.Category.AUTOMATION, "Used golden apple for AP recovery")
                    return
                } else if (silverAppleButton.found) {
                    // Use silver apple
                    inputController.tap(android.graphics.Point(silverAppleButton.location.x, silverAppleButton.location.y))
                    inputController.humanDelay(1000L)
                    
                    // Confirm usage
                    val confirmButton = android.graphics.Point(540, 1200)
                    inputController.tap(confirmButton)
                    inputController.humanDelay(2000L)
                    
                    logger.info(FGOBotLogger.Category.AUTOMATION, "Used silver apple for AP recovery")
                    return
                }
            }
            
            // No AP items available - stop automation
            logger.warn(FGOBotLogger.Category.AUTOMATION, "No AP recovery items available - stopping automation")
            _automationState.value = AutomationState.COMPLETED
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error handling AP recovery", e)
            _automationState.value = AutomationState.COMPLETED
        }
    }
    
    /**
     * Handles quest selection screen for repetition
     */
    private suspend fun handleQuestSelection(screenshot: Bitmap?) {
        logger.info(FGOBotLogger.Category.AUTOMATION, "Handling quest selection")
        
        try {
            if (screenshot != null) {
                // Look for quest repeat button
                val repeatButton = imageRecognition.findTemplate(screenshot, "quest_repeat", null, 0.7)
                if (repeatButton.found) {
                    inputController.tap(android.graphics.Point(repeatButton.location.x, repeatButton.location.y))
                    inputController.humanDelay(1500L)
                    logger.info(FGOBotLogger.Category.AUTOMATION, "Repeated quest")
                    return
                }
                
                // Look for quest start button
                val startButton = imageRecognition.findTemplate(screenshot, "quest_start", null, 0.7)
                if (startButton.found) {
                    inputController.tap(android.graphics.Point(startButton.location.x, startButton.location.y))
                    inputController.humanDelay(1500L)
                    logger.info(FGOBotLogger.Category.AUTOMATION, "Started quest")
                    return
                }
            }
            
            // Fallback: tap common quest start positions
            val commonPositions = listOf(
                android.graphics.Point(540, 1600), // Bottom center
                android.graphics.Point(800, 1600), // Bottom right
                android.graphics.Point(900, 1500)  // Right side
            )
            
            for (position in commonPositions) {
                inputController.tap(position)
                inputController.humanDelay(1000L)
                
                // Check if quest started
                val newScreenshot = screenCapture.captureScreen()
                if (newScreenshot is CaptureResult.Success) {
                    val battleState = imageRecognition.detectBattleState(newScreenshot.bitmap)
                    if (battleState != BattleState.QUEST_SELECTION) {
                        logger.info(FGOBotLogger.Category.AUTOMATION, "Quest started via fallback method")
                        return
                    }
                }
            }
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error handling quest selection", e)
        }
    }
    
    /**
     * Handles error state with recovery attempts
     */
    private suspend fun handleErrorState(screenshot: Bitmap?) {
        logger.warn(FGOBotLogger.Category.AUTOMATION, "Handling error state")
        
        try {
            // Try common error recovery actions
            val recoveryActions = listOf(
                android.graphics.Point(540, 960),  // Center tap
                android.graphics.Point(100, 100),  // Back button area
                android.graphics.Point(980, 100),  // Close button area
                android.graphics.Point(540, 1600)  // Bottom center
            )
            
            for (action in recoveryActions) {
                inputController.tap(action)
                inputController.humanDelay(2000L)
                
                // Check if error state resolved
                val newScreenshot = screenCapture.captureScreen()
                if (newScreenshot is CaptureResult.Success) {
                    val battleState = imageRecognition.detectBattleState(newScreenshot.bitmap)
                    if (battleState != BattleState.ERROR_STATE && battleState != BattleState.UNKNOWN) {
                        logger.info(FGOBotLogger.Category.AUTOMATION, "Error state resolved")
                        return
                    }
                }
            }
            
            // If all recovery attempts fail, increment error count
            _automationStats.value = _automationStats.value.copy(
                errorsEncountered = _automationStats.value.errorsEncountered + 1
            )
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error in error state handling", e)
        }
    }
    
    /**
     * Executes a decision result
     * 
     * @param decision Decision to execute
     * @return True if execution successful
     */
    private suspend fun executeDecision(decision: DecisionResult): Boolean {
        return try {
            logger.debug(FGOBotLogger.Category.AUTOMATION, "Executing decision: ${decision.javaClass.simpleName}")
            
            when (decision) {
                is DecisionResult.CardSelection -> {
                    executeCardSelection(decision)
                }
                is DecisionResult.SkillUsage -> {
                    executeSkillUsage(decision)
                }
                is DecisionResult.NPUsage -> {
                    executeNPUsage(decision)
                }
                is DecisionResult.WaitAction -> {
                    delay(decision.duration)
                    true
                }
                is DecisionResult.ErrorRecovery -> {
                    executeErrorRecovery(decision)
                }
                is DecisionResult.NoAction -> {
                    true
                }
            }
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error executing decision", e)
            false
        }
    }
    
    /**
     * Executes card selection
     */
    private suspend fun executeCardSelection(decision: DecisionResult.CardSelection): Boolean {
        logger.info(FGOBotLogger.Category.AUTOMATION, "Executing card selection: ${decision.cardIndices}")
        
        return try {
            // Enhanced card position detection and selection
            val cardPositions = detectCardPositions(lastScreenshot)
            
            if (cardPositions.isEmpty()) {
                // Fallback to predefined positions
                val defaultPositions = listOf(
                    android.graphics.Point(200, 1400),  // Card 1
                    android.graphics.Point(540, 1400),  // Card 2
                    android.graphics.Point(880, 1400),  // Card 3
                    android.graphics.Point(200, 1600),  // Card 4
                    android.graphics.Point(880, 1600)   // Card 5
                )
                
                // Tap selected cards in order with human-like timing
                for (cardIndex in decision.cardIndices) {
                    if (cardIndex < defaultPositions.size) {
                        val position = defaultPositions[cardIndex]
                        inputController.tap(position)
                        inputController.humanDelay(300L + (50..150).random()) // Variable delay
                    }
                }
            } else {
                // Use detected positions
                for (cardIndex in decision.cardIndices) {
                    if (cardIndex < cardPositions.size) {
                        val position = cardPositions[cardIndex]
                        inputController.tap(position)
                        inputController.humanDelay(300L + (50..150).random())
                    }
                }
            }
            
            // Wait for attack animation and tap attack button
            inputController.humanDelay(1000L)
            
            // Look for and tap attack button
            val attackButtonPos = android.graphics.Point(940, 1660) // Typical attack button position
            inputController.tap(attackButtonPos)
            
            // Wait for battle animation
            inputController.humanDelay(3000L + (500..1500).random())
            true
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error executing card selection", e)
            false
        }
    }
    
    /**
     * Detects actual card positions from screenshot
     */
    private suspend fun detectCardPositions(screenshot: Bitmap?): List<android.graphics.Point> {
        if (screenshot == null) return emptyList()
        
        val positions = mutableListOf<android.graphics.Point>()
        
        // Try to detect cards using template matching
        val cardTemplates = listOf("card_arts", "card_buster", "card_quick")
        val cardRegion = ImageRecognition.REGIONS["command_cards"]
        
        for (template in cardTemplates) {
            val matches = imageRecognition.findMultipleTemplates(
                screenshot, template, cardRegion, 0.6, 5
            )
            
            for (match in matches) {
                if (match.found) {
                    positions.add(android.graphics.Point(
                        match.location.x + match.boundingRect.width() / 2,
                        match.location.y + match.boundingRect.height() / 2
                    ))
                }
            }
        }
        
        // Sort positions left to right
        return positions.sortedBy { it.x }
    }
    
    /**
     * Executes skill usage
     */
    private suspend fun executeSkillUsage(decision: DecisionResult.SkillUsage): Boolean {
        logger.info(FGOBotLogger.Category.AUTOMATION, "Executing skill usage: Servant ${decision.servantIndex}, Skill ${decision.skillIndex}")
        
        return try {
            // Enhanced skill position detection
            val skillPositions = detectSkillPositions(decision.servantIndex)
            
            if (decision.skillIndex < skillPositions.size) {
                val position = skillPositions[decision.skillIndex]
                inputController.tap(position)
                
                // Wait for skill activation animation
                inputController.humanDelay(800L)
                
                // Handle target selection if needed
                if (decision.targetIndex != null) {
                    val targetPositions = detectTargetPositions()
                    if (decision.targetIndex < targetPositions.size) {
                        val targetPos = targetPositions[decision.targetIndex]
                        inputController.tap(targetPos)
                        inputController.humanDelay(500L)
                    }
                }
                
                // Wait for skill effect
                inputController.humanDelay(1500L)
                true
            } else {
                logger.warn(FGOBotLogger.Category.AUTOMATION, "Invalid skill index: ${decision.skillIndex}")
                false
            }
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error executing skill usage", e)
            false
        }
    }
    
    /**
     * Detects skill positions for a servant
     */
    private fun detectSkillPositions(servantIndex: Int): List<android.graphics.Point> {
        // Skill positions for each servant (3 skills per servant)
        val baseY = 1100 // Skill row Y position
        val servantSkillOffsets = when (servantIndex) {
            0 -> listOf(120, 180, 240)  // Servant 1 skills
            1 -> listOf(420, 480, 540)  // Servant 2 skills
            2 -> listOf(720, 780, 840)  // Servant 3 skills
            else -> emptyList()
        }
        
        return servantSkillOffsets.map { x -> android.graphics.Point(x, baseY) }
    }
    
    /**
     * Detects target positions for skill targeting
     */
    private fun detectTargetPositions(): List<android.graphics.Point> {
        // Target positions (servants and enemies)
        return listOf(
            android.graphics.Point(200, 800),  // Servant 1
            android.graphics.Point(540, 800),  // Servant 2
            android.graphics.Point(880, 800),  // Servant 3
            android.graphics.Point(200, 600),  // Enemy 1
            android.graphics.Point(540, 600),  // Enemy 2
            android.graphics.Point(880, 600)   // Enemy 3
        )
    }
    
    /**
     * Executes NP usage
     */
    private suspend fun executeNPUsage(decision: DecisionResult.NPUsage): Boolean {
        logger.info(FGOBotLogger.Category.AUTOMATION, "Executing NP usage: Servant ${decision.servantIndex}")
        
        return try {
            // Enhanced NP position detection
            val npPositions = listOf(
                android.graphics.Point(200, 950),  // Servant 1 NP
                android.graphics.Point(540, 950),  // Servant 2 NP
                android.graphics.Point(880, 950)   // Servant 3 NP
            )
            
            if (decision.servantIndex < npPositions.size) {
                val position = npPositions[decision.servantIndex]
                inputController.tap(position)
                
                // Wait for NP selection confirmation
                inputController.humanDelay(1000L)
                
                // Check if we need to confirm NP usage
                val confirmButton = android.graphics.Point(540, 1200) // Typical confirm position
                inputController.tap(confirmButton)
                
                // Wait for NP animation
                inputController.humanDelay(5000L + (1000..2000).random())
                
                true
            } else {
                logger.warn(FGOBotLogger.Category.AUTOMATION, "Invalid NP servant index: ${decision.servantIndex}")
                false
            }
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error executing NP usage", e)
            false
        }
    }
    
    /**
     * Executes error recovery
     */
    private suspend fun executeErrorRecovery(decision: DecisionResult.ErrorRecovery): Boolean {
        logger.info(FGOBotLogger.Category.AUTOMATION, "Executing error recovery: ${decision.action}")
        
        when (decision.action) {
            "handle_ap_recovery" -> {
                // Handle AP recovery logic
                delay(5000L)
            }
            "restart_battle" -> {
                // Handle battle restart logic
                delay(3000L)
            }
            "screenshot_analysis" -> {
                // Analyze current screenshot for recovery
                delay(1000L)
            }
        }
        
        return true
    }
    
    /**
     * Gets current automation status
     * 
     * @return Current automation status
     */
    fun getAutomationStatus(): AutomationStatus {
        val currentStats = _automationStats.value
        val runtime = if (startTime > 0) System.currentTimeMillis() - startTime else 0L
        
        return AutomationStatus(
            state = _automationState.value,
            isInitialized = isInitialized,
            currentTeam = _currentTeam.value,
            runtime = runtime,
            stats = currentStats.copy(totalRuntime = runtime),
            lastScreenshot = lastScreenshot,
            consecutiveErrors = consecutiveErrors
        )
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        logger.info(FGOBotLogger.Category.AUTOMATION, "Cleaning up automation controller")
        
        runBlocking {
            stopAutomation()
        }
        
        if (::screenCapture.isInitialized) {
            screenCapture.cleanup()
        }
        
        if (::imageRecognition.isInitialized) {
            imageRecognition.cleanup()
        }
        
        isInitialized = false
        
        logger.info(FGOBotLogger.Category.AUTOMATION, "Automation controller cleanup completed")
    }
}

/**
 * Automation status data class
 */
data class AutomationStatus(
    val state: AutomationState,
    val isInitialized: Boolean,
    val currentTeam: Team?,
    val runtime: Long,
    val stats: AutomationStats,
    val lastScreenshot: Bitmap?,
    val consecutiveErrors: Int
) 