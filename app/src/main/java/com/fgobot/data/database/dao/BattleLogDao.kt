/*
 * FGO Bot - Battle Log Data Access Object
 * 
 * This file defines the DAO interface for BattleLog entity operations.
 * Provides methods for CRUD operations, queries, and battle analytics.
 */

package com.fgobot.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.fgobot.data.database.entities.BattleLog

/**
 * Data Access Object for BattleLog entity
 * 
 * Provides methods for battle log database operations including:
 * - Basic CRUD operations
 * - Battle log queries and filtering
 * - Performance analytics and statistics
 * - Battle history tracking
 */
@Dao
interface BattleLogDao {
    
    // ==================== INSERT OPERATIONS ====================
    
    /**
     * Inserts a new battle log into the database
     * 
     * @param battleLog BattleLog to insert
     * @return ID of the inserted battle log
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBattleLog(battleLog: BattleLog): Long
    
    /**
     * Inserts multiple battle logs into the database
     * 
     * @param battleLogs List of battle logs to insert
     * @return List of inserted battle log IDs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBattleLogs(battleLogs: List<BattleLog>): List<Long>
    
    // ==================== UPDATE OPERATIONS ====================
    
    /**
     * Updates an existing battle log
     * 
     * @param battleLog BattleLog to update
     * @return Number of rows affected
     */
    @Update
    suspend fun updateBattleLog(battleLog: BattleLog): Int
    
    /**
     * Updates multiple battle logs
     * 
     * @param battleLogs List of battle logs to update
     * @return Number of rows affected
     */
    @Update
    suspend fun updateBattleLogs(battleLogs: List<BattleLog>): Int
    
    // ==================== DELETE OPERATIONS ====================
    
    /**
     * Deletes a battle log from the database
     * 
     * @param battleLog BattleLog to delete
     * @return Number of rows affected
     */
    @Delete
    suspend fun deleteBattleLog(battleLog: BattleLog): Int
    
    /**
     * Deletes a battle log by ID
     * 
     * @param battleLogId ID of battle log to delete
     * @return Number of rows affected
     */
    @Query("DELETE FROM battle_logs WHERE id = :battleLogId")
    suspend fun deleteBattleLogById(battleLogId: Long): Int
    
    /**
     * Deletes all battle logs
     * 
     * @return Number of rows affected
     */
    @Query("DELETE FROM battle_logs")
    suspend fun deleteAllBattleLogs(): Int
    
    /**
     * Deletes battle logs older than specified time
     * 
     * @param cutoffTime Timestamp cutoff for deletion
     * @return Number of rows affected
     */
    @Query("DELETE FROM battle_logs WHERE timestamp < :cutoffTime")
    suspend fun deleteOldBattleLogs(cutoffTime: Long): Int
    
    // ==================== QUERY OPERATIONS ====================
    
    /**
     * Gets a battle log by ID
     * 
     * @param battleLogId BattleLog ID
     * @return BattleLog or null if not found
     */
    @Query("SELECT * FROM battle_logs WHERE id = :battleLogId")
    suspend fun getBattleLogById(battleLogId: Long): BattleLog?
    
    /**
     * Gets a battle log by ID as Flow
     * 
     * @param battleLogId BattleLog ID
     * @return Flow of BattleLog or null
     */
    @Query("SELECT * FROM battle_logs WHERE id = :battleLogId")
    fun getBattleLogByIdFlow(battleLogId: Long): Flow<BattleLog?>
    
    /**
     * Gets all battle logs
     * 
     * @return List of all battle logs
     */
    @Query("SELECT * FROM battle_logs ORDER BY timestamp DESC")
    suspend fun getAllBattleLogs(): List<BattleLog>
    
    /**
     * Gets all battle logs as Flow
     * 
     * @return Flow of all battle logs
     */
    @Query("SELECT * FROM battle_logs ORDER BY timestamp DESC")
    fun getAllBattleLogsFlow(): Flow<List<BattleLog>>
    
    /**
     * Gets battle logs by quest ID
     * 
     * @param questId Quest ID
     * @return Flow of battle logs for the specified quest
     */
    @Query("SELECT * FROM battle_logs WHERE questId = :questId ORDER BY timestamp DESC")
    fun getBattleLogsByQuest(questId: Int): Flow<List<BattleLog>>
    
    /**
     * Gets battle logs by team ID
     * 
     * @param teamId Team ID
     * @return Flow of battle logs for the specified team
     */
    @Query("SELECT * FROM battle_logs WHERE teamId = :teamId ORDER BY timestamp DESC")
    fun getBattleLogsByTeam(teamId: Long): Flow<List<BattleLog>>
    
