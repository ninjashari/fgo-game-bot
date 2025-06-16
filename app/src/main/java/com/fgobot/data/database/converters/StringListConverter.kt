/*
 * FGO Bot - Database Type Converters
 * 
 * This file defines type converters for Room database to handle complex data types.
 * Converts between Kotlin types and database-storable types.
 */

package com.fgobot.data.database.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Type converter for List<String> to JSON string and vice versa
 * Used by Room database to store string lists in SQLite
 */
class StringListConverter {
    
    private val gson = Gson()
    
    /**
     * Converts a List<String> to JSON string for database storage
     * 
     * @param value List of strings to convert
     * @return JSON string representation or null if input is null
     */
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return if (value == null) null else gson.toJson(value)
    }
    
    /**
     * Converts JSON string back to List<String>
     * 
     * @param value JSON string to convert
     * @return List of strings or empty list if input is null
     */
    @TypeConverter
    fun toStringList(value: String?): List<String> {
        return if (value == null) {
            emptyList()
        } else {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(value, listType) ?: emptyList()
        }
    }
} 