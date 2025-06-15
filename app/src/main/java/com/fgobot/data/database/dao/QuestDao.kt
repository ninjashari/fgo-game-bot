package com.fgobot.data.database.dao

import androidx.room.*
import com.fgobot.data.database.entities.QuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    
    @Query("SELECT * FROM quests")
    fun getAllQuests(): Flow<List<QuestEntity>>
    
    @Query("SELECT * FROM quests WHERE id = :id")
    suspend fun getQuestById(id: Int): QuestEntity?
    
    @Query("SELECT * FROM quests WHERE type = :type")
    fun getQuestsByType(type: String): Flow<List<QuestEntity>>
    
    @Query("SELECT * FROM quests WHERE difficulty = :difficulty")
    fun getQuestsByDifficulty(difficulty: String): Flow<List<QuestEntity>>
    
    @Query("SELECT * FROM quests WHERE name LIKE '%' || :name || '%'")
    fun searchQuestsByName(name: String): Flow<List<QuestEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: QuestEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuests(quests: List<QuestEntity>)
    
    @Update
    suspend fun updateQuest(quest: QuestEntity)
    
    @Delete
    suspend fun deleteQuest(quest: QuestEntity)
    
    @Query("DELETE FROM quests")
    suspend fun deleteAllQuests()
} 