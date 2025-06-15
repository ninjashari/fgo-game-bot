/*
 * FGO Bot - Network Monitoring Interceptor
 * 
 * This file implements network request monitoring and performance analytics.
 * Tracks request/response metrics, network availability, and error statistics.
 */

package com.fgobot.data.api.interceptors

import com.fgobot.core.logging.FGOBotLogger
import com.fgobot.data.api.NetworkStats
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

/**
 * Network Monitoring Interceptor
 * 
 * Monitors network requests and provides performance analytics including:
 * - Request/response timing
 * - Success/failure rates
 * - Data transfer statistics
 * - Network availability checking
 * - Error tracking and reporting
 */
class NetworkInterceptor(
    private val logger: FGOBotLogger
) : Interceptor {
    
    // Network statistics tracking
    private val totalRequests = AtomicLong(0)
    private val successfulRequests = AtomicLong(0)
    private val failedRequests = AtomicLong(0)
    private val totalResponseTime = AtomicLong(0)
    private val totalBytesReceived = AtomicLong(0)
    private val totalBytesSent = AtomicLong(0)
    
    // Network availability
    @Volatile
    private var isNetworkAvailable = true
    
    /**
     * Intercepts network requests to monitor performance and collect statistics
     * 
     * @param chain Interceptor chain
     * @return HTTP response
     * @throws IOException Network or HTTP errors
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()
        
        // Increment total request counter
        totalRequests.incrementAndGet()
        
        // Track request size
        val requestBodySize = request.body?.contentLength() ?: 0
        if (requestBodySize > 0) {
            totalBytesSent.addAndGet(requestBodySize)
        }
        
        // Log request details
        logger.debug(FGOBotLogger.Category.NETWORK, "Network Request: ${request.method} ${request.url}")
        
        try {
            // Execute the request
            val response = chain.proceed(request)
            val endTime = System.currentTimeMillis()
            val responseTime = endTime - startTime
            
            // Update statistics
            totalResponseTime.addAndGet(responseTime)
            
            // Track response size
            val responseBodySize = response.body?.contentLength() ?: 0
            if (responseBodySize > 0) {
                totalBytesReceived.addAndGet(responseBodySize)
            }
            
            // Check if request was successful
            if (response.isSuccessful) {
                successfulRequests.incrementAndGet()
                isNetworkAvailable = true
                
                logger.debug(FGOBotLogger.Category.NETWORK, 
                    "Network Response: ${response.code} ${response.message} " +
                    "(${responseTime}ms, ${responseBodySize} bytes)"
                )
            } else {
                failedRequests.incrementAndGet()
                
                logger.warn(FGOBotLogger.Category.NETWORK,
                    "Network Error Response: ${response.code} ${response.message} " +
                    "(${responseTime}ms)"
                )
            }
            
            return response
            
        } catch (e: IOException) {
            val endTime = System.currentTimeMillis()
            val responseTime = endTime - startTime
            
            // Update failure statistics
            failedRequests.incrementAndGet()
            totalResponseTime.addAndGet(responseTime)
            isNetworkAvailable = false
            
            // Log network error
            logger.error(FGOBotLogger.Category.NETWORK, "Network Exception: ${e.message} (${responseTime}ms)", e)
            
            // Re-throw the exception
            throw e
        }
    }
    
    /**
     * Checks if network is currently available
     * 
     * @return True if network is available
     */
    fun isNetworkAvailable(): Boolean {
        return isNetworkAvailable
    }
    
    /**
     * Gets comprehensive network statistics
     * 
     * @return Network performance statistics
     */
    fun getNetworkStats(): NetworkStats {
        val totalReqs = totalRequests.get()
        val avgResponseTime = if (totalReqs > 0) {
            totalResponseTime.get() / totalReqs
        } else {
            0L
        }
        
        return NetworkStats(
            totalRequests = totalReqs,
            successfulRequests = successfulRequests.get(),
            failedRequests = failedRequests.get(),
            averageResponseTime = avgResponseTime,
            totalBytesReceived = totalBytesReceived.get(),
            totalBytesSent = totalBytesSent.get()
        )
    }
    
    /**
     * Resets all network statistics
     * 
     * Useful for periodic statistics reporting or testing
     */
    fun resetStats() {
        totalRequests.set(0)
        successfulRequests.set(0)
        failedRequests.set(0)
        totalResponseTime.set(0)
        totalBytesReceived.set(0)
        totalBytesSent.set(0)
        
        logger.info(FGOBotLogger.Category.NETWORK, "Network statistics reset")
    }
    
    /**
     * Gets formatted statistics summary
     * 
     * @return Human-readable statistics summary
     */
    fun getStatsSummary(): String {
        val stats = getNetworkStats()
        return buildString {
            appendLine("=== Network Statistics ===")
            appendLine("Total Requests: ${stats.totalRequests}")
            appendLine("Successful: ${stats.successfulRequests} (${String.format("%.1f", stats.successRate)}%)")
            appendLine("Failed: ${stats.failedRequests} (${String.format("%.1f", stats.failureRate)}%)")
            appendLine("Average Response Time: ${stats.averageResponseTime}ms")
            appendLine("Data Received: ${formatBytes(stats.totalBytesReceived)}")
            appendLine("Data Sent: ${formatBytes(stats.totalBytesSent)}")
            appendLine("Network Available: $isNetworkAvailable")
        }
    }
    
    /**
     * Formats byte count into human-readable format
     * 
     * @param bytes Byte count
     * @return Formatted byte string (e.g., "1.5 MB")
     */
    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return String.format("%.1f %s", size, units[unitIndex])
    }
} 