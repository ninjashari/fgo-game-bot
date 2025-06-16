/*
 * FGO Bot - Servant Repository
 * 
 * This file defines the repository interface and implementation for Servant data management.
 * Handles data synchronization between API and local database, caching, and business logic.
 */

package com.fgobot.data.repository

import com.fgobot.data.api.AtlasAcademyApiService
import com.fgobot.data.database.dao.ServantDao
import com.fgobot.data.database.entities.Servant
import com.fgobot.data.mappers.toServantEntities
import com.fgobot.data.mappers.updateEntity
import com.fgobot.core.error.FGOBotException
import com.fgobot.core.logging.FGOLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch

/**
 * Repository interface for Servant data operations
 * 
 * Provides abstraction layer between data sources and business logic.
 * Handles data synchronization, caching, and error management.
 */
interface ServantRepository {
    
    /**
     * Gets all servants as Flow for reactive updates
     * 
     * @return Flow of servant list
     */
    fun getAllServants(): Flow<List<Servant>>
    
    /**
     * Gets a servant by ID
     * 
     * @param servantId Servant ID
     * @return Servant or null if not found
     */
    suspend fun getServantById(servantId: Int): Servant?
    
    /**
     * Gets servants by class
     * 
     * @param className Servant class
     * @return Flow of servants
     */
    fun getServantsByClass(className: String): Flow<List<Servant>>
    
    /**
     * Gets owned servants
     * 
     * @return Flow of owned servants
     */
    fun getOwnedServants(): Flow<List<Servant>>
    
    /**
     * Searches servants by name
     * 
     * @param query Search query
     * @return Flow of matching servants
     */
    fun searchServants(query: String): Flow<List<Servant>>
    
    /**
     * Synchronizes servant data from API
     * 
     * @param forceRefresh Force refresh even if data is recent
     * @return Success/failure result
     */
    suspend fun syncServantData(forceRefresh: Boolean = false): Result<Unit>
    
    /**
     * Updates servant ownership status
     * 
     * @param servantId Servant ID
     * @param isOwned Ownership status
     * @param currentLevel Current level
     * @param skillLevels Skill levels
     * @return Success/failure result
     */
    suspend fun updateServantOwnership(
        servantId: Int,
        isOwned: Boolean,
        currentLevel: Int = 1,
        skillLevels: List<Int> = listOf(1, 1, 1)
    ): Result<Unit>
    
    /**
     * Gets servant statistics
     * 
     * @return Repository statistics
     */
    suspend fun getServantStats(): ServantStats
}

/**
 * Data class for servant statistics
 */
data class ServantStats(
    val totalCount: Int,
    val ownedCount: Int,
    val classCount: Int,
    val averageOwnedRarity: Double,
    val lastSyncTime: Long
)

/**
 * Implementation of ServantRepository
 * 
 * Coordinates data access between API service and local database.
 * Implements caching, synchronization, and error handling strategies.
 */
