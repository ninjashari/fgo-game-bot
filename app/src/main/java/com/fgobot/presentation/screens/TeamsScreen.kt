/**
 * FGO Bot - Teams Screen
 * 
 * This screen provides team management functionality for the FGO Bot application.
 * Users can create, edit, and manage their servant teams for automation.
 * 
 * Features:
 * - Team creation and editing
 * - Servant selection and configuration
 * - Team strategy settings
 * - Team performance analytics
 */

package com.fgobot.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fgobot.presentation.components.*
import com.fgobot.presentation.theme.FGOBotTheme

/**
 * Teams screen composable
 * 
 * @param onNavigateBack Navigation callback to go back
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsScreen(
    onNavigateBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Team Management",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
        
        // Placeholder content
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = "Teams",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Team Management",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "This feature will allow you to create and manage your servant teams for automation. Coming soon!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                FGOBotPrimaryButton(
                    text = "Create New Team",
                    onClick = { /* TODO: Implement team creation */ },
                    fillMaxWidth = true
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TeamsScreenPreview() {
    FGOBotTheme {
        TeamsScreen()
    }
} 