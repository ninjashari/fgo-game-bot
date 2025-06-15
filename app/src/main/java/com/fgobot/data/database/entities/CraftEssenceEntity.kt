package com.fgobot.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "craft_essences")
data class CraftEssenceEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val rarity: Int,
    val cost: Int,
    val atkBase: Int,
    val atkMax: Int,
    val hpBase: Int,
    val hpMax: Int,
    val effects: String, // JSON string of effects
    val skillIcon: String?,
    val bondEquipOwner: Int?, // Servant ID if bond CE
    val valentineEquipOwner: Int?, // Servant ID if valentine CE
    val owned: Boolean = false,
    val level: Int = 1,
    val limitBreak: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) 