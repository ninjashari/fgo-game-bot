package com.fgobot.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servants")
data class ServantEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val className: String,
    val rarity: Int,
    val cost: Int,
    val atkBase: Int,
    val atkMax: Int,
    val hpBase: Int,
    val hpMax: Int,
    val attribute: String,
    val gender: String,
    val traits: String, // JSON string of traits
    val skills: String, // JSON string of skills
    val noblePhantasm: String, // JSON string of NP data
    val cardHits: String, // JSON string of card hit counts
    val starAbsorb: Int,
    val starGen: Float,
    val npCharge: String, // JSON string of NP charge rates
    val deathRate: Float,
    val criticalWeight: Int,
    val owned: Boolean = false,
    val level: Int = 1,
    val ascension: Int = 0,
    val skillLevels: String = "1,1,1", // Comma-separated skill levels
    val npLevel: Int = 1,
    val fouHp: Int = 0,
    val fouAtk: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) 