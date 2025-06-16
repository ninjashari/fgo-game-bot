/*
 * FGO Bot - Quest Data Access Object
 * 
 * This file defines the DAO interface for Quest entity operations.
 * Provides methods for CRUD operations, queries, and quest management.
 */

package com.fgobot.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.fgobot.data.database.entities.Quest

/**
 * Data Access Object for Quest entity
 * 
 * Provides methods for quest database operations including:
 * - Basic CRUD operations
 * - Quest queries and filtering
 * - Type and difficulty-based searches
 * - Completion tracking and analytics
 */
@Dao
interface QuestDao {
    
    // ==================== INSERT OPERATIONS ====================
    
    /**
     * Inserts a new quest into the database
     * 
     * @param quest Quest to insert
     * @return ID of the inserted quest
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: Quest): Long
    
    /**
     * Inserts multiple quests into the database
     * 
     * @param quests List of quests to insert
     * @return List of inserted quest IDs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuests(quests: List<Quest>): List<Long>
    
    // ==================== UPDATE OPERATIONS ====================
    
    /**
     * Updates an existing quest
     * 
     * @param quest Quest to update
     * @return Number of rows affected
     */
    @Update
    suspend fun updateQuest(quest: Quest): Int
    
    /**
     * Updates multiple quests
     * 
     * @param quests List of quests to update
     * @return Number of rows affected
     */
    @Update
    suspend fun updateQuests(quests: List<Quest>): Int
    
    // ==================== DELETE OPERATIONS ====================
    
    /**
     * Deletes a quest from the database
     * 
     * @param quest Quest to delete
     * @return Number of rows affected
     */
    @Delete
    suspend fun deleteQuest(quest: Quest): Int
    
    /**
     * Deletes a quest by ID
     * 
     * @param questId ID of quest to delete
     * @return Number of rows affected
     */
    @Query("DELETE FROM quests WHERE id = :questId")
    suspend fun deleteQuestById(questId: Int): Int
    
    /**
     * Deletes all quests
     * 
     * @return Number of rows affected
     */
    @Query("DELETE FROM quests")
    suspend fun deleteAllQuests(): Int
    
    // ==================== QUERY OPERATIONS ====================
    
    /**
     * Gets a quest by ID
     * 
     * @param questId Quest ID
     * @return Quest or null if not found
     */
    @Query("SELECT * FROM quests WHERE id = :questId")
    suspend fun getQuestById(questId: Int): Quest?
    
    /**
     * Gets a quest by ID as Flow
     * 
     * @param questId Quest ID
     * @return Flow of Quest or null
     */
    @Query("SELECT * FROM quests WHERE id = :questId")
    fun getQuestByIdFlow(questId: Int): Flow<Quest?>
    
    /**
     * Gets all quests
     * 
     * @return List of all quests
     */
    @Query("SELECT * FROM quests ORDER BY chapter ASC, questOrder ASC")
    suspend fun getAllQuests(): List<Quest>
    
    /**
     * Gets all quests as Flow
     * 
     * @return Flow of all quests
     */
    @Query("SELECT * FROM quests ORDER BY chapter ASC, questOrder ASC")
    fun getAllQuestsFlow(): Flow<List<Quest>>
    
    /**
     * Gets quests by type
     * 
     * @param questType Quest type (Main, Free, Event, etc.)
     * @return Flow of quests of the specified type
     */
    @Query("SELECT * FROM quests WHERE questType = :questType ORDER BY chapter ASC, questOrder ASC")
    fun getQuestsByType(questType: String): Flow<List<Quest>>
    
    /**
     * Gets quests by chapter
     * 
     * @param chapter Chapter number
     * @return Flow of quests in the specified chapter
     */
    @Query("SELECT * FROM quests WHERE chapter = :chapter ORDER BY questOrder ASC")
    fun getQuestsByChapter(chapter: String): Flow<List<Quest>>
    
    /**
     * Gets available quests (unlocked and not completed)
     * 
     * @return Flow of available quests
     */
    @Query("SELECT * FROM quests WHERE isUnlocked = 1 AND isCompleted = 0 ORDER BY chapter ASC, questOrder ASC")
    fun getAvailableQuests(): Flow<List<Quest>>
    
