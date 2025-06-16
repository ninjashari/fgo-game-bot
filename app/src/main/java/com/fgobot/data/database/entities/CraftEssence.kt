/*
 * FGO Bot - Craft Essence Entity
 * 
 * This file defines the Craft Essence entity for the Room database.
 * Represents a Fate/Grand Order craft essence with all relevant game data.
 */

package com.fgobot.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.fgobot.data.database.converters.StringListConverter

/**
 * Craft Essence entity representing a Fate/Grand Order craft essence
 * 
 * @param id Unique craft essence ID from Atlas Academy API
 * @param collectionNo Collection number for sorting
 * @param name Craft essence name
 * @param rarity Star rating (1-5)
 * @param cost Team cost value
 * @param maxLevel Maximum level for this craft essence
 * @param atkBase Base attack stat
 * @param atkMax Maximum attack stat at max level
 * @param hpBase Base HP stat
 * @param hpMax Maximum HP stat at max level
 * @param effects List of effect descriptions
 * @param skillIcon Skill icon identifier
 * @param imageUrl URL to craft essence image
 * @param isOwned Whether the user owns this craft essence
 * @param currentLevel Current level if owned
 * @param isMaxLimitBroken Whether the craft essence is max limit broken
 * @param lastUpdated Timestamp of last data update
 */
@Entity(tableName = "craft_essences")
@TypeConverters(StringListConverter::class)
data class CraftEssence(
    @PrimaryKey
    val id: Int,
    val collectionNo: Int,
    val name: String,
    val rarity: Int,
    val cost: Int,
    val maxLevel: Int,
    val atkBase: Int,
    val atkMax: Int,
    val hpBase: Int,
    val hpMax: Int,
    val effects: List<String>,
    val skillIcon: String,
    val imageUrl: String,
    
    // User-specific data
    val isOwned: Boolean = false,
    val currentLevel: Int = 1,
    val isMaxLimitBroken: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
) 