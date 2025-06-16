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
            logger.info(FGOBotLogger.Category.AUTOMATION, "Initializing automation controller")
            _automationState.value = AutomationState.INITIALIZING
            
            // Initialize core systems
            screenCapture = ScreenCapture(context, logger)
            imageRecognition = ImageRecognition(context, logger)
            inputController = InputController(accessibilityService, logger)
            decisionEngine = DecisionEngine(imageRecognition, logger)
            
            // Initialize each system
            val screenCaptureInit = screenCapture.initialize(resultCode, data)
            val imageRecognitionInit = imageRecognition.initialize()
            
            if (screenCaptureInit && imageRecognitionInit) {
                isInitialized = true
                _automationState.value = AutomationState.IDLE
                logger.info(FGOBotLogger.Category.AUTOMATION, "Automation controller initialized successfully")
                true
            } else {
                logger.error(FGOBotLogger.Category.AUTOMATION, "Failed to initialize core systems")
                _automationState.value = AutomationState.ERROR
                false
            }
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error during initialization", e)
            _automationState.value = AutomationState.ERROR
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
     * Handles battle result screen
     */
    private suspend fun handleBattleResult(screenshot: Bitmap?) {
        logger.info(FGOBotLogger.Category.AUTOMATION, "Handling battle result")
        
        // For now, assume victory (can be enhanced with actual detection)
        val isVictory = true // TODO: Implement actual victory detection
        
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
        
        // Tap center of screen to continue through result screens
        inputController.tap(android.graphics.Point(540, 960))
        inputController.humanDelay(2000L)
    }
    
    /**
     * Handles AP recovery screen
     */
    private suspend fun handleAPRecovery(screenshot: Bitmap?) {
        logger.info(FGOBotLogger.Category.AUTOMATION, "Handling AP recovery")
        
        // For now, stop automation when AP runs out
        // This can be enhanced to use AP items or wait for natural recovery
        logger.warn(FGOBotLogger.Category.AUTOMATION, "AP depleted - stopping automation")
        _automationState.value = AutomationState.COMPLETED
    }
    
    /**
     * Handles quest selection screen
     */
    private suspend fun handleQuestSelection(screenshot: Bitmap?) {
        logger.info(FGOBotLogger.Category.AUTOMATION, "Handling quest selection")
        
        // For now, tap center-right area where repeat button usually is
        inputController.tap(android.graphics.Point(800, 1600))
        inputController.humanDelay(1500L)
    }
    
    /**
     * Handles error states
     */
    private suspend fun handleErrorState(screenshot: Bitmap?) {
        logger.warn(FGOBotLogger.Category.AUTOMATION, "Handling error state")
        
        // Try tapping center of screen to dismiss dialogs
        inputController.tap(android.graphics.Point(540, 960))
        inputController.humanDelay(2000L)
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
            // For now, use predefined card positions (can be enhanced with actual detection)
            val cardPositions = listOf(
                android.graphics.Point(200, 1400),  // Card 1
                android.graphics.Point(540, 1400),  // Card 2
                android.graphics.Point(880, 1400)   // Card 3
            )
            
            // Tap selected cards in order
            for (cardIndex in decision.cardIndices) {
                if (cardIndex < cardPositions.size) {
                    val position = cardPositions[cardIndex]
                    inputController.tap(position)
                    inputController.humanDelay(300L) // Delay between card taps
                }
            }
            
            // Wait for attack animation
            inputController.humanDelay(2000L)
            true
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error executing card selection", e)
            false
        }
    }
    
    /**
     * Executes skill usage
     */
    private suspend fun executeSkillUsage(decision: DecisionResult.SkillUsage): Boolean {
        logger.info(FGOBotLogger.Category.AUTOMATION, "Executing skill usage: Servant ${decision.servantIndex}, Skill ${decision.skillIndex}")
        
        return try {
            // For now, use predefined skill positions (3 skills per servant, 3 servants)
            val skillPositions = listOf(
                // Servant 1 skills
                android.graphics.Point(150, 1100), android.graphics.Point(250, 1100), android.graphics.Point(350, 1100),
                // Servant 2 skills  
                android.graphics.Point(450, 1100), android.graphics.Point(550, 1100), android.graphics.Point(650, 1100),
                // Servant 3 skills
                android.graphics.Point(750, 1100), android.graphics.Point(850, 1100), android.graphics.Point(950, 1100)
            )
            
            // Calculate skill button index (3 skills per servant)
            val skillButtonIndex = (decision.servantIndex * 3) + decision.skillIndex
            
            if (skillButtonIndex < skillPositions.size) {
                val position = skillPositions[skillButtonIndex]
                inputController.tap(position)
                
                // Wait for skill animation
                inputController.humanDelay(1500L)
                
                // Handle target selection if needed
                decision.targetIndex?.let { targetIndex ->
                    if (targetIndex >= 0) {
                        val targetPositions = listOf(
                            android.graphics.Point(200, 800),  // Target 1
                            android.graphics.Point(540, 800),  // Target 2
                            android.graphics.Point(880, 800)   // Target 3
                        )
                        if (targetIndex < targetPositions.size) {
                            val targetPosition = targetPositions[targetIndex]
                            inputController.tap(targetPosition)
                            inputController.humanDelay(500L)
                        }
                    }
                }
                
                true
            } else {
                logger.warn(FGOBotLogger.Category.AUTOMATION, "Invalid skill button index: $skillButtonIndex")
                false
            }
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error executing skill usage", e)
            false
        }
    }
    
    /**
     * Executes NP usage
     */
    private suspend fun executeNPUsage(decision: DecisionResult.NPUsage): Boolean {
        logger.info(FGOBotLogger.Category.AUTOMATION, "Executing NP usage: Servant ${decision.servantIndex}")
        
        return try {
            // For now, use predefined NP positions (3 servants)
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