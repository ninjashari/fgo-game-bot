/*
 * FGO Bot - API Client Configuration
 * 
 * This file configures the Retrofit client for Atlas Academy API integration.
 * Provides centralized HTTP client configuration with interceptors, caching, and error handling.
 */

package com.fgobot.data.api

import android.content.Context
import com.fgobot.core.logging.FGOBotLogger
import com.fgobot.data.api.interceptors.NetworkInterceptor
import com.fgobot.data.api.interceptors.CacheInterceptor
import com.fgobot.data.api.interceptors.RateLimitInterceptor
import com.fgobot.data.api.AtlasAcademyService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * API Client Configuration
 * 
 * Provides configured Retrofit client for Atlas Academy API with:
 * - HTTP logging and network monitoring
 * - Response caching for offline support
 * - Rate limiting to respect API guidelines
 * - Error handling and retry mechanisms
 * - JSON serialization/deserialization
 */
class ApiClient(
    private val context: Context,
    private val logger: FGOBotLogger
) {
    
    companion object {
        // Atlas Academy API Configuration
        private const val BASE_URL = "https://api.atlasacademy.io/"
        private const val API_VERSION = "nice"
        private const val REGION = "NA" // North America region
        
        // HTTP Configuration
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 60L
        private const val WRITE_TIMEOUT = 60L
        private const val CACHE_SIZE = 50L * 1024 * 1024 // 50MB cache
        
        // Rate Limiting Configuration
        private const val MAX_REQUESTS_PER_MINUTE = 60
        private const val RATE_LIMIT_WINDOW_MS = 60_000L
    }
    
    /**
     * Configured Gson instance for JSON serialization
     * 
     * Features:
     * - Null value serialization
     * - Date format handling
     * - Field naming policy
     * - Pretty printing for debugging
     */
    private val gson: Gson by lazy {
        GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .setFieldNamingPolicy(com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .serializeNulls()
            .setPrettyPrinting()
            .create()
    }
    
    /**
     * HTTP cache for offline support and performance
     * 
     * Caches API responses to reduce network requests and enable offline functionality
     */
    private val httpCache: Cache by lazy {
        val cacheDir = File(context.cacheDir, "http_cache")
        Cache(cacheDir, CACHE_SIZE)
    }
    
    /**
     * HTTP logging interceptor for debugging
     * 
     * Logs HTTP requests and responses for development and debugging purposes
     */
    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor { message ->
            logger.debug(FGOBotLogger.Category.NETWORK, "HTTP: $message")
        }.apply {
            level = if (com.fgobot.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }
    }
    
    /**
     * Network monitoring interceptor
     * 
     * Monitors network requests for performance analytics and error tracking
     */
    private val networkInterceptor: NetworkInterceptor by lazy {
        NetworkInterceptor(logger)
    }
    
    /**
     * Cache control interceptor
     * 
     * Manages HTTP caching behavior for different types of requests
     */
    private val cacheInterceptor: CacheInterceptor by lazy {
        CacheInterceptor(context)
    }
    
    /**
     * Rate limiting interceptor
     * 
     * Implements rate limiting to respect Atlas Academy API guidelines
     */
    private val rateLimitInterceptor: RateLimitInterceptor by lazy {
        RateLimitInterceptor(
            maxRequests = MAX_REQUESTS_PER_MINUTE,
            windowMs = RATE_LIMIT_WINDOW_MS,
            logger = logger
        )
    }
    
    /**
     * Configured OkHttp client
     * 
     * Features:
     * - Connection pooling and keep-alive
     * - Automatic retries with exponential backoff
     * - Request/response interceptors
     * - HTTP caching
     * - Rate limiting
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .cache(httpCache)
            .addInterceptor(rateLimitInterceptor)
            .addInterceptor(cacheInterceptor)
            .addInterceptor(loggingInterceptor)
            .addNetworkInterceptor(networkInterceptor)
            .retryOnConnectionFailure(true)
            .build()
    }
    
    /**
     * Configured Retrofit instance
     * 
     * Main Retrofit client for Atlas Academy API communication
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Atlas Academy API service
     * 
     * Provides access to all Atlas Academy API endpoints
     */
    val atlasAcademyService: AtlasAcademyService by lazy {
        retrofit.create(AtlasAcademyService::class.java)
    }
    
    /**
     * Gets the full API URL for a specific endpoint
     * 
     * @param endpoint API endpoint path
     * @param region API region (default: NA)
     * @return Complete API URL
     */
    fun getApiUrl(endpoint: String, region: String = REGION): String {
        return "${BASE_URL}${API_VERSION}/${region}/${endpoint.trimStart('/')}"
    }
    
    /**
     * Clears HTTP cache
     * 
     * Useful for forcing fresh data retrieval or managing storage space
     */
    suspend fun clearCache() {
        try {
            httpCache.evictAll()
            logger.info(FGOBotLogger.Category.NETWORK, "HTTP cache cleared successfully")
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.ERROR, "Failed to clear HTTP cache: ${e.message}", e)
        }
    }
    
    /**
     * Gets current cache size in bytes
     * 
     * @return Current cache size
     */
    fun getCacheSize(): Long {
        return httpCache.size()
    }
    
    /**
     * Gets maximum cache size in bytes
     * 
     * @return Maximum cache size
     */
    fun getMaxCacheSize(): Long {
        return httpCache.maxSize()
    }
    
    /**
     * Gets cache hit count
     * 
     * @return Number of cache hits
     */
    fun getCacheHitCount(): Int {
        return httpCache.hitCount()
    }
    
    /**
     * Gets cache miss count
     * 
     * @return Number of cache misses
     */
    fun getCacheMissCount(): Int {
        return httpCache.requestCount() - httpCache.hitCount()
    }
    
    /**
     * Gets cache hit rate as percentage
     * 
     * @return Cache hit rate (0-100)
     */
    fun getCacheHitRate(): Double {
        val totalRequests = httpCache.requestCount()
        return if (totalRequests > 0) {
            (httpCache.hitCount().toDouble() / totalRequests) * 100
        } else {
            0.0
        }
    }
    
    /**
     * Checks if network is available
     * 
     * @return True if network is available
     */
    fun isNetworkAvailable(): Boolean {
        return networkInterceptor.isNetworkAvailable()
    }
    
    /**
     * Gets network statistics
     * 
     * @return Network performance statistics
     */
    fun getNetworkStats(): NetworkStats {
        return networkInterceptor.getNetworkStats()
    }
    
    /**
     * Closes the API client and releases resources
     * 
     * Should be called when the client is no longer needed
     */
    fun close() {
        try {
            httpCache.close()
            okHttpClient.dispatcher.executorService.shutdown()
            okHttpClient.connectionPool.evictAll()
            logger.info(FGOBotLogger.Category.NETWORK, "API client closed successfully")
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.ERROR, "Error closing API client: ${e.message}", e)
        }
    }
}

/**
 * Network performance statistics
 */
data class NetworkStats(
    val totalRequests: Long,
    val successfulRequests: Long,
    val failedRequests: Long,
    val averageResponseTime: Long,
    val totalBytesReceived: Long,
    val totalBytesSent: Long
) {
    /**
     * Gets success rate as percentage
     */
    val successRate: Double
        get() = if (totalRequests > 0) {
            (successfulRequests.toDouble() / totalRequests) * 100
        } else {
            0.0
        }
    
    /**
     * Gets failure rate as percentage
     */
    val failureRate: Double
        get() = if (totalRequests > 0) {
            (failedRequests.toDouble() / totalRequests) * 100
        } else {
            0.0
        }
} 