/*
 * FGO Bot - Craft Essence Data Access Object
 * 
 * This file defines the DAO interface for CraftEssence entity operations.
 * Provides methods for CRUD operations, queries, and craft essence management.
 */

package com.fgobot.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.fgobot.data.database.entities.CraftEssence

/**
 * Data Access Object for CraftEssence entity
 * 
 * Provides methods for craft essence database operations including:
 * - Basic CRUD operations
 * - Craft essence queries and filtering
 * - Rarity and type-based searches
 * - Usage statistics and analytics
 */
@Dao
interface CraftEssenceDao {
    
    // ==================== INSERT OPERATIONS ====================
    
    /**
     * Inserts a new craft essence into the database
     * 
     * @param craftEssence CraftEssence to insert
     * @return ID of the inserted craft essence
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCraftEssence(craftEssence: CraftEssence): Long
    
    /**
     * Inserts multiple craft essences into the database
     * 
     * @param craftEssences List of craft essences to insert
     * @return List of inserted craft essence IDs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCraftEssences(craftEssences: List<CraftEssence>): List<Long>
    
    // ==================== UPDATE OPERATIONS ====================
    
    /**
     * Updates an existing craft essence
     * 
     * @param craftEssence CraftEssence to update
     * @return Number of rows affected
     */
    @Update
    suspend fun updateCraftEssence(craftEssence: CraftEssence): Int
    
    /**
     * Updates multiple craft essences
     * 
     * @param craftEssences List of craft essences to update
     * @return Number of rows affected
     */
    @Update
    suspend fun updateCraftEssences(craftEssences: List<CraftEssence>): Int
    
    // ==================== DELETE OPERATIONS ====================
    
    /**
     * Deletes a craft essence from the database
     * 
     * @param craftEssence CraftEssence to delete
     * @return Number of rows affected
     */
    @Delete
    suspend fun deleteCraftEssence(craftEssence: CraftEssence): Int
    
    /**
     * Deletes a craft essence by ID
     * 
     * @param craftEssenceId ID of craft essence to delete
     * @return Number of rows affected
     */
    @Query("DELETE FROM craft_essences WHERE id = :craftEssenceId")
    suspend fun deleteCraftEssenceById(craftEssenceId: Int): Int
    
    /**
     * Deletes all craft essences
     * 
     * @return Number of rows affected
     */
    @Query("DELETE FROM craft_essences")
    suspend fun deleteAllCraftEssences(): Int
    
    // ==================== QUERY OPERATIONS ====================
    
    /**
     * Gets a craft essence by ID
     * 
     * @param craftEssenceId CraftEssence ID
     * @return CraftEssence or null if not found
     */
    @Query("SELECT * FROM craft_essences WHERE id = :craftEssenceId")
    suspend fun getCraftEssenceById(craftEssenceId: Int): CraftEssence?
    
    /**
     * Gets a craft essence by ID as Flow
     * 
     * @param craftEssenceId CraftEssence ID
     * @return Flow of CraftEssence or null
     */
    @Query("SELECT * FROM craft_essences WHERE id = :craftEssenceId")
    fun getCraftEssenceByIdFlow(craftEssenceId: Int): Flow<CraftEssence?>
    
    /**
     * Gets all craft essences
     * 
     * @return List of all craft essences
     */
    @Query("SELECT * FROM craft_essences ORDER BY collectionNo ASC")
    suspend fun getAllCraftEssences(): List<CraftEssence>
    
    /**
     * Gets all craft essences as Flow
     * 
     * @return Flow of all craft essences
     */
    @Query("SELECT * FROM craft_essences ORDER BY collectionNo ASC")
    fun getAllCraftEssencesFlow(): Flow<List<CraftEssence>>
    
    /**
     * Gets craft essences by rarity
     * 
     * @param rarity Star rating (1-5)
     * @return Flow of craft essences of the specified rarity
     */
    @Query("SELECT * FROM craft_essences WHERE rarity = :rarity ORDER BY collectionNo ASC")
    fun getCraftEssencesByRarity(rarity: Int): Flow<List<CraftEssence>>
    
    /**
     * Gets owned craft essences only
     * 
     * @return Flow of craft essences owned by the user
     */
    @Query("SELECT * FROM craft_essences WHERE isOwned = 1 ORDER BY collectionNo ASC")
    fun getOwnedCraftEssences(): Flow<List<CraftEssence>>
    
    /**
     * Search craft essences by name
     * 
     * @param searchQuery Search query (partial name match)
     * @return Flow of craft essences matching the search query
     */
    @Query("SELECT * FROM craft_essences WHERE name LIKE '%' || :searchQuery || '%' ORDER BY collectionNo ASC")
    fun searchCraftEssencesByName(searchQuery: String): Flow<List<CraftEssence>>
    
    /**
     * Gets craft essences with specific effects
     * 
     * @param effect Effect to search for
     * @return Flow of craft essences with the specified effect
     */
    @Query("SELECT * FROM craft_essences WHERE effects LIKE '%' || :effect || '%' ORDER BY collectionNo ASC")
    fun getCraftEssencesByEffect(effect: String): Flow<List<CraftEssence>>
    
