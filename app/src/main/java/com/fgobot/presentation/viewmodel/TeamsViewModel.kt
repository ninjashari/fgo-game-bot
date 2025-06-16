/**
 * FGO Bot - Teams ViewModel
 * 
 * This ViewModel manages team data and provides a bridge between
 * the UI layer and the team repository. It handles team creation,
 * editing, deletion, and state management.
 * 
 * Features:
 * - Team CRUD operations
 * - Real-time team list updates
 * - Error handling and user feedback
 * - Database integration
 */

package com.fgobot.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fgobot.core.logging.FGOBotLogger
import com.fgobot.data.database.entities.Team
import com.fgobot.data.repository.TeamRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI state for teams screen
 */
data class TeamsUiState(
    val teams: List<Team> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showCreateDialog: Boolean = false
)

/**
 * User actions for teams
 */
sealed class TeamsAction {
    object LoadTeams : TeamsAction()
    data class CreateTeam(val team: Team) : TeamsAction()
    data class UpdateTeam(val team: Team) : TeamsAction()
    data class DeleteTeam(val teamId: Long) : TeamsAction()
    object ShowCreateDialog : TeamsAction()
    object HideCreateDialog : TeamsAction()
    object ClearError : TeamsAction()
}

/**
 * ViewModel for teams screen
 * 
 * Manages team data, handles user interactions, and provides
 * real-time updates for the teams interface.
 */
class TeamsViewModel(
    private val teamRepository: TeamRepository
) : ViewModel() {
    
    private val logger = FGOBotLogger
    
    // UI state
    private val _uiState = MutableStateFlow(TeamsUiState())
    val uiState: StateFlow<TeamsUiState> = _uiState.asStateFlow()
    
    init {
        // Load teams on initialization
        loadTeams()
    }
    
    /**
     * Handle user actions
     */
    fun handleAction(action: TeamsAction) {
        viewModelScope.launch {
            try {
                when (action) {
                    is TeamsAction.LoadTeams -> loadTeams()
                    is TeamsAction.CreateTeam -> createTeam(action.team)
                    is TeamsAction.UpdateTeam -> updateTeam(action.team)
                    is TeamsAction.DeleteTeam -> deleteTeam(action.teamId)
                    is TeamsAction.ShowCreateDialog -> showCreateDialog()
                    is TeamsAction.HideCreateDialog -> hideCreateDialog()
                    is TeamsAction.ClearError -> clearError()
                }
            } catch (e: Exception) {
                logger.error(FGOBotLogger.Category.UI, "Error handling teams action: $action", e)
                _uiState.value = _uiState.value.copy(errorMessage = "Action failed: ${e.message}")
            }
        }
    }
    
    /**
     * Load teams from repository
     */
    private fun loadTeams() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                teamRepository.getAllTeams().collect { teams ->
                    _uiState.value = _uiState.value.copy(
                        teams = teams,
                        isLoading = false
                    )
                }
                
                logger.info(FGOBotLogger.Category.UI, "Teams loaded successfully")
                
            } catch (e: Exception) {
                logger.error(FGOBotLogger.Category.UI, "Error loading teams", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load teams: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * Create a new team
     */
    private suspend fun createTeam(team: Team) {
        try {
            val result = teamRepository.createTeam(team)
            
            if (result.isSuccess) {
                logger.info(FGOBotLogger.Category.UI, "Team created successfully: ${team.name}")
                _uiState.value = _uiState.value.copy(showCreateDialog = false)
                // Teams will be automatically updated via the Flow
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to create team: $error")
            }
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.UI, "Error creating team: ${team.name}", e)
            _uiState.value = _uiState.value.copy(errorMessage = "Failed to create team: ${e.message}")
        }
    }
    
    /**
     * Update an existing team
     */
    private suspend fun updateTeam(team: Team) {
        try {
            val result = teamRepository.updateTeam(team)
            
            if (result.isSuccess) {
                logger.info(FGOBotLogger.Category.UI, "Team updated successfully: ${team.name}")
                // Teams will be automatically updated via the Flow
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to update team: $error")
            }
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.UI, "Error updating team: ${team.name}", e)
            _uiState.value = _uiState.value.copy(errorMessage = "Failed to update team: ${e.message}")
        }
    }
    
    /**
     * Delete a team
     */
    private suspend fun deleteTeam(teamId: Long) {
        try {
            val result = teamRepository.deleteTeam(teamId)
            
            if (result.isSuccess) {
                logger.info(FGOBotLogger.Category.UI, "Team deleted successfully: ID $teamId")
                // Teams will be automatically updated via the Flow
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to delete team: $error")
            }
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.UI, "Error deleting team: ID $teamId", e)
            _uiState.value = _uiState.value.copy(errorMessage = "Failed to delete team: ${e.message}")
        }
    }
    
    /**
     * Show create team dialog
     */
    private fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }
    
    /**
     * Hide create team dialog
     */
    private fun hideCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }
    
    /**
     * Clear error message
     */
    private fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
} 