package com.fgobot.data.database.dao

import androidx.room.*
import com.fgobot.data.database.entities.ServantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServantDao {
    
    @Query("SELECT * FROM servants")
    fun getAllServants(): Flow<List<ServantEntity>>
    
    @Query("SELECT * FROM servants WHERE owned = 1")
    fun getOwnedServants(): Flow<List<ServantEntity>>
    
    @Query("SELECT * FROM servants WHERE id = :id")
    suspend fun getServantById(id: Int): ServantEntity?
    
    @Query("SELECT * FROM servants WHERE className = :className")
    fun getServantsByClass(className: String): Flow<List<ServantEntity>>
    
    @Query("SELECT * FROM servants WHERE rarity = :rarity")
    fun getServantsByRarity(rarity: Int): Flow<List<ServantEntity>>
    
    @Query("SELECT * FROM servants WHERE owned = 1 AND className = :className")
    fun getOwnedServantsByClass(className: String): Flow<List<ServantEntity>>
    
    @Query("SELECT * FROM servants WHERE name LIKE '%' || :name || '%'")
    fun searchServantsByName(name: String): Flow<List<ServantEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServant(servant: ServantEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServants(servants: List<ServantEntity>)
    
    @Update
    suspend fun updateServant(servant: ServantEntity)
    
    @Query("UPDATE servants SET owned = :owned WHERE id = :id")
    suspend fun updateServantOwnership(id: Int, owned: Boolean)
    
    @Query("UPDATE servants SET level = :level, ascension = :ascension WHERE id = :id")
    suspend fun updateServantLevel(id: Int, level: Int, ascension: Int)
    
    @Query("UPDATE servants SET skillLevels = :skillLevels WHERE id = :id")
    suspend fun updateServantSkills(id: Int, skillLevels: String)
    
    @Delete
    suspend fun deleteServant(servant: ServantEntity)
    
    @Query("DELETE FROM servants")
    suspend fun deleteAllServants()
} 