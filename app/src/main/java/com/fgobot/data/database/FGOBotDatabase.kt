/*
 * FGO Bot - Room Database
 * 
 * This file defines the main Room database for the FGO Bot application.
 * Contains all entities, DAOs, and database configuration.
 */

package com.fgobot.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.fgobot.data.database.converters.IntListConverter
import com.fgobot.data.database.converters.StringListConverter
import com.fgobot.data.database.dao.ServantDao
import com.fgobot.data.database.dao.TeamDao
import com.fgobot.data.database.entities.BattleLog
import com.fgobot.data.database.entities.CraftEssence
import com.fgobot.data.database.entities.Quest
import com.fgobot.data.database.entities.Servant
import com.fgobot.data.database.entities.Team

/**
 * Main Room database for FGO Bot application
 * 
 * Contains all entities and provides access to DAOs for database operations.
 * Implements singleton pattern to ensure single database instance.
 */
@Database(
    entities = [
        Servant::class,
        CraftEssence::class,
        Quest::class,
        Team::class,
        BattleLog::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(
    StringListConverter::class,
    IntListConverter::class
)
abstract class FGOBotDatabase : RoomDatabase() {
    
    /**
     * Provides access to Servant DAO
     * 
     * @return ServantDao instance
     */
    abstract fun servantDao(): ServantDao
    
    /**
     * Provides access to Team DAO
     * 
     * @return TeamDao instance
     */
    abstract fun teamDao(): TeamDao
    
    companion object {
        
        /**
         * Database name
         */
        private const val DATABASE_NAME = "fgobot_database"
        
        /**
         * Singleton instance of the database
         */
        @Volatile
        private var INSTANCE: FGOBotDatabase? = null
        
        /**
         * Gets the singleton database instance
         * 
         * @param context Application context
         * @return FGOBotDatabase instance
         */
        fun getDatabase(context: Context): FGOBotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FGOBotDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // For development - remove in production
                    .build()
                
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Closes the database instance (for testing)
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
} 