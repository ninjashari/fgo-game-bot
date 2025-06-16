/*
 * FGO Bot - Servant DAO
 * 
 * This file defines the Data Access Object for Servant entities.
 * Provides database operations for servant data management.
 */

package com.fgobot.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fgobot.data.database.entities.Servant
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Servant entities
 * Provides methods for CRUD operations and complex queries
 */
@Dao
interface ServantDao {
    
    /**
     * Insert a new servant into the database
     * 
     * @param servant Servant entity to insert
     * @return Row ID of the inserted servant
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServant(servant: Servant): Long
    
    /**
     * Insert multiple servants into the database
     * 
     * @param servants List of servant entities to insert
     * @return List of row IDs of the inserted servants
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServants(servants: List<Servant>): List<Long>
    
    /**
     * Update an existing servant in the database
     * 
     * @param servant Servant entity with updated data
     * @return Number of rows affected
     */
    @Update
    suspend fun updateServant(servant: Servant): Int
    
    /**
     * Delete a servant from the database
     * 
     * @param servant Servant entity to delete
     * @return Number of rows affected
     */
    @Delete
    suspend fun deleteServant(servant: Servant): Int
    
    /**
     * Get a servant by ID
     * 
     * @param id Servant ID
     * @return Servant entity or null if not found
     */
    @Query("SELECT * FROM servants WHERE id = :id")
    suspend fun getServantById(id: Int): Servant?
    
    /**
     * Get all servants as a Flow for reactive updates
     * 
     * @return Flow of list of all servants
     */
    @Query("SELECT * FROM servants ORDER BY collectionNo ASC")
    fun getAllServantsFlow(): Flow<List<Servant>>
    
    /**
     * Get all servants as a regular list
     * 
     * @return List of all servants
     */
    @Query("SELECT * FROM servants ORDER BY collectionNo ASC")
    suspend fun getAllServants(): List<Servant>
    
    /**
     * Get servants by class
     * 
     * @param className Servant class (Saber, Archer, etc.)
     * @return Flow of servants of the specified class
     */
    @Query("SELECT * FROM servants WHERE className = :className ORDER BY collectionNo ASC")
    fun getServantsByClass(className: String): Flow<List<Servant>>
    
    /**
     * Get servants by rarity
     * 
     * @param rarity Star rating (1-5)
     * @return Flow of servants of the specified rarity
     */
    @Query("SELECT * FROM servants WHERE rarity = :rarity ORDER BY collectionNo ASC")
    fun getServantsByRarity(rarity: Int): Flow<List<Servant>>
    
    /**
     * Get owned servants only
     * 
     * @return Flow of servants owned by the user
     */
    @Query("SELECT * FROM servants WHERE isOwned = 1 ORDER BY collectionNo ASC")
    fun getOwnedServants(): Flow<List<Servant>>
    
    /**
     * Search servants by name
     * 
     * @param searchQuery Search query (partial name match)
     * @return Flow of servants matching the search query
     */
    @Query("SELECT * FROM servants WHERE name LIKE '%' || :searchQuery || '%' ORDER BY collectionNo ASC")
    fun searchServantsByName(searchQuery: String): Flow<List<Servant>>
    
    /**
     * Get servants with specific traits
     * 
     * @param trait Trait to search for
     * @return Flow of servants with the specified trait
     */
    @Query("SELECT * FROM servants WHERE traits LIKE '%' || :trait || '%' ORDER BY collectionNo ASC")
    fun getServantsByTrait(trait: String): Flow<List<Servant>>
    
    /**
     * Get servants by class and rarity
     * 
     * @param className Servant class
     * @param rarity Star rating
     * @return Flow of servants matching both criteria
     */
    @Query("SELECT * FROM servants WHERE className = :className AND rarity = :rarity ORDER BY collectionNo ASC")
    fun getServantsByClassAndRarity(className: String, rarity: Int): Flow<List<Servant>>
    
    /**
     * Update servant ownership status
     * 
     * @param servantId Servant ID
     * @param isOwned New ownership status
     * @param currentLevel Current level if owned
     * @param skillLevels Current skill levels if owned
     * @return Number of rows affected
     */
    @Query("""
        UPDATE servants 
        SET isOwned = :isOwned, 
            currentLevel = :currentLevel, 
            currentSkillLevels = :skillLevels,
            lastUpdated = :timestamp
        WHERE id = :servantId
    """)
    suspend fun updateServantOwnership(
        servantId: Int, 
        isOwned: Boolean, 
        currentLevel: Int = 1,
        skillLevels: String = "[1,1,1]",
        timestamp: Long = System.currentTimeMillis()
    ): Int
    
    /**
     * Get total servant count
     * 
     * @return Total number of servants
     */
    @Query("SELECT COUNT(*) FROM servants")
    suspend fun getTotalServantCount(): Int
    
    /**
     * Get owned servant count
     * 
     * @return Number of owned servants
     */
    @Query("SELECT COUNT(*) FROM servants WHERE isOwned = 1")
    suspend fun getOwnedServantCount(): Int
    
    /**
     * Get distinct class count
     * 
     * @return Number of different servant classes
     */
    @Query("SELECT COUNT(DISTINCT className) FROM servants")
    suspend fun getDistinctClassCount(): Int
    
    /**
     * Get average rarity of owned servants
     * 
     * @return Average rarity of owned servants
     */
    @Query("SELECT AVG(rarity) FROM servants WHERE isOwned = 1")
    suspend fun getAverageOwnedRarity(): Double?
    
    /**
     * Delete all servants (for data refresh)
     * 
     * @return Number of rows deleted
     */
    @Query("DELETE FROM servants")
    suspend fun deleteAllServants(): Int
    
    /**
     * Get servants that need data update (older than specified time)
     * 
     * @param cutoffTime Timestamp cutoff for updates
     * @return List of servants needing update
     */
    @Query("SELECT * FROM servants WHERE lastUpdated < :cutoffTime")
    suspend fun getServantsNeedingUpdate(cutoffTime: Long): List<Servant>
} 