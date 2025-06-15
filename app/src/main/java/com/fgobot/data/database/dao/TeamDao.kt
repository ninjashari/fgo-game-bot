/*
 * FGO Bot - Team Data Access Object
 * 
 * This file defines the DAO interface for Team entity operations.
 * Provides methods for CRUD operations, queries, and team management.
 */

package com.fgobot.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.fgobot.data.database.entities.Team

/**
 * Data Access Object for Team entity
 * 
 * Provides methods for team database operations including:
 * - Basic CRUD operations
 * - Team queries and filtering
 * - Team composition management
 * - Battle statistics
 */
@Dao
interface TeamDao {
    
    // ==================== INSERT OPERATIONS ====================
    
    /**
     * Inserts a new team into the database
     * 
     * @param team Team to insert
     * @return ID of the inserted team
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: Team): Long
    
    /**
     * Inserts multiple teams into the database
     * 
     * @param teams List of teams to insert
     * @return List of inserted team IDs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeams(teams: List<Team>): List<Long>
    
    // ==================== UPDATE OPERATIONS ====================
    
    /**
     * Updates an existing team
     * 
     * @param team Team to update
     * @return Number of rows affected
     */
    @Update
    suspend fun updateTeam(team: Team): Int
    
    /**
     * Updates multiple teams
     * 
     * @param teams List of teams to update
     * @return Number of rows affected
     */
    @Update
    suspend fun updateTeams(teams: List<Team>): Int
    
    // ==================== DELETE OPERATIONS ====================
    
    /**
     * Deletes a team from the database
     * 
     * @param team Team to delete
     * @return Number of rows affected
     */
    @Delete
    suspend fun deleteTeam(team: Team): Int
    
    /**
     * Deletes a team by ID
     * 
     * @param teamId ID of team to delete
     * @return Number of rows affected
     */
    @Query("DELETE FROM teams WHERE id = :teamId")
    suspend fun deleteTeamById(teamId: Long): Int
    
    /**
     * Deletes all teams
     * 
     * @return Number of rows affected
     */
    @Query("DELETE FROM teams")
    suspend fun deleteAllTeams(): Int
    
    // ==================== QUERY OPERATIONS ====================
    
    /**
     * Gets a team by ID
     * 
     * @param teamId Team ID
     * @return Team or null if not found
     */
    @Query("SELECT * FROM teams WHERE id = :teamId")
    suspend fun getTeamById(teamId: Long): Team?
    
    /**
     * Gets a team by ID as Flow
     * 
     * @param teamId Team ID
     * @return Flow of Team or null
     */
    @Query("SELECT * FROM teams WHERE id = :teamId")
    fun getTeamByIdFlow(teamId: Long): Flow<Team?>
    
    /**
     * Gets all teams
     * 
     * @return List of all teams
     */
    @Query("SELECT * FROM teams ORDER BY name ASC")
    suspend fun getAllTeams(): List<Team>
    
    /**
     * Gets all teams as Flow
     * 
     * @return Flow of all teams
     */
    @Query("SELECT * FROM teams ORDER BY name ASC")
    fun getAllTeamsFlow(): Flow<List<Team>>
    
    /**
     * Gets teams by name (partial match)
     * 
     * @param name Team name to search for
     * @return List of matching teams
     */
    @Query("SELECT * FROM teams WHERE name LIKE '%' || :name || '%' ORDER BY name ASC")
    suspend fun getTeamsByName(name: String): List<Team>
    
    /**
     * Gets teams by strategy
     * 
     * @param strategy Strategy to filter by
     * @return List of teams for the strategy
     */
    @Query("SELECT * FROM teams WHERE strategy = :strategy ORDER BY name ASC")
    suspend fun getTeamsByStrategy(strategy: String): List<Team>
    
    /**
     * Gets teams containing a specific servant
     * 
     * @param servantId Servant ID to search for
     * @return List of teams containing the servant
     */
    @Query("SELECT * FROM teams WHERE servantIds LIKE '%' || :servantId || '%' ORDER BY name ASC")
    suspend fun getTeamsWithServant(servantId: Int): List<Team>
    