    /**
     * Gets successful battle logs only
     * 
     * @return Flow of successful battle logs
     */
    @Query("SELECT * FROM battle_logs WHERE isVictory = 1 ORDER BY timestamp DESC")
    fun getSuccessfulBattleLogs(): Flow<List<BattleLog>>
    
    /**
     * Gets failed battle logs only
     * 
     * @return Flow of failed battle logs
     */
    @Query("SELECT * FROM battle_logs WHERE isVictory = 0 ORDER BY timestamp DESC")
    fun getFailedBattleLogs(): Flow<List<BattleLog>>
    
    /**
     * Gets battle logs within date range
     * 
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return Flow of battle logs within the date range
     */
    @Query("SELECT * FROM battle_logs WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getBattleLogsByDateRange(startTime: Long, endTime: Long): Flow<List<BattleLog>>
    
    /**
     * Gets battle logs by duration range
     * 
     * @param minDuration Minimum battle duration in seconds
     * @param maxDuration Maximum battle duration in seconds
     * @return Flow of battle logs within the duration range
     */
    @Query("SELECT * FROM battle_logs WHERE battleDuration BETWEEN :minDuration AND :maxDuration ORDER BY timestamp DESC")
    fun getBattleLogsByDurationRange(minDuration: Long, maxDuration: Long): Flow<List<BattleLog>>
    
    /**
     * Gets recent battle logs
     * 
     * @param limit Number of logs to return
     * @return Flow of recent battle logs
     */
    @Query("SELECT * FROM battle_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentBattleLogs(limit: Int = 50): Flow<List<BattleLog>>
    
    // ==================== STATISTICS OPERATIONS ====================
    
    /**
     * Gets total battle count
     * 
     * @return Total number of battles
     */
    @Query("SELECT COUNT(*) FROM battle_logs")
    suspend fun getTotalBattleCount(): Int
    
    /**
     * Gets victory count
     * 
     * @return Number of victorious battles
     */
    @Query("SELECT COUNT(*) FROM battle_logs WHERE isVictory = 1")
    suspend fun getVictoryCount(): Int
    
    /**
     * Gets defeat count
     * 
     * @return Number of defeated battles
     */
    @Query("SELECT COUNT(*) FROM battle_logs WHERE isVictory = 0")
    suspend fun getDefeatCount(): Int
    
    /**
     * Gets win rate percentage
     * 
     * @return Win rate as percentage (0-100)
     */
    @Query("""
        SELECT CASE 
            WHEN COUNT(*) = 0 THEN 0.0 
            ELSE (CAST(SUM(CASE WHEN isVictory = 1 THEN 1 ELSE 0 END) AS REAL) / COUNT(*)) * 100 
        END 
        FROM battle_logs
    """)
    suspend fun getWinRatePercentage(): Double
    
    /**
     * Gets average battle duration
     * 
     * @return Average battle duration in seconds
     */
    @Query("SELECT AVG(battleDuration) FROM battle_logs")
    suspend fun getAverageBattleDuration(): Double?
    
    /**
     * Gets total battle time
     * 
     * @return Total time spent in battles (seconds)
     */
    @Query("SELECT SUM(battleDuration) FROM battle_logs")
    suspend fun getTotalBattleTime(): Long?
    
    /**
     * Gets battle count by quest
     * 
     * @param questId Quest ID
     * @return Number of battles for the specified quest
     */
    @Query("SELECT COUNT(*) FROM battle_logs WHERE questId = :questId")
    suspend fun getBattleCountByQuest(questId: Int): Int
    
    /**
     * Gets battle count by team
     * 
     * @param teamId Team ID
     * @return Number of battles for the specified team
     */
    @Query("SELECT COUNT(*) FROM battle_logs WHERE teamId = :teamId")
    suspend fun getBattleCountByTeam(teamId: Long): Int
    
    /**
     * Gets win rate by quest
     * 
     * @param questId Quest ID
     * @return Win rate for the specified quest
     */
    @Query("""
        SELECT CASE 
            WHEN COUNT(*) = 0 THEN 0.0 
            ELSE (CAST(SUM(CASE WHEN isVictory = 1 THEN 1 ELSE 0 END) AS REAL) / COUNT(*)) * 100 
        END 
        FROM battle_logs WHERE questId = :questId
    """)
    suspend fun getWinRateByQuest(questId: Int): Double
    
    /**
     * Gets win rate by team
     * 
     * @param teamId Team ID
     * @return Win rate for the specified team
     */
    @Query("""
        SELECT CASE 
            WHEN COUNT(*) = 0 THEN 0.0 
            ELSE (CAST(SUM(CASE WHEN isVictory = 1 THEN 1 ELSE 0 END) AS REAL) / COUNT(*)) * 100 
        END 
        FROM battle_logs WHERE teamId = :teamId
    """)
    suspend fun getWinRateByTeam(teamId: Long): Double
    
    /**
     * Gets fastest battles
     * 
     * @param limit Number of battles to return
     * @return List of fastest battles
     */
    @Query("SELECT * FROM battle_logs WHERE isVictory = 1 ORDER BY battleDuration ASC LIMIT :limit")
    suspend fun getFastestBattles(limit: Int = 10): List<BattleLog>
    
    /**
     * Gets slowest battles
     * 
     * @param limit Number of battles to return
     * @return List of slowest battles
     */
    @Query("SELECT * FROM battle_logs ORDER BY battleDuration DESC LIMIT :limit")
    suspend fun getSlowestBattles(limit: Int = 10): List<BattleLog>
    
    /**
     * Gets most used teams (by battle count)
     * 
     * @param limit Number of teams to return
     * @return List of team IDs with battle counts
     */
    @Query("""
        SELECT teamId, COUNT(*) as battleCount 
        FROM battle_logs 
        GROUP BY teamId 
        ORDER BY battleCount DESC 
        LIMIT :limit
    """)
    suspend fun getMostUsedTeams(limit: Int = 10): List<TeamBattleCount>
    
    /**
     * Gets most farmed quests (by battle count)
     * 
     * @param limit Number of quests to return
     * @return List of quest IDs with battle counts
     */
    @Query("""
        SELECT questId, COUNT(*) as battleCount 
        FROM battle_logs 
        GROUP BY questId 
        ORDER BY battleCount DESC 
        LIMIT :limit
    """)
    suspend fun getMostFarmedQuests(limit: Int = 10): List<QuestBattleCount>
    
    // ==================== UTILITY OPERATIONS ====================
    
    /**
     * Gets battle logs that need cleanup (older than specified time)
     * 
     * @param cutoffTime Timestamp cutoff for cleanup
     * @return List of battle logs needing cleanup
     */
    @Query("SELECT * FROM battle_logs WHERE timestamp < :cutoffTime")
    suspend fun getBattleLogsForCleanup(cutoffTime: Long): List<BattleLog>
    
    /**
     * Gets daily battle statistics
     * 
     * @param dayStart Start of day timestamp
     * @param dayEnd End of day timestamp
     * @return Daily battle statistics
     */
    @Query("""
        SELECT 
            COUNT(*) as totalBattles,
            SUM(CASE WHEN isVictory = 1 THEN 1 ELSE 0 END) as victories,
            SUM(CASE WHEN isVictory = 0 THEN 1 ELSE 0 END) as defeats,
            AVG(battleDuration) as avgDuration,
            SUM(battleDuration) as totalDuration
        FROM battle_logs 
        WHERE timestamp BETWEEN :dayStart AND :dayEnd
    """)
    suspend fun getDailyBattleStats(dayStart: Long, dayEnd: Long): DailyBattleStats?
    
    /**
     * Gets performance trends over time
     * 
     * @param days Number of days to analyze
     * @return List of daily performance data
     */
    @Query("""
        SELECT 
            DATE(timestamp/1000, 'unixepoch') as date,
            COUNT(*) as battleCount,
            AVG(CASE WHEN isVictory = 1 THEN 1.0 ELSE 0.0 END) * 100 as winRate,
            AVG(battleDuration) as avgDuration
        FROM battle_logs 
        WHERE timestamp >= :cutoffTime
        GROUP BY DATE(timestamp/1000, 'unixepoch')
        ORDER BY date DESC
    """)
    suspend fun getPerformanceTrends(cutoffTime: Long): List<DailyPerformance>
}

/**
 * Data class for team battle count results
 */
data class TeamBattleCount(
    val teamId: Long,
    val battleCount: Int
)

/**
 * Data class for quest battle count results
 */
data class QuestBattleCount(
    val questId: Int,
    val battleCount: Int
)

/**
 * Data class for daily battle statistics
 */
data class DailyBattleStats(
    val totalBattles: Int,
    val victories: Int,
    val defeats: Int,
    val avgDuration: Double?,
    val totalDuration: Long?
)

/**
 * Data class for daily performance trends
 */
data class DailyPerformance(
    val date: String,
    val battleCount: Int,
    val winRate: Double,
    val avgDuration: Double?
) 