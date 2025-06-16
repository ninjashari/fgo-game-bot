/*
 * FGO Bot - Servant Entity
 * 
 * This file defines the Servant entity for the Room database.
 * Represents a Fate/Grand Order servant with all relevant game data.
 */

package com.fgobot.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.fgobot.data.database.converters.StringListConverter

/**
 * Servant entity representing a Fate/Grand Order servant
 * 
 * @param id Unique servant ID from Atlas Academy API
 * @param collectionNo Collection number for sorting
 * @param name Servant's name
 * @param className Servant's class (Saber, Archer, etc.)
 * @param rarity Servant's star rating (1-5)
 * @param cost Team cost value
 * @param maxLevel Maximum level for this servant
 * @param atkBase Base attack stat
 * @param atkMax Maximum attack stat at max level
 * @param hpBase Base HP stat
 * @param hpMax Maximum HP stat at max level
 * @param skills List of skill names
 * @param noblePhantasm Noble Phantasm name
 * @param cardHits List of hit counts for each card type [Quick, Arts, Buster]
 * @param starAbsorb Star absorption rate
 * @param starGen Star generation rate
 * @param npCharge NP charge rate
 * @param critDamage Critical damage multiplier
 * @param classAttackRate Class attack rate modifier
 * @param classDefenseRate Class defense rate modifier
 * @param deathRate Death rate (instant kill resistance)
 * @param attribute Servant attribute (Man, Earth, Sky, Star, Beast)
 * @param traits List of servant traits
 * @param gender Servant gender
 * @param imageUrl URL to servant portrait image
 * @param isOwned Whether the user owns this servant
 * @param currentLevel Current level if owned
 * @param currentSkillLevels Current skill levels if owned [skill1, skill2, skill3]
 * @param lastUpdated Timestamp of last data update
 */
@Entity(tableName = "servants")
@TypeConverters(StringListConverter::class)
data class Servant(
    @PrimaryKey
    val id: Int,
    val collectionNo: Int,
    val name: String,
    val className: String,
    val rarity: Int,
    val cost: Int,
    val maxLevel: Int,
    val atkBase: Int,
    val atkMax: Int,
    val hpBase: Int,
    val hpMax: Int,
    val skills: List<String>,
    val noblePhantasm: String,
    val cardHits: List<Int>, // [Quick, Arts, Buster, Extra, NP]
    val starAbsorb: Int,
    val starGen: Double,
    val npCharge: Double,
    val critDamage: Double,
    val classAttackRate: Double,
    val classDefenseRate: Double,
    val deathRate: Double,
    val attribute: String,
    val traits: List<String>,
    val gender: String,
    val imageUrl: String,
    
    // User-specific data
    val isOwned: Boolean = false,
    val currentLevel: Int = 1,
    val currentSkillLevels: List<Int> = listOf(1, 1, 1),
    val lastUpdated: Long = System.currentTimeMillis()
) 