    /**
     * Gets teams with specific support servant class
     * 
     * @param supportClass Support servant class
     * @return List of teams with the support class
     */
    @Query("SELECT * FROM teams WHERE supportServantClass = :supportClass ORDER BY name ASC")
    suspend fun getTeamsBySupportClass(supportClass: String): List<Team>
    
    /**
     * Gets default teams
     * 
     * @return List of default teams
     */
    @Query("SELECT * FROM teams WHERE isDefault = 1 ORDER BY name ASC")
    suspend fun getDefaultTeams(): List<Team>
    
    /**
     * Gets default teams as Flow
     * 
     * @return Flow of default teams
     */
    @Query("SELECT * FROM teams WHERE isDefault = 1 ORDER BY name ASC")
    fun getDefaultTeamsFlow(): Flow<List<Team>>
    
    // ==================== STATISTICS OPERATIONS ====================
    
    /**
     * Gets total number of teams
     * 
     * @return Total team count
     */
    @Query("SELECT COUNT(*) FROM teams")
    suspend fun getTeamCount(): Int
    
    /**
     * Gets number of teams by strategy
     * 
     * @param strategy Strategy type
     * @return Number of teams for the strategy
     */
    @Query("SELECT COUNT(*) FROM teams WHERE strategy = :strategy")
    suspend fun getTeamCountByStrategy(strategy: String): Int
    
    /**
     * Gets number of default teams
     * 
     * @return Number of default teams
     */
    @Query("SELECT COUNT(*) FROM teams WHERE isDefault = 1")
    suspend fun getDefaultTeamCount(): Int
    
    /**
     * Gets teams with highest win rates
     * 
     * @param limit Number of teams to return
     * @return List of top performing teams
     */
    @Query("""
        SELECT * FROM teams 
        WHERE usageCount > 0 
        ORDER BY winRate DESC, usageCount DESC 
        LIMIT :limit
    """)
    suspend fun getTopPerformingTeams(limit: Int = 10): List<Team>
    
    /**
     * Gets most used teams
     * 
     * @param limit Number of teams to return
     * @return List of most used teams
     */
    @Query("SELECT * FROM teams ORDER BY usageCount DESC LIMIT :limit")
    suspend fun getMostUsedTeams(limit: Int = 10): List<Team>
    
    // ==================== UTILITY OPERATIONS ====================
    
    /**
     * Updates team battle statistics
     * 
     * @param teamId Team ID
     * @param won Whether the battle was won
     * @param battleTime Battle completion time in seconds
     */
    @Query("""
        UPDATE teams 
        SET usageCount = usageCount + 1,
            winRate = CASE 
                WHEN usageCount = 0 THEN CASE WHEN :won THEN 100.0 ELSE 0.0 END
                ELSE (winRate * usageCount + CASE WHEN :won THEN 100.0 ELSE 0.0 END) / (usageCount + 1)
            END,
            averageTime = CASE 
                WHEN usageCount = 0 THEN :battleTime
                ELSE (averageTime * usageCount + :battleTime) / (usageCount + 1)
            END,
            lastUsed = :timestamp
        WHERE id = :teamId
    """)
    suspend fun updateBattleStats(
        teamId: Long, 
        won: Boolean, 
        battleTime: Long = 0L,
        timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Toggles team default status
     * 
     * @param teamId Team ID
     */
    @Query("UPDATE teams SET isDefault = NOT isDefault WHERE id = :teamId")
    suspend fun toggleDefault(teamId: Long)
    
    /**
     * Updates team last used timestamp
     * 
     * @param teamId Team ID
     * @param timestamp Timestamp to set
     */
    @Query("UPDATE teams SET lastUsed = :timestamp WHERE id = :teamId")
    suspend fun updateLastUsed(teamId: Long, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Checks if team name exists
     * 
     * @param name Team name to check
     * @param excludeId Team ID to exclude from check (for updates)
     * @return True if name exists
     */
    @Query("SELECT COUNT(*) > 0 FROM teams WHERE name = :name AND (:excludeId IS NULL OR id != :excludeId)")
    suspend fun isTeamNameExists(name: String, excludeId: Long? = null): Boolean
} 