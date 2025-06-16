/*
 * FGO Bot - Logging System
 * 
 * This file defines the comprehensive logging system for the FGO Bot application.
 * Uses Timber for structured logging with different levels and categories.
 */

package com.fgobot.core.logging

import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Simple logger interface for consistent logging across the application
 */
interface FGOLogger {
    fun v(tag: String, message: String, throwable: Throwable? = null)
    fun d(tag: String, message: String, throwable: Throwable? = null)
    fun i(tag: String, message: String, throwable: Throwable? = null)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

/**
 * Implementation of FGOLogger using FGOBotLogger
 */
class FGOLoggerImpl : FGOLogger {
    override fun v(tag: String, message: String, throwable: Throwable?) {
        FGOBotLogger.verbose(FGOBotLogger.Category.GENERAL, "[$tag] $message", throwable)
    }
    
    override fun d(tag: String, message: String, throwable: Throwable?) {
        FGOBotLogger.debug(FGOBotLogger.Category.GENERAL, "[$tag] $message", throwable)
    }
    
    override fun i(tag: String, message: String, throwable: Throwable?) {
        FGOBotLogger.info(FGOBotLogger.Category.GENERAL, "[$tag] $message", throwable)
    }
    
    override fun w(tag: String, message: String, throwable: Throwable?) {
        FGOBotLogger.warn(FGOBotLogger.Category.GENERAL, "[$tag] $message", throwable)
    }
    
    override fun e(tag: String, message: String, throwable: Throwable?) {
        FGOBotLogger.error(FGOBotLogger.Category.ERROR, "[$tag] $message", throwable)
    }
}

/**
 * Centralized logging system for FGO Bot application
 * 
 * Provides structured logging with different levels, categories, and output destinations.
 * Supports both console logging (for development) and file logging (for production).
 */
object FGOBotLogger {
    
    /**
     * Log categories for different parts of the application
     */
    enum class Category(val tag: String) {
        DATABASE("DB"),
        NETWORK("NET"),
        UI("UI"),
        AUTOMATION("AUTO"),
        PERFORMANCE("PERF"),
        ERROR("ERR"),
        GENERAL("GEN")
    }
    
    /**
     * Log levels matching Android's Log levels
     */
    enum class Level(val priority: Int) {
        VERBOSE(Log.VERBOSE),
        DEBUG(Log.DEBUG),
        INFO(Log.INFO),
        WARN(Log.WARN),
        ERROR(Log.ERROR)
    }
    
    private var isInitialized = false
    private var logDirectory: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    /**
     * Initialize the logging system
     * 
     * @param context Application context
     * @param enableFileLogging Whether to enable file logging
     * @param enableDebugLogging Whether to enable debug logging
     */
    fun initialize(
        context: Context,
        enableFileLogging: Boolean = true,
        enableDebugLogging: Boolean = true
    ) {
        if (isInitialized) return
        
        // Plant debug tree for development
        if (enableDebugLogging) {
            Timber.plant(DebugTree())
        }
        
        // Plant file tree for production logging
        if (enableFileLogging) {
            setupFileLogging(context)
            logDirectory?.let { dir ->
                Timber.plant(FileLoggingTree(dir))
            }
        }
        
        isInitialized = true
        info(Category.GENERAL, "FGO Bot Logger initialized")
    }
    
    /**
     * Set up file logging directory and cleanup old logs
     * 
     * @param context Application context
     */
    private fun setupFileLogging(context: Context) {
        try {
            logDirectory = File(context.filesDir, "logs").apply {
                if (!exists()) {
                    mkdirs()
                }
            }
            
            // Clean up old log files (keep last 7 days)
            cleanupOldLogs()
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to setup file logging")
        }
    }
    
