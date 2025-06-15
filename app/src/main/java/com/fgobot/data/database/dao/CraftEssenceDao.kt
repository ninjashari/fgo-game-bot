package com.fgobot.data.database.dao

import androidx.room.*
import com.fgobot.data.database.entities.CraftEssenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CraftEssenceDao {
    
    @Query("SELECT * FROM craft_essences")
    fun getAllCraftEssences(): Flow<List<CraftEssenceEntity>>
    
    @Query("SELECT * FROM craft_essences WHERE owned = 1")
    fun getOwnedCraftEssences(): Flow<List<CraftEssenceEntity>>
    
    @Query("SELECT * FROM craft_essences WHERE id = :id")
    suspend fun getCraftEssenceById(id: Int): CraftEssenceEntity?
    
    @Query("SELECT * FROM craft_essences WHERE rarity = :rarity")
    fun getCraftEssencesByRarity(rarity: Int): Flow<List<CraftEssenceEntity>>
    
    @Query("SELECT * FROM craft_essences WHERE name LIKE '%' || :name || '%'")
    fun searchCraftEssencesByName(name: String): Flow<List<CraftEssenceEntity>>
    
    @Query("SELECT * FROM craft_essences WHERE bondEquipOwner = :servantId")
    suspend fun getBondCraftEssence(servantId: Int): CraftEssenceEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCraftEssence(craftEssence: CraftEssenceEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCraftEssences(craftEssences: List<CraftEssenceEntity>)
    
    @Update
    suspend fun updateCraftEssence(craftEssence: CraftEssenceEntity)
    
    @Query("UPDATE craft_essences SET owned = :owned WHERE id = :id")
    suspend fun updateCraftEssenceOwnership(id: Int, owned: Boolean)
    
    @Query("UPDATE craft_essences SET level = :level, limitBreak = :limitBreak WHERE id = :id")
    suspend fun updateCraftEssenceLevel(id: Int, level: Int, limitBreak: Int)
    
    @Delete
    suspend fun deleteCraftEssence(craftEssence: CraftEssenceEntity)
    
    @Query("DELETE FROM craft_essences")
    suspend fun deleteAllCraftEssences()
} 