/**
 * FGO Bot - Home Screen
 * 
 * This screen serves as the main dashboard for the FGO Bot application.
 * It provides an overview of the automation status, quick access to main features,
 * and displays important information and statistics.
 * 
 * Features:
 * - Quick automation status overview
 * - Navigation to main features
 * - Recent activity summary
 * - System status indicators
 */

package com.fgobot.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.fgobot.R
import com.fgobot.presentation.components.*
import com.fgobot.presentation.theme.FGOBotTheme
import kotlinx.coroutines.delay

/**
 * Home screen composable
 * 
 * @param onNavigateToAutomation Navigation callback for automation screen
 * @param onNavigateToTeams Navigation callback for teams screen
 * @param onNavigateToSettings Navigation callback for settings screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAutomation: () -> Unit = {},
    onNavigateToTeams: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome header
        item {
            WelcomeHeader()
        }
        
        // Quick actions
        item {
            QuickActionsCard(
                onNavigateToAutomation = onNavigateToAutomation,
                onNavigateToTeams = onNavigateToTeams,
                onNavigateToSettings = onNavigateToSettings
            )
        }
        
        // System status
        item {
            SystemStatusCard()
        }
        
        // Recent activity
        item {
            RecentActivityCard()
        }
        
        // Tips and information
        item {
            TipsCard()
        }
    }
}

/**
 * Welcome header section
 */
@Composable
private fun WelcomeHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "FGO Bot",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = stringResource(R.string.main_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = stringResource(R.string.main_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Quick actions card
 */
@Composable
private fun QuickActionsCard(
    onNavigateToAutomation: () -> Unit,
    onNavigateToTeams: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    QuickActionButton(
                        icon = Icons.Default.PlayArrow,
                        label = "Start Automation",
                        onClick = onNavigateToAutomation,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                item {
                    QuickActionButton(
                        icon = Icons.Default.Group,
                        label = "Manage Teams",
                        onClick = onNavigateToTeams,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                item {
                    QuickActionButton(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        onClick = onNavigateToSettings,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

/**
 * Quick action button
 */
@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        modifier = Modifier.width(120.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(32.dp),
                tint = color
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = color
            )
        }
    }
}

/**
 * System status card
 */
@Composable
private fun SystemStatusCard() {
    // Check accessibility service status dynamically
    val isAccessibilityEnabled = remember { mutableStateOf(false) }
    
    // Update accessibility status periodically
    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityEnabled.value = com.fgobot.core.FGOAccessibilityService.isServiceRunning()
            delay(2000L) // Check every 2 seconds
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "System Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            StatusItem(
                label = "Accessibility Service",
                status = if (isAccessibilityEnabled.value) "Enabled" else "Disabled",
                isGood = isAccessibilityEnabled.value
            )
            
            StatusItem(
                label = "Screen Capture",
                status = "Not Granted",
                isGood = false
            )
            
            StatusItem(
                label = "OpenCV Engine",
                status = "Loaded",
                isGood = true
            )
            
            StatusItem(
                label = "Database",
                status = "Connected",
                isGood = true
            )
        }
    }
}

/**
 * Status item component
 */
@Composable
private fun StatusItem(
    label: String,
    status: String,
    isGood: Boolean
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
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isGood) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = status,
                modifier = Modifier.size(16.dp),
                tint = if (isGood) Color.Green else Color.Red
            )
            
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = if (isGood) Color.Green else Color.Red,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Recent activity card
 */
@Composable
private fun RecentActivityCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Placeholder for recent activity
            Text(
                text = "No recent activity",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Tips and information card
 */
@Composable
private fun TipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Tips",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Text(
                    text = "Tips & Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            TipItem(
                text = "Make sure to enable the Accessibility Service for automation to work properly."
            )
            
            TipItem(
                text = "Create and configure your teams before starting automation."
            )
            
            TipItem(
                text = "Monitor the automation regularly and check battle logs for performance insights."
            )
        }
    }
}

/**
 * Individual tip item
 */
@Composable
private fun TipItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Circle,
            contentDescription = null,
            modifier = Modifier.size(6.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Preview composable
 */
@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    FGOBotTheme {
        HomeScreen()
    }
} 