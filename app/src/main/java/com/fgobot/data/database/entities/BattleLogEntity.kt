package com.fgobot.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battle_logs")
data class BattleLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questId: Int,
    val questName: String,
    val teamConfigId: Long?,
    val result: String, // WIN, LOSS, RETREAT
    val turns: Int,
    val duration: Long, // Battle duration in milliseconds
    val apUsed: Int,
    val bondGained: Int,
    val expGained: Int,
    val qpGained: Int,
    val drops: String, // JSON string of items dropped
    val errors: String?, // JSON string of any errors encountered
    val strategy: String?, // JSON string of strategy used
    val performance: Float, // Performance score 0-1
    val createdAt: Long = System.currentTimeMillis()
) 