    /**
     * Gets completed quests
     * 
     * @return Flow of completed quests
     */
    @Query("SELECT * FROM quests WHERE isCompleted = 1 ORDER BY chapter ASC, questOrder ASC")
    fun getCompletedQuests(): Flow<List<Quest>>
    
    /**
     * Search quests by name
     * 
     * @param searchQuery Search query (partial name match)
     * @return Flow of quests matching the search query
     */
    @Query("SELECT * FROM quests WHERE name LIKE '%' || :searchQuery || '%' ORDER BY chapter ASC, questOrder ASC")
    fun searchQuestsByName(searchQuery: String): Flow<List<Quest>>
    
    /**
     * Gets quests by difficulty range
     * 
     * @param minDifficulty Minimum difficulty
     * @param maxDifficulty Maximum difficulty
     * @return Flow of quests within the difficulty range
     */
    @Query("SELECT * FROM quests WHERE difficulty BETWEEN :minDifficulty AND :maxDifficulty ORDER BY difficulty ASC")
    fun getQuestsByDifficultyRange(minDifficulty: Int, maxDifficulty: Int): Flow<List<Quest>>
    
    /**
     * Gets quests by AP cost range
     * 
     * @param minAp Minimum AP cost
     * @param maxAp Maximum AP cost
     * @return Flow of quests within the AP cost range
     */
    @Query("SELECT * FROM quests WHERE apCost BETWEEN :minAp AND :maxAp ORDER BY apCost ASC")
    fun getQuestsByApCostRange(minAp: Int, maxAp: Int): Flow<List<Quest>>
    
    /**
     * Gets quests with specific drops
     * 
     * @param dropItem Item to search for in drops
     * @return Flow of quests that drop the specified item
     */
    @Query("SELECT * FROM quests WHERE drops LIKE '%' || :dropItem || '%' ORDER BY chapter ASC, questOrder ASC")
    fun getQuestsWithDrop(dropItem: String): Flow<List<Quest>>
    
    /**
     * Gets farming quests (repeatable quests)
     * 
     * @return Flow of farming quests
     */
    @Query("SELECT * FROM quests WHERE isRepeatable = 1 ORDER BY apCost ASC")
    fun getFarmingQuests(): Flow<List<Quest>>
    
    // ==================== STATISTICS OPERATIONS ====================
    
    /**
     * Gets total quest count
     * 
     * @return Total number of quests
     */
    @Query("SELECT COUNT(*) FROM quests")
    suspend fun getTotalQuestCount(): Int
    
    /**
     * Gets completed quest count
     * 
     * @return Number of completed quests
     */
    @Query("SELECT COUNT(*) FROM quests WHERE isCompleted = 1")
    suspend fun getCompletedQuestCount(): Int
    
    /**
     * Gets quest count by type
     * 
     * @param questType Quest type
     * @return Number of quests of the specified type
     */
    @Query("SELECT COUNT(*) FROM quests WHERE questType = :questType")
    suspend fun getQuestCountByType(questType: String): Int
    
    /**
     * Gets quest count by chapter
     * 
     * @param chapter Chapter
     * @return Number of quests in the specified chapter
     */
    @Query("SELECT COUNT(*) FROM quests WHERE chapter = :chapter")
    suspend fun getQuestCountByChapter(chapter: String): Int
    
    /**
     * Gets average difficulty of completed quests
     * 
     * @return Average difficulty of completed quests
     */
    @Query("SELECT AVG(difficulty) FROM quests WHERE isCompleted = 1")
    suspend fun getAverageCompletedDifficulty(): Double?
    
    /**
     * Gets total AP spent on completed quests
     * 
     * @return Total AP spent
     */
    @Query("SELECT SUM(apCost * completionCount) FROM quests WHERE isCompleted = 1")
    suspend fun getTotalApSpent(): Int?
    
    /**
     * Gets most efficient farming quests (best drop rate per AP)
     * 
     * @param limit Number of quests to return
     * @return List of most efficient farming quests
     */
    @Query("""
        SELECT * FROM quests 
        WHERE isRepeatable = 1 AND apCost > 0
        ORDER BY (CAST(LENGTH(drops) - LENGTH(REPLACE(drops, ',', '')) AS REAL) / apCost) DESC 
        LIMIT :limit
    """)
    suspend fun getMostEfficientFarmingQuests(limit: Int = 10): List<Quest>
    
