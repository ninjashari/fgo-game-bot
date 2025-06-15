/**
 * FGO Bot - Example Unit Test
 * 
 * This file contains basic unit tests to verify the project setup
 * and testing framework configuration.
 * 
 * Features:
 * - JUnit 4 testing framework
 * - Basic assertion testing
 * - Project setup verification
 */

package com.fgobot

import org.junit.Test
import org.junit.Assert.*

/**
 * ExampleUnitTest - Basic unit tests for project verification
 * 
 * These tests verify that the basic project setup is working correctly
 * and that the testing framework is properly configured.
 */
class ExampleUnitTest {
    
    /**
     * Test basic addition operation
     * Verifies that basic arithmetic works as expected
     */
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
    
    /**
     * Test string operations
     * Verifies that string manipulation works correctly
     */
    @Test
    fun string_operations_work() {
        val appName = "FGO Bot"
        assertTrue("App name should not be empty", appName.isNotEmpty())
        assertEquals("FGO Bot", appName)
    }
    
    /**
     * Test boolean logic
     * Verifies that boolean operations work as expected
     */
    @Test
    fun boolean_logic_works() {
        val isAndroid14Ready = true
        val isProjectSetup = true
        
        assertTrue("Project should be Android 14 ready", isAndroid14Ready)
        assertTrue("Project should be set up", isProjectSetup)
        assertTrue("Both conditions should be true", isAndroid14Ready && isProjectSetup)
    }
    
    /**
     * Test list operations
     * Verifies that collection operations work correctly
     */
    @Test
    fun list_operations_work() {
        val features = listOf(
            "Android 14 Support",
            "Jetpack Compose UI",
            "Room Database",
            "Retrofit API",
            "Material Design 3"
        )
        
        assertEquals(5, features.size)
        assertTrue("Should contain Android 14 Support", features.contains("Android 14 Support"))
        assertTrue("Should contain Jetpack Compose UI", features.contains("Jetpack Compose UI"))
    }
} 