package com.fgobot.data.database.dao

import androidx.room.*
import com.fgobot.data.database.entities.BattleLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BattleLogDao {
    
    @Query("SELECT * FROM battle_logs ORDER BY createdAt DESC")
    fun getAllBattleLogs(): Flow<List<BattleLogEntity>>
    
    @Query("SELECT * FROM battle_logs WHERE id = :id")
    suspend fun getBattleLogById(id: Long): BattleLogEntity?
    
    @Query("SELECT * FROM battle_logs WHERE questId = :questId ORDER BY createdAt DESC")
    fun getBattleLogsByQuest(questId: Int): Flow<List<BattleLogEntity>>
    
    @Query("SELECT * FROM battle_logs WHERE result = :result ORDER BY createdAt DESC")
    fun getBattleLogsByResult(result: String): Flow<List<BattleLogEntity>>
    
    @Query("SELECT * FROM battle_logs WHERE teamConfigId = :teamConfigId ORDER BY createdAt DESC")
    fun getBattleLogsByTeamConfig(teamConfigId: Long): Flow<List<BattleLogEntity>>
    
    @Query("SELECT AVG(performance) FROM battle_logs WHERE questId = :questId AND result = 'WIN'")
    suspend fun getAveragePerformanceForQuest(questId: Int): Float?
    
    @Query("SELECT COUNT(*) FROM battle_logs WHERE result = 'WIN' AND questId = :questId")
    suspend fun getWinCountForQuest(questId: Int): Int
    
    @Query("SELECT COUNT(*) FROM battle_logs WHERE questId = :questId")
    suspend fun getTotalBattlesForQuest(questId: Int): Int
    
    @Query("SELECT * FROM battle_logs ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentBattleLogs(limit: Int): Flow<List<BattleLogEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBattleLog(battleLog: BattleLogEntity): Long
    
    @Update
    suspend fun updateBattleLog(battleLog: BattleLogEntity)
    
    @Delete
    suspend fun deleteBattleLog(battleLog: BattleLogEntity)
    
    @Query("DELETE FROM battle_logs WHERE createdAt < :timestamp")
    suspend fun deleteOldBattleLogs(timestamp: Long)
    
    @Query("DELETE FROM battle_logs")
    suspend fun deleteAllBattleLogs()
} 