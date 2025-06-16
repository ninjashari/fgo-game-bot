/*
 * FGO Bot - Cache Management System
 * 
 * This file provides comprehensive caching capabilities for the FGO Bot.
 * Handles in-memory caching, disk caching, cache invalidation, and performance monitoring.
 */

package com.fgobot.data.cache

import android.content.Context
import android.util.LruCache
import com.fgobot.core.logging.FGOLogger
import com.fgobot.data.database.entities.Servant
import com.fgobot.data.database.entities.CraftEssence
import com.fgobot.data.database.entities.Quest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * Cache entry wrapper
 * 
 * Contains cached data with metadata for cache management.
 */
data class CacheEntry<T>(
    val data: T,
    val timestamp: Long = System.currentTimeMillis(),
    val expiryTime: Long = timestamp + DEFAULT_CACHE_DURATION,
    val accessCount: Int = 0,
    val lastAccessTime: Long = timestamp
) : Serializable {
    
    companion object {
        private const val DEFAULT_CACHE_DURATION = 24 * 60 * 60 * 1000L // 24 hours
    }
    
    val isExpired: Boolean get() = System.currentTimeMillis() > expiryTime
    val age: Long get() = System.currentTimeMillis() - timestamp
    
    fun accessed(): CacheEntry<T> = copy(
        accessCount = accessCount + 1,
        lastAccessTime = System.currentTimeMillis()
    )
}

/**
 * Cache statistics
 * 
 * Contains metrics about cache performance.
 */
data class CacheStats(
    val hitCount: Long = 0,
    val missCount: Long = 0,
    val evictionCount: Long = 0,
    val memorySize: Long = 0,
    val diskSize: Long = 0,
    val entryCount: Int = 0
) {
    val hitRate: Double get() = if (hitCount + missCount > 0) hitCount.toDouble() / (hitCount + missCount) else 0.0
    val totalRequests: Long get() = hitCount + missCount
}

/**
 * Cache configuration
 * 
 * Contains settings for cache behavior.
 */
data class CacheConfig(
    val memoryMaxSize: Int = 50 * 1024 * 1024, // 50MB
    val diskMaxSize: Long = 100 * 1024 * 1024L, // 100MB
    val defaultTtl: Long = 24 * 60 * 60 * 1000L, // 24 hours
    val cleanupInterval: Long = 60 * 60 * 1000L, // 1 hour
    val enableDiskCache: Boolean = true,
    val enableMemoryCache: Boolean = true
)

/**
 * Cache manager interface
 * 
 * Defines contracts for cache operations.
 */
interface CacheManager {
    
    /**
     * Gets cache statistics
     * 
     * @return Current cache statistics
     */
    suspend fun getStats(): CacheStats
    
    /**
     * Puts data into cache
     * 
     * @param key Cache key
     * @param data Data to cache
     * @param ttl Time to live in milliseconds
     */
    suspend fun <T : Serializable> put(key: String, data: T, ttl: Long? = null)
    
    /**
     * Gets data from cache
     * 
     * @param key Cache key
     * @param type Data type class
     * @return Cached data or null if not found/expired
     */
    suspend fun <T : Serializable> get(key: String, type: Class<T>): T?
    
    /**
     * Removes data from cache
     * 
     * @param key Cache key
     */
    suspend fun remove(key: String)
    
    /**
     * Clears all cache data
     */
    suspend fun clear()
    
    /**
     * Clears expired entries
     */
    suspend fun clearExpired()
    
    /**
     * Checks if key exists in cache
     * 
     * @param key Cache key
     * @return True if key exists and not expired
     */
    suspend fun contains(key: String): Boolean
    
    /**
     * Gets all cache keys
     * 
     * @return Set of all cache keys
     */
    suspend fun getKeys(): Set<String>
}

/**
 * Implementation of CacheManager
 * 
 * Provides multi-level caching with memory and disk storage.
 * Implements LRU eviction, TTL expiration, and performance monitoring.
 */
