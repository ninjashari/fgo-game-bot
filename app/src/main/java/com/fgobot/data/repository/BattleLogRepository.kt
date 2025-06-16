/*
 * FGO Bot - Battle Log Repository
 * 
 * This file defines the repository interface and implementation for Battle Log data management.
 * Handles battle analytics, performance tracking, and data aggregation.
 */

package com.fgobot.data.repository

import com.fgobot.data.database.dao.BattleLogDao
import com.fgobot.data.database.entities.BattleLog
import com.fgobot.core.error.FGOBotException
import com.fgobot.core.logging.FGOLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Repository interface for Battle Log data operations
 */
interface BattleLogRepository {
    fun getAllBattleLogs(): Flow<List<BattleLog>>
    suspend fun getBattleLogById(battleLogId: Long): BattleLog?
    fun getBattleLogsByQuest(questId: Int): Flow<List<BattleLog>>
    suspend fun recordBattleLog(battleLog: BattleLog): Result<Long>
    suspend fun getBattleAnalytics(): BattleAnalytics
}

/**
 * Battle analytics data class
 */
data class BattleAnalytics(
    val totalBattles: Int,
    val successfulBattles: Int,
    val successRate: Double,
    val averageDuration: Long
)

/**
 * Battle log repository implementation
 */
class BattleLogRepositoryImpl(
    private val battleLogDao: BattleLogDao,
    private val logger: FGOLogger
) : BattleLogRepository {
    
    companion object {
        private const val TAG = "BattleLogRepository"
    }
    
    override fun getAllBattleLogs(): Flow<List<BattleLog>> {
        return battleLogDao.getAllBattleLogsFlow()
            .catch { exception ->
                logger.e(TAG, "Error getting all battle logs", exception)
                throw FGOBotException.DatabaseException("Failed to get battle logs", exception)
            }
    }
    
    override suspend fun getBattleLogById(battleLogId: Long): BattleLog? {
        return try {
            battleLogDao.getBattleLogById(battleLogId)
        } catch (exception: Exception) {
            logger.e(TAG, "Error getting battle log by ID: $battleLogId", exception)
            throw FGOBotException.DatabaseException("Failed to get battle log", exception)
        }
    }
    
    override fun getBattleLogsByQuest(questId: Int): Flow<List<BattleLog>> {
        return battleLogDao.getBattleLogsByQuest(questId)
            .catch { exception ->
                logger.e(TAG, "Error getting battle logs by quest: $questId", exception)
                throw FGOBotException.DatabaseException("Failed to get battle logs by quest", exception)
            }
    }
    
    override suspend fun recordBattleLog(battleLog: BattleLog): Result<Long> {
        return try {
            val battleLogId = battleLogDao.insertBattleLog(battleLog)
            logger.d(TAG, "Recorded battle log: Quest ${battleLog.questId}, Result: ${battleLog.result}")
            Result.success(battleLogId)
        } catch (exception: Exception) {
            logger.e(TAG, "Error recording battle log", exception)
            Result.failure(FGOBotException.DatabaseException("Failed to record battle log", exception))
        }
    }
    
    override suspend fun getBattleAnalytics(): BattleAnalytics {
        return try {
            val totalBattles = battleLogDao.getTotalBattleCount()
            val successfulBattles = battleLogDao.getVictoryCount()
            val successRate = if (totalBattles > 0) successfulBattles.toDouble() / totalBattles * 100 else 0.0
            val averageDuration = (battleLogDao.getAverageBattleDuration() ?: 0.0).toLong()
            
            BattleAnalytics(
                totalBattles = totalBattles,
                successfulBattles = successfulBattles,
                successRate = successRate,
                averageDuration = averageDuration
            )
            
        } catch (exception: Exception) {
            logger.e(TAG, "Error getting battle analytics", exception)
            throw FGOBotException.DatabaseException("Failed to get battle analytics", exception)
        }
    }
} 