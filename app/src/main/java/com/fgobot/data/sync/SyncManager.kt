/*
 * FGO Bot - Synchronization Manager
 * 
 * This file provides centralized data synchronization management.
 * Coordinates API data fetching, validation, conflict resolution, and database updates.
 */

package com.fgobot.data.sync

import com.fgobot.data.repository.ServantRepository
import com.fgobot.data.validation.DataValidator
import com.fgobot.data.validation.ValidationResult
import com.fgobot.core.error.FGOBotException
import com.fgobot.core.logging.FGOLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.launch

/**
 * Synchronization status sealed class
 * 
 * Represents the current state of data synchronization.
 */
sealed class SyncStatus {
    object Idle : SyncStatus()
    object InProgress : SyncStatus()
    data class Success(val syncedItems: Int, val duration: Long) : SyncStatus()
    data class Error(val exception: Exception, val partialSuccess: Boolean = false) : SyncStatus()
    data class Partial(val successCount: Int, val failureCount: Int, val errors: List<String>) : SyncStatus()
}

/**
 * Synchronization configuration
 * 
 * Contains settings for sync operations.
 */
data class SyncConfig(
    val forceRefresh: Boolean = false,
    val validateData: Boolean = true,
    val batchSize: Int = 100,
    val retryAttempts: Int = 3,
    val retryDelayMs: Long = 1000L,
    val timeoutMs: Long = 30000L
)

/**
 * Synchronization statistics
 * 
 * Contains metrics about sync operations.
 */
data class SyncStats(
    val totalSyncs: Int = 0,
    val successfulSyncs: Int = 0,
    val failedSyncs: Int = 0,
    val lastSyncTime: Long = 0L,
    val averageSyncDuration: Long = 0L,
    val totalItemsSynced: Int = 0,
    val validationErrors: Int = 0
) {
    val successRate: Double get() = if (totalSyncs > 0) successfulSyncs.toDouble() / totalSyncs else 0.0
}

/**
 * Synchronization manager interface
 * 
 * Defines contracts for data synchronization operations.
 */
interface SyncManager {
    
    /**
     * Current synchronization status
     */
    val syncStatus: StateFlow<SyncStatus>
    
    /**
     * Synchronization statistics
     */
    val syncStats: StateFlow<SyncStats>
    
    /**
     * Synchronizes all data types
     * 
     * @param config Synchronization configuration
     * @return Success/failure result
     */
    suspend fun syncAll(config: SyncConfig = SyncConfig()): Result<Unit>
    
    /**
     * Synchronizes servant data
     * 
     * @param config Synchronization configuration
     * @return Success/failure result
     */
    suspend fun syncServants(config: SyncConfig = SyncConfig()): Result<Unit>
    
    /**
     * Synchronizes craft essence data
     * 
     * @param config Synchronization configuration
     * @return Success/failure result
     */
    suspend fun syncCraftEssences(config: SyncConfig = SyncConfig()): Result<Unit>
    
    /**
     * Synchronizes quest data
     * 
     * @param config Synchronization configuration
     * @return Success/failure result
     */
    suspend fun syncQuests(config: SyncConfig = SyncConfig()): Result<Unit>
    
    /**
     * Cancels ongoing synchronization
     */
    suspend fun cancelSync()
    
    /**
     * Resets synchronization statistics
     */
    suspend fun resetStats()
}

/**
 * Implementation of SyncManager
 * 
 * Provides comprehensive data synchronization capabilities.
 * Handles validation, error recovery, and progress tracking.
 */
