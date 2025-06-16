/**
 * FGO Bot - Settings Repository
 * 
 * This repository manages persistent storage of application settings using SharedPreferences.
 * Provides a clean interface for reading and writing user preferences with proper defaults.
 * 
 * Features:
 * - Type-safe settings access
 * - Default value management
 * - Reactive settings updates via Flow
 * - Batch settings operations
 */

package com.fgobot.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.fgobot.core.logging.FGOLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Settings data class containing all app settings
 */
data class AppSettings(
    // Automation Settings
    val enableHumanLikeTiming: Boolean = true,
    val enableErrorRecovery: Boolean = true,
    val enableLearning: Boolean = false,
    val maxBattles: Int = 10,
    val screenshotInterval: Int = 1000,
    
    // Notification Settings
    val enableNotifications: Boolean = true,
    val enableVibration: Boolean = false,
    
    // App Settings
    val enableDarkMode: Boolean = false,
    
    // Performance Settings
    val enablePerformanceMode: Boolean = false,
    val enableDebugLogging: Boolean = false
)

/**
 * Settings repository interface
 */
interface SettingsRepository {
    
    /**
     * Current settings as StateFlow for reactive updates
     */
    val settings: StateFlow<AppSettings>
    
    /**
     * Update automation settings
     */
    suspend fun updateAutomationSettings(
        enableHumanLikeTiming: Boolean? = null,
        enableErrorRecovery: Boolean? = null,
        enableLearning: Boolean? = null,
        maxBattles: Int? = null,
        screenshotInterval: Int? = null
    )
    
    /**
     * Update notification settings
     */
    suspend fun updateNotificationSettings(
        enableNotifications: Boolean? = null,
        enableVibration: Boolean? = null
    )
    
    /**
     * Update app settings
     */
    suspend fun updateAppSettings(
        enableDarkMode: Boolean? = null,
        enablePerformanceMode: Boolean? = null,
        enableDebugLogging: Boolean? = null
    )
    
    /**
     * Reset all settings to defaults
     */
    suspend fun resetToDefaults()
    
    /**
     * Export settings as JSON string
     */
    suspend fun exportSettings(): String
    
    /**
     * Import settings from JSON string
     */
    suspend fun importSettings(json: String): Boolean
}

