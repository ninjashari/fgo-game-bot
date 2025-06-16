/*
 * FGO Bot - Offline Manager
 * 
 * This file provides offline mode management for the FGO Bot application.
 * Handles network state monitoring, offline/online switching, and cache fallback.
 */

package com.fgobot.data.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.fgobot.data.cache.CacheManager
import com.fgobot.core.logging.FGOLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Network state sealed class
 */
sealed class NetworkState {
    object Connected : NetworkState()
    object Disconnected : NetworkState()
    object Limited : NetworkState()
    data class Error(val message: String) : NetworkState()
}

/**
 * Offline mode configuration
 */
data class OfflineConfig(
    val enableOfflineMode: Boolean = true,
    val cacheValidityDuration: Long = 24 * 60 * 60 * 1000L, // 24 hours
    val maxCacheSize: Long = 100 * 1024 * 1024L, // 100MB
    val enableBackgroundSync: Boolean = true,
    val syncRetryAttempts: Int = 3,
    val syncRetryDelay: Long = 5000L // 5 seconds
)

/**
 * Offline manager interface
 */
interface OfflineManager {
    
    /**
     * Current network state
     */
    val networkState: StateFlow<NetworkState>
    
    /**
     * Whether the app is currently in offline mode
     */
    val isOfflineMode: StateFlow<Boolean>
    
    /**
     * Initialize offline manager
     */
    fun initialize()
    
    /**
     * Manually set offline mode
     */
    suspend fun setOfflineMode(enabled: Boolean)
    
    /**
     * Check if data is available offline
     */
    suspend fun isDataAvailableOffline(key: String): Boolean
    
    /**
     * Get cached data for offline use
     */
    suspend fun <T : java.io.Serializable> getCachedData(key: String, type: Class<T>): T?
    
    /**
     * Store data for offline use
     */
    suspend fun <T : java.io.Serializable> storeCachedData(key: String, data: T, ttl: Long? = null)
    
    /**
     * Clear offline cache
     */
    suspend fun clearOfflineCache()
    
    /**
     * Get offline statistics
     */
    suspend fun getOfflineStats(): OfflineStats
}

/**
 * Offline statistics data class
 */
data class OfflineStats(
    val isOfflineMode: Boolean,
    val networkState: NetworkState,
    val cacheSize: Long,
    val cachedItemCount: Int,
    val lastSyncTime: Long,
    val offlineDuration: Long,
    val cacheHitRate: Double
)

/**
 * Implementation of OfflineManager
 */
