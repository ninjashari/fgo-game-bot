/*
 * FGO Bot - Battle Log Entity
 * 
 * This file defines the BattleLog entity for the Room database.
 * Represents a record of battle automation execution and results.
 */

package com.fgobot.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.fgobot.data.database.converters.StringListConverter

/**
 * Battle Log entity representing a battle automation execution record
 * 
 * @param id Unique battle log ID (auto-generated)
 * @param questId ID of the quest that was run
 * @param teamId ID of the team that was used
 * @param result Battle result (Victory, Defeat, Error, Cancelled)
 * @param duration Battle duration in seconds
 * @param turnsCompleted Number of turns completed
 * @param damageDealt Total damage dealt
 * @param damageTaken Total damage taken
 * @param skillsUsed List of skills that were used
 * @param npUsed List of Noble Phantasms that were used
 * @param drops List of items that dropped
 * @param bondGained Bond points gained
 * @param expGained Master experience gained
 * @param qpGained QP gained
 * @param apSpent AP spent on the battle
 * @param errorMessage Error message if battle failed
 * @param automationVersion Version of automation logic used
 * @param timestamp Timestamp when battle was executed
 */
@Entity(
    tableName = "battle_logs",
    foreignKeys = [
        ForeignKey(
            entity = Quest::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Team::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["questId"]),
        Index(value = ["teamId"]),
        Index(value = ["timestamp"]),
        Index(value = ["result"])
    ]
)
@TypeConverters(StringListConverter::class)
data class BattleLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questId: Int,
    val teamId: Long,
    val result: String, // Victory, Defeat, Error, Cancelled
    val duration: Long, // in seconds
    val turnsCompleted: Int,
    val damageDealt: Long,
    val damageTaken: Long,
    val skillsUsed: List<String>,
    val npUsed: List<String>,
    val drops: List<String>,
    val bondGained: Int,
    val expGained: Int,
    val qpGained: Int,
    val apSpent: Int,
    val errorMessage: String = "",
    val automationVersion: String,
    val timestamp: Long = System.currentTimeMillis()
) 