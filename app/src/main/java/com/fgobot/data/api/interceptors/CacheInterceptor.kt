/*
 * FGO Bot - Cache Control Interceptor
 * 
 * This file implements HTTP caching strategies for API responses.
 * Manages cache behavior for different types of requests and offline support.
 */

package com.fgobot.data.api.interceptors

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Cache Control Interceptor
 * 
 * Manages HTTP caching behavior with different strategies for:
 * - Static data (servants, craft essences) - Long cache duration
 * - Dynamic data (events, news) - Short cache duration
 * - Offline support - Serve stale cache when network unavailable
 * - Cache invalidation - Force fresh data when needed
 */
class CacheInterceptor(
    private val context: Context
) : Interceptor {
    
    companion object {
        // Cache durations in seconds
        private const val CACHE_STATIC_DATA = 24 * 60 * 60 // 24 hours for static data
        private const val CACHE_DYNAMIC_DATA = 5 * 60 // 5 minutes for dynamic data
        private const val CACHE_OFFLINE_STALE = 7 * 24 * 60 * 60 // 7 days for offline stale data
        
        // Static data endpoints (long cache)
        private val STATIC_ENDPOINTS = setOf(
            "servant",
            "svt",
            "craft-essence",
            "ce",
            "class",
            "attribute",
            "trait",
            "skill",
            "noble-phantasm",
            "np"
        )
        
        // Dynamic data endpoints (short cache)
        private val DYNAMIC_ENDPOINTS = setOf(
            "event",
            "news",
            "war",
            "quest",
            "item"
        )
    }
    
    /**
     * Intercepts HTTP responses to apply appropriate caching strategies
     * 
     * @param chain Interceptor chain
     * @return HTTP response with cache headers
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val originalResponse = chain.proceed(request)
        
        // Determine cache strategy based on endpoint
        val cacheStrategy = determineCacheStrategy(request.url.toString())
        
        // Check network availability
        val isNetworkAvailable = isNetworkAvailable()
        
        return when {
            // Network available - apply normal caching
            isNetworkAvailable -> {
                originalResponse.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", cacheStrategy.onlineCacheControl)
                    .build()
            }
            
            // Network unavailable - serve stale cache
            else -> {
                originalResponse.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", cacheStrategy.offlineCacheControl)
                    .build()
            }
        }
    }
    
    /**
     * Determines the appropriate cache strategy for a given URL
     * 
     * @param url Request URL
     * @return Cache strategy configuration
     */
    private fun determineCacheStrategy(url: String): CacheStrategy {
        return when {
            // Static data - long cache duration
            STATIC_ENDPOINTS.any { endpoint -> url.contains("/$endpoint/") || url.endsWith("/$endpoint") } -> {
                CacheStrategy.STATIC_DATA
            }
            
            // Dynamic data - short cache duration
            DYNAMIC_ENDPOINTS.any { endpoint -> url.contains("/$endpoint/") || url.endsWith("/$endpoint") } -> {
                CacheStrategy.DYNAMIC_DATA
            }
            
            // Default - medium cache duration
            else -> {
                CacheStrategy.DEFAULT
            }
        }
    }
    
    /**
     * Checks if network is currently available
     * 
     * @return True if network is available
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Cache strategy configuration
     */
    private enum class CacheStrategy(
        val onlineCacheControl: String,
        val offlineCacheControl: String
    ) {
        /**
         * Static data strategy - Long cache duration
         * Used for servants, craft essences, and other rarely changing data
         */
        STATIC_DATA(
            onlineCacheControl = "public, max-age=$CACHE_STATIC_DATA",
            offlineCacheControl = "public, only-if-cached, max-stale=$CACHE_OFFLINE_STALE"
        ),
        
        /**
         * Dynamic data strategy - Short cache duration
         * Used for events, news, and frequently changing data
         */
        DYNAMIC_DATA(
            onlineCacheControl = "public, max-age=$CACHE_DYNAMIC_DATA",
            offlineCacheControl = "public, only-if-cached, max-stale=$CACHE_OFFLINE_STALE"
        ),
        
        /**
         * Default strategy - Medium cache duration
         * Used for general API endpoints
         */
        DEFAULT(
            onlineCacheControl = "public, max-age=${CACHE_DYNAMIC_DATA * 2}", // 10 minutes
            offlineCacheControl = "public, only-if-cached, max-stale=$CACHE_OFFLINE_STALE"
        )
    }
}

/**
 * Cache control utilities for manual cache management
 */
object CacheControlUtils {
    
    /**
     * Creates cache control for forcing fresh data (no cache)
     * 
     * @return CacheControl that bypasses cache
     */
    fun noCache(): CacheControl {
        return CacheControl.Builder()
            .noCache()
            .noStore()
            .build()
    }
    
    /**
     * Creates cache control for offline-only requests
     * 
     * @return CacheControl that only uses cache
     */
    fun onlyCache(): CacheControl {
        return CacheControl.Builder()
            .onlyIfCached()
            .maxStale(7, TimeUnit.DAYS)
            .build()
    }
    
    /**
     * Creates cache control with custom max age
     * 
     * @param maxAge Maximum age in seconds
     * @return CacheControl with specified max age
     */
    fun maxAge(maxAge: Int): CacheControl {
        return CacheControl.Builder()
            .maxAge(maxAge, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Creates cache control for static data
     * 
     * @return CacheControl optimized for static data
     */
    fun staticData(): CacheControl {
        return CacheControl.Builder()
            .maxAge(24, TimeUnit.HOURS)
            .build()
    }
    
    /**
     * Creates cache control for dynamic data
     * 
     * @return CacheControl optimized for dynamic data
     */
    fun dynamicData(): CacheControl {
        return CacheControl.Builder()
            .maxAge(5, TimeUnit.MINUTES)
            .build()
    }
} 