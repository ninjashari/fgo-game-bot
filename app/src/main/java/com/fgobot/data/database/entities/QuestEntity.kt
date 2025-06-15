package com.fgobot.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val type: String, // Main, Free, Event, etc.
    val chapter: String?,
    val spotName: String?,
    val recommendedLevel: Int,
    val bond: Int,
    val experience: Int,
    val qp: Int,
    val apCost: Int,
    val enemies: String, // JSON string of enemy data
    val drops: String, // JSON string of possible drops
    val phases: Int,
    val className: String?, // Recommended class
    val traits: String?, // JSON string of useful traits
    val difficulty: String, // Easy, Medium, Hard
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) 