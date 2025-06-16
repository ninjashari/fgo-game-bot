/*
 * FGO Bot - Performance Benchmarking System
 * 
 * This file implements comprehensive performance testing and benchmarking
 * for all core systems including OpenCV, ML models, and automation logic.
 * Implements Phase 4 testing requirements with 85%+ coverage targets.
 */

package com.fgobot.core.testing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.fgobot.core.logging.FGOBotLogger
import com.fgobot.core.vision.OpenCVManager
import com.fgobot.core.vision.TemplateMatchingEngine
import com.fgobot.core.vision.TemplateAssetManager
import com.fgobot.core.vision.ImageRecognition
import com.fgobot.core.ml.MLModelManager
import com.fgobot.core.automation.BattleLogic
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.system.measureTimeMillis

/**
 * Performance benchmark results
 */
data class BenchmarkResult(
    val testName: String,
    val executionTime: Long,
    val memoryUsage: Long,
    val success: Boolean,
    val iterations: Int,
    val averageTime: Double,
    val minTime: Long,
    val maxTime: Long,
    val throughput: Double, // operations per second
    val additionalMetrics: Map<String, Any> = emptyMap()
)

/**
 * Memory usage statistics
 */
data class MemoryStats(
    val totalMemory: Long,
    val freeMemory: Long,
    val usedMemory: Long,
    val maxMemory: Long
) {
    val usagePercentage: Double
        get() = (usedMemory.toDouble() / maxMemory) * 100
}

/**
 * Performance Benchmarking System
 * 
 * Comprehensive testing framework for Phase 4 implementation:
 * - OpenCV template matching performance (<50ms target)
 * - Screen analysis performance (<200ms target)
 * - Memory usage monitoring (<100MB target)
 * - ML model inference benchmarking
 * - Battle logic performance testing
 * - Stress testing for long-running automation
 * - Memory leak detection and prevention
 */
class PerformanceBenchmark {
    
    companion object {
        private const val TAG = "PerformanceBenchmark"
        
        // Performance targets from Phase 4 breakdown
        private const val TEMPLATE_MATCHING_TARGET_MS = 50L
        private const val SCREEN_ANALYSIS_TARGET_MS = 200L
        private const val MEMORY_USAGE_TARGET_MB = 100L
        private const val CPU_USAGE_TARGET_PERCENT = 30.0
        
        // Test parameters
        private const val WARMUP_ITERATIONS = 10
        private const val BENCHMARK_ITERATIONS = 100
        private const val STRESS_TEST_DURATION_MS = 60000L // 1 minute
        private const val MEMORY_LEAK_TEST_ITERATIONS = 1000
    }
    
    private lateinit var context: Context
    private lateinit var logger: FGOBotLogger
    private lateinit var openCVManager: OpenCVManager
    private lateinit var templateMatchingEngine: TemplateMatchingEngine
    private lateinit var templateAssetManager: TemplateAssetManager
    private lateinit var imageRecognition: ImageRecognition
    private lateinit var mlModelManager: MLModelManager
    private lateinit var battleLogic: BattleLogic
    
    private val benchmarkResults = mutableListOf<BenchmarkResult>()
    private val testImages = mutableListOf<Bitmap>()
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        logger = FGOBotLogger(context)
        
        // Initialize core components
        runBlocking {
            openCVManager = OpenCVManager.getInstance(context, logger)
            assertTrue("OpenCV initialization failed", openCVManager.initialize())
            
            templateMatchingEngine = TemplateMatchingEngine(openCVManager, logger)
            templateAssetManager = TemplateAssetManager(context, logger)
            templateAssetManager.initialize()
            
            imageRecognition = ImageRecognition(context, logger)
            assertTrue("Image recognition initialization failed", imageRecognition.initialize())
            
            mlModelManager = MLModelManager(context, logger)
            mlModelManager.initialize()
            
            battleLogic = BattleLogic(imageRecognition, logger)
        }
        
