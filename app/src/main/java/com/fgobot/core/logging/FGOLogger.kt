package com.fgobot.core.logging

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object FGOLogger {
    
    private const val TAG = "FGOBot"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    enum class LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }
    
    fun v(message: String, tag: String = TAG) {
        Log.v(tag, formatMessage(message))
    }
    
    fun d(message: String, tag: String = TAG) {
        Log.d(tag, formatMessage(message))
    }
    
    fun i(message: String, tag: String = TAG) {
        Log.i(tag, formatMessage(message))
    }
    
    fun w(message: String, tag: String = TAG) {
        Log.w(tag, formatMessage(message))
    }
    
    fun w(message: String, throwable: Throwable, tag: String = TAG) {
        Log.w(tag, formatMessage(message), throwable)
    }
    
    fun e(message: String, tag: String = TAG) {
        Log.e(tag, formatMessage(message))
    }
    
    fun e(message: String, throwable: Throwable, tag: String = TAG) {
        Log.e(tag, formatMessage(message), throwable)
    }
    
    // Specific logging methods for different components
    fun logBattleStart(questName: String, teamConfig: String) {
        i("Battle started - Quest: $questName, Team: $teamConfig", "Battle")
    }
    
    fun logBattleEnd(questName: String, result: String, duration: Long) {
        i("Battle ended - Quest: $questName, Result: $result, Duration: ${duration}ms", "Battle")
    }
    
    fun logTeamSelection(teamName: String, servants: List<String>) {
        i("Team selected - Name: $teamName, Servants: ${servants.joinToString(", ")}", "TeamBuilder")
    }
    
    fun logApiCall(endpoint: String, duration: Long, success: Boolean) {
        val status = if (success) "SUCCESS" else "FAILED"
        i("API call - Endpoint: $endpoint, Duration: ${duration}ms, Status: $status", "API")
    }
    
    fun logScreenCapture(success: Boolean, processingTime: Long) {
        val status = if (success) "SUCCESS" else "FAILED"
        d("Screen capture - Status: $status, Processing time: ${processingTime}ms", "ScreenCapture")
    }
    
    fun logImageRecognition(element: String, confidence: Float, processingTime: Long) {
        d("Image recognition - Element: $element, Confidence: $confidence, Time: ${processingTime}ms", "ImageRecognition")
    }
    
    fun logUserAction(action: String, target: String) {
        d("User action - Action: $action, Target: $target", "UserAction")
    }
    
    fun logError(component: String, error: String, throwable: Throwable? = null) {
        if (throwable != null) {
            e("Error in $component: $error", throwable, "Error")
        } else {
            e("Error in $component: $error", "Error")
        }
    }
    
    fun logPerformance(component: String, operation: String, duration: Long) {
        d("Performance - Component: $component, Operation: $operation, Duration: ${duration}ms", "Performance")
    }
    
    private fun formatMessage(message: String): String {
        return "${dateFormat.format(Date())} - $message"
    }
} 