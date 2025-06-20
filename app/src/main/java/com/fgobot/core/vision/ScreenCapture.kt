/*
 * FGO Bot - Screen Capture System
 * 
 * This file implements the screen capture functionality using MediaProjection API.
 * Provides high-performance screenshot capabilities for real-time image analysis.
 */

package com.fgobot.core.vision

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.WindowManager
import com.fgobot.core.logging.FGOBotLogger
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Screen capture configuration
 */
data class CaptureConfig(
    val width: Int = 1080,
    val height: Int = 1920,
    val density: Int = DisplayMetrics.DENSITY_HIGH,
    val format: Int = PixelFormat.RGBA_8888
)

/**
 * Screen capture result
 */
sealed class CaptureResult {
    data class Success(val bitmap: Bitmap, val timestamp: Long) : CaptureResult()
    data class Error(val message: String, val cause: Throwable? = null) : CaptureResult()
}

/**
 * Screen capture system using MediaProjection
 * 
 * Provides efficient screenshot capabilities for real-time image analysis.
 * Optimized for FGO's 1080p resolution with performance considerations.
 */
class ScreenCapture(
    private val context: Context,
    private val logger: FGOBotLogger,
    private val config: CaptureConfig = CaptureConfig()
) {
    
    companion object {
        private const val TAG = "ScreenCapture"
        private const val VIRTUAL_DISPLAY_NAME = "FGOBot_ScreenCapture"
        private const val REQUEST_CODE_SCREEN_CAPTURE = 1001
    }
    
    private val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    
    private var isCapturing = false
    private var captureCount = 0L
    private var lastCaptureTime = 0L
    
    /**
     * Initializes the screen capture system
     * 
     * @param resultCode Result code from MediaProjection permission request
     * @param data Intent data from MediaProjection permission request
     * @return True if initialization successful
     */
    suspend fun initialize(resultCode: Int, data: Intent): Boolean {
        return try {
            logger.info(FGOBotLogger.Category.AUTOMATION, "Initializing screen capture system")
            
            // Create MediaProjection
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            
            // Setup background thread for image processing
            backgroundThread = HandlerThread("ScreenCapture").apply { start() }
            backgroundHandler = Handler(backgroundThread!!.looper)
            
            // Setup ImageReader
            setupImageReader()
            
            // Create VirtualDisplay
            createVirtualDisplay()
            
            isCapturing = true
            logger.info(FGOBotLogger.Category.AUTOMATION, "Screen capture system initialized successfully")
            true
        } catch (e: Exception) {
            logger.error(FGOBotLogger.Category.AUTOMATION, "Failed to initialize screen capture", e)
            cleanup()
            false
        }
    }
    
    /**
     * Captures a single screenshot
     * 
     * @return CaptureResult containing the bitmap or error
     */
    suspend fun captureScreen(): CaptureResult {
        if (!isCapturing || imageReader == null) {
            return CaptureResult.Error("Screen capture not initialized")
        }
        
        return suspendCancellableCoroutine { continuation ->
            try {
                val startTime = System.currentTimeMillis()
                
                imageReader?.setOnImageAvailableListener({ reader ->
                    try {
                        val image = reader.acquireLatestImage()
                        if (image != null) {
                            val bitmap = convertImageToBitmap(image)
                            image.close()
                            
                            captureCount++
                            lastCaptureTime = System.currentTimeMillis()
                            
                            val processingTime = lastCaptureTime - startTime
                            logger.debug(FGOBotLogger.Category.AUTOMATION, 
                                "Screenshot captured in ${processingTime}ms (${bitmap.width}x${bitmap.height})")
                            
                            continuation.resume(CaptureResult.Success(bitmap, lastCaptureTime))
                        } else {
                            continuation.resumeWithException(Exception("Failed to acquire image"))
                        }
                    } catch (e: Exception) {
                        logger.error(FGOBotLogger.Category.AUTOMATION, "Error processing captured image", e)
                        continuation.resumeWithException(e)
                    }
                }, backgroundHandler)
                
                // Trigger capture by invalidating the virtual display
                virtualDisplay?.let { display ->
                    // The image will be captured automatically when the display updates
                }
                
            } catch (e: Exception) {
                logger.error(FGOBotLogger.Category.AUTOMATION, "Error initiating screen capture", e)
                continuation.resumeWithException(e)
            }
        }
    }
    
    /**
     * Gets screen capture statistics
     * 
     * @return Capture statistics
     */
    fun getCaptureStats(): CaptureStats {
        return CaptureStats(
            isCapturing = isCapturing,
            captureCount = captureCount,
            lastCaptureTime = lastCaptureTime,
            configWidth = config.width,
            configHeight = config.height
        )
    }
    
    /**
     * Stops screen capture and releases resources
     */
    fun cleanup() {
        logger.info(FGOBotLogger.Category.AUTOMATION, "Cleaning up screen capture resources")
        
        isCapturing = false
        
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null
        
        virtualDisplay?.release()
        virtualDisplay = null
        
        mediaProjection?.stop()
        mediaProjection = null
        
        backgroundThread?.quitSafely()
        backgroundThread = null
        backgroundHandler = null
        
        logger.info(FGOBotLogger.Category.AUTOMATION, "Screen capture cleanup completed")
    }
    
    /**
     * Sets up the ImageReader for capturing screenshots
     */
    private fun setupImageReader() {
        imageReader = ImageReader.newInstance(
            config.width,
            config.height,
            config.format,
            2 // Buffer size
        )
        
        logger.debug(FGOBotLogger.Category.AUTOMATION, 
            "ImageReader configured: ${config.width}x${config.height}, format=${config.format}")
    }
    
    /**
     * Creates the VirtualDisplay for screen mirroring
     */
    private fun createVirtualDisplay() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            VIRTUAL_DISPLAY_NAME,
            config.width,
            config.height,
            config.density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            backgroundHandler
        )
        
        logger.debug(FGOBotLogger.Category.AUTOMATION, "VirtualDisplay created: $VIRTUAL_DISPLAY_NAME")
    }
    
    /**
     * Converts Image to Bitmap
     * 
     * @param image Image from ImageReader
     * @return Converted Bitmap
     */
    private fun convertImageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * config.width
        
        val bitmap = Bitmap.createBitmap(
            config.width + rowPadding / pixelStride,
            config.height,
            Bitmap.Config.ARGB_8888
        )
        
        bitmap.copyPixelsFromBuffer(buffer)
        
        // Crop to exact dimensions if there's padding
        return if (rowPadding != 0) {
            Bitmap.createBitmap(bitmap, 0, 0, config.width, config.height)
        } else {
            bitmap
        }
    }
    
    /**
     * Creates intent for requesting MediaProjection permission
     * 
     * @return Intent for permission request
     */
    fun createScreenCaptureIntent(): Intent {
        return mediaProjectionManager.createScreenCaptureIntent()
    }
}

/**
 * Screen capture statistics
 */
data class CaptureStats(
    val isCapturing: Boolean,
    val captureCount: Long,
    val lastCaptureTime: Long,
    val configWidth: Int,
    val configHeight: Int
) 