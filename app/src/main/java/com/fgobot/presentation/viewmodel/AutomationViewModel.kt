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
     * Update all permission statuses
     */
    fun updatePermissionStatus() {
        val isAccessibilityEnabled = FGOAccessibilityService.isServiceRunning()
        
        _uiState.value = _uiState.value.copy(
            isAccessibilityServiceEnabled = isAccessibilityEnabled
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
            }
        }
    }
    
    /**
     * Start periodic updates for real-time data
     */
    private fun startPeriodicUpdates() {
        viewModelScope.launch {
            while (true) {
                updatePermissionStatus()
                updateAutomationState()
                delay(2000L) // Update every 2 seconds
            }
        }
    }
    
    /**
     * Update automation state from controller
     */
    private fun updateAutomationState() {
        automationController?.let { controller ->
            viewModelScope.launch {
                // Collect automation state
                controller.automationState.collect { state ->
                    _uiState.value = _uiState.value.copy(automationState = state)
                }
            }
            
            viewModelScope.launch {
                // Collect automation stats
                controller.automationStats.collect { stats ->
                    _uiState.value = _uiState.value.copy(automationStats = stats)
                }
            }
            
            viewModelScope.launch {
                // Collect current team
                controller.currentTeam.collect { team ->
                    _uiState.value = _uiState.value.copy(currentTeam = team)
                }
            }
        }
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
        val selectedTeam = _selectedTeam.value
        if (selectedTeam == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select a team first")
            return
        }
        
        val controller = automationController
        if (controller == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Accessibility service not available")
            return
        }
        
        if (!_uiState.value.isInitialized) {
            _uiState.value = _uiState.value.copy(errorMessage = "Automation not initialized. Please grant screen capture permission.")
            return
        }
        
        val success = controller.startAutomation(selectedTeam)
        if (!success) {
            _uiState.value = _uiState.value.copy(errorMessage = "Failed to start automation")
        } else {
            logger.info(FGOBotLogger.Category.UI, "Automation started with team: ${selectedTeam.name}")
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
        val controller = automationController
        if (controller == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Accessibility service not available")
            return
        }
        
        try {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val success = controller.initialize(resultCode, data)
            _uiState.value = _uiState.value.copy(
                isInitialized = success,
                isScreenCapturePermissionGranted = success,
                isLoading = false,
                errorMessage = if (!success) "Failed to initialize automation" else null
            )
            
            if (success) {
                logger.info(FGOBotLogger.Category.UI, "Automation initialized successfully")
            } else {
                logger.error(FGOBotLogger.Category.UI, "Automation initialization failed")
            }
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.UI, "Error during automation initialization", e)
            _uiState.value = _uiState.value.copy(
                isInitialized = false,
                isScreenCapturePermissionGranted = false,
                isLoading = false,
                errorMessage = "Initialization error: ${e.message}"
            )
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
                _uiState.value.isInitialized &&
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
} 