    /**
     * Gets most completed quests
     * 
     * @param limit Number of quests to return
     * @return List of most completed quests
     */
    @Query("SELECT * FROM quests ORDER BY completionCount DESC LIMIT :limit")
    suspend fun getMostCompletedQuests(limit: Int = 10): List<Quest>
    
    // ==================== UTILITY OPERATIONS ====================
    
    /**
     * Updates quest completion status
     * 
     * @param questId Quest ID
     * @param isCompleted Completion status
     * @param timestamp Update timestamp
     * @return Number of rows affected
     */
    @Query("""
        UPDATE quests 
        SET isCompleted = :isCompleted,
            completionCount = completionCount + CASE WHEN :isCompleted THEN 1 ELSE 0 END,
            lastCompleted = CASE WHEN :isCompleted THEN :timestamp ELSE lastCompleted END,
            lastUpdated = :timestamp
        WHERE id = :questId
    """)
    suspend fun updateQuestCompletion(
        questId: Int, 
        isCompleted: Boolean,
        timestamp: Long = System.currentTimeMillis()
    ): Int
    
    /**
     * Updates quest unlock status
     * 
     * @param questId Quest ID
     * @param isUnlocked Unlock status
     * @param timestamp Update timestamp
     * @return Number of rows affected
     */
    @Query("""
        UPDATE quests 
        SET isUnlocked = :isUnlocked,
            lastUpdated = :timestamp
        WHERE id = :questId
    """)
    suspend fun updateQuestUnlockStatus(
        questId: Int, 
        isUnlocked: Boolean,
        timestamp: Long = System.currentTimeMillis()
    ): Int
    
    /**
     * Increments quest completion count
     * 
     * @param questId Quest ID
     * @param timestamp Update timestamp
     * @return Number of rows affected
     */
    @Query("""
        UPDATE quests 
        SET completionCount = completionCount + 1,
            lastCompleted = :timestamp,
            lastUpdated = :timestamp
        WHERE id = :questId
    """)
    suspend fun incrementQuestCompletion(
        questId: Int,
        timestamp: Long = System.currentTimeMillis()
    ): Int
    
    /**
     * Updates quest drops information
     * 
     * @param questId Quest ID
     * @param drops New drops information
     * @param timestamp Update timestamp
     * @return Number of rows affected
     */
    @Query("""
        UPDATE quests 
        SET drops = :drops,
            lastUpdated = :timestamp
        WHERE id = :questId
    """)
    suspend fun updateQuestDrops(
        questId: Int, 
        drops: String,
        timestamp: Long = System.currentTimeMillis()
    ): Int
    
    /**
     * Checks if quest name exists
     * 
     * @param name Quest name to check
     * @param excludeId Quest ID to exclude from check (for updates)
     * @return True if name exists
     */
    @Query("SELECT COUNT(*) > 0 FROM quests WHERE name = :name AND (:excludeId IS NULL OR id != :excludeId)")
    suspend fun isQuestNameExists(name: String, excludeId: Int? = null): Boolean
    
    /**
     * Gets quests that need data update (older than specified time)
     * 
     * @param cutoffTime Timestamp cutoff for updates
     * @return List of quests needing update
     */
    @Query("SELECT * FROM quests WHERE lastUpdated < :cutoffTime")
    suspend fun getQuestsNeedingUpdate(cutoffTime: Long): List<Quest>
    
    /**
     * Gets quests by multiple criteria
     * 
     * @param questType Quest type filter (null for all)
     * @param chapter Chapter filter (null for all)
     * @param isCompleted Completion status filter (null for all)
     * @param isUnlocked Unlock status filter (null for all)
     * @return Flow of quests matching the criteria
     */
    @Query("""
        SELECT * FROM quests 
        WHERE (:questType IS NULL OR questType = :questType)
          AND (:chapter IS NULL OR chapter = :chapter)
          AND (:isCompleted IS NULL OR isCompleted = :isCompleted)
          AND (:isUnlocked IS NULL OR isUnlocked = :isUnlocked)
        ORDER BY chapter ASC, questOrder ASC
    """)
    fun getQuestsByCriteria(
        questType: String? = null,
        chapter: String? = null,
        isCompleted: Boolean? = null,
        isUnlocked: Boolean? = null
    ): Flow<List<Quest>>
} 