    /**
     * Gets craft essences by rarity range
     * 
     * @param minRarity Minimum rarity
     * @param maxRarity Maximum rarity
     * @return Flow of craft essences within the rarity range
     */
    @Query("SELECT * FROM craft_essences WHERE rarity BETWEEN :minRarity AND :maxRarity ORDER BY rarity DESC, collectionNo ASC")
    fun getCraftEssencesByRarityRange(minRarity: Int, maxRarity: Int): Flow<List<CraftEssence>>
    
    /**
     * Gets craft essences by ATK/HP stats
     * 
     * @param minAtk Minimum ATK value
     * @param minHp Minimum HP value
     * @return Flow of craft essences meeting stat requirements
     */
    @Query("SELECT * FROM craft_essences WHERE atkMax >= :minAtk AND hpMax >= :minHp ORDER BY (atkMax + hpMax) DESC")
    fun getCraftEssencesByStats(minAtk: Int, minHp: Int): Flow<List<CraftEssence>>
    
    // ==================== STATISTICS OPERATIONS ====================
    
    /**
     * Gets total craft essence count
     * 
     * @return Total number of craft essences
     */
    @Query("SELECT COUNT(*) FROM craft_essences")
    suspend fun getTotalCraftEssenceCount(): Int
    
    /**
     * Gets owned craft essence count
     * 
     * @return Number of owned craft essences
     */
    @Query("SELECT COUNT(*) FROM craft_essences WHERE isOwned = 1")
    suspend fun getOwnedCraftEssenceCount(): Int
    
    /**
     * Gets craft essence count by rarity
     * 
     * @param rarity Rarity level
     * @return Number of craft essences of the specified rarity
     */
    @Query("SELECT COUNT(*) FROM craft_essences WHERE rarity = :rarity")
    suspend fun getCraftEssenceCountByRarity(rarity: Int): Int
    
    /**
     * Gets average rarity of owned craft essences
     * 
     * @return Average rarity of owned craft essences
     */
    @Query("SELECT AVG(rarity) FROM craft_essences WHERE isOwned = 1")
    suspend fun getAverageOwnedRarity(): Double?
    
    /**
     * Gets highest ATK craft essences
     * 
     * @param limit Number of craft essences to return
     * @return List of highest ATK craft essences
     */
    @Query("SELECT * FROM craft_essences ORDER BY atkMax DESC LIMIT :limit")
    suspend fun getHighestAtkCraftEssences(limit: Int = 10): List<CraftEssence>
    
    /**
     * Gets highest HP craft essences
     * 
     * @param limit Number of craft essences to return
     * @return List of highest HP craft essences
     */
    @Query("SELECT * FROM craft_essences ORDER BY hpMax DESC LIMIT :limit")
    suspend fun getHighestHpCraftEssences(limit: Int = 10): List<CraftEssence>
    
    // ==================== UTILITY OPERATIONS ====================
    
    /**
     * Updates craft essence ownership status
     * 
     * @param craftEssenceId CraftEssence ID
     * @param isOwned New ownership status
     * @param currentLevel Current level if owned
     * @param timestamp Update timestamp
     * @return Number of rows affected
     */
    @Query("""
        UPDATE craft_essences 
        SET isOwned = :isOwned, 
            currentLevel = :currentLevel,
            lastUpdated = :timestamp
        WHERE id = :craftEssenceId
    """)
    suspend fun updateCraftEssenceOwnership(
        craftEssenceId: Int, 
        isOwned: Boolean, 
        currentLevel: Int = 1,
        timestamp: Long = System.currentTimeMillis()
    ): Int
    
    /**
     * Updates craft essence level
     * 
     * @param craftEssenceId CraftEssence ID
     * @param newLevel New level
     * @param timestamp Update timestamp
     * @return Number of rows affected
     */
    @Query("""
        UPDATE craft_essences 
        SET currentLevel = :newLevel,
            lastUpdated = :timestamp
        WHERE id = :craftEssenceId AND isOwned = 1
    """)
    suspend fun updateCraftEssenceLevel(
        craftEssenceId: Int, 
        newLevel: Int,
        timestamp: Long = System.currentTimeMillis()
    ): Int
    
    /**
     * Checks if craft essence name exists
     * 
     * @param name CraftEssence name to check
     * @param excludeId CraftEssence ID to exclude from check (for updates)
     * @return True if name exists
     */
    @Query("SELECT COUNT(*) > 0 FROM craft_essences WHERE name = :name AND (:excludeId IS NULL OR id != :excludeId)")
    suspend fun isCraftEssenceNameExists(name: String, excludeId: Int? = null): Boolean
    
    /**
     * Gets craft essences that need data update (older than specified time)
     * 
     * @param cutoffTime Timestamp cutoff for updates
     * @return List of craft essences needing update
     */
    @Query("SELECT * FROM craft_essences WHERE lastUpdated < :cutoffTime")
    suspend fun getCraftEssencesNeedingUpdate(cutoffTime: Long): List<CraftEssence>
} 