class ServantRepositoryImpl(
    private val apiService: AtlasAcademyApiService,
    private val servantDao: ServantDao,
    private val logger: FGOLogger
) : ServantRepository {
    
    companion object {
        private const val SYNC_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val TAG = "ServantRepository"
    }
    
    private var lastSyncTime: Long = 0L
    
    override fun getAllServants(): Flow<List<Servant>> {
        return servantDao.getAllServantsFlow()
            .catch { exception ->
                logger.e(TAG, "Error getting all servants", exception)
                throw FGOBotException.DatabaseException("Failed to get servants", exception)
            }
    }
    
    override suspend fun getServantById(servantId: Int): Servant? {
        return try {
            servantDao.getServantById(servantId)
        } catch (exception: Exception) {
            logger.e(TAG, "Error getting servant by ID: $servantId", exception)
            throw FGOBotException.DatabaseException("Failed to get servant", exception)
        }
    }
    
    override fun getServantsByClass(className: String): Flow<List<Servant>> {
        return servantDao.getServantsByClass(className)
            .catch { exception ->
                logger.e(TAG, "Error getting servants by class: $className", exception)
                throw FGOBotException.DatabaseException("Failed to get servants by class", exception)
            }
    }
    
    override fun getOwnedServants(): Flow<List<Servant>> {
        return servantDao.getOwnedServants()
            .catch { exception ->
                logger.e(TAG, "Error getting owned servants", exception)
                throw FGOBotException.DatabaseException("Failed to get owned servants", exception)
            }
    }
    
    override fun searchServants(query: String): Flow<List<Servant>> {
        return servantDao.searchServantsByName(query)
            .catch { exception ->
                logger.e(TAG, "Error searching servants: $query", exception)
                throw FGOBotException.DatabaseException("Failed to search servants", exception)
            }
    }
    
    override suspend fun syncServantData(forceRefresh: Boolean): Result<Unit> {
        return try {
            val currentTime = System.currentTimeMillis()
            
            // Check if sync is needed
            if (!forceRefresh && (currentTime - lastSyncTime) < SYNC_INTERVAL_MS) {
                logger.d(TAG, "Skipping sync - data is recent")
                return Result.success(Unit)
            }
            
            logger.i(TAG, "Starting servant data synchronization")
            
            // Fetch data from API
            val response = apiService.getAllServants()
            if (!response.isSuccessful) {
                throw FGOBotException.NetworkException("API request failed: ${response.code()}")
            }
            
            val apiServants = response.body() ?: throw FGOBotException.NetworkException("Empty response body")
            logger.d(TAG, "Fetched ${apiServants.size} servants from API")
            
            // Get existing servants for ownership preservation
            val existingServants = servantDao.getAllServants()
            val ownedServantIds = existingServants
                .filter { it.isOwned }
                .map { it.id }
                .toSet()
            
            // Convert API data to entities
            val servantEntities = apiServants.toServantEntities(ownedServantIds)
            
            // Update database
            servantDao.insertServants(servantEntities)
            
            lastSyncTime = currentTime
            logger.i(TAG, "Servant data synchronization completed successfully")
            
            Result.success(Unit)
            
        } catch (exception: Exception) {
            logger.e(TAG, "Error during servant data synchronization", exception)
            when (exception) {
                is FGOBotException.NetworkException -> Result.failure(exception)
                is FGOBotException.DatabaseException -> Result.failure(exception)
                else -> Result.failure(FGOBotException.UnknownException("Sync failed", exception))
            }
        }
    }
    
    override suspend fun updateServantOwnership(
        servantId: Int,
        isOwned: Boolean,
        currentLevel: Int,
        skillLevels: List<Int>
    ): Result<Unit> {
        return try {
            val skillLevelsJson = skillLevels.joinToString(",", "[", "]")
            val rowsAffected = servantDao.updateServantOwnership(
                servantId = servantId,
                isOwned = isOwned,
                currentLevel = currentLevel,
                skillLevels = skillLevelsJson
            )
            
            if (rowsAffected > 0) {
                logger.d(TAG, "Updated servant ownership: $servantId -> $isOwned")
                Result.success(Unit)
            } else {
                logger.w(TAG, "No servant found with ID: $servantId")
                Result.failure(FGOBotException.DataNotFoundException("Servant not found"))
            }
            
        } catch (exception: Exception) {
            logger.e(TAG, "Error updating servant ownership: $servantId", exception)
            Result.failure(FGOBotException.DatabaseException("Failed to update ownership", exception))
        }
    }
    
    override suspend fun getServantStats(): ServantStats {
        return try {
            val totalCount = servantDao.getTotalServantCount()
            val ownedCount = servantDao.getOwnedServantCount()
            val classCount = servantDao.getDistinctClassCount()
            val averageRarity = servantDao.getAverageOwnedRarity() ?: 0.0
            
            ServantStats(
                totalCount = totalCount,
                ownedCount = ownedCount,
                classCount = classCount,
                averageOwnedRarity = averageRarity,
                lastSyncTime = lastSyncTime
            )
            
        } catch (exception: Exception) {
            logger.e(TAG, "Error getting servant statistics", exception)
            throw FGOBotException.DatabaseException("Failed to get statistics", exception)
        }
    }
} 