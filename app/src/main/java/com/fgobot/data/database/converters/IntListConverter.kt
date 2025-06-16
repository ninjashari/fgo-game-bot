/*
 * FGO Bot - Integer List Type Converter
 * 
 * This file defines Room type converters for List<Int> data types.
 * Converts between List<Int> and String for database storage.
 */

package com.fgobot.data.database.converters

import androidx.room.TypeConverter

/**
 * Type converter for List<Int> data types in Room database
 * 
 * Converts List<Int> to comma-separated String for storage
 * and converts back to List<Int> when retrieving from database.
 */
class IntListConverter {
    
    /**
     * Converts List<Int> to String for database storage
     * 
     * @param intList List of integers to convert
     * @return Comma-separated string representation, or null if input is null
     */
    @TypeConverter
    fun fromIntList(intList: List<Int>?): String? {
        return intList?.joinToString(",")
    }
    
    /**
     * Converts String to List<Int> when retrieving from database
     * 
     * @param intString Comma-separated string of integers
     * @return List of integers, or empty list if input is null/empty
     */
    @TypeConverter
    fun toIntList(intString: String?): List<Int> {
        return if (intString.isNullOrEmpty()) {
            emptyList()
        } else {
            intString.split(",").mapNotNull { 
                it.trim().toIntOrNull() 
            }
        }
    }
} 