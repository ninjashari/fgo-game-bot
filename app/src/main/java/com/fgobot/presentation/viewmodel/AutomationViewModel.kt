/**
 * FGO Bot - Automation ViewModel
 * 
 * This ViewModel manages the automation state and provides a bridge between
 * the UI layer and the core automation systems. It handles state management,
 * user interactions, and real-time updates for the automation interface.
 * 
 * Features:
 * - Automation state management
 * - Real-time statistics updates
 * - Team selection and configuration
 * - Error handling and user feedback
 * - Permission status management
 * - Screen capture permission handling
 */

package com.fgobot.presentation.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fgobot.core.automation.AutomationController
import com.fgobot.core.automation.AutomationState
import com.fgobot.core.automation.AutomationStats
import com.fgobot.core.FGOAccessibilityService
import com.fgobot.core.logging.FGOBotLogger
import com.fgobot.data.database.entities.Team
import com.fgobot.data.database.entities.BattleLog
import com.fgobot.data.repository.TeamRepository
import com.fgobot.data.repository.BattleLogRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * UI state for automation screen
 */
data class AutomationUiState(
    val automationState: AutomationState = AutomationState.IDLE,
    val currentTeam: Team? = null,
    val automationStats: AutomationStats = AutomationStats(0, 0, 0, 0L, 0L, 0, 0, 0),
    val availableTeams: List<Team> = emptyList(),
    val recentBattleLogs: List<BattleLog> = emptyList(),
    val isAccessibilityServiceEnabled: Boolean = false,
    val isScreenCapturePermissionGranted: Boolean = false,
    val isInitialized: Boolean = false,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

/**
 * User actions for automation
 */
sealed class AutomationAction {
    object StartAutomation : AutomationAction()
    object StopAutomation : AutomationAction()
    object PauseAutomation : AutomationAction()
    object ResumeAutomation : AutomationAction()
    data class SelectTeam(val team: Team) : AutomationAction()
    object RefreshTeams : AutomationAction()
    object ClearError : AutomationAction()
    data class InitializeAutomation(val resultCode: Int, val data: Intent) : AutomationAction()
    object RequestScreenCapturePermission : AutomationAction()
    object OpenAccessibilitySettings : AutomationAction()
    object RetryInitialization : AutomationAction()
    
    // FGA-inspired: Service control actions
    object StartService : AutomationAction()
    object StopService : AutomationAction()
}

/**
 * ViewModel for automation screen
 * 
 * Manages the automation state, handles user interactions, and provides
 * real-time updates for the automation interface.
 */
class AutomationViewModel(
    application: Application,
    private val teamRepository: TeamRepository,
    private val battleLogRepository: BattleLogRepository
) : AndroidViewModel(application) {
    
    private val logger = FGOBotLogger
    
    // Automation controller (initialized when accessibility service is available)
    private var automationController: AutomationController? = null
    
    // Screen capture permission launcher
    private var screenCapturePermissionLauncher: ((Intent) -> Unit)? = null
    
    // Accessibility settings launcher
    private var accessibilitySettingsLauncher: (() -> Unit)? = null
    
    // MediaProjection manager
    private val mediaProjectionManager = application.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    
    // UI state
    private val _uiState = MutableStateFlow(AutomationUiState())
    val uiState: StateFlow<AutomationUiState> = _uiState.asStateFlow()
    
    // Selected team for automation
    private val _selectedTeam = MutableStateFlow<Team?>(null)
    val selectedTeam: StateFlow<Team?> = _selectedTeam.asStateFlow()
    
    // Flag to track if we are collecting controller state
    private var isCollectingControllerState = false
    
    init {
        // Initialize ViewModel
        viewModelScope.launch {
            initializeViewModel()
        }
        
        // Start periodic updates
        startPeriodicUpdates()
    }
    
    /**
     * Set screen capture permission launcher
     */
    fun setScreenCapturePermissionLauncher(launcher: (Intent) -> Unit) {
        screenCapturePermissionLauncher = launcher
    }
    
    /**
     * Set accessibility settings launcher
     */
    fun setAccessibilitySettingsLauncher(launcher: () -> Unit) {
        accessibilitySettingsLauncher = launcher
    }
    
    /**
     * Initialize the ViewModel with initial data
     */
    private suspend fun initializeViewModel() {
        try {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Check all permission statuses
            updatePermissionStatus()
            
            // Load available teams
            var teams = teamRepository.getAllTeams().first()
            
            // Create a default test team if no teams exist
            if (teams.isEmpty()) {
                createDefaultTestTeam()
                // Reload teams after creating default team
                teams = teamRepository.getAllTeams().first()
            }
            
            // Load recent battle logs
            val recentLogs = battleLogRepository.getAllBattleLogs().first().take(10)
            
            _uiState.value = _uiState.value.copy(
                availableTeams = teams,
                recentBattleLogs = recentLogs,
                isLoading = false
            )
            
            logger.info(FGOBotLogger.Category.UI, "AutomationViewModel initialized successfully")
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.UI, "Error initializing AutomationViewModel", e)
            _uiState.value = _uiState.value.copy(
                errorMessage = "Failed to initialize: ${e.message}",
                isLoading = false
            )
        }
    }
    
    /**
     * Creates a default test team for immediate testing
     */
    private suspend fun createDefaultTestTeam() {
        try {
            val defaultTeam = Team(
                id = 0, // Auto-generated
                name = "Default Test Team",
                description = "Auto-created team for testing automation",
                servantIds = listOf(1, 2, 3), // Placeholder servant IDs
                craftEssenceIds = listOf(1, 2, 3), // Placeholder CE IDs
                supportServantClass = "Any",
                supportCraftEssence = "Any",
                strategy = "Auto",
                isDefault = true
            )
            
            val result = teamRepository.createTeam(defaultTeam)
            if (result.isSuccess) {
                logger.info(FGOBotLogger.Category.UI, "Created default test team for automation testing")
            }
        } catch (e: Exception) {
            logger.warn(FGOBotLogger.Category.UI, "Failed to create default test team", e)
            // Don't fail initialization if we can't create the test team
        }
    }
    
    /**
     * Update permission status including accessibility service state
     */
    private fun updatePermissionStatus() {
        val isAccessibilityEnabled = FGOAccessibilityService.isServiceRunning()
        
        _uiState.value = _uiState.value.copy(
            isAccessibilityServiceEnabled = isAccessibilityEnabled
        )
        
        logger.debug(
            FGOBotLogger.Category.UI,
            "Permission status updated - Accessibility: $isAccessibilityEnabled"
        )
        
        // Initialize automation controller when accessibility service becomes available
        if (isAccessibilityEnabled && automationController == null) {
            val service = FGOAccessibilityService.getInstance()
            if (service != null) {
                automationController = AutomationController(
                    getApplication(),
                    service,
                    logger
                )
                // Reset the collection flag for the new controller
                isCollectingControllerState = false
            }
        }
    }
    
    /**
     * Start periodic updates for UI state
     */
    private fun startPeriodicUpdates() {
        viewModelScope.launch {
            while (true) {
                delay(1000) // Update every second
                
                // Update permission status
                updatePermissionStatus()
                
                // Update automation stats if controller is available
                if (isCollectingControllerState) {
                    updateAutomationState()
                }
            }
        }
    }
    
    /**
     * Update automation state from controller
     */
    private fun updateAutomationState() {
        val controller = automationController
        if (controller != null) {
            // Only start collecting if we haven't already started
            if (!isCollectingControllerState) {
                isCollectingControllerState = true
                
                // Collect automation state
                viewModelScope.launch {
                    controller.automationState.collect { state ->
                        _uiState.value = _uiState.value.copy(automationState = state)
                        logger.debug(FGOBotLogger.Category.UI, "Automation state updated to: $state")
                    }
                }
                
                // Collect automation stats
                viewModelScope.launch {
                    controller.automationStats.collect { stats ->
                        _uiState.value = _uiState.value.copy(automationStats = stats)
                    }
                }
                
                // Collect current team
                viewModelScope.launch {
                    controller.currentTeam.collect { team ->
                        _uiState.value = _uiState.value.copy(currentTeam = team)
                    }
                }
            }
        }
    }
    
    /**
     * Refresh permission status (public method for external calls)
     */
    fun refreshPermissionStatus() {
        updatePermissionStatus()
    }
    
    /**
     * Handle user actions
     */
    fun handleAction(action: AutomationAction) {
        viewModelScope.launch {
            try {
                when (action) {
                    is AutomationAction.StartAutomation -> startAutomation()
                    is AutomationAction.StopAutomation -> stopAutomation()
                    is AutomationAction.PauseAutomation -> pauseAutomation()
                    is AutomationAction.ResumeAutomation -> resumeAutomation()
                    is AutomationAction.SelectTeam -> selectTeam(action.team)
                    is AutomationAction.RefreshTeams -> refreshTeams()
                    is AutomationAction.ClearError -> clearError()
                    is AutomationAction.InitializeAutomation -> initializeAutomation(action.resultCode, action.data)
                    is AutomationAction.RequestScreenCapturePermission -> requestScreenCapturePermission()
                    is AutomationAction.OpenAccessibilitySettings -> openAccessibilitySettings()
                    is AutomationAction.RetryInitialization -> retryInitialization()
                    is AutomationAction.StartService -> startService()
                    is AutomationAction.StopService -> stopService()
                }
            } catch (e: Exception) {
                logger.error(FGOBotLogger.Category.UI, "Error handling action: $action", e)
                _uiState.value = _uiState.value.copy(errorMessage = "Action failed: ${e.message}")
            }
        }
    }
    
    /**
     * Start automation with selected team
     */
    private suspend fun startAutomation() {
        try {
            // Check if accessibility service is enabled
            if (!_uiState.value.isAccessibilityServiceEnabled) {
                _uiState.value = _uiState.value.copy(errorMessage = "Accessibility service not enabled. Please enable it in Settings.")
                return
            }
            
            // Check if team is selected
            if (_selectedTeam.value == null) {
                _uiState.value = _uiState.value.copy(errorMessage = "Please select a team before starting automation.")
                return
            }
            
            // FGA-inspired approach: Request screen capture permission if not granted
            if (!_uiState.value.isScreenCapturePermissionGranted) {
                logger.info(FGOBotLogger.Category.UI, "Screen capture permission not granted, requesting...")
                requestScreenCapturePermission()
                return
            }
            
            // Start automation if all conditions are met
            val controller = automationController
            if (controller != null) {
                val team = _selectedTeam.value!!
                logger.info(FGOBotLogger.Category.AUTOMATION, "Starting automation with team: ${team.name}")
                
                // Let the controller handle its own state - don't override it manually
                val startSuccess = controller.startAutomation(team)
                
                if (!startSuccess) {
                    _uiState.value = _uiState.value.copy(
                        automationState = AutomationState.ERROR,
                        errorMessage = "Failed to start automation - controller returned false"
                    )
                    logger.error(FGOBotLogger.Category.AUTOMATION, "Controller failed to start automation")
                }
                // If successful, the controller's state flow will update the UI state automatically
                
            } else {
                throw IllegalStateException("Automation controller not initialized")
            }
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Error starting automation", e)
            _uiState.value = _uiState.value.copy(
                automationState = AutomationState.ERROR,
                errorMessage = "Failed to start automation: ${e.message}"
            )
        }
    }
    
    /**
     * Stop automation
     */
    private suspend fun stopAutomation() {
        automationController?.stopAutomation()
        logger.info(FGOBotLogger.Category.UI, "Automation stopped by user")
    }
    
    /**
     * Pause automation
     */
    private fun pauseAutomation() {
        automationController?.pauseAutomation()
        logger.info(FGOBotLogger.Category.UI, "Automation paused by user")
    }
    
    /**
     * Resume automation
     */
    private fun resumeAutomation() {
        automationController?.resumeAutomation()
        logger.info(FGOBotLogger.Category.UI, "Automation resumed by user")
    }
    
    /**
     * Select team for automation
     */
    private fun selectTeam(team: Team) {
        _selectedTeam.value = team
        logger.info(FGOBotLogger.Category.UI, "Team selected: ${team.name}")
    }
    
    /**
     * Refresh available teams
     */
    private suspend fun refreshTeams() {
        try {
            val teams = teamRepository.getAllTeams().first()
            _uiState.value = _uiState.value.copy(availableTeams = teams)
            logger.info(FGOBotLogger.Category.UI, "Teams refreshed: ${teams.size} teams loaded")
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.UI, "Error refreshing teams", e)
            _uiState.value = _uiState.value.copy(errorMessage = "Failed to refresh teams: ${e.message}")
        }
    }
    
    /**
     * Clear error message
     */
    private fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * Initialize automation with screen capture permission
     */
    private suspend fun initializeAutomation(resultCode: Int, data: Intent) {
        try {
            logger.info(FGOBotLogger.Category.AUTOMATION, "Starting automation initialization...")
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            if (resultCode == Activity.RESULT_OK) {
                logger.info(FGOBotLogger.Category.AUTOMATION, "Screen capture permission granted, proceeding with initialization")
                
                // Initialize automation controller with screen capture permission
                val service = FGOAccessibilityService.getInstance()
                if (service != null) {
                    logger.info(FGOBotLogger.Category.AUTOMATION, "Accessibility service available, creating controller")
                    
                    // Create new controller if not exists
                    if (automationController == null) {
                        automationController = AutomationController(
                            getApplication(),
                            service,
                            logger
                        )
                        // Reset the collection flag for the new controller
                        isCollectingControllerState = false
                        logger.info(FGOBotLogger.Category.AUTOMATION, "AutomationController created")
                    }
                    
                    // Initialize the controller with screen capture permission
                    val controller = automationController!!
                    logger.info(FGOBotLogger.Category.AUTOMATION, "Calling controller.initialize()...")
                    
                    // Add timeout for the entire initialization process
                    val initSuccess = withTimeoutOrNull(15000L) { // 15 second timeout
                        controller.initialize(resultCode, data)
                    } ?: false
                    
                    logger.info(FGOBotLogger.Category.AUTOMATION, "Controller initialization result: $initSuccess")
                    
                    if (initSuccess) {
                        _uiState.value = _uiState.value.copy(
                            isInitialized = true,
                            isScreenCapturePermissionGranted = true,
                            isLoading = false,
                            errorMessage = null
                        )
                    } else {
                        handleInitializationError("Initialization failed or timed out")
                    }
                    
                    if (initSuccess) {
                        logger.info(FGOBotLogger.Category.AUTOMATION, "Automation initialized successfully")
                        
                        // FGA-inspired approach: Automatically start automation after permission granted
                        if (_selectedTeam.value != null && _uiState.value.isAccessibilityServiceEnabled) {
                            logger.info(FGOBotLogger.Category.AUTOMATION, "Auto-starting automation after permission granted")
                            startAutomation()
                        } else {
                            logger.info(FGOBotLogger.Category.AUTOMATION, "Not auto-starting: team=${_selectedTeam.value?.name}, accessibility=${_uiState.value.isAccessibilityServiceEnabled}")
                        }
                    } else {
                        logger.error(FGOBotLogger.Category.AUTOMATION, "Failed to initialize automation controller")
                    }
                } else {
                    logger.error(FGOBotLogger.Category.AUTOMATION, "Accessibility service not available")
                    throw IllegalStateException("Accessibility service not available")
                }
            } else {
                logger.warn(FGOBotLogger.Category.AUTOMATION, "Screen capture permission denied, resultCode: $resultCode")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Screen capture permission denied"
                )
            }
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.UI, "Error during automation initialization", e)
            handleInitializationError("Initialization error: ${e.message}")
        }
    }
    
    /**
     * Request screen capture permission
     */
    private fun requestScreenCapturePermission() {
        try {
            val intent = mediaProjectionManager.createScreenCaptureIntent()
            screenCapturePermissionLauncher?.invoke(intent)
            logger.info(FGOBotLogger.Category.UI, "Screen capture permission requested")
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.UI, "Error requesting screen capture permission", e)
            _uiState.value = _uiState.value.copy(errorMessage = "Failed to request screen capture permission: ${e.message}")
        }
    }
    
    /**
     * Initialize automation with screen capture permission
     */
    fun initializeAutomationWithPermission(resultCode: Int, data: Intent) {
        viewModelScope.launch {
            initializeAutomation(resultCode, data)
        }
    }
    
    /**
     * Check if automation is ready to start
     */
    fun isAutomationReady(): Boolean {
        return _uiState.value.isAccessibilityServiceEnabled &&
                _uiState.value.isInitialized &&
                _selectedTeam.value != null
    }
    
    /**
     * Get detailed automation status
     */
    fun getDetailedStatus(): String {
        val uiState = _uiState.value
        return when {
            !uiState.isAccessibilityServiceEnabled -> "Accessibility service not enabled"
            !uiState.isInitialized -> "Screen capture permission not granted"
            _selectedTeam.value == null -> "No team selected"
            else -> "Ready to start automation"
        }
    }
    
    /**
     * Get automation state display text
     */
    fun getAutomationStateText(): String {
        return when (_uiState.value.automationState) {
            AutomationState.IDLE -> "Ready"
            AutomationState.INITIALIZING -> "Initializing..."
            AutomationState.RUNNING -> "Running"
            AutomationState.PAUSED -> "Paused"
            AutomationState.STOPPING -> "Stopping..."
            AutomationState.ERROR -> "Error"
            AutomationState.COMPLETED -> "Completed"
        }
    }
    
    /**
     * Check if automation can be started
     */
    fun canStartAutomation(): Boolean {
        return _uiState.value.isAccessibilityServiceEnabled &&
                _selectedTeam.value != null &&
                _uiState.value.automationState == AutomationState.IDLE
    }
    
    /**
     * Check if automation can be stopped
     */
    fun canStopAutomation(): Boolean {
        return _uiState.value.automationState in listOf(
            AutomationState.RUNNING,
            AutomationState.PAUSED
        )
    }
    
    /**
     * Check if automation can be paused
     */
    fun canPauseAutomation(): Boolean {
        return _uiState.value.automationState == AutomationState.RUNNING
    }
    
    /**
     * Check if automation can be resumed
     */
    fun canResumeAutomation(): Boolean {
        return _uiState.value.automationState == AutomationState.PAUSED
    }
    
    /**
     * Open accessibility settings
     */
    private fun openAccessibilitySettings() {
        try {
            accessibilitySettingsLauncher?.invoke()
            logger.info(FGOBotLogger.Category.UI, "Accessibility settings opened")
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.UI, "Error opening accessibility settings", e)
            _uiState.value = _uiState.value.copy(errorMessage = "Failed to open accessibility settings: ${e.message}")
        }
    }
    
    /**
     * Retry initialization after failure
     */
    private suspend fun retryInitialization() {
        try {
            logger.info(FGOBotLogger.Category.UI, "Retrying automation initialization...")
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            // FGA-inspired: Clean up previous failed initialization
            automationController?.cleanup()
            automationController = null
            isCollectingControllerState = false
            
            // Request screen capture permission again
            requestScreenCapturePermission()
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.UI, "Error during retry initialization", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Retry failed: ${e.message}"
            )
        }
    }
    
    /**
     * FGA-inspired: Enhanced error handling with specific recovery actions
     */
    private fun handleInitializationError(error: String) {
        logger.error(FGOBotLogger.Category.UI, "Initialization error: $error")
        
        val errorMessage = when {
            error.contains("timeout") || error.contains("timed out") -> {
                "Initialization timed out. This usually happens due to screen capture issues. Try again or restart the app."
            }
            error.contains("MediaProjection") || error.contains("screen capture") -> {
                "Screen capture permission issue. Please grant permission when prompted."
            }
            error.contains("Accessibility") -> {
                "Accessibility service not available. Please enable it in Settings."
            }
            error.contains("OpenCV") -> {
                "Image recognition system failed to initialize. Using fallback mode."
            }
            else -> {
                "Initialization failed: $error. Please try again."
            }
        }
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = errorMessage,
            isInitialized = false
        )
    }
    
    /**
     * FGA-inspired: Start service (restart if already enabled, redirect to settings if not)
     */
    private fun startService() {
        try {
            logger.info(FGOBotLogger.Category.UI, "Starting service")
            
            val service = FGOAccessibilityService.getInstance()
            if (service != null) {
                // Service is already running, just restart it
                service.restartService()
                logger.info(FGOBotLogger.Category.UI, "Service restarted")
                
                // Refresh permission status to update UI
                viewModelScope.launch {
                    delay(500) // Give service time to restart
                    updatePermissionStatus()
                }
            } else {
                // Service not running, redirect to accessibility settings
                logger.info(FGOBotLogger.Category.UI, "Service not running, redirecting to accessibility settings")
                openAccessibilitySettings()
            }
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.UI, "Error starting service", e)
            _uiState.value = _uiState.value.copy(errorMessage = "Failed to start service: ${e.message}")
        }
    }
    
    /**
     * FGA-inspired: Stop service and hide floating overlay
     */
    private fun stopService() {
        try {
            logger.info(FGOBotLogger.Category.UI, "Stopping service and hiding floating overlay")
            
            // Stop any running automation
            viewModelScope.launch {
                automationController?.stopAutomation()
            }
            
            // Tell accessibility service to hide floating overlay
            val service = FGOAccessibilityService.getInstance()
            service?.stopService()
            
            // Update UI state
            _uiState.value = _uiState.value.copy(
                automationState = AutomationState.IDLE,
                errorMessage = null
            )
            
            // Refresh permission status to update UI
            viewModelScope.launch {
                delay(500) // Give service time to stop
                updatePermissionStatus()
            }
            
            logger.info(FGOBotLogger.Category.UI, "Service stopped successfully")
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.UI, "Error stopping service", e)
            _uiState.value = _uiState.value.copy(errorMessage = "Failed to stop service: ${e.message}")
        }
    }
} 