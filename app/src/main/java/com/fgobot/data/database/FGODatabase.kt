package com.fgobot.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.fgobot.data.database.dao.*
import com.fgobot.data.database.entities.*

@Database(
    entities = [
        ServantEntity::class,
        CraftEssenceEntity::class,
        QuestEntity::class,
        TeamConfigEntity::class,
        BattleLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FGODatabase : RoomDatabase() {
    
    abstract fun servantDao(): ServantDao
    abstract fun craftEssenceDao(): CraftEssenceDao
    abstract fun questDao(): QuestDao
    abstract fun teamConfigDao(): TeamConfigDao
    abstract fun battleLogDao(): BattleLogDao
    
    companion object {
        @Volatile
        private var INSTANCE: FGODatabase? = null
        
        fun getDatabase(context: Context): FGODatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FGODatabase::class.java,
                    "fgo_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 