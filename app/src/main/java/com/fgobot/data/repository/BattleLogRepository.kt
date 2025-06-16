/*
 * FGO Bot - Battle Log Repository
 * 
 * This file defines the repository interface and implementation for BattleLog data management.
 * Handles battle logging, analytics, and performance tracking.
 */

package com.fgobot.data.repository

import com.fgobot.data.database.dao.BattleLogDao
import com.fgobot.data.database.entities.BattleLog
import com.fgobot.core.error.FGOBotException
import com.fgobot.core.logging.FGOLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Repository interface for BattleLog data operations
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
    val victories: Int,
    val winRate: Double,
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
                logger.e(TAG, "Error getting battle logs for quest: $questId", exception)
                throw FGOBotException.DatabaseException("Failed to get battle logs for quest", exception)
            }
    }
    
    override suspend fun recordBattleLog(battleLog: BattleLog): Result<Long> {
        return try {
            val battleLogId = battleLogDao.insertBattleLog(battleLog)
            logger.d(TAG, "Recorded new battle log with ID: $battleLogId")
            Result.success(battleLogId)
        } catch (exception: Exception) {
            logger.e(TAG, "Error recording battle log", exception)
            Result.failure(FGOBotException.DatabaseException("Failed to record battle log", exception))
        }
    }
    
    override suspend fun getBattleAnalytics(): BattleAnalytics {
        return try {
            val totalBattles = battleLogDao.getTotalBattleCount()
            val victories = battleLogDao.getVictoryCount()
            val winRate = battleLogDao.getWinRatePercentage()
            val averageDuration = battleLogDao.getAverageBattleDuration() ?: 0.0
            
            BattleAnalytics(
                totalBattles = totalBattles,
                victories = victories,
                winRate = winRate,
                averageDuration = averageDuration.toLong()
            )
            
        } catch (exception: Exception) {
            logger.e(TAG, "Error getting battle analytics", exception)
            throw FGOBotException.DatabaseException("Failed to get analytics", exception)
        }
    }
} 