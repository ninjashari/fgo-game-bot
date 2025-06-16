/*
 * FGO Bot - Analytics Manager
 * 
 * This file provides comprehensive analytics and performance monitoring for the FGO Bot.
 * Tracks data usage, performance metrics, and provides insights for optimization.
 */

package com.fgobot.data.analytics

import com.fgobot.core.logging.FGOLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Analytics event types
 */
enum class AnalyticsEvent {
    DATABASE_QUERY,
    API_REQUEST,
    CACHE_ACCESS,
    SYNC_OPERATION,
    USER_ACTION,
    ERROR_OCCURRED,
    PERFORMANCE_METRIC
}

/**
 * Performance metric data class
 */
data class PerformanceMetric(
    val operation: String,
    val duration: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean = true,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Analytics data point
 */
data class AnalyticsDataPoint(
    val event: AnalyticsEvent,
    val operation: String,
    val duration: Long,
    val timestamp: Long,
    val success: Boolean,
    val metadata: Map<String, Any>
)

/**
 * Analytics summary
 */
data class AnalyticsSummary(
    val totalEvents: Long,
    val successfulEvents: Long,
    val failedEvents: Long,
    val successRate: Double,
    val averageDuration: Long,
    val totalDuration: Long,
    val eventBreakdown: Map<AnalyticsEvent, Long>,
    val topOperations: List<Pair<String, Long>>,
    val performanceMetrics: Map<String, Double>
)

/**
 * Analytics manager interface
 */
interface AnalyticsManager {
    
    /**
     * Current analytics summary
     */
    val analyticsSummary: StateFlow<AnalyticsSummary>
    
    /**
     * Track an analytics event
     */
    suspend fun trackEvent(
        event: AnalyticsEvent,
        operation: String,
        duration: Long = 0L,
        success: Boolean = true,
        metadata: Map<String, Any> = emptyMap()
    )
    
    /**
     * Track a performance metric
     */
    suspend fun trackPerformance(metric: PerformanceMetric)
    
    /**
     * Start tracking an operation
     */
    suspend fun startOperation(operationId: String, operation: String): Long
    
    /**
     * End tracking an operation
     */
    suspend fun endOperation(
        operationId: String,
        startTime: Long,
        success: Boolean = true,
        metadata: Map<String, Any> = emptyMap()
    )
    
    /**
     * Get analytics for a specific event type
     */
    suspend fun getEventAnalytics(event: AnalyticsEvent): EventAnalytics
    
    /**
     * Get performance analytics for an operation
     */
    suspend fun getOperationAnalytics(operation: String): OperationAnalytics
    
    /**
     * Export analytics data
     */
    suspend fun exportAnalytics(): String
    
    /**
     * Clear analytics data
     */
    suspend fun clearAnalytics()
    
    /**
     * Get real-time performance metrics
     */
    suspend fun getRealTimeMetrics(): RealTimeMetrics
}

/**
 * Event analytics data class
 */
data class EventAnalytics(
    val event: AnalyticsEvent,
    val totalCount: Long,
    val successCount: Long,
    val failureCount: Long,
    val successRate: Double,
    val averageDuration: Long,
    val minDuration: Long,
    val maxDuration: Long,
    val recentEvents: List<AnalyticsDataPoint>
)

/**
 * Operation analytics data class
 */
data class OperationAnalytics(
    val operation: String,
    val totalCount: Long,
    val successCount: Long,
    val failureCount: Long,
    val successRate: Double,
    val averageDuration: Long,
    val minDuration: Long,
    val maxDuration: Long,
    val totalDuration: Long,
    val performanceTrend: List<Long>
)

/**
 * Real-time metrics data class
 */
data class RealTimeMetrics(
    val currentOperations: Int,
    val operationsPerSecond: Double,
    val averageResponseTime: Long,
    val errorRate: Double,
    val memoryUsage: Long,
    val cacheHitRate: Double
)

/**
 * Analytics manager implementation
 */
class AnalyticsManagerImpl(
    private val logger: FGOLogger
) : AnalyticsManager {
    
    companion object {
        private const val TAG = "AnalyticsManager"
        private const val MAX_EVENTS_STORED = 10000
        private const val MAX_RECENT_EVENTS = 100
    }
    
    private val mutex = Mutex()
    private val events = mutableListOf<AnalyticsDataPoint>()
    private val operationCounters = ConcurrentHashMap<String, AtomicLong>()
    private val eventCounters = ConcurrentHashMap<AnalyticsEvent, AtomicLong>()
    private val activeOperations = ConcurrentHashMap<String, Pair<String, Long>>()
    
    private val _analyticsSummary = MutableStateFlow(AnalyticsSummary(
        totalEvents = 0L,
        successfulEvents = 0L,
        failedEvents = 0L,
        successRate = 0.0,
        averageDuration = 0L,
        totalDuration = 0L,
        eventBreakdown = emptyMap(),
        topOperations = emptyList(),
        performanceMetrics = emptyMap()
    ))
    override val analyticsSummary: StateFlow<AnalyticsSummary> = _analyticsSummary.asStateFlow()
    
    override suspend fun trackEvent(
        event: AnalyticsEvent,
        operation: String,
        duration: Long,
        success: Boolean,
        metadata: Map<String, Any>
    ) {
        mutex.withLock {
            val dataPoint = AnalyticsDataPoint(
                event = event,
                operation = operation,
                duration = duration,
                timestamp = System.currentTimeMillis(),
                success = success,
                metadata = metadata
            )
            
            // Add to events list
            events.add(dataPoint)
            
            // Maintain max events limit
            if (events.size > MAX_EVENTS_STORED) {
                events.removeAt(0)
            }
            
            // Update counters
            operationCounters.computeIfAbsent(operation) { AtomicLong(0) }.incrementAndGet()
            eventCounters.computeIfAbsent(event) { AtomicLong(0) }.incrementAndGet()
            
            // Update summary
            updateAnalyticsSummary()
            
            logger.d(TAG, "Tracked event: $event, operation: $operation, duration: ${duration}ms, success: $success")
        }
    }
    
    override suspend fun trackPerformance(metric: PerformanceMetric) {
        trackEvent(
            event = AnalyticsEvent.PERFORMANCE_METRIC,
            operation = metric.operation,
            duration = metric.duration,
            success = metric.success,
            metadata = metric.metadata
        )
    }
    
    override suspend fun startOperation(operationId: String, operation: String): Long {
        val startTime = System.currentTimeMillis()
        activeOperations[operationId] = Pair(operation, startTime)
        logger.d(TAG, "Started operation: $operationId ($operation)")
        return startTime
    }
    
    override suspend fun endOperation(
        operationId: String,
        startTime: Long,
        success: Boolean,
        metadata: Map<String, Any>
    ) {
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        val operationInfo = activeOperations.remove(operationId)
        if (operationInfo != null) {
            val (operation, _) = operationInfo
            
            trackEvent(
                event = AnalyticsEvent.USER_ACTION,
                operation = operation,
                duration = duration,
                success = success,
                metadata = metadata
            )
            
            logger.d(TAG, "Ended operation: $operationId ($operation), duration: ${duration}ms, success: $success")
        } else {
            logger.w(TAG, "Attempted to end unknown operation: $operationId")
        }
    }
    
    override suspend fun getEventAnalytics(event: AnalyticsEvent): EventAnalytics {
        return mutex.withLock {
            val eventData = events.filter { it.event == event }
            val totalCount = eventData.size.toLong()
            val successCount = eventData.count { it.success }.toLong()
            val failureCount = totalCount - successCount
            val successRate = if (totalCount > 0) successCount.toDouble() / totalCount * 100 else 0.0
            
            val durations = eventData.map { it.duration }
            val averageDuration = if (durations.isNotEmpty()) durations.average().toLong() else 0L
            val minDuration = durations.minOrNull() ?: 0L
            val maxDuration = durations.maxOrNull() ?: 0L
            
            val recentEvents = eventData.takeLast(MAX_RECENT_EVENTS)
            
            EventAnalytics(
                event = event,
                totalCount = totalCount,
                successCount = successCount,
                failureCount = failureCount,
                successRate = successRate,
                averageDuration = averageDuration,
                minDuration = minDuration,
                maxDuration = maxDuration,
                recentEvents = recentEvents
            )
        }
    }
    
    override suspend fun getOperationAnalytics(operation: String): OperationAnalytics {
        return mutex.withLock {
            val operationData = events.filter { it.operation == operation }
            val totalCount = operationData.size.toLong()
            val successCount = operationData.count { it.success }.toLong()
            val failureCount = totalCount - successCount
            val successRate = if (totalCount > 0) successCount.toDouble() / totalCount * 100 else 0.0
            
            val durations = operationData.map { it.duration }
            val averageDuration = if (durations.isNotEmpty()) durations.average().toLong() else 0L
            val minDuration = durations.minOrNull() ?: 0L
            val maxDuration = durations.maxOrNull() ?: 0L
            val totalDuration = durations.sum()
            
            // Performance trend (last 10 operations)
            val performanceTrend = operationData.takeLast(10).map { it.duration }
            
            OperationAnalytics(
                operation = operation,
                totalCount = totalCount,
                successCount = successCount,
                failureCount = failureCount,
                successRate = successRate,
                averageDuration = averageDuration,
                minDuration = minDuration,
                maxDuration = maxDuration,
                totalDuration = totalDuration,
                performanceTrend = performanceTrend
            )
        }
    }
    
    override suspend fun exportAnalytics(): String {
        return mutex.withLock {
            val summary = _analyticsSummary.value
            
            buildString {
                appendLine("FGO Bot Analytics Report")
                appendLine("Generated: ${java.util.Date()}")
                appendLine("=".repeat(50))
                appendLine()
                
                appendLine("Summary:")
                appendLine("Total Events: ${summary.totalEvents}")
                appendLine("Successful Events: ${summary.successfulEvents}")
                appendLine("Failed Events: ${summary.failedEvents}")
                appendLine("Success Rate: ${"%.2f".format(summary.successRate)}%")
                appendLine("Average Duration: ${summary.averageDuration}ms")
                appendLine("Total Duration: ${summary.totalDuration}ms")
                appendLine()
                
                appendLine("Event Breakdown:")
                summary.eventBreakdown.forEach { (event, count) ->
                    appendLine("  $event: $count")
                }
                appendLine()
                
                appendLine("Top Operations:")
                summary.topOperations.take(10).forEach { (operation, count) ->
                    appendLine("  $operation: $count")
                }
                appendLine()
                
                appendLine("Performance Metrics:")
                summary.performanceMetrics.forEach { (metric, value) ->
                    appendLine("  $metric: $value")
                }
            }
        }
    }
    
    override suspend fun clearAnalytics() {
        mutex.withLock {
            events.clear()
            operationCounters.clear()
            eventCounters.clear()
            activeOperations.clear()
            
            _analyticsSummary.value = AnalyticsSummary(
                totalEvents = 0L,
                successfulEvents = 0L,
                failedEvents = 0L,
                successRate = 0.0,
                averageDuration = 0L,
                totalDuration = 0L,
                eventBreakdown = emptyMap(),
                topOperations = emptyList(),
                performanceMetrics = emptyMap()
            )
            
            logger.i(TAG, "Analytics data cleared")
        }
    }
    
    override suspend fun getRealTimeMetrics(): RealTimeMetrics {
        val currentTime = System.currentTimeMillis()
        val recentEvents = events.filter { currentTime - it.timestamp < 60000 } // Last minute
        
        val currentOperations = activeOperations.size
        val operationsPerSecond = recentEvents.size / 60.0
        val averageResponseTime = if (recentEvents.isNotEmpty()) {
            recentEvents.map { it.duration }.average().toLong()
        } else 0L
        
        val errorRate = if (recentEvents.isNotEmpty()) {
            recentEvents.count { !it.success }.toDouble() / recentEvents.size * 100
        } else 0.0
        
        // Mock values for memory usage and cache hit rate
        val memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val cacheHitRate = 85.0 // This would come from cache manager
        
        return RealTimeMetrics(
            currentOperations = currentOperations,
            operationsPerSecond = operationsPerSecond,
            averageResponseTime = averageResponseTime,
            errorRate = errorRate,
            memoryUsage = memoryUsage,
            cacheHitRate = cacheHitRate
        )
    }
    
    /**
     * Updates the analytics summary
     */
    private fun updateAnalyticsSummary() {
        val totalEvents = events.size.toLong()
        val successfulEvents = events.count { it.success }.toLong()
        val failedEvents = totalEvents - successfulEvents
        val successRate = if (totalEvents > 0) successfulEvents.toDouble() / totalEvents * 100 else 0.0
        
        val durations = events.map { it.duration }
        val averageDuration = if (durations.isNotEmpty()) durations.average().toLong() else 0L
        val totalDuration = durations.sum()
        
        val eventBreakdown = events.groupBy { it.event }.mapValues { it.value.size.toLong() }
        val topOperations = events.groupBy { it.operation }
            .mapValues { it.value.size.toLong() }
            .toList()
            .sortedByDescending { it.second }
            .take(10)
        
        val performanceMetrics = mapOf(
            "avg_response_time" to averageDuration.toDouble(),
            "success_rate" to successRate,
            "events_per_hour" to if (totalEvents > 0) totalEvents * 3600.0 / (System.currentTimeMillis() / 1000.0) else 0.0
        )
        
        _analyticsSummary.value = AnalyticsSummary(
            totalEvents = totalEvents,
            successfulEvents = successfulEvents,
            failedEvents = failedEvents,
            successRate = successRate,
            averageDuration = averageDuration,
            totalDuration = totalDuration,
            eventBreakdown = eventBreakdown,
            topOperations = topOperations,
            performanceMetrics = performanceMetrics
        )
    }
} 