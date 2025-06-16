/**
 * FGO Bot - Settings Screen
 * 
 * This screen provides configuration options for the FGO Bot application.
 * Users can adjust automation settings, performance options, and app preferences.
 * 
 * Features:
 * - Automation configuration
 * - Performance settings
 * - Data management options
 * - About information
 */

package com.fgobot.presentation.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fgobot.data.repository.SettingsRepository
import com.fgobot.data.repository.SettingsRepositoryImpl
import com.fgobot.core.logging.FGOLoggerImpl
import com.fgobot.presentation.components.*
import com.fgobot.presentation.theme.FGOBotTheme
import kotlinx.coroutines.launch

/**
 * Settings screen composable
 * 
 * @param settingsRepository Repository for managing settings persistence
 * @param onNavigateBack Navigation callback to go back
 * @param onNavigateToHome Navigation callback to go to home
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateToHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Create repository if not provided (for preview and testing)
    val repository = settingsRepository ?: remember(context) {
        SettingsRepositoryImpl(context, FGOLoggerImpl())
    }
    
    // Collect settings from repository
    val settings by repository.settings.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHome) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Accessibility Settings
            item {
                SettingsSection(
                    title = "Accessibility",
                    icon = Icons.Default.Accessibility
                ) {
                    SettingsItem(
                        title = "Accessibility Service",
                        subtitle = "Required for automation to work",
                        icon = Icons.Default.TouchApp,
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
            
            // Automation Settings
            item {
                SettingsSection(
                    title = "Automation",
                    icon = Icons.Default.SmartToy
                ) {
                    SwitchSettingsItem(
                        title = "Human-like Timing",
                        subtitle = "Add random delays to mimic human behavior",
                        checked = settings.enableHumanLikeTiming,
                        onCheckedChange = { 
                            coroutineScope.launch {
                                repository.updateAutomationSettings(enableHumanLikeTiming = it)
                            }
                        }
                    )
                    
                    SwitchSettingsItem(
                        title = "Error Recovery",
                        subtitle = "Automatically recover from common errors",
                        checked = settings.enableErrorRecovery,
                        onCheckedChange = { 
                            coroutineScope.launch {
                                repository.updateAutomationSettings(enableErrorRecovery = it)
                            }
                        }
                    )
                    
                    SwitchSettingsItem(
                        title = "Learning Mode",
                        subtitle = "Improve decisions based on battle outcomes",
                        checked = settings.enableLearning,
                        onCheckedChange = { 
                            coroutineScope.launch {
                                repository.updateAutomationSettings(enableLearning = it)
                            }
                        }
                    )
                    
                    SliderSettingsItem(
                        title = "Max Battles",
                        subtitle = "Maximum battles per automation run (0 = unlimited)",
                        value = settings.maxBattles.toFloat(),
                        valueRange = 0f..100f,
                        steps = 99,
                        onValueChange = { 
                            coroutineScope.launch {
                                repository.updateAutomationSettings(maxBattles = it.toInt())
                            }
                        },
                        valueText = if (settings.maxBattles == 0) "Unlimited" else settings.maxBattles.toString()
                    )
                    
                    SliderSettingsItem(
                        title = "Screenshot Interval",
                        subtitle = "Time between screenshots (milliseconds)",
                        value = settings.screenshotInterval.toFloat(),
                        valueRange = 500f..5000f,
                        steps = 45,
                        onValueChange = { 
                            coroutineScope.launch {
                                repository.updateAutomationSettings(screenshotInterval = it.toInt())
                            }
                        },
                        valueText = "${settings.screenshotInterval}ms"
                    )
                }
            }
            
            // Notifications Settings
            item {
                SettingsSection(
                    title = "Notifications",
                    icon = Icons.Default.Notifications
                ) {
                    SwitchSettingsItem(
                        title = "Enable Notifications",
                        subtitle = "Show notifications for automation events",
                        checked = settings.enableNotifications,
                        onCheckedChange = { 
                            coroutineScope.launch {
                                repository.updateNotificationSettings(enableNotifications = it)
                            }
                        }
                    )
                    
                    SwitchSettingsItem(
                        title = "Vibration",
                        subtitle = "Vibrate on important events",
                        checked = settings.enableVibration,
                        onCheckedChange = { 
                            coroutineScope.launch {
                                repository.updateNotificationSettings(enableVibration = it)
                            }
                        }
                    )
                }
            }
            
            // App Settings
            item {
                SettingsSection(
                    title = "App Settings",
                    icon = Icons.Default.Tune
                ) {
                    SwitchSettingsItem(
                        title = "Dark Mode",
                        subtitle = "Use dark theme",
                        checked = settings.enableDarkMode,
                        onCheckedChange = { 
                            coroutineScope.launch {
                                repository.updateAppSettings(enableDarkMode = it)
                            }
                        }
                    )
                    
                    SettingsItem(
                        title = "Reset Settings",
                        subtitle = "Reset all settings to default values",
                        icon = Icons.Default.RestartAlt,
                        onClick = {
                            coroutineScope.launch {
                                repository.resetToDefaults()
                            }
                        }
                    )
                    
                    SettingsItem(
                        title = "Export Settings",
                        subtitle = "Export settings configuration",
                        icon = Icons.Default.FileDownload,
                        onClick = {
                            coroutineScope.launch {
                                repository.exportSettings()
                                // TODO: Save to file or share
                                // For now, this will just export to logs
                            }
                        }
                    )
                }
            }
            
            // About Section
            item {
                SettingsSection(
                    title = "About",
                    icon = Icons.Default.Info
                ) {
                    SettingsItem(
                        title = "Version",
                        subtitle = "FGO Bot v1.0.0",
                        icon = Icons.Default.AppSettingsAlt,
                        onClick = { }
                    )
                    
                    SettingsItem(
                        title = "Help & Support",
                        subtitle = "Get help and report issues",
                        icon = Icons.Default.Help,
                        onClick = {
                            // TODO: Navigate to help screen
                        }
                    )
                    
                    SettingsItem(
                        title = "Privacy Policy",
                        subtitle = "View privacy policy",
                        icon = Icons.Default.PrivacyTip,
                        onClick = {
                            // TODO: Open privacy policy
                        }
                    )
                }
            }
        }
    }
}

/**
 * Settings section with title and icon
 */
@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            content()
        }
    }
}

/**
 * Basic settings item
 */
@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Switch settings item
 */
@Composable
private fun SwitchSettingsItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

/**
 * Slider settings item
 */
@Composable
private fun SliderSettingsItem(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    valueText: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    FGOBotTheme {
        SettingsScreen()
    }
} 