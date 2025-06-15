package com.fgobot.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "team_configs")
data class TeamConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val servant1Id: Int?,
    val servant1CeId: Int?,
    val servant2Id: Int?,
    val servant2CeId: Int?,
    val servant3Id: Int?,
    val servant3CeId: Int?,
    val supportServantClass: String?, // Preferred support class
    val supportCeId: Int?,
    val mysticCode: String,
    val questType: String?, // What type of quest this team is for
    val strategy: String, // JSON string of battle strategy
    val priority: Int = 0, // Higher priority teams are preferred
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) 