/**
 * Implementation of SettingsRepository using SharedPreferences
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val context: Context,
    private val logger: FGOLogger
) : SettingsRepository {
    
    companion object {
        private const val TAG = "SettingsRepository"
        private const val PREFS_NAME = "fgo_bot_settings"
        
        // Automation Settings Keys
        private const val KEY_ENABLE_HUMAN_TIMING = "enable_human_timing"
        private const val KEY_ENABLE_ERROR_RECOVERY = "enable_error_recovery"
        private const val KEY_ENABLE_LEARNING = "enable_learning"
        private const val KEY_MAX_BATTLES = "max_battles"
        private const val KEY_SCREENSHOT_INTERVAL = "screenshot_interval"
        
        // Notification Settings Keys
        private const val KEY_ENABLE_NOTIFICATIONS = "enable_notifications"
        private const val KEY_ENABLE_VIBRATION = "enable_vibration"
        
        // App Settings Keys
        private const val KEY_ENABLE_DARK_MODE = "enable_dark_mode"
        private const val KEY_ENABLE_PERFORMANCE_MODE = "enable_performance_mode"
        private const val KEY_ENABLE_DEBUG_LOGGING = "enable_debug_logging"
    }
    
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private val _settings = MutableStateFlow(loadSettings())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    
    init {
        logger.i(TAG, "Settings repository initialized")
        
        // Listen for preference changes
        sharedPreferences.registerOnSharedPreferenceChangeListener { _, key ->
            logger.d(TAG, "Setting changed: $key")
            _settings.value = loadSettings()
        }
    }
    
    /**
     * Load settings from SharedPreferences
     */
    private fun loadSettings(): AppSettings {
        return try {
            AppSettings(
                // Automation Settings
                enableHumanLikeTiming = sharedPreferences.getBoolean(KEY_ENABLE_HUMAN_TIMING, true),
                enableErrorRecovery = sharedPreferences.getBoolean(KEY_ENABLE_ERROR_RECOVERY, true),
                enableLearning = sharedPreferences.getBoolean(KEY_ENABLE_LEARNING, false),
                maxBattles = sharedPreferences.getInt(KEY_MAX_BATTLES, 10),
                screenshotInterval = sharedPreferences.getInt(KEY_SCREENSHOT_INTERVAL, 1000),
                
                // Notification Settings
                enableNotifications = sharedPreferences.getBoolean(KEY_ENABLE_NOTIFICATIONS, true),
                enableVibration = sharedPreferences.getBoolean(KEY_ENABLE_VIBRATION, false),
                
                // App Settings
                enableDarkMode = sharedPreferences.getBoolean(KEY_ENABLE_DARK_MODE, false),
                enablePerformanceMode = sharedPreferences.getBoolean(KEY_ENABLE_PERFORMANCE_MODE, false),
                enableDebugLogging = sharedPreferences.getBoolean(KEY_ENABLE_DEBUG_LOGGING, false)
            )
        } catch (e: Exception) {
            logger.e(TAG, "Error loading settings, using defaults", e)
            AppSettings() // Return default settings
        }
    }
    
    override suspend fun updateAutomationSettings(
        enableHumanLikeTiming: Boolean?,
        enableErrorRecovery: Boolean?,
        enableLearning: Boolean?,
        maxBattles: Int?,
        screenshotInterval: Int?
    ) {
        logger.d(TAG, "Updating automation settings")
        
        sharedPreferences.edit {
            enableHumanLikeTiming?.let { putBoolean(KEY_ENABLE_HUMAN_TIMING, it) }
            enableErrorRecovery?.let { putBoolean(KEY_ENABLE_ERROR_RECOVERY, it) }
            enableLearning?.let { putBoolean(KEY_ENABLE_LEARNING, it) }
            maxBattles?.let { putInt(KEY_MAX_BATTLES, it) }
            screenshotInterval?.let { putInt(KEY_SCREENSHOT_INTERVAL, it) }
        }
        
        // Force immediate update of StateFlow on main thread
        withContext(Dispatchers.Main) {
            _settings.value = loadSettings()
        }
    }
    
    override suspend fun updateNotificationSettings(
        enableNotifications: Boolean?,
        enableVibration: Boolean?
    ) {
        logger.d(TAG, "Updating notification settings")
        
        sharedPreferences.edit {
            enableNotifications?.let { putBoolean(KEY_ENABLE_NOTIFICATIONS, it) }
            enableVibration?.let { putBoolean(KEY_ENABLE_VIBRATION, it) }
        }
        
        // Force immediate update of StateFlow on main thread
        withContext(Dispatchers.Main) {
            _settings.value = loadSettings()
        }
    }
    
    override suspend fun updateAppSettings(
        enableDarkMode: Boolean?,
        enablePerformanceMode: Boolean?,
        enableDebugLogging: Boolean?
    ) {
        logger.d(TAG, "Updating app settings")
        
        sharedPreferences.edit {
            enableDarkMode?.let { putBoolean(KEY_ENABLE_DARK_MODE, it) }
            enablePerformanceMode?.let { putBoolean(KEY_ENABLE_PERFORMANCE_MODE, it) }
            enableDebugLogging?.let { putBoolean(KEY_ENABLE_DEBUG_LOGGING, it) }
        }
        
        // Force immediate update of StateFlow on main thread
        withContext(Dispatchers.Main) {
            _settings.value = loadSettings()
        }
    }
    
    override suspend fun resetToDefaults() {
        logger.i(TAG, "Resetting all settings to defaults")
        
        sharedPreferences.edit {
            clear()
        }
        
        // Force immediate update of StateFlow on main thread
        withContext(Dispatchers.Main) {
            _settings.value = loadSettings()
        }
    }
    
    override suspend fun exportSettings(): String {
        return try {
            val currentSettings = _settings.value
            // Using a simple JSON-like format for export
            buildString {
                appendLine("{")
                appendLine("  \"enableHumanLikeTiming\": ${currentSettings.enableHumanLikeTiming},")
                appendLine("  \"enableErrorRecovery\": ${currentSettings.enableErrorRecovery},")
                appendLine("  \"enableLearning\": ${currentSettings.enableLearning},")
                appendLine("  \"maxBattles\": ${currentSettings.maxBattles},")
                appendLine("  \"screenshotInterval\": ${currentSettings.screenshotInterval},")
                appendLine("  \"enableNotifications\": ${currentSettings.enableNotifications},")
                appendLine("  \"enableVibration\": ${currentSettings.enableVibration},")
                appendLine("  \"enableDarkMode\": ${currentSettings.enableDarkMode},")
                appendLine("  \"enablePerformanceMode\": ${currentSettings.enablePerformanceMode},")
                appendLine("  \"enableDebugLogging\": ${currentSettings.enableDebugLogging}")
                appendLine("}")
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error exporting settings", e)
            "{}"
        }
    }
    
    override suspend fun importSettings(json: String): Boolean {
        return try {
            logger.i(TAG, "Importing settings from JSON")
            
            // Simple JSON parsing for basic settings import
            // In a production app, you'd use a proper JSON parser like Gson
            val lines = json.lines().map { it.trim() }
            
            sharedPreferences.edit {
                lines.forEach { line ->
                    when {
                        line.contains("enableHumanLikeTiming") -> {
                            val value = line.substringAfter(":").replace(",", "").trim().toBoolean()
                            putBoolean(KEY_ENABLE_HUMAN_TIMING, value)
                        }
                        line.contains("enableErrorRecovery") -> {
                            val value = line.substringAfter(":").replace(",", "").trim().toBoolean()
                            putBoolean(KEY_ENABLE_ERROR_RECOVERY, value)
                        }
                        line.contains("enableLearning") -> {
                            val value = line.substringAfter(":").replace(",", "").trim().toBoolean()
                            putBoolean(KEY_ENABLE_LEARNING, value)
                        }
                        line.contains("maxBattles") -> {
                            val value = line.substringAfter(":").replace(",", "").trim().toInt()
                            putInt(KEY_MAX_BATTLES, value)
                        }
                        line.contains("screenshotInterval") -> {
                            val value = line.substringAfter(":").replace(",", "").trim().toInt()
                            putInt(KEY_SCREENSHOT_INTERVAL, value)
                        }
                        line.contains("enableNotifications") -> {
                            val value = line.substringAfter(":").replace(",", "").trim().toBoolean()
                            putBoolean(KEY_ENABLE_NOTIFICATIONS, value)
                        }
                        line.contains("enableVibration") -> {
                            val value = line.substringAfter(":").replace(",", "").trim().toBoolean()
                            putBoolean(KEY_ENABLE_VIBRATION, value)
                        }
                        line.contains("enableDarkMode") -> {
                            val value = line.substringAfter(":").replace(",", "").trim().toBoolean()
                            putBoolean(KEY_ENABLE_DARK_MODE, value)
                        }
                        line.contains("enablePerformanceMode") -> {
                            val value = line.substringAfter(":").replace(",", "").trim().toBoolean()
                            putBoolean(KEY_ENABLE_PERFORMANCE_MODE, value)
                        }
                        line.contains("enableDebugLogging") -> {
                            val value = line.substringAfter(":").replace(",", "").trim().toBoolean()
                            putBoolean(KEY_ENABLE_DEBUG_LOGGING, value)
                        }
                    }
                }
            }
            
            logger.i(TAG, "Settings imported successfully")
            true
        } catch (e: Exception) {
            logger.e(TAG, "Error importing settings", e)
            false
        }
    }
}