class CacheManagerImpl(
    private val context: Context,
    private val logger: FGOLogger,
    private val config: CacheConfig = CacheConfig()
) : CacheManager {
    
    companion object {
        private const val TAG = "CacheManager"
        private const val CACHE_DIR = "fgobot_cache"
        private const val STATS_FILE = "cache_stats.dat"
    }
    
    // In-memory cache using LRU
    private val memoryCache = object : LruCache<String, CacheEntry<*>>(config.memoryMaxSize) {
        override fun sizeOf(key: String, value: CacheEntry<*>): Int {
            // Estimate size - this is a simplified calculation
            return key.length * 2 + 1024 // Approximate size
        }
        
        override fun entryRemoved(evicted: Boolean, key: String, oldValue: CacheEntry<*>, newValue: CacheEntry<*>?) {
            if (evicted) {
                stats = stats.copy(evictionCount = stats.evictionCount + 1)
                logger.d(TAG, "Memory cache entry evicted: $key")
            }
        }
    }
    
    // Disk cache directory
    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    // Cache statistics
    private var stats = CacheStats()
    private val statsMutex = Mutex()
    
    // Cleanup mutex
    private val cleanupMutex = Mutex()
    private var lastCleanupTime = 0L
    
    init {
        logger.i(TAG, "Cache manager initialized with config: $config")
        loadStats()
    }
    
    override suspend fun getStats(): CacheStats = statsMutex.withLock {
        stats.copy(
            memorySize = memoryCache.size().toLong(),
            diskSize = calculateDiskSize(),
            entryCount = memoryCache.size() + getDiskEntryCount()
        )
    }
    
    override suspend fun <T : Serializable> put(key: String, data: T, ttl: Long?) {
        val effectiveTtl = ttl ?: config.defaultTtl
        val entry = CacheEntry(data, expiryTime = System.currentTimeMillis() + effectiveTtl)
        
        // Store in memory cache if enabled
        if (config.enableMemoryCache) {
            memoryCache.put(key, entry)
            logger.d(TAG, "Stored in memory cache: $key")
        }
        
        // Store in disk cache if enabled
        if (config.enableDiskCache) {
            storeToDisk(key, entry)
            logger.d(TAG, "Stored in disk cache: $key")
        }
        
        // Trigger cleanup if needed
        triggerCleanupIfNeeded()
    }
    
    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Serializable> get(key: String, type: Class<T>): T? {
        // Try memory cache first
        if (config.enableMemoryCache) {
            val memoryEntry = memoryCache.get(key) as? CacheEntry<T>
            if (memoryEntry != null && !memoryEntry.isExpired) {
                statsMutex.withLock {
                    stats = stats.copy(hitCount = stats.hitCount + 1)
                }
                // Update access info
                memoryCache.put(key, memoryEntry.accessed())
                logger.d(TAG, "Memory cache hit: $key")
                return memoryEntry.data
            }
        }
        
        // Try disk cache
        if (config.enableDiskCache) {
            val diskEntry = loadFromDisk<T>(key)
            if (diskEntry != null && !diskEntry.isExpired) {
                // Promote to memory cache
                if (config.enableMemoryCache) {
                    memoryCache.put(key, diskEntry.accessed())
                }
                statsMutex.withLock {
                    stats = stats.copy(hitCount = stats.hitCount + 1)
                }
                logger.d(TAG, "Disk cache hit: $key")
                return diskEntry.data
            }
        }
        
        // Cache miss
        statsMutex.withLock {
            stats = stats.copy(missCount = stats.missCount + 1)
        }
        logger.d(TAG, "Cache miss: $key")
        return null
    }
    
    override suspend fun remove(key: String) {
        // Remove from memory cache
        memoryCache.remove(key)
        
        // Remove from disk cache
        val diskFile = File(cacheDir, key.hashCode().toString())
        if (diskFile.exists()) {
            diskFile.delete()
            logger.d(TAG, "Removed from disk cache: $key")
        }
        
        logger.d(TAG, "Removed from cache: $key")
    }
    
    override suspend fun clear() {
        // Clear memory cache
        memoryCache.evictAll()
        
        // Clear disk cache
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name != STATS_FILE) {
                file.delete()
            }
        }
        
        // Reset statistics
        statsMutex.withLock {
            stats = CacheStats()
        }
        
        logger.i(TAG, "Cache cleared")
    }
    
    override suspend fun clearExpired() = cleanupMutex.withLock {
        logger.d(TAG, "Starting expired cache cleanup")
        val startTime = System.currentTimeMillis()
        var removedCount = 0
        
        // Clean memory cache
        val memoryKeys = memoryCache.snapshot().keys.toList()
        memoryKeys.forEach { key ->
            val entry = memoryCache.get(key)
            if (entry?.isExpired == true) {
                memoryCache.remove(key)
                removedCount++
            }
        }
        
        // Clean disk cache
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name != STATS_FILE) {
                try {
                    val entry = loadFromDiskFile<Serializable>(file)
                    if (entry?.isExpired == true) {
                        file.delete()
                        removedCount++
                    }
                } catch (e: Exception) {
                    // Corrupted file, remove it
                    file.delete()
                    removedCount++
                    logger.w(TAG, "Removed corrupted cache file: ${file.name}")
                }
            }
        }
        
        lastCleanupTime = System.currentTimeMillis()
        val duration = lastCleanupTime - startTime
        logger.i(TAG, "Cache cleanup completed: removed $removedCount entries in ${duration}ms")
    }
    
    override suspend fun contains(key: String): Boolean {
        // Check memory cache
        if (config.enableMemoryCache) {
            val memoryEntry = memoryCache.get(key)
            if (memoryEntry != null && !memoryEntry.isExpired) {
                return true
            }
        }
        
        // Check disk cache
        if (config.enableDiskCache) {
            val diskEntry = loadFromDisk<Serializable>(key)
            if (diskEntry != null && !diskEntry.isExpired) {
                return true
            }
        }
        
        return false
    }
    
    override suspend fun getKeys(): Set<String> {
        val keys = mutableSetOf<String>()
        
        // Add memory cache keys
        if (config.enableMemoryCache) {
            keys.addAll(memoryCache.snapshot().keys)
        }
        
        // Add disk cache keys (this is approximate since we hash the keys)
        if (config.enableDiskCache) {
            // For disk cache, we'd need to maintain a separate index
            // This is a simplified implementation
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name != STATS_FILE) {
                    keys.add(file.name) // This would be the hashed key
                }
            }
        }
        
        return keys
    }
    
    /**
     * Stores cache entry to disk
     */
    private fun <T : Serializable> storeToDisk(key: String, entry: CacheEntry<T>) {
        try {
            val file = File(cacheDir, key.hashCode().toString())
            ObjectOutputStream(FileOutputStream(file)).use { oos ->
                oos.writeObject(entry)
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to store to disk cache: $key", e)
        }
    }
    
    /**
     * Loads cache entry from disk
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Serializable> loadFromDisk(key: String): CacheEntry<T>? {
        return try {
            val file = File(cacheDir, key.hashCode().toString())
            if (!file.exists()) return null
            
            loadFromDiskFile(file)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to load from disk cache: $key", e)
            null
        }
    }
    
    /**
     * Loads cache entry from disk file
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Serializable> loadFromDiskFile(file: File): CacheEntry<T>? {
        return try {
            ObjectInputStream(FileInputStream(file)).use { ois ->
                ois.readObject() as CacheEntry<T>
            }
        } catch (e: Exception) {
            logger.w(TAG, "Failed to load cache file: ${file.name}", e)
            null
        }
    }
    
    /**
     * Calculates total disk cache size
     */
    private fun calculateDiskSize(): Long {
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
    
    /**
     * Gets disk cache entry count
     */
    private fun getDiskEntryCount(): Int {
        return cacheDir.listFiles()?.count { it.isFile && it.name != STATS_FILE } ?: 0
    }
    
    /**
     * Triggers cleanup if needed based on interval
     */
    private suspend fun triggerCleanupIfNeeded() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCleanupTime > config.cleanupInterval) {
            clearExpired()
        }
    }
    
    /**
     * Loads cache statistics from disk
     */
    private fun loadStats() {
        try {
            val statsFile = File(cacheDir, STATS_FILE)
            if (statsFile.exists()) {
                ObjectInputStream(FileInputStream(statsFile)).use { ois ->
                    stats = ois.readObject() as CacheStats
                }
                logger.d(TAG, "Loaded cache statistics")
            }
        } catch (e: Exception) {
            logger.w(TAG, "Failed to load cache statistics", e)
        }
    }
    
    /**
     * Saves cache statistics to disk
     */
    private fun saveStats() {
        try {
            val statsFile = File(cacheDir, STATS_FILE)
            ObjectOutputStream(FileOutputStream(statsFile)).use { oos ->
                oos.writeObject(stats)
            }
        } catch (e: Exception) {
            logger.w(TAG, "Failed to save cache statistics", e)
        }
    }
}

/**
 * Cache key utilities
 * 
 * Provides standardized cache key generation.
 */
object CacheKeys {
    
    private const val SEPARATOR = ":"
    
    fun servantList(): String = "servants${SEPARATOR}all"
    fun servantById(id: Int): String = "servant${SEPARATOR}$id"
    fun servantsByClass(className: String): String = "servants${SEPARATOR}class${SEPARATOR}$className"
    fun ownedServants(): String = "servants${SEPARATOR}owned"
    
    fun craftEssenceList(): String = "craft_essences${SEPARATOR}all"
    fun craftEssenceById(id: Int): String = "craft_essence${SEPARATOR}$id"
    fun craftEssencesByRarity(rarity: Int): String = "craft_essences${SEPARATOR}rarity${SEPARATOR}$rarity"
    
    fun questList(): String = "quests${SEPARATOR}all"
    fun questById(id: Int): String = "quest${SEPARATOR}$id"
    fun questsByType(type: String): String = "quests${SEPARATOR}type${SEPARATOR}$type"
    
    fun apiServants(): String = "api${SEPARATOR}servants"
    fun apiCraftEssences(): String = "api${SEPARATOR}craft_essences"
    fun apiQuests(): String = "api${SEPARATOR}quests"
} 