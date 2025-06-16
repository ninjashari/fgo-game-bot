/*
 * FGO Bot - Craft Essence Repository
 * 
 * This file defines the repository interface and implementation for Craft Essence data management.
 * Handles data synchronization between API and local database, caching, and business logic.
 */

package com.fgobot.data.repository

import com.fgobot.data.api.AtlasAcademyApiService
import com.fgobot.data.database.dao.CraftEssenceDao
import com.fgobot.data.database.entities.CraftEssence
import com.fgobot.data.mappers.toCraftEssenceEntities
import com.fgobot.core.error.FGOBotException
import com.fgobot.core.logging.FGOLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Repository interface for Craft Essence data operations
 * 
 * Provides abstraction layer between data sources and business logic.
 * Handles data synchronization, caching, and error management.
 */
interface CraftEssenceRepository {
    
    /**
     * Gets all craft essences as Flow for reactive updates
     * 
     * @return Flow of craft essence list
     */
    fun getAllCraftEssences(): Flow<List<CraftEssence>>
    
    /**
     * Gets a craft essence by ID
     * 
     * @param craftEssenceId Craft essence ID
     * @return Craft essence or null if not found
     */
    suspend fun getCraftEssenceById(craftEssenceId: Int): CraftEssence?
    
    /**
     * Gets craft essences by rarity
     * 
     * @param rarity Star rating
     * @return Flow of craft essences
     */
    fun getCraftEssencesByRarity(rarity: Int): Flow<List<CraftEssence>>
    
    /**
     * Gets owned craft essences
     * 
     * @return Flow of owned craft essences
     */
    fun getOwnedCraftEssences(): Flow<List<CraftEssence>>
    
    /**
     * Searches craft essences by name
     * 
     * @param query Search query
     * @return Flow of matching craft essences
     */
    fun searchCraftEssences(query: String): Flow<List<CraftEssence>>
    
    /**
     * Gets craft essences by effect type
     * 
     * @param effectType Effect type
     * @return Flow of craft essences with specified effect
     */
    fun getCraftEssencesByEffect(effectType: String): Flow<List<CraftEssence>>
    
    /**
     * Synchronizes craft essence data from API
     * 
     * @param forceRefresh Force refresh even if data is recent
     * @return Success/failure result
     */
    suspend fun syncCraftEssenceData(forceRefresh: Boolean = false): Result<Unit>
    
    /**
     * Updates craft essence ownership status
     * 
     * @param craftEssenceId Craft essence ID
     * @param isOwned Ownership status
     * @param currentLevel Current level
     * @param limitBreakLevel Limit break level
     * @return Success/failure result
     */
    suspend fun updateCraftEssenceOwnership(
        craftEssenceId: Int,
        isOwned: Boolean,
        currentLevel: Int = 1,
        limitBreakLevel: Int = 0
    ): Result<Unit>
    
    /**
     * Gets craft essence statistics
     * 
     * @return Repository statistics
     */
    suspend fun getCraftEssenceStats(): CraftEssenceStats
}

/**
 * Data class for craft essence statistics
 */
data class CraftEssenceStats(
    val totalCount: Int,
    val ownedCount: Int,
    val rarityDistribution: Map<Int, Int>,
    val averageOwnedRarity: Double,
    val maxLevelCount: Int,
    val limitBreakCount: Int,
    val lastSyncTime: Long
)

/**
 * Implementation of CraftEssenceRepository
 * 
 * Coordinates data access between API service and local database.
 * Implements caching, synchronization, and error handling strategies.
 */
