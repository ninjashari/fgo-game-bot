/*
 * FGO Bot - Team Entity
 * 
 * This file defines the Team entity for the Room database.
 * Represents a team composition for battle automation.
 */

package com.fgobot.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.fgobot.data.database.converters.IntListConverter
import com.fgobot.data.database.converters.StringListConverter

/**
 * Team entity representing a battle team composition
 * 
 * @param id Unique team ID (auto-generated)
 * @param name User-defined team name
 * @param description Team description or notes
 * @param servantIds List of servant IDs in the team (6 slots: main 3 + sub 3)
 * @param craftEssenceIds List of craft essence IDs equipped (6 slots)
 * @param supportServantClass Preferred support servant class
 * @param supportCraftEssence Preferred support craft essence
 * @param strategy Battle strategy identifier
 * @param skillPriority Skill usage priority order
 * @param npPriority Noble Phantasm usage priority
 * @param isDefault Whether this is the default team
 * @param usageCount Number of times this team has been used
 * @param winRate Win rate percentage (0-100)
 * @param averageTime Average battle completion time in seconds
 * @param createdAt Timestamp when team was created
 * @param lastUsed Timestamp when team was last used
 * @param lastUpdated Timestamp of last modification
 */
@Entity(tableName = "teams")
@TypeConverters(IntListConverter::class, StringListConverter::class)
data class Team(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val servantIds: List<Int>, // 6 slots: [main1, main2, main3, sub1, sub2, sub3]
    val craftEssenceIds: List<Int>, // 6 slots corresponding to servants
    val supportServantClass: String = "Any",
    val supportCraftEssence: String = "Any",
    val strategy: String = "Auto",
    val skillPriority: List<String> = emptyList(),
    val npPriority: List<String> = emptyList(),
    
    // Usage statistics
    val isDefault: Boolean = false,
    val usageCount: Int = 0,
    val winRate: Double = 0.0,
    val averageTime: Long = 0L, // in seconds
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
) 