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
 */

package com.fgobot.presentation.viewmodel

import android.app.Application
import android.content.Intent
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
     * Initialize the ViewModel with initial data
     */
    private suspend fun initializeViewModel() {
        try {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Check accessibility service status
            val isAccessibilityEnabled = FGOAccessibilityService.isServiceRunning()
            
            // Load available teams
            val teams = teamRepository.getAllTeams().first()
            
            // Load recent battle logs
            val recentLogs = battleLogRepository.getAllBattleLogs().first().take(10)
            
            _uiState.value = _uiState.value.copy(
                availableTeams = teams,
                recentBattleLogs = recentLogs,
                isAccessibilityServiceEnabled = isAccessibilityEnabled,
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
     * Start periodic updates for real-time data
     */
    private fun startPeriodicUpdates() {
        viewModelScope.launch {
            while (true) {
                updateAccessibilityServiceStatus()
                updateAutomationState()
                delay(1000L) // Update every second
            }
        }
    }
    
    /**
     * Update accessibility service status
     */
    private fun updateAccessibilityServiceStatus() {
        val isEnabled = FGOAccessibilityService.isServiceRunning()
        if (_uiState.value.isAccessibilityServiceEnabled != isEnabled) {
            _uiState.value = _uiState.value.copy(isAccessibilityServiceEnabled = isEnabled)
            
            // Initialize automation controller when service becomes available
            if (isEnabled && automationController == null) {
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
        
        val success = controller.initialize(resultCode, data)
        _uiState.value = _uiState.value.copy(
            isInitialized = success,
            errorMessage = if (!success) "Failed to initialize automation" else null
        )
        
        if (success) {
            logger.info(FGOBotLogger.Category.UI, "Automation initialized successfully")
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
} 