class CraftEssenceRepositoryImpl(
    private val apiService: AtlasAcademyApiService,
    private val craftEssenceDao: CraftEssenceDao,
    private val logger: FGOLogger
) : CraftEssenceRepository {
    
    companion object {
        private const val SYNC_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val TAG = "CraftEssenceRepository"
    }
    
    private var lastSyncTime: Long = 0L
    
    override fun getAllCraftEssences(): Flow<List<CraftEssence>> {
        return craftEssenceDao.getAllCraftEssencesFlow()
            .catch { exception ->
                logger.e(TAG, "Error getting all craft essences", exception)
                throw FGOBotException.DatabaseException("Failed to get craft essences", exception)
            }
    }
    
    override suspend fun getCraftEssenceById(craftEssenceId: Int): CraftEssence? {
        return try {
            craftEssenceDao.getCraftEssenceById(craftEssenceId)
        } catch (exception: Exception) {
            logger.e(TAG, "Error getting craft essence by ID: $craftEssenceId", exception)
            throw FGOBotException.DatabaseException("Failed to get craft essence", exception)
        }
    }
    
    override fun getCraftEssencesByRarity(rarity: Int): Flow<List<CraftEssence>> {
        return craftEssenceDao.getCraftEssencesByRarity(rarity)
            .catch { exception ->
                logger.e(TAG, "Error getting craft essences by rarity: $rarity", exception)
                throw FGOBotException.DatabaseException("Failed to get craft essences by rarity", exception)
            }
    }
    
    override fun getOwnedCraftEssences(): Flow<List<CraftEssence>> {
        return craftEssenceDao.getOwnedCraftEssences()
            .catch { exception ->
                logger.e(TAG, "Error getting owned craft essences", exception)
                throw FGOBotException.DatabaseException("Failed to get owned craft essences", exception)
            }
    }
    
    override fun searchCraftEssences(query: String): Flow<List<CraftEssence>> {
        return craftEssenceDao.searchCraftEssencesByName(query)
            .catch { exception ->
                logger.e(TAG, "Error searching craft essences: $query", exception)
                throw FGOBotException.DatabaseException("Failed to search craft essences", exception)
            }
    }
    
    override fun getCraftEssencesByEffect(effectType: String): Flow<List<CraftEssence>> {
        return craftEssenceDao.getCraftEssencesByEffect(effectType)
            .catch { exception ->
                logger.e(TAG, "Error getting craft essences by effect: $effectType", exception)
                throw FGOBotException.DatabaseException("Failed to get craft essences by effect", exception)
            }
    }
    
    override suspend fun syncCraftEssenceData(forceRefresh: Boolean): Result<Unit> {
        return try {
            val currentTime = System.currentTimeMillis()
            
            // Check if sync is needed
            if (!forceRefresh && (currentTime - lastSyncTime) < SYNC_INTERVAL_MS) {
                logger.d(TAG, "Skipping sync - data is recent")
                return Result.success(Unit)
            }
            
            logger.i(TAG, "Starting craft essence data synchronization")
            
            // Fetch data from API
            val response = apiService.getAllCraftEssences()
            if (!response.isSuccessful) {
                throw FGOBotException.NetworkException("API request failed: ${response.code()}")
            }
            
            val apiCraftEssences = response.body() ?: throw FGOBotException.NetworkException("Empty response body")
            logger.d(TAG, "Fetched ${apiCraftEssences.size} craft essences from API")
            
            // Get existing craft essences for ownership preservation
            val existingCraftEssences = craftEssenceDao.getAllCraftEssences()
            val ownedCraftEssenceIds = existingCraftEssences
                .filter { it.isOwned }
                .map { it.id }
                .toSet()
            
            // Convert API data to entities
            val craftEssenceEntities = apiCraftEssences.toCraftEssenceEntities(ownedCraftEssenceIds)
            
            // Update database
            craftEssenceDao.insertCraftEssences(craftEssenceEntities)
            
            lastSyncTime = currentTime
            logger.i(TAG, "Craft essence data synchronization completed successfully")
            
            Result.success(Unit)
            
        } catch (exception: Exception) {
            logger.e(TAG, "Error during craft essence data synchronization", exception)
            when (exception) {
                is FGOBotException.NetworkException -> Result.failure(exception)
                is FGOBotException.DatabaseException -> Result.failure(exception)
                else -> Result.failure(FGOBotException.UnknownException("Sync failed", exception))
            }
        }
    }
    
    override suspend fun updateCraftEssenceOwnership(
        craftEssenceId: Int,
        isOwned: Boolean,
        currentLevel: Int,
        limitBreakLevel: Int
    ): Result<Unit> {
        return try {
            val rowsAffected = craftEssenceDao.updateCraftEssenceOwnership(
                craftEssenceId = craftEssenceId,
                isOwned = isOwned,
                currentLevel = currentLevel
            )
            
            if (rowsAffected > 0) {
                logger.d(TAG, "Updated craft essence ownership: $craftEssenceId -> $isOwned")
                Result.success(Unit)
            } else {
                logger.w(TAG, "No craft essence found with ID: $craftEssenceId")
                Result.failure(FGOBotException.DataNotFoundException("Craft essence not found"))
            }
            
        } catch (exception: Exception) {
            logger.e(TAG, "Error updating craft essence ownership: $craftEssenceId", exception)
            Result.failure(FGOBotException.DatabaseException("Failed to update ownership", exception))
        }
    }
    
    override suspend fun getCraftEssenceStats(): CraftEssenceStats {
        return try {
            val totalCount = craftEssenceDao.getTotalCraftEssenceCount()
            val ownedCount = craftEssenceDao.getOwnedCraftEssenceCount()
            val averageRarity = craftEssenceDao.getAverageOwnedRarity() ?: 0.0
            
            // Get rarity distribution
            val rarityDistribution = mutableMapOf<Int, Int>()
            for (rarity in 1..5) {
                val count = craftEssenceDao.getCraftEssenceCountByRarity(rarity)
                if (count > 0) {
                    rarityDistribution[rarity] = count
                }
            }
            
            CraftEssenceStats(
                totalCount = totalCount,
                ownedCount = ownedCount,
                rarityDistribution = rarityDistribution,
                averageOwnedRarity = averageRarity,
                maxLevelCount = 0,
                limitBreakCount = 0,
                lastSyncTime = lastSyncTime
            )
            
        } catch (exception: Exception) {
            logger.e(TAG, "Error getting craft essence statistics", exception)
            throw FGOBotException.DatabaseException("Failed to get statistics", exception)
        }
    }
} 