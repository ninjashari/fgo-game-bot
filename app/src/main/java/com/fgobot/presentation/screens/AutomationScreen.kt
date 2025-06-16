/**
 * FGO Bot - Automation Screen
 * 
 * This screen provides the main user interface for controlling the FGO Bot automation.
 * It displays real-time automation status, statistics, and provides controls for
 * starting, stopping, and configuring automation runs.
 * 
 * Features:
 * - Real-time automation status display
 * - Start/Stop/Pause automation controls
 * - Team selection interface
 * - Statistics and performance metrics
 * - Error handling and user feedback
 */

package com.fgobot.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fgobot.R
import com.fgobot.core.automation.AutomationState
import com.fgobot.core.automation.AutomationStats
import com.fgobot.data.database.entities.Team
import com.fgobot.data.database.entities.BattleLog
import com.fgobot.presentation.components.*
import com.fgobot.presentation.theme.FGOBotTheme
import com.fgobot.presentation.viewmodel.AutomationViewModel
import com.fgobot.presentation.viewmodel.AutomationAction
import com.fgobot.presentation.viewmodel.AutomationUiState

/**
 * Main automation screen composable
 * 
 * @param viewModel Automation ViewModel
 * @param onNavigateToTeams Navigation callback for team management
 * @param onNavigateToSettings Navigation callback for settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationScreen(
    viewModel: AutomationViewModel,
    onNavigateToTeams: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTeam by viewModel.selectedTeam.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        AutomationHeader(
            automationState = uiState.automationState,
            isAccessibilityEnabled = uiState.isAccessibilityServiceEnabled,
            onNavigateToSettings = onNavigateToSettings
        )
        
        // Error display
        uiState.errorMessage?.let { error ->
            ErrorCard(
                message = error,
                onDismiss = { viewModel.handleAction(AutomationAction.ClearError) }
            )
        }
        
        // Main content
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Control panel
            item {
                AutomationControlPanel(
                    uiState = uiState,
                    selectedTeam = selectedTeam,
                    onStartAutomation = { 
                    if (!uiState.isScreenCapturePermissionGranted) {
                        viewModel.handleAction(AutomationAction.RequestScreenCapturePermission)
                    } else {
                        viewModel.handleAction(AutomationAction.StartAutomation)
                    }
                },
                    onStopAutomation = { viewModel.handleAction(AutomationAction.StopAutomation) },
                    onPauseAutomation = { viewModel.handleAction(AutomationAction.PauseAutomation) },
                    onResumeAutomation = { viewModel.handleAction(AutomationAction.ResumeAutomation) },
                    canStart = viewModel.canStartAutomation(),
                    canStop = viewModel.canStopAutomation(),
                    canPause = viewModel.canPauseAutomation(),
                    canResume = viewModel.canResumeAutomation()
                )
            }
            
            // Team selection
            item {
                TeamSelectionCard(
                    availableTeams = uiState.availableTeams,
                    selectedTeam = selectedTeam,
                    onTeamSelected = { team -> viewModel.handleAction(AutomationAction.SelectTeam(team)) },
                    onNavigateToTeams = onNavigateToTeams,
                    onRefreshTeams = { viewModel.handleAction(AutomationAction.RefreshTeams) }
                )
            }
            
            // Statistics
            item {
                AutomationStatsCard(stats = uiState.automationStats)
            }
            
            // Recent battle logs
            item {
                RecentBattleLogsCard(battleLogs = uiState.recentBattleLogs)
            }
        }
    }
}

/**
 * Automation header with status and settings
 */
