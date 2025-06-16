/*
 * FGO Bot - Team Repository
 * 
 * This file defines the repository interface and implementation for Team data management.
 * Handles team configuration, performance tracking, and validation.
 */

package com.fgobot.data.repository

import com.fgobot.data.database.dao.TeamDao
import com.fgobot.data.database.entities.Team
import com.fgobot.core.error.FGOBotException
import com.fgobot.core.logging.FGOLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Repository interface for Team data operations
 */
interface TeamRepository {
    fun getAllTeams(): Flow<List<Team>>
    suspend fun getTeamById(teamId: Long): Team?
    suspend fun createTeam(team: Team): Result<Long>
    suspend fun updateTeam(team: Team): Result<Unit>
    suspend fun deleteTeam(teamId: Long): Result<Unit>
    suspend fun getTeamStats(): TeamStats
}

/**
 * Team statistics data class
 */
data class TeamStats(
    val totalTeams: Int
)

/**
 * Team repository implementation
 */
class TeamRepositoryImpl(
    private val teamDao: TeamDao,
    private val logger: FGOLogger
) : TeamRepository {
    
    companion object {
        private const val TAG = "TeamRepository"
    }
    
    override fun getAllTeams(): Flow<List<Team>> {
        return teamDao.getAllTeamsFlow()
            .catch { exception ->
                logger.e(TAG, "Error getting all teams", exception)
                throw FGOBotException.DatabaseException("Failed to get teams", exception)
            }
    }
    
    override suspend fun getTeamById(teamId: Long): Team? {
        return try {
            teamDao.getTeamById(teamId)
        } catch (exception: Exception) {
            logger.e(TAG, "Error getting team by ID: $teamId", exception)
            throw FGOBotException.DatabaseException("Failed to get team", exception)
        }
    }
    
    override suspend fun createTeam(team: Team): Result<Long> {
        return try {
            val teamId = teamDao.insertTeam(team)
            logger.d(TAG, "Created new team: ${team.name} with ID: $teamId")
            Result.success(teamId)
        } catch (exception: Exception) {
            logger.e(TAG, "Error creating team: ${team.name}", exception)
            Result.failure(FGOBotException.DatabaseException("Failed to create team", exception))
        }
    }
    
    override suspend fun updateTeam(team: Team): Result<Unit> {
        return try {
            val rowsAffected = teamDao.updateTeam(team)
            if (rowsAffected > 0) {
                logger.d(TAG, "Updated team: ${team.name}")
                Result.success(Unit)
            } else {
                logger.w(TAG, "No team found with ID: ${team.id}")
                Result.failure(FGOBotException.DataNotFoundException("Team not found"))
            }
        } catch (exception: Exception) {
            logger.e(TAG, "Error updating team: ${team.name}", exception)
            Result.failure(FGOBotException.DatabaseException("Failed to update team", exception))
        }
    }
    
    override suspend fun deleteTeam(teamId: Long): Result<Unit> {
        return try {
            val team = teamDao.getTeamById(teamId)
            if (team != null) {
                teamDao.deleteTeam(team)
                logger.d(TAG, "Deleted team: ${team.name}")
                Result.success(Unit)
            } else {
                logger.w(TAG, "No team found with ID: $teamId")
                Result.failure(FGOBotException.DataNotFoundException("Team not found"))
            }
        } catch (exception: Exception) {
            logger.e(TAG, "Error deleting team: $teamId", exception)
            Result.failure(FGOBotException.DatabaseException("Failed to delete team", exception))
        }
    }
    
    override suspend fun getTeamStats(): TeamStats {
        return try {
            val totalTeams = teamDao.getTeamCount()
            
            TeamStats(
                totalTeams = totalTeams
            )
            
        } catch (exception: Exception) {
            logger.e(TAG, "Error getting team statistics", exception)
            throw FGOBotException.DatabaseException("Failed to get statistics", exception)
        }
    }
} 