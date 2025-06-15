package com.fgobot.data.database.dao

import androidx.room.*
import com.fgobot.data.database.entities.TeamConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamConfigDao {
    
    @Query("SELECT * FROM team_configs WHERE isActive = 1 ORDER BY priority DESC")
    fun getActiveTeamConfigs(): Flow<List<TeamConfigEntity>>
    
    @Query("SELECT * FROM team_configs WHERE id = :id")
    suspend fun getTeamConfigById(id: Long): TeamConfigEntity?
    
    @Query("SELECT * FROM team_configs WHERE questType = :questType AND isActive = 1 ORDER BY priority DESC")
    fun getTeamConfigsForQuestType(questType: String): Flow<List<TeamConfigEntity>>
    
    @Query("SELECT * FROM team_configs WHERE name LIKE '%' || :name || '%'")
    fun searchTeamConfigsByName(name: String): Flow<List<TeamConfigEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeamConfig(teamConfig: TeamConfigEntity): Long
    
    @Update
    suspend fun updateTeamConfig(teamConfig: TeamConfigEntity)
    
    @Query("UPDATE team_configs SET isActive = :isActive WHERE id = :id")
    suspend fun updateTeamConfigStatus(id: Long, isActive: Boolean)
    
    @Query("UPDATE team_configs SET priority = :priority WHERE id = :id")
    suspend fun updateTeamConfigPriority(id: Long, priority: Int)
    
    @Delete
    suspend fun deleteTeamConfig(teamConfig: TeamConfigEntity)
    
    @Query("DELETE FROM team_configs")
    suspend fun deleteAllTeamConfigs()
} 