@Composable
private fun AutomationHeader(
    automationState: AutomationState,
    isAccessibilityEnabled: Boolean,
    onNavigateToSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (automationState) {
                AutomationState.RUNNING -> MaterialTheme.colorScheme.primaryContainer
                AutomationState.ERROR -> MaterialTheme.colorScheme.errorContainer
                AutomationState.PAUSED -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.main_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusIndicator(
                        isActive = automationState == AutomationState.RUNNING,
                        color = when (automationState) {
                            AutomationState.RUNNING -> Color.Green
                            AutomationState.ERROR -> Color.Red
                            AutomationState.PAUSED -> Color.Yellow
                            else -> Color.Gray
                        }
                    )
                    
                    Text(
                        text = getAutomationStateText(automationState),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (!isAccessibilityEnabled) {
                    Text(
                        text = "⚠️ Accessibility service not enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        }
    }
}

/**
 * Status indicator dot
 */
@Composable
private fun StatusIndicator(
    isActive: Boolean,
    color: Color
) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(
                color = if (isActive) color else color.copy(alpha = 0.3f),
                shape = CircleShape
            )
    )
}

/**
 * Automation control panel with enhanced functionality
 */
@Composable
private fun AutomationControlPanel(
    uiState: AutomationUiState,
    selectedTeam: Team?,
    onStartAutomation: () -> Unit,
    onStopAutomation: () -> Unit,
    onPauseAutomation: () -> Unit,
    onResumeAutomation: () -> Unit,
    canStart: Boolean,
    canStop: Boolean,
    canPause: Boolean,
    canResume: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Automation Control",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Status indicators
            AutomationStatusIndicators(uiState = uiState, selectedTeam = selectedTeam)
            
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Start button
                FGOBotSuccessButton(
                    text = "Start",
                    onClick = onStartAutomation,
                    enabled = canStart && !uiState.isLoading,
                    modifier = Modifier.weight(1f)
                )
                
                // Pause/Resume button
                if (canPause) {
                    FGOBotSecondaryButton(
                        text = "Pause",
                        onClick = onPauseAutomation,
                        modifier = Modifier.weight(1f)
                    )
                } else if (canResume) {
                    FGOBotPrimaryButton(
                        text = "Resume",
                        onClick = onResumeAutomation,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Stop button
                FGOBotDangerButton(
                    text = "Stop",
                    onClick = onStopAutomation,
                    enabled = canStop,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Loading indicator
            if (uiState.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Initializing automation...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Status text
            if (!canStart && !uiState.isLoading) {
                Text(
                    text = getStatusMessage(uiState, selectedTeam),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Automation status indicators
 */
@Composable
private fun AutomationStatusIndicators(
    uiState: AutomationUiState,
    selectedTeam: Team?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Accessibility service status
        StatusIndicatorRow(
            label = "Accessibility Service",
            isActive = uiState.isAccessibilityServiceEnabled,
            activeText = "Enabled",
            inactiveText = "Disabled"
        )
        
        // Screen capture permission status
        StatusIndicatorRow(
            label = "Screen Capture",
            isActive = uiState.isScreenCapturePermissionGranted,
            activeText = "Permitted",
            inactiveText = "Not Permitted"
        )
        
        // Team selection status
        StatusIndicatorRow(
            label = "Team Selection",
            isActive = selectedTeam != null,
            activeText = selectedTeam?.name ?: "None",
            inactiveText = "No team selected"
        )
    }
}

/**
 * Status indicator row
 */
@Composable
private fun StatusIndicatorRow(
    label: String,
    isActive: Boolean,
    activeText: String,
    inactiveText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusIndicator(
                isActive = isActive,
                color = if (isActive) Color.Green else Color.Red
            )
            
            Text(
                text = if (isActive) activeText else inactiveText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Gets appropriate status message
 */
private fun getStatusMessage(uiState: AutomationUiState, selectedTeam: Team?): String {
    return when {
        !uiState.isAccessibilityServiceEnabled -> 
            "⚠️ Please enable accessibility service in Settings"
        !uiState.isScreenCapturePermissionGranted -> 
            "⚠️ Screen capture permission required - tap Start to grant"
        selectedTeam == null -> 
            "⚠️ Please select a team to start automation"
        else -> 
            "✅ Ready to start automation"
    }
}

/**
 * Team selection card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamSelectionCard(
    availableTeams: List<Team>,
    selectedTeam: Team?,
    onTeamSelected: (Team) -> Unit,
    onNavigateToTeams: () -> Unit,
    onRefreshTeams: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Team Selection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    IconButton(onClick = onRefreshTeams) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Teams"
                        )
                    }
                    
                    IconButton(onClick = onNavigateToTeams) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Manage Teams"
                        )
                    }
                }
            }
            
            // Team dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedTeam?.name ?: "Select a team",
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableTeams.forEach { team ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = team.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${team.servantIds.size} servants",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onTeamSelected(team)
                                expanded = false
                            }
                        )
                    }
                    
                    if (availableTeams.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "No teams available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = { }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Automation statistics card
 */
@Composable
private fun AutomationStatsCard(stats: AutomationStats) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Battles",
                    value = stats.battlesCompleted.toString(),
                    icon = Icons.Default.PlayArrow
                )
                
                StatItem(
                    label = "Won",
                    value = stats.battlesWon.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = Color.Green
                )
                
                StatItem(
                    label = "Lost",
                    value = stats.battlesLost.toString(),
                    icon = Icons.Default.Cancel,
                    color = Color.Red
                )
                
                StatItem(
                    label = "Errors",
                    value = stats.errorsEncountered.toString(),
                    icon = Icons.Default.Warning,
                    color = Color(0xFFFF9800) // Orange color
                )
            }
            
            // Runtime and average battle time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Runtime",
                    value = formatDuration(stats.totalRuntime),
                    icon = Icons.Default.Timer
                )
                
                StatItem(
                    label = "Avg Battle",
                    value = formatDuration(stats.averageBattleTime),
                    icon = Icons.Default.Speed
                )
            }
        }
    }
}

/**
 * Individual stat item
 */
@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Recent battle logs card
 */
@Composable
private fun RecentBattleLogsCard(battleLogs: List<BattleLog>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Recent Battles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (battleLogs.isEmpty()) {
                Text(
                    text = "No recent battles",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                battleLogs.take(5).forEach { log ->
                    BattleLogItem(battleLog = log)
                }
            }
        }
    }
}

/**
 * Individual battle log item
 */
@Composable
private fun BattleLogItem(battleLog: BattleLog) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Quest ${battleLog.questId}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "${formatDuration(battleLog.duration * 1000)} • ${battleLog.turnsCompleted} turns",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = when (battleLog.result) {
                    "Victory" -> Icons.Default.CheckCircle
                    "Defeat" -> Icons.Default.Cancel
                    else -> Icons.Default.Warning
                },
                contentDescription = battleLog.result,
                tint = when (battleLog.result) {
                    "Victory" -> Color.Green
                    "Defeat" -> Color.Red
                    else -> Color(0xFFFF9800) // Orange color
                },
                modifier = Modifier.size(16.dp)
            )
            
            Text(
                text = battleLog.result,
                style = MaterialTheme.typography.bodySmall,
                color = when (battleLog.result) {
                    "Victory" -> Color.Green
                    "Defeat" -> Color.Red
                    else -> Color(0xFFFF9800) // Orange color
                }
            )
        }
    }
}

/**
 * Error card for displaying error messages
 */
@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/**
 * Helper functions
 */
private fun getAutomationStateText(state: AutomationState): String {
    return when (state) {
        AutomationState.IDLE -> "Ready"
        AutomationState.INITIALIZING -> "Initializing..."
        AutomationState.RUNNING -> "Running"
        AutomationState.PAUSED -> "Paused"
        AutomationState.STOPPING -> "Stopping..."
        AutomationState.ERROR -> "Error"
        AutomationState.COMPLETED -> "Completed"
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

/**
 * Preview composables
 */
@Preview(showBackground = true)
@Composable
private fun AutomationScreenPreview() {
    FGOBotTheme {
        // Preview implementation would go here
        // This is a placeholder for the preview
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Automation Screen Preview")
        }
    }
} 