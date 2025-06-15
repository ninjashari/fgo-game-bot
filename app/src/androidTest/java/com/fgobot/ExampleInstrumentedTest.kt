/**
 * FGO Bot - Example Instrumented Test
 * 
 * This file contains basic instrumented tests that run on an Android device
 * to verify the project setup and Android testing framework configuration.
 * 
 * Features:
 * - Android instrumented testing
 * - Context verification
 * - App package verification
 */

package com.fgobot

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * ExampleInstrumentedTest - Basic instrumented tests for Android verification
 * 
 * These tests run on an Android device and verify that the app context
 * and basic Android functionality work correctly.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    
    /**
     * Test app context
     * Verifies that the app context is available and has the correct package name
     */
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.fgobot", appContext.packageName)
    }
    
    /**
     * Test app resources
     * Verifies that app resources are accessible
     */
    @Test
    fun appResources_areAccessible() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val appName = appContext.getString(R.string.app_name)
        
        assertNotNull("App name should not be null", appName)
        assertEquals("FGO Bot", appName)
    }
    
    /**
     * Test package manager
     * Verifies that the package manager is available and working
     */
    @Test
    fun packageManager_isWorking() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = appContext.packageManager
        
        assertNotNull("Package manager should not be null", packageManager)
        
        val packageInfo = packageManager.getPackageInfo(appContext.packageName, 0)
        assertNotNull("Package info should not be null", packageInfo)
        assertEquals("com.fgobot", packageInfo.packageName)
    }
} 