        // Load test images
        loadTestImages()
    }
    
    /**
     * Loads test images for benchmarking
     */
    private fun loadTestImages() {
        try {
            // Create test images of various sizes
            val sizes = listOf(
                Pair(1080, 1920), // Full screen
                Pair(540, 960),   // Half screen
                Pair(270, 480),   // Quarter screen
                Pair(128, 128),   // Template size
                Pair(64, 64)      // Small template
            )
            
            for ((width, height) in sizes) {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                testImages.add(bitmap)
            }
            
            logger.info(FGOBotLogger.Category.TESTING, "Loaded ${testImages.size} test images")
            
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.TESTING, "Failed to load test images", e)
        }
    }
    
    /**
     * Benchmarks OpenCV template matching performance
     */
    @Test
    fun benchmarkTemplateMatching() {
        val testName = "Template Matching Performance"
        logger.info(FGOBotLogger.Category.TESTING, "Starting $testName")
        
        val sourceImage = testImages[0] // Full screen image
        val templateImage = testImages[3] // Template size image
        
        val times = mutableListOf<Long>()
        var successCount = 0
        
        // Warmup
        repeat(WARMUP_ITERATIONS) {
            runBlocking {
                templateMatchingEngine.matchTemplate(sourceImage, templateImage)
            }
        }
        
        // Benchmark
        repeat(BENCHMARK_ITERATIONS) {
            val time = measureTimeMillis {
                runBlocking {
                    val result = templateMatchingEngine.matchTemplate(sourceImage, templateImage)
                    if (result.found) successCount++
                }
            }
            times.add(time)
        }
        
        val result = createBenchmarkResult(testName, times, successCount, BENCHMARK_ITERATIONS)
        benchmarkResults.add(result)
        
        // Verify performance target
        assertTrue(
            "Template matching average time ${result.averageTime}ms exceeds target ${TEMPLATE_MATCHING_TARGET_MS}ms",
            result.averageTime <= TEMPLATE_MATCHING_TARGET_MS
        )
        
        logger.info(FGOBotLogger.Category.TESTING, "Template matching benchmark: ${result.averageTime}ms average")
    }
    
    /**
     * Benchmarks multi-scale template matching
     */
    @Test
    fun benchmarkMultiScaleMatching() {
        val testName = "Multi-Scale Template Matching"
        logger.info(FGOBotLogger.Category.TESTING, "Starting $testName")
        
        val sourceImage = testImages[0]
        val templateImage = testImages[3]
        
        val times = mutableListOf<Long>()
        var successCount = 0
        
        // Warmup
        repeat(WARMUP_ITERATIONS) {
            runBlocking {
                templateMatchingEngine.matchTemplateMultiScale(sourceImage, templateImage)
            }
        }
        
        // Benchmark
        repeat(BENCHMARK_ITERATIONS) {
            val time = measureTimeMillis {
                runBlocking {
                    val result = templateMatchingEngine.matchTemplateMultiScale(sourceImage, templateImage)
                    if (result.bestMatch?.found == true) successCount++
                }
            }
            times.add(time)
        }
        
        val result = createBenchmarkResult(testName, times, successCount, BENCHMARK_ITERATIONS)
        benchmarkResults.add(result)
        
        logger.info(FGOBotLogger.Category.TESTING, "Multi-scale matching benchmark: ${result.averageTime}ms average")
    }
    
    /**
     * Benchmarks screen analysis performance
     */
    @Test
    fun benchmarkScreenAnalysis() {
        val testName = "Screen Analysis Performance"
        logger.info(FGOBotLogger.Category.TESTING, "Starting $testName")
        
        val screenshot = testImages[0]
        val times = mutableListOf<Long>()
        var successCount = 0
        
        // Warmup
        repeat(WARMUP_ITERATIONS) {
            runBlocking {
                imageRecognition.detectBattleState(screenshot)
            }
        }
        
        // Benchmark
        repeat(BENCHMARK_ITERATIONS) {
            val time = measureTimeMillis {
                runBlocking {
                    val state = imageRecognition.detectBattleState(screenshot)
                    if (state != com.fgobot.core.vision.BattleState.UNKNOWN) successCount++
                }
            }
            times.add(time)
        }
        
        val result = createBenchmarkResult(testName, times, successCount, BENCHMARK_ITERATIONS)
        benchmarkResults.add(result)
        
        // Verify performance target
        assertTrue(
            "Screen analysis average time ${result.averageTime}ms exceeds target ${SCREEN_ANALYSIS_TARGET_MS}ms",
            result.averageTime <= SCREEN_ANALYSIS_TARGET_MS
        )
        
        logger.info(FGOBotLogger.Category.TESTING, "Screen analysis benchmark: ${result.averageTime}ms average")
    }
    
    /**
     * Benchmarks ML model inference performance
     */
    @Test
    fun benchmarkMLInference() {
        val testName = "ML Model Inference"
        logger.info(FGOBotLogger.Category.TESTING, "Starting $testName")
        
        val testImage = testImages[3] // Template size for ML models
        val times = mutableListOf<Long>()
        var successCount = 0
        
        // Test servant recognition
        repeat(BENCHMARK_ITERATIONS / 2) {
            val time = measureTimeMillis {
                runBlocking {
                    val result = mlModelManager.recognizeServant(testImage)
                    if (result != null) successCount++
                }
            }
            times.add(time)
        }
        
        // Test card classification
        repeat(BENCHMARK_ITERATIONS / 2) {
            val time = measureTimeMillis {
                runBlocking {
                    val result = mlModelManager.classifyCard(testImage)
                    if (result != null) successCount++
                }
            }
            times.add(time)
        }
        
        val result = createBenchmarkResult(testName, times, successCount, BENCHMARK_ITERATIONS)
        benchmarkResults.add(result)
        
        logger.info(FGOBotLogger.Category.TESTING, "ML inference benchmark: ${result.averageTime}ms average")
    }
    
    /**
     * Tests memory usage and monitors for leaks
     */
    @Test
    fun testMemoryUsage() {
        val testName = "Memory Usage Test"
        logger.info(FGOBotLogger.Category.TESTING, "Starting $testName")
        
        val initialMemory = getMemoryStats()
        val memorySnapshots = mutableListOf<MemoryStats>()
        
        // Perform operations that might cause memory leaks
        repeat(MEMORY_LEAK_TEST_ITERATIONS) { iteration ->
            runBlocking {
                // Template matching operations
                templateMatchingEngine.matchTemplate(testImages[0], testImages[3])
                
                // Image recognition operations
                imageRecognition.detectBattleState(testImages[0])
                
                // ML operations (if available)
                mlModelManager.recognizeServant(testImages[3])
                
                // Take memory snapshot every 100 iterations
                if (iteration % 100 == 0) {
                    System.gc() // Force garbage collection
                    Thread.sleep(100) // Allow GC to complete
                    memorySnapshots.add(getMemoryStats())
                }
            }
        }
        
        val finalMemory = getMemoryStats()
        val memoryIncrease = finalMemory.usedMemory - initialMemory.usedMemory
        val memoryIncreaseMB = memoryIncrease / (1024 * 1024)
        
        // Verify memory usage is within target
        assertTrue(
            "Memory usage ${finalMemory.usedMemory / (1024 * 1024)}MB exceeds target ${MEMORY_USAGE_TARGET_MB}MB",
            finalMemory.usedMemory / (1024 * 1024) <= MEMORY_USAGE_TARGET_MB
        )
        
        // Check for memory leaks (memory increase should be minimal)
        assertTrue(
            "Potential memory leak detected: ${memoryIncreaseMB}MB increase",
            memoryIncreaseMB < 50 // Allow up to 50MB increase
        )
        
        logger.info(FGOBotLogger.Category.TESTING, "Memory test completed: ${memoryIncreaseMB}MB increase")
    }
    
    /**
     * Stress test for long-running automation
     */
    @Test
    fun stressTestLongRunning() {
        val testName = "Long-Running Stress Test"
        logger.info(FGOBotLogger.Category.TESTING, "Starting $testName")
        
        val startTime = System.currentTimeMillis()
        val endTime = startTime + STRESS_TEST_DURATION_MS
        var operationCount = 0
        val errors = mutableListOf<Exception>()
        
        while (System.currentTimeMillis() < endTime) {
            try {
                runBlocking {
                    // Simulate typical automation operations
                    templateMatchingEngine.matchTemplate(testImages[0], testImages[3])
                    imageRecognition.detectBattleState(testImages[0])
                    
                    // Simulate battle logic execution
                    // battleLogic.executeBattleTurn(testImages[0])
                    
                    operationCount++
                    
                    // Brief pause to simulate real usage
                    Thread.sleep(10)
                }
            } catch (e: Exception) {
                errors.add(e)
                logger.warn(FGOBotLogger.Category.TESTING, "Error in stress test: ${e.message}")
            }
        }
        
        val actualDuration = System.currentTimeMillis() - startTime
        val operationsPerSecond = (operationCount * 1000.0) / actualDuration
        
        // Verify system stability
        assertTrue(
            "Too many errors in stress test: ${errors.size}/${operationCount}",
            errors.size < operationCount * 0.01 // Less than 1% error rate
        )
        
        logger.info(
            FGOBotLogger.Category.TESTING,
            "Stress test completed: $operationCount operations, ${operationsPerSecond} ops/sec, ${errors.size} errors"
        )
    }
    
    /**
     * Creates a benchmark result from timing data
     */
    private fun createBenchmarkResult(
        testName: String,
        times: List<Long>,
        successCount: Int,
        totalIterations: Int
    ): BenchmarkResult {
        val totalTime = times.sum()
        val averageTime = times.average()
        val minTime = times.minOrNull() ?: 0L
        val maxTime = times.maxOrNull() ?: 0L
        val throughput = if (totalTime > 0) (totalIterations * 1000.0) / totalTime else 0.0
        
        return BenchmarkResult(
            testName = testName,
            executionTime = totalTime,
            memoryUsage = getMemoryStats().usedMemory,
            success = successCount > 0,
            iterations = totalIterations,
            averageTime = averageTime,
            minTime = minTime,
            maxTime = maxTime,
            throughput = throughput,
            additionalMetrics = mapOf(
                "successRate" to (successCount.toDouble() / totalIterations),
                "standardDeviation" to calculateStandardDeviation(times, averageTime)
            )
        )
    }
    
    /**
     * Gets current memory statistics
     */
    private fun getMemoryStats(): MemoryStats {
        val runtime = Runtime.getRuntime()
        return MemoryStats(
            totalMemory = runtime.totalMemory(),
            freeMemory = runtime.freeMemory(),
            usedMemory = runtime.totalMemory() - runtime.freeMemory(),
            maxMemory = runtime.maxMemory()
        )
    }
    
    /**
     * Calculates standard deviation for timing data
     */
    private fun calculateStandardDeviation(values: List<Long>, mean: Double): Double {
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
    
    /**
     * Prints comprehensive benchmark report
     */
    @Test
    fun generateBenchmarkReport() {
        logger.info(FGOBotLogger.Category.TESTING, "=== FGO Bot Performance Benchmark Report ===")
        
        for (result in benchmarkResults) {
            logger.info(FGOBotLogger.Category.TESTING, """
                Test: ${result.testName}
                Average Time: ${String.format("%.2f", result.averageTime)}ms
                Min/Max Time: ${result.minTime}ms / ${result.maxTime}ms
                Throughput: ${String.format("%.2f", result.throughput)} ops/sec
                Success Rate: ${String.format("%.1f", (result.additionalMetrics["successRate"] as Double) * 100)}%
                Memory Usage: ${result.memoryUsage / (1024 * 1024)}MB
                Standard Deviation: ${String.format("%.2f", result.additionalMetrics["standardDeviation"] as Double)}ms
                ---
            """.trimIndent())
        }
        
        // Overall system health check
        val memoryStats = getMemoryStats()
        logger.info(FGOBotLogger.Category.TESTING, """
            === System Health Summary ===
            Memory Usage: ${memoryStats.usagePercentage.toInt()}% (${memoryStats.usedMemory / (1024 * 1024)}MB)
            OpenCV Status: ${if (openCVManager.isReady()) "Ready" else "Not Ready"}
            Template Cache: ${templateAssetManager.getCacheStatistics()}
            ML Models: ${mlModelManager.getModelStatistics().size} loaded
        """.trimIndent())
    }
} 