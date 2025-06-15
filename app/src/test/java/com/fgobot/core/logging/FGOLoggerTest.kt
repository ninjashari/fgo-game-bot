package com.fgobot.core.logging

import org.junit.Test

class FGOLoggerTest {
    
    @Test
    fun testLoggerMethods() {
        // Test that logger methods don't throw exceptions
        FGOLogger.d("Debug message")
        FGOLogger.i("Info message")
        FGOLogger.w("Warning message")
        FGOLogger.e("Error message")
    }
    
    @Test
    fun testBattleLogging() {
        FGOLogger.logBattleStart("Test Quest", "Test Team")
        FGOLogger.logBattleEnd("Test Quest", "WIN", 30000L)
    }
    
    @Test
    fun testTeamSelectionLogging() {
        FGOLogger.logTeamSelection("Test Team", listOf("Artoria", "Waver", "Merlin"))
    }
    
    @Test
    fun testApiCallLogging() {
        FGOLogger.logApiCall("/basic/servant", 1500L, true)
        FGOLogger.logApiCall("/basic/equip", 2000L, false)
    }
    
    @Test
    fun testPerformanceLogging() {
        FGOLogger.logPerformance("Database", "Insert Servant", 50L)
        FGOLogger.logPerformance("ImageRecognition", "Detect Button", 200L)
    }
    
    @Test
    fun testErrorLogging() {
        FGOLogger.logError("TestComponent", "Test error message")
        FGOLogger.logError("TestComponent", "Test error with exception", RuntimeException("Test"))
    }
} 