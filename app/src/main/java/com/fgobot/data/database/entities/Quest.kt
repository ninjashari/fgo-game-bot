/*
 * FGO Bot - Quest Entity
 * 
 * This file defines the Quest entity for the Room database.
 * Represents a Fate/Grand Order quest with all relevant game data.
 */

package com.fgobot.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.fgobot.data.database.converters.StringListConverter

/**
 * Quest entity representing a Fate/Grand Order quest
 * 
 * @param id Unique quest ID from Atlas Academy API
 * @param name Quest name
 * @param type Quest type (Main, Free, Daily, Event, etc.)
 * @param chapter Chapter or section name
 * @param apCost AP cost to run the quest
 * @param bondPoints Bond points gained
 * @param experience Master experience gained
 * @param qp QP (money) gained
 * @param drops List of possible drop items
 * @param enemies List of enemy types in the quest
 * @param phases Number of battle phases
 * @param difficulty Difficulty rating (1-5)
 * @param isRepeatable Whether the quest can be repeated
 * @param isUnlocked Whether the quest is unlocked for the user
 * @param completionCount Number of times completed by user
 * @param bestTime Best completion time in seconds
 * @param lastCompleted Timestamp of last completion
 * @param lastUpdated Timestamp of last data update
 */
@Entity(tableName = "quests")
@TypeConverters(StringListConverter::class)
data class Quest(
    @PrimaryKey
    val id: Int,
    val name: String,
    val type: String,
    val chapter: String,
    val apCost: Int,
    val bondPoints: Int,
    val experience: Int,
    val qp: Int,
    val drops: List<String>,
    val enemies: List<String>,
    val phases: Int,
    val difficulty: Int,
    
    // User-specific data
    val isRepeatable: Boolean = true,
    val isUnlocked: Boolean = false,
    val completionCount: Int = 0,
    val bestTime: Long = 0L, // in seconds
    val lastCompleted: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
) 