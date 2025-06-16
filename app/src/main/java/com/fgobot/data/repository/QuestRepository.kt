/*
 * FGO Bot - Quest Repository
 * 
 * This file defines the repository interface and implementation for Quest data management.
 * Handles data synchronization between API and local database, quest tracking, and analytics.
 */

package com.fgobot.data.repository

import com.fgobot.data.api.AtlasAcademyApiService
import com.fgobot.data.database.dao.QuestDao
import com.fgobot.data.database.entities.Quest
import com.fgobot.data.mappers.toQuestEntities
import com.fgobot.core.error.FGOBotException
import com.fgobot.core.logging.FGOLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Repository interface for Quest data operations
 * 
 * Provides abstraction layer between data sources and business logic.
 * Handles data synchronization, quest tracking, and analytics.
 */
interface QuestRepository {
    
    /**
     * Gets all quests as Flow for reactive updates
     * 
     * @return Flow of quest list
     */
    fun getAllQuests(): Flow<List<Quest>>
    
    /**
     * Gets a quest by ID
     * 
     * @param questId Quest ID
     * @return Quest or null if not found
     */
    suspend fun getQuestById(questId: Int): Quest?
    
    /**
     * Gets quests by type
     * 
     * @param questType Quest type (Main, Free, Daily, etc.)
     * @return Flow of quests
     */
    fun getQuestsByType(questType: String): Flow<List<Quest>>
    
    /**
     * Gets completed quests
     * 
     * @return Flow of completed quests
     */
    fun getCompletedQuests(): Flow<List<Quest>>
    
    /**
     * Gets available quests (not completed)
     * 
     * @return Flow of available quests
     */
    fun getAvailableQuests(): Flow<List<Quest>>
    
    /**
     * Synchronizes quest data from API
     * 
     * @param forceRefresh Force refresh even if data is recent
     * @return Success/failure result
     */
    suspend fun syncQuestData(forceRefresh: Boolean = false): Result<Unit>
    
    /**
     * Updates quest completion status
     * 
     * @param questId Quest ID
     * @param isCompleted Completion status
     * @return Success/failure result
     */
    suspend fun updateQuestCompletion(questId: Int, isCompleted: Boolean): Result<Unit>
    
    /**
     * Gets quest statistics
     * 
     * @return Repository statistics
     */
    suspend fun getQuestStats(): QuestStats
}

/**
 * Data class for quest statistics
 */
data class QuestStats(
    val totalCount: Int,
    val completedCount: Int,
    val lastSyncTime: Long
)

/**
 * Implementation of QuestRepository
 * 
 * Coordinates data access between API service and local database.
 * Implements caching, synchronization, and error handling strategies.
 */
class QuestRepositoryImpl(
    private val apiService: AtlasAcademyApiService,
    private val questDao: QuestDao,
    private val logger: FGOLogger
) : QuestRepository {
    
    companion object {
        private const val SYNC_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val TAG = "QuestRepository"
    }
    
    private var lastSyncTime: Long = 0L
    
    override fun getAllQuests(): Flow<List<Quest>> {
        return questDao.getAllQuestsFlow()
            .catch { exception ->
                logger.e(TAG, "Error getting all quests", exception)
                throw FGOBotException.DatabaseException("Failed to get quests", exception)
            }
    }
    
    override suspend fun getQuestById(questId: Int): Quest? {
        return try {
            questDao.getQuestById(questId)
        } catch (exception: Exception) {
            logger.e(TAG, "Error getting quest by ID: $questId", exception)
            throw FGOBotException.DatabaseException("Failed to get quest", exception)
        }
    }
    
    override fun getQuestsByType(questType: String): Flow<List<Quest>> {
        return questDao.getQuestsByType(questType)
            .catch { exception ->
                logger.e(TAG, "Error getting quests by type: $questType", exception)
                throw FGOBotException.DatabaseException("Failed to get quests by type", exception)
            }
    }
    
    override fun getCompletedQuests(): Flow<List<Quest>> {
        return questDao.getCompletedQuests()
            .catch { exception ->
                logger.e(TAG, "Error getting completed quests", exception)
                throw FGOBotException.DatabaseException("Failed to get completed quests", exception)
            }
    }
    
    override fun getAvailableQuests(): Flow<List<Quest>> {
        return questDao.getAvailableQuests()
            .catch { exception ->
                logger.e(TAG, "Error getting available quests", exception)
                throw FGOBotException.DatabaseException("Failed to get available quests", exception)
            }
    }
    
    override suspend fun syncQuestData(forceRefresh: Boolean): Result<Unit> {
        return try {
            val currentTime = System.currentTimeMillis()
            
            // Check if sync is needed
            if (!forceRefresh && (currentTime - lastSyncTime) < SYNC_INTERVAL_MS) {
                logger.d(TAG, "Skipping sync - data is recent")
                return Result.success(Unit)
            }
            
            logger.i(TAG, "Starting quest data synchronization")
            
            // Fetch data from API
            val response = apiService.getAllQuests()
            if (!response.isSuccessful) {
                throw FGOBotException.NetworkException("API request failed: ${response.code()}")
            }
            
            val apiQuests = response.body() ?: throw FGOBotException.NetworkException("Empty response body")
            logger.d(TAG, "Fetched ${apiQuests.size} quests from API")
            
            val questEntities = apiQuests.toQuestEntities()
            
            // Update database
            questDao.insertQuests(questEntities)
            
            lastSyncTime = currentTime
            logger.i(TAG, "Quest data synchronization completed successfully")
            
            Result.success(Unit)
            
        } catch (exception: Exception) {
            logger.e(TAG, "Error during quest data synchronization", exception)
            when (exception) {
                is FGOBotException.NetworkException -> Result.failure(exception)
                is FGOBotException.DatabaseException -> Result.failure(exception)
                else -> Result.failure(FGOBotException.UnknownException("Sync failed", exception))
            }
        }
    }
    
    override suspend fun updateQuestCompletion(questId: Int, isCompleted: Boolean): Result<Unit> {
        return try {
            // Since we changed the DAO to increment completion count, we only call it if isCompleted is true
            val rowsAffected = if (isCompleted) {
                questDao.updateQuestCompletion(questId)
            } else {
                // For now, we don't support decrementing completion count
                // This could be enhanced later if needed
                logger.w(TAG, "Decrementing quest completion not supported for quest: $questId")
                0
            }
            
            if (rowsAffected > 0) {
                logger.d(TAG, "Updated quest completion: $questId -> $isCompleted")
                Result.success(Unit)
            } else {
                logger.w(TAG, "No quest found with ID: $questId or completion not updated")
                Result.failure(FGOBotException.DataNotFoundException("Quest not found or not updated"))
            }
            
        } catch (exception: Exception) {
            logger.e(TAG, "Error updating quest completion: $questId", exception)
            Result.failure(FGOBotException.DatabaseException("Failed to update completion", exception))
        }
    }
    
    override suspend fun getQuestStats(): QuestStats {
        return try {
            val totalCount = questDao.getTotalQuestCount()
            val completedCount = questDao.getCompletedQuestCount()
            
            QuestStats(
                totalCount = totalCount,
                completedCount = completedCount,
                lastSyncTime = lastSyncTime
            )
            
        } catch (exception: Exception) {
            logger.e(TAG, "Error getting quest statistics", exception)
            throw FGOBotException.DatabaseException("Failed to get statistics", exception)
        }
    }
} 