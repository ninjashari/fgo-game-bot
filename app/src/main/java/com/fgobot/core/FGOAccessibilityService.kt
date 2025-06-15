package com.fgobot.core

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class FGOAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "FGOAccessibilityService"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Will implement event handling logic later
        Log.d(TAG, "Accessibility event received: ${event.eventType}")
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected")
    }
} 