    /**
     * Clean up log files older than 7 days
     */
    private fun cleanupOldLogs() {
        logDirectory?.let { dir ->
            val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) // 7 days
            
            dir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    file.delete()
                }
            }
        }
    }
    
    /**
     * Log verbose message
     * 
     * @param category Log category
     * @param message Log message
     * @param throwable Optional throwable
     */
    fun verbose(category: Category, message: String, throwable: Throwable? = null) {
        log(Level.VERBOSE, category, message, throwable)
    }
    
    /**
     * Log debug message
     * 
     * @param category Log category
     * @param message Log message
     * @param throwable Optional throwable
     */
    fun debug(category: Category, message: String, throwable: Throwable? = null) {
        log(Level.DEBUG, category, message, throwable)
    }
    
    /**
     * Log info message
     * 
     * @param category Log category
     * @param message Log message
     * @param throwable Optional throwable
     */
    fun info(category: Category, message: String, throwable: Throwable? = null) {
        log(Level.INFO, category, message, throwable)
    }
    
    /**
     * Log warning message
     * 
     * @param category Log category
     * @param message Log message
     * @param throwable Optional throwable
     */
    fun warn(category: Category, message: String, throwable: Throwable? = null) {
        log(Level.WARN, category, message, throwable)
    }
    
    /**
     * Log error message
     * 
     * @param category Log category
     * @param message Log message
     * @param throwable Optional throwable
     */
    fun error(category: Category, message: String, throwable: Throwable? = null) {
        log(Level.ERROR, category, message, throwable)
    }
    
    /**
     * Internal log method
     * 
     * @param level Log level
     * @param category Log category
     * @param message Log message
     * @param throwable Optional throwable
     */
    private fun log(level: Level, category: Category, message: String, throwable: Throwable?) {
        val tag = "FGOBot-${category.tag}"
        
        when (level) {
            Level.VERBOSE -> if (throwable != null) Timber.tag(tag).v(throwable, message) else Timber.tag(tag).v(message)
            Level.DEBUG -> if (throwable != null) Timber.tag(tag).d(throwable, message) else Timber.tag(tag).d(message)
            Level.INFO -> if (throwable != null) Timber.tag(tag).i(throwable, message) else Timber.tag(tag).i(message)
            Level.WARN -> if (throwable != null) Timber.tag(tag).w(throwable, message) else Timber.tag(tag).w(message)
            Level.ERROR -> if (throwable != null) Timber.tag(tag).e(throwable, message) else Timber.tag(tag).e(message)
        }
    }
    
    /**
     * Get log files for export or viewing
     * 
     * @return List of log files
     */
    fun getLogFiles(): List<File> {
        return logDirectory?.listFiles()?.toList() ?: emptyList()
    }
    
    /**
     * Export logs to a single file
     * 
     * @param outputFile Output file for exported logs
     * @return True if export successful, false otherwise
     */
    fun exportLogs(outputFile: File): Boolean {
        return try {
            val logFiles = getLogFiles().sortedBy { it.name }
            
            FileWriter(outputFile).use { writer ->
                writer.write("FGO Bot Logs Export - ${dateFormat.format(Date())}\n")
                writer.write("${"=".repeat(50)}\n\n")
                
                logFiles.forEach { file ->
                    writer.write("=== ${file.name} ===\n")
                    file.readText().let { content ->
                        writer.write(content)
                        writer.write("\n\n")
                    }
                }
            }
            true
        } catch (e: IOException) {
            error(Category.ERROR, "Failed to export logs", e)
            false
        }
    }
}

/**
 * Custom Timber tree for debug logging
 */
private class DebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String {
        return "FGOBot(${element.fileName}:${element.lineNumber})"
    }
}

/**
 * Custom Timber tree for file logging
 */
private class FileLoggingTree(private val logDirectory: File) : Timber.Tree() {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            val timestamp = dateFormat.format(Date())
            val priorityString = when (priority) {
                Log.VERBOSE -> "V"
                Log.DEBUG -> "D"
                Log.INFO -> "I"
                Log.WARN -> "W"
                Log.ERROR -> "E"
                else -> "U"
            }
            
            val logMessage = buildString {
                append("$timestamp $priorityString/$tag: $message")
                if (t != null) {
                    append("\n")
                    append(Log.getStackTraceString(t))
                }
                append("\n")
            }
            
            val logFile = File(logDirectory, "fgobot-${fileDateFormat.format(Date())}.log")
            logFile.appendText(logMessage)
            
        } catch (e: IOException) {
            // Fallback to system log if file logging fails
            Log.e("FGOBot-Logger", "Failed to write to log file", e)
        }
    }
} 