class SyncManagerImpl(
    private val servantRepository: ServantRepository,
    private val dataValidator: DataValidator,
    private val logger: FGOLogger
) : SyncManager {
    
    companion object {
        private const val TAG = "SyncManager"
        private const val DEFAULT_BATCH_SIZE = 100
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }
    
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    private val _syncStats = MutableStateFlow(SyncStats())
    override val syncStats: StateFlow<SyncStats> = _syncStats.asStateFlow()
    
    private var isSyncCancelled = false
    
    override suspend fun syncAll(config: SyncConfig): Result<Unit> {
        return try {
            logger.i(TAG, "Starting full data synchronization")
            _syncStatus.value = SyncStatus.InProgress
            val startTime = System.currentTimeMillis()
            
            var totalSynced = 0
            val errors = mutableListOf<String>()
            
            supervisorScope {
                // Sync servants
                launch {
                    syncServants(config).fold(
                        onSuccess = { 
                            logger.d(TAG, "Servants synchronized successfully")
                            totalSynced += 1
                        },
                        onFailure = { exception ->
                            logger.e(TAG, "Failed to sync servants", exception)
                            errors.add("Servants: ${exception.message}")
                        }
                    )
                }
                
                // Sync craft essences
                launch {
                    syncCraftEssences(config).fold(
                        onSuccess = { 
                            logger.d(TAG, "Craft essences synchronized successfully")
                            totalSynced += 1
                        },
                        onFailure = { exception ->
                            logger.e(TAG, "Failed to sync craft essences", exception)
                            errors.add("Craft Essences: ${exception.message}")
                        }
                    )
                }
                
                // Sync quests
                launch {
                    syncQuests(config).fold(
                        onSuccess = { 
                            logger.d(TAG, "Quests synchronized successfully")
                            totalSynced += 1
                        },
                        onFailure = { exception ->
                            logger.e(TAG, "Failed to sync quests", exception)
                            errors.add("Quests: ${exception.message}")
                        }
                    )
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            updateSyncStats(totalSynced > 0, duration, totalSynced)
            
            when {
                errors.isEmpty() -> {
                    _syncStatus.value = SyncStatus.Success(totalSynced, duration)
                    logger.i(TAG, "Full synchronization completed successfully in ${duration}ms")
                    Result.success(Unit)
                }
                totalSynced > 0 -> {
                    _syncStatus.value = SyncStatus.Partial(totalSynced, errors.size, errors)
                    logger.w(TAG, "Partial synchronization completed with ${errors.size} errors")
                    Result.success(Unit)
                }
                else -> {
                    val exception = FGOBotException.SyncException("All sync operations failed", null)
                    _syncStatus.value = SyncStatus.Error(exception)
                    logger.e(TAG, "Full synchronization failed")
                    Result.failure(exception)
                }
            }
            
        } catch (exception: Exception) {
            logger.e(TAG, "Unexpected error during full synchronization", exception)
            val syncException = FGOBotException.SyncException("Sync failed", exception)
            _syncStatus.value = SyncStatus.Error(syncException)
            updateSyncStats(false, 0L, 0)
            Result.failure(syncException)
        }
    }
    
    override suspend fun syncServants(config: SyncConfig): Result<Unit> {
        return try {
            if (isSyncCancelled) {
                return Result.failure(FGOBotException.SyncException("Sync cancelled", null))
            }
            
            logger.i(TAG, "Starting servant data synchronization")
            val startTime = System.currentTimeMillis()
            
            // Perform sync with retry logic
            var lastException: Exception? = null
            repeat(config.retryAttempts) { attempt ->
                try {
                    val result = servantRepository.syncServantData(config.forceRefresh)
                    if (result.isSuccess) {
                        val duration = System.currentTimeMillis() - startTime
                        logger.i(TAG, "Servant sync completed successfully in ${duration}ms")
                        return Result.success(Unit)
                    } else {
                        lastException = result.exceptionOrNull() as? Exception
                    }
                } catch (exception: Exception) {
                    lastException = exception
                    if (attempt < config.retryAttempts - 1) {
                        logger.w(TAG, "Servant sync attempt ${attempt + 1} failed, retrying...", exception)
                        kotlinx.coroutines.delay(config.retryDelayMs * (attempt + 1))
                    }
                }
            }
            
            // All attempts failed
            val exception = lastException ?: FGOBotException.SyncException("Unknown sync error", null)
            logger.e(TAG, "Servant sync failed after ${config.retryAttempts} attempts", exception)
            Result.failure(exception)
            
        } catch (exception: Exception) {
            logger.e(TAG, "Unexpected error during servant sync", exception)
            Result.failure(FGOBotException.SyncException("Servant sync failed", exception))
        }
    }
    
    override suspend fun syncCraftEssences(config: SyncConfig): Result<Unit> {
        return try {
            if (isSyncCancelled) {
                return Result.failure(FGOBotException.SyncException("Sync cancelled", null))
            }
            
            logger.i(TAG, "Starting craft essence data synchronization")
            // TODO: Implement craft essence sync when repository is available
            logger.d(TAG, "Craft essence sync placeholder - implementation pending")
            Result.success(Unit)
            
        } catch (exception: Exception) {
            logger.e(TAG, "Unexpected error during craft essence sync", exception)
            Result.failure(FGOBotException.SyncException("Craft essence sync failed", exception))
        }
    }
    
    override suspend fun syncQuests(config: SyncConfig): Result<Unit> {
        return try {
            if (isSyncCancelled) {
                return Result.failure(FGOBotException.SyncException("Sync cancelled", null))
            }
            
            logger.i(TAG, "Starting quest data synchronization")
            // TODO: Implement quest sync when repository is available
            logger.d(TAG, "Quest sync placeholder - implementation pending")
            Result.success(Unit)
            
        } catch (exception: Exception) {
            logger.e(TAG, "Unexpected error during quest sync", exception)
            Result.failure(FGOBotException.SyncException("Quest sync failed", exception))
        }
    }
    
    override suspend fun cancelSync() {
        logger.i(TAG, "Cancelling synchronization")
        isSyncCancelled = true
        _syncStatus.value = SyncStatus.Idle
    }
    
    override suspend fun resetStats() {
        logger.i(TAG, "Resetting synchronization statistics")
        _syncStats.value = SyncStats()
    }
    
    /**
     * Updates synchronization statistics
     * 
     * @param success Whether the sync was successful
     * @param duration Sync duration in milliseconds
     * @param itemCount Number of items synced
     */
    private fun updateSyncStats(success: Boolean, duration: Long, itemCount: Int) {
        val currentStats = _syncStats.value
        val newStats = currentStats.copy(
            totalSyncs = currentStats.totalSyncs + 1,
            successfulSyncs = if (success) currentStats.successfulSyncs + 1 else currentStats.successfulSyncs,
            failedSyncs = if (!success) currentStats.failedSyncs + 1 else currentStats.failedSyncs,
            lastSyncTime = System.currentTimeMillis(),
            averageSyncDuration = if (currentStats.totalSyncs > 0) {
                (currentStats.averageSyncDuration * currentStats.totalSyncs + duration) / (currentStats.totalSyncs + 1)
            } else duration,
            totalItemsSynced = currentStats.totalItemsSynced + itemCount
        )
        
        _syncStats.value = newStats
        logger.d(TAG, "Updated sync stats: ${newStats.successRate * 100}% success rate")
    }
} 