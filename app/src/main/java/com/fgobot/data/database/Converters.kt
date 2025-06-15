package com.fgobot.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            Gson().fromJson<List<String>>(value, object : TypeToken<List<String>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return Gson().toJson(value)
    }
    
    @TypeConverter
    fun toIntList(value: String): List<Int> {
        return try {
            Gson().fromJson<List<Int>>(value, object : TypeToken<List<Int>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    @TypeConverter
    fun fromMap(value: Map<String, Any>): String {
        return Gson().toJson(value)
    }
    
    @TypeConverter
    fun toMap(value: String): Map<String, Any> {
        return try {
            Gson().fromJson<Map<String, Any>>(value, object : TypeToken<Map<String, Any>>() {}.type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
} 