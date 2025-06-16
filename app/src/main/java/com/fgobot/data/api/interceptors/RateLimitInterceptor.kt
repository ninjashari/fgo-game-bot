/*
 * FGO Bot - Rate Limiting Interceptor
 * 
 * This file implements rate limiting for API requests to respect Atlas Academy API guidelines.
 * Uses token bucket algorithm to control request frequency and prevent API abuse.
 */

package com.fgobot.data.api.interceptors

import com.fgobot.core.logging.FGOBotLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Rate Limiting Interceptor
 * 
 * Implements rate limiting using token bucket algorithm to:
 * - Respect Atlas Academy API rate limits
 * - Prevent API abuse and potential blocking
 * - Provide smooth request distribution over time
 * - Handle burst requests gracefully
 * - Log rate limiting events for monitoring
 */
class RateLimitInterceptor(
    private val maxRequests: Int,
    private val windowMs: Long,
    private val logger: FGOBotLogger
) : Interceptor {
    
    // Token bucket implementation
    private val availableTokens = AtomicInteger(maxRequests)
    private val lastRefillTime = AtomicLong(System.currentTimeMillis())
    private val totalRequestsBlocked = AtomicLong(0)
    private val totalWaitTime = AtomicLong(0)
    
    // Rate limiting statistics
    private val requestsInCurrentWindow = AtomicInteger(0)
    private val windowStartTime = AtomicLong(System.currentTimeMillis())
    
    /**
     * Intercepts requests to apply rate limiting
     * 
     * @param chain Interceptor chain
     * @return HTTP response after rate limiting check
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val startTime = System.currentTimeMillis()
        
        // Wait for available token
        val waitTime = waitForToken()
        
        if (waitTime > 0) {
            totalRequestsBlocked.incrementAndGet()
            totalWaitTime.addAndGet(waitTime)
            
            logger.debug(FGOBotLogger.Category.NETWORK, "Rate limit applied: waited ${waitTime}ms for token")
        }
        
        // Update request statistics
        updateRequestStats()
        
        // Proceed with the request
        return chain.proceed(chain.request())
    }
    
    /**
     * Waits for an available token using token bucket algorithm
     * 
     * @return Wait time in milliseconds
     */
    private fun waitForToken(): Long {
        val startWaitTime = System.currentTimeMillis()
        
        runBlocking {
            while (true) {
                // Refill tokens based on elapsed time
                refillTokens()
                
                // Try to acquire a token
                if (availableTokens.get() > 0 && availableTokens.decrementAndGet() >= 0) {
                    break
                }
                
                // No tokens available, wait and try again
                val waitInterval = calculateWaitInterval()
                delay(waitInterval)
                
                // Prevent infinite waiting
                val totalWaitTime = System.currentTimeMillis() - startWaitTime
                if (totalWaitTime > windowMs) {
                    logger.error(FGOBotLogger.Category.NETWORK, "Rate limiter timeout after ${totalWaitTime}ms")
                    break
                }
            }
        }
        
        return System.currentTimeMillis() - startWaitTime
    }
    
    /**
     * Refills tokens based on elapsed time since last refill
     */
    private fun refillTokens() {
        val currentTime = System.currentTimeMillis()
        val lastRefill = lastRefillTime.get()
        val timeSinceRefill = currentTime - lastRefill
        
        if (timeSinceRefill >= windowMs) {
            // Full window has passed, refill all tokens
            val tokensToAdd = maxRequests - availableTokens.get()
            if (tokensToAdd > 0) {
                availableTokens.addAndGet(tokensToAdd)
                lastRefillTime.set(currentTime)
                
                logger.debug(FGOBotLogger.Category.NETWORK, "Rate limiter: refilled $tokensToAdd tokens")
            }
        } else {
            // Partial refill based on elapsed time
            val tokensToAdd = ((timeSinceRefill.toDouble() / windowMs) * maxRequests).toInt()
            if (tokensToAdd > 0) {
                val currentTokens = availableTokens.get()
                val newTokens = minOf(currentTokens + tokensToAdd, maxRequests)
                if (newTokens > currentTokens) {
                    availableTokens.set(newTokens)
                    lastRefillTime.set(currentTime)
                }
            }
        }
    }
    
    /**
     * Calculates optimal wait interval when no tokens are available
     * 
     * @return Wait interval in milliseconds
     */
    private fun calculateWaitInterval(): Long {
        val tokensNeeded = 1
        val refillRate = maxRequests.toDouble() / windowMs
        val timeForTokens = (tokensNeeded / refillRate).toLong()
        
        // Add small buffer to avoid tight polling
        return maxOf(timeForTokens, 100L)
    }
    
    /**
     * Updates request statistics for monitoring
     */
    private fun updateRequestStats() {
        val currentTime = System.currentTimeMillis()
        val windowStart = windowStartTime.get()
        
        // Reset window if it has expired
        if (currentTime - windowStart >= windowMs) {
            requestsInCurrentWindow.set(0)
            windowStartTime.set(currentTime)
        }
        
        requestsInCurrentWindow.incrementAndGet()
    }
    
    /**
     * Gets current rate limiting statistics
     * 
     * @return Rate limiting statistics
     */
    fun getRateLimitStats(): RateLimitStats {
        val currentTime = System.currentTimeMillis()
        val windowStart = windowStartTime.get()
        val windowAge = currentTime - windowStart
        
        return RateLimitStats(
            maxRequestsPerWindow = maxRequests,
            windowSizeMs = windowMs,
            availableTokens = availableTokens.get(),
            requestsInCurrentWindow = requestsInCurrentWindow.get(),
            currentWindowAgeMs = windowAge,
            totalRequestsBlocked = totalRequestsBlocked.get(),
            totalWaitTimeMs = totalWaitTime.get(),
            averageWaitTimeMs = if (totalRequestsBlocked.get() > 0) {
                totalWaitTime.get() / totalRequestsBlocked.get()
            } else {
                0L
            }
        )
    }
    
    /**
     * Resets rate limiting statistics
     * 
     * Useful for testing or periodic statistics reporting
     */
    fun resetStats() {
        totalRequestsBlocked.set(0)
        totalWaitTime.set(0)
        requestsInCurrentWindow.set(0)
        windowStartTime.set(System.currentTimeMillis())
        
        logger.info(FGOBotLogger.Category.NETWORK, "Rate limiter statistics reset")
    }
    
    /**
     * Gets formatted statistics summary
     * 
     * @return Human-readable statistics summary
     */
    fun getStatsSummary(): String {
        val stats = getRateLimitStats()
        return buildString {
            appendLine("=== Rate Limiting Statistics ===")
            appendLine("Max Requests per Window: ${stats.maxRequestsPerWindow}")
            appendLine("Window Size: ${stats.windowSizeMs}ms")
            appendLine("Available Tokens: ${stats.availableTokens}")
            appendLine("Requests in Current Window: ${stats.requestsInCurrentWindow}")
            appendLine("Current Window Age: ${stats.currentWindowAgeMs}ms")
            appendLine("Total Requests Blocked: ${stats.totalRequestsBlocked}")
            appendLine("Total Wait Time: ${stats.totalWaitTimeMs}ms")
            appendLine("Average Wait Time: ${stats.averageWaitTimeMs}ms")
            
            val utilizationRate = if (stats.windowSizeMs > 0) {
                (stats.requestsInCurrentWindow.toDouble() / stats.maxRequestsPerWindow) * 100
            } else {
                0.0
            }
            appendLine("Current Utilization: ${String.format("%.1f", utilizationRate)}%")
        }
    }
    
    /**
     * Checks if rate limiter is currently throttling requests
     * 
     * @return True if requests are being throttled
     */
    fun isThrottling(): Boolean {
        return availableTokens.get() <= 0
    }
    
    /**
     * Gets estimated time until next token is available
     * 
     * @return Estimated wait time in milliseconds
     */
    fun getEstimatedWaitTime(): Long {
        return if (availableTokens.get() > 0) {
            0L
        } else {
            calculateWaitInterval()
        }
    }
}

/**
 * Rate limiting statistics data class
 */
data class RateLimitStats(
    val maxRequestsPerWindow: Int,
    val windowSizeMs: Long,
    val availableTokens: Int,
    val requestsInCurrentWindow: Int,
    val currentWindowAgeMs: Long,
    val totalRequestsBlocked: Long,
    val totalWaitTimeMs: Long,
    val averageWaitTimeMs: Long
) {
    /**
     * Gets current utilization rate as percentage
     */
    val utilizationRate: Double
        get() = if (maxRequestsPerWindow > 0) {
            (requestsInCurrentWindow.toDouble() / maxRequestsPerWindow) * 100
        } else {
            0.0
        }
    
    /**
     * Checks if rate limiter is near capacity
     */
    val isNearCapacity: Boolean
        get() = utilizationRate > 80.0
    
    /**
     * Checks if rate limiter is at capacity
     */
    val isAtCapacity: Boolean
        get() = availableTokens <= 0
} 