class OfflineManagerImpl(
    private val context: Context,
    private val cacheManager: CacheManager,
    private val logger: FGOLogger,
    private val config: OfflineConfig = OfflineConfig()
) : OfflineManager {
    
    companion object {
        private const val TAG = "OfflineManager"
        private const val OFFLINE_CACHE_PREFIX = "offline_"
    }
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Disconnected)
    override val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    private val _isOfflineMode = MutableStateFlow(false)
    override val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()
    
    private var offlineStartTime: Long = 0L
    private var lastSyncTime: Long = 0L
    private var cacheHits: Long = 0L
    private var cacheRequests: Long = 0L
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            logger.d(TAG, "Network available")
            _networkState.value = NetworkState.Connected
            updateOfflineMode()
        }
        
        override fun onLost(network: Network) {
            logger.d(TAG, "Network lost")
            _networkState.value = NetworkState.Disconnected
            updateOfflineMode()
        }
        
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val hasValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            
            when {
                hasInternet && hasValidated -> {
                    logger.d(TAG, "Network capabilities: Full connectivity")
                    _networkState.value = NetworkState.Connected
                }
                hasInternet -> {
                    logger.d(TAG, "Network capabilities: Limited connectivity")
                    _networkState.value = NetworkState.Limited
                }
                else -> {
                    logger.d(TAG, "Network capabilities: No connectivity")
                    _networkState.value = NetworkState.Disconnected
                }
            }
            updateOfflineMode()
        }
        
        override fun onUnavailable() {
            logger.d(TAG, "Network unavailable")
            _networkState.value = NetworkState.Disconnected
            updateOfflineMode()
        }
    }
    
    override fun initialize() {
        logger.i(TAG, "Initializing offline manager")
        
        // Register network callback
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            
            // Check initial network state
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            
            if (activeNetwork != null && networkCapabilities != null) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val hasValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                
                _networkState.value = when {
                    hasInternet && hasValidated -> NetworkState.Connected
                    hasInternet -> NetworkState.Limited
                    else -> NetworkState.Disconnected
                }
            } else {
                _networkState.value = NetworkState.Disconnected
            }
            
            updateOfflineMode()
            logger.i(TAG, "Offline manager initialized successfully")
            
        } catch (exception: Exception) {
            logger.e(TAG, "Failed to initialize offline manager", exception)
            _networkState.value = NetworkState.Error("Failed to initialize network monitoring")
        }
    }
    
    override suspend fun setOfflineMode(enabled: Boolean) {
        logger.i(TAG, "Manually setting offline mode: $enabled")
        
        if (enabled && !_isOfflineMode.value) {
            offlineStartTime = System.currentTimeMillis()
        } else if (!enabled && _isOfflineMode.value) {
            offlineStartTime = 0L
        }
        
        _isOfflineMode.value = enabled
    }
    
    override suspend fun isDataAvailableOffline(key: String): Boolean {
        val cacheKey = OFFLINE_CACHE_PREFIX + key
        return cacheManager.contains(cacheKey)
    }
    
    override suspend fun <T : java.io.Serializable> getCachedData(key: String, type: Class<T>): T? {
        cacheRequests++
        val cacheKey = OFFLINE_CACHE_PREFIX + key
        
        return try {
            val data = cacheManager.get(cacheKey, type)
            if (data != null) {
                cacheHits++
                logger.d(TAG, "Cache hit for offline data: $key")
            } else {
                logger.d(TAG, "Cache miss for offline data: $key")
            }
            data
        } catch (exception: Exception) {
            logger.e(TAG, "Error getting cached data: $key", exception)
            null
        }
    }
    
    override suspend fun <T : java.io.Serializable> storeCachedData(key: String, data: T, ttl: Long?) {
        val cacheKey = OFFLINE_CACHE_PREFIX + key
        val effectiveTtl = ttl ?: config.cacheValidityDuration
        
        try {
            cacheManager.put(cacheKey, data, effectiveTtl)
            logger.d(TAG, "Stored data for offline use: $key")
        } catch (exception: Exception) {
            logger.e(TAG, "Error storing cached data: $key", exception)
        }
    }
    
    override suspend fun clearOfflineCache() {
        try {
            val keys = cacheManager.getKeys()
            val offlineKeys = keys.filter { it.startsWith(OFFLINE_CACHE_PREFIX) }
            
            offlineKeys.forEach { key ->
                cacheManager.remove(key)
            }
            
            logger.i(TAG, "Cleared ${offlineKeys.size} offline cache entries")
        } catch (exception: Exception) {
            logger.e(TAG, "Error clearing offline cache", exception)
        }
    }
    
    override suspend fun getOfflineStats(): OfflineStats {
        val stats = cacheManager.getStats()
        val offlineDuration = if (offlineStartTime > 0) {
            System.currentTimeMillis() - offlineStartTime
        } else 0L
        
        val cacheHitRate = if (cacheRequests > 0) {
            cacheHits.toDouble() / cacheRequests
        } else 0.0
        
        return OfflineStats(
            isOfflineMode = _isOfflineMode.value,
            networkState = _networkState.value,
            cacheSize = stats.diskSize,
            cachedItemCount = stats.entryCount,
            lastSyncTime = lastSyncTime,
            offlineDuration = offlineDuration,
            cacheHitRate = cacheHitRate
        )
    }
    
    /**
     * Updates offline mode based on network state and configuration
     */
    private fun updateOfflineMode() {
        if (!config.enableOfflineMode) {
            _isOfflineMode.value = false
            return
        }
        
        val shouldBeOffline = when (_networkState.value) {
            is NetworkState.Connected -> false
            is NetworkState.Limited -> true
            is NetworkState.Disconnected -> true
            is NetworkState.Error -> true
        }
        
        if (shouldBeOffline && !_isOfflineMode.value) {
            offlineStartTime = System.currentTimeMillis()
            logger.i(TAG, "Switching to offline mode")
        } else if (!shouldBeOffline && _isOfflineMode.value) {
            offlineStartTime = 0L
            lastSyncTime = System.currentTimeMillis()
            logger.i(TAG, "Switching to online mode")
        }
        
        _isOfflineMode.value = shouldBeOffline
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            logger.d(TAG, "Offline manager cleanup completed")
        } catch (exception: Exception) {
            logger.e(TAG, "Error during offline manager cleanup", exception)
        }
    }
} 