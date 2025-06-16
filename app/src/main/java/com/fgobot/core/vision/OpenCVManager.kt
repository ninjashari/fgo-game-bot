/*
 * FGO Bot - OpenCV Manager
 * 
 * This file manages OpenCV initialization, lifecycle, and provides
 * a centralized interface for all OpenCV operations in the FGO Bot.
 * Handles library loading, memory management, and error recovery.
 */

package com.fgobot.core.vision

import android.content.Context
import com.fgobot.core.logging.FGOBotLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.util.concurrent.atomic.AtomicBoolean

/**
 * OpenCV Manager for FGO Bot
 * 
 * Manages OpenCV library initialization, lifecycle, and provides
 * centralized access to OpenCV functionality. Handles:
 * - Library loading and initialization
 * - Memory management and cleanup
 * - Error recovery and fallback mechanisms
 * - Performance monitoring and optimization
 * - Thread-safe operations
 */
class OpenCVManager private constructor(
    private val context: Context,
    private val logger: FGOBotLogger
) {
    
    companion object {
        private const val TAG = "OpenCVManager"
        
        @Volatile
        private var INSTANCE: OpenCVManager? = null
        
        /**
         * Gets the singleton instance of OpenCVManager
         * 
         * @param context Application context
         * @param logger Logger instance
         * @return OpenCVManager instance
         */
        fun getInstance(context: Context, logger: FGOBotLogger): OpenCVManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OpenCVManager(context.applicationContext, logger).also { INSTANCE = it }
            }
        }
    }
    
    // Initialization state
    private val isInitialized = AtomicBoolean(false)
    private val initializationMutex = Mutex()
    
    // Performance metrics
    private var initializationTime: Long = 0
    private var memoryUsage: Long = 0
    
    // Error tracking
    private var lastError: Exception? = null
    private var initializationAttempts: Int = 0
    
    /**
     * Initializes OpenCV library
     * 
     * @return True if initialization successful, false otherwise
     */
    suspend fun initialize(): Boolean {
        return initializationMutex.withLock {
            if (isInitialized.get()) {
                logger.debug(FGOBotLogger.Category.VISION, "OpenCV already initialized")
                return@withLock true
            }
            
            val startTime = System.currentTimeMillis()
            initializationAttempts++
            
            try {
                logger.info(FGOBotLogger.Category.VISION, "Initializing OpenCV (attempt $initializationAttempts)")
                
                // Initialize OpenCV using the new Maven Central distribution
                val success = OpenCVLoader.initLocal()
                
                if (success) {
                    // Verify OpenCV functionality
                    if (verifyOpenCVFunctionality()) {
                        isInitialized.set(true)
                        initializationTime = System.currentTimeMillis() - startTime
                        memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
                        
                        logger.info(
                            FGOBotLogger.Category.VISION,
                            "OpenCV initialized successfully in ${initializationTime}ms"
                        )
                        
                        logOpenCVInfo()
                        return@withLock true
                    } else {
                        logger.error(FGOBotLogger.Category.VISION, "OpenCV functionality verification failed")
                        return@withLock false
                    }
                } else {
                    logger.error(FGOBotLogger.Category.VISION, "OpenCV initialization failed")
                    return@withLock false
                }
                
            } catch (e: Exception) {
                lastError = e
                logger.error(
                    FGOBotLogger.Category.VISION,
                    "OpenCV initialization exception: ${e.message}",
                    e
                )
                return@withLock false
            }
        }
    }
    
    /**
     * Verifies OpenCV functionality after initialization
     * 
     * @return True if OpenCV is working correctly
     */
    private fun verifyOpenCVFunctionality(): Boolean {
        return try {
            // Test basic Mat operations
            val testMat = Mat.zeros(100, 100, CvType.CV_8UC3)
            val rows = testMat.rows()
            val cols = testMat.cols()
            val channels = testMat.channels()
            
            testMat.release()
            
            // Verify expected values
            val isValid = rows == 100 && cols == 100 && channels == 3
            
            if (isValid) {
                logger.debug(FGOBotLogger.Category.VISION, "OpenCV functionality verification passed")
            } else {
                logger.error(
                    FGOBotLogger.Category.VISION,
                    "OpenCV functionality verification failed: rows=$rows, cols=$cols, channels=$channels"
                )
            }
            
            isValid
        } catch (e: Exception) {
            logger.error(
                FGOBotLogger.Category.VISION,
                "OpenCV functionality verification exception: ${e.message}",
                e
            )
            false
        }
    }
    
    /**
     * Logs OpenCV version and configuration information
     */
    private fun logOpenCVInfo() {
        try {
            val version = Core.getVersionString()
            val buildInfo = Core.getBuildInformation()
            
            logger.info(FGOBotLogger.Category.VISION, "OpenCV Version: $version")
            logger.debug(FGOBotLogger.Category.VISION, "OpenCV Build Info: $buildInfo")
            
            // Log performance metrics
            logger.info(
                FGOBotLogger.Category.VISION,
                "OpenCV Performance - Init Time: ${initializationTime}ms, Memory: ${memoryUsage / 1024}KB"
            )
            
        } catch (e: Exception) {
            logger.warn(
                FGOBotLogger.Category.VISION,
                "Could not retrieve OpenCV information: ${e.message}"
            )
        }
    }
    
    /**
     * Checks if OpenCV is initialized and ready for use
     * 
     * @return True if OpenCV is initialized
     */
    fun isReady(): Boolean {
        return isInitialized.get()
    }
    
    /**
     * Gets OpenCV version string
     * 
     * @return OpenCV version or null if not initialized
     */
    fun getVersion(): String? {
        return if (isReady()) {
            try {
                Core.getVersionString()
            } catch (e: Exception) {
                logger.warn(FGOBotLogger.Category.VISION, "Could not get OpenCV version: ${e.message}")
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Gets initialization metrics
     * 
     * @return Map of initialization metrics
     */
    fun getInitializationMetrics(): Map<String, Any> {
        return mapOf(
            "isInitialized" to isInitialized.get(),
            "initializationTime" to initializationTime,
            "memoryUsage" to memoryUsage,
            "attempts" to initializationAttempts,
            "lastError" to (lastError?.message ?: "None"),
            "version" to (getVersion() ?: "Unknown")
        )
    }
    
    /**
     * Performs cleanup and releases OpenCV resources
     */
    fun cleanup() {
        try {
            if (isInitialized.get()) {
                logger.info(FGOBotLogger.Category.VISION, "Cleaning up OpenCV resources")
                
                // Force garbage collection to clean up native memory
                System.gc()
                
                logger.debug(FGOBotLogger.Category.VISION, "OpenCV cleanup completed")
            }
        } catch (e: Exception) {
            logger.error(
                FGOBotLogger.Category.VISION,
                "Error during OpenCV cleanup: ${e.message}",
                e
            )
        }
    }
    
    /**
     * Reinitializes OpenCV (useful for error recovery)
     * 
     * @return True if reinitialization successful
     */
    suspend fun reinitialize(): Boolean {
        logger.info(FGOBotLogger.Category.VISION, "Reinitializing OpenCV")
        
        // Reset state
        isInitialized.set(false)
        lastError = null
        
        // Cleanup first
        cleanup()
        
        // Wait a bit for cleanup to complete
        kotlinx.coroutines.delay(100)
        
        // Reinitialize
        return initialize()
    }
    
    /**
     * Gets memory usage statistics
     * 
     * @return Memory usage information
     */
    fun getMemoryStats(): Map<String, Long> {
        val runtime = Runtime.getRuntime()
        return mapOf(
            "totalMemory" to runtime.totalMemory(),
            "freeMemory" to runtime.freeMemory(),
            "usedMemory" to (runtime.totalMemory() - runtime.freeMemory()),
            "maxMemory" to runtime.maxMemory(),
            "openCVMemory" to memoryUsage
        )
    }
    
    /**
     * Checks if OpenCV is functioning properly
     * 
     * @return True if OpenCV is working correctly
     */
    suspend fun healthCheck(): Boolean {
        return if (isReady()) {
            verifyOpenCVFunctionality()
        } else {
            logger.warn(FGOBotLogger.Category.VISION, "OpenCV health check failed - not initialized")
            false
        }
    }
} 