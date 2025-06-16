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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fgobot.data.database.entities.Team
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
    var showCreateTeamDialog by remember { mutableStateOf(false) }
    var teams by remember { mutableStateOf(listOf<Team>()) }
    
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
            
            Row {
                IconButton(onClick = { showCreateTeamDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Team"
                    )
                }
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        }
        
        // Teams list or empty state
        if (teams.isEmpty()) {
            // Empty state
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
                        text = "No Teams Created",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Create your first team to start automating FGO battles. Teams help organize your servants and strategies.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    FGOBotPrimaryButton(
                        text = "Create New Team",
                        onClick = { showCreateTeamDialog = true },
                        fillMaxWidth = true
                    )
                }
            }
        } else {
            // Teams list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(teams) { team ->
                    TeamCard(
                        team = team,
                        onEdit = { /* TODO: Implement edit */ },
                        onDelete = { 
                            teams = teams.filter { it.id != team.id }
                        }
                    )
                }
            }
        }
    }
    
    // Create team dialog
    if (showCreateTeamDialog) {
        CreateTeamDialog(
            onDismiss = { showCreateTeamDialog = false },
            onTeamCreated = { newTeam ->
                teams = teams + newTeam
                showCreateTeamDialog = false
            }
        )
    }
}

/**
 * Team card component
 */
@Composable
private fun TeamCard(
    team: Team,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = team.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (team.description.isNotEmpty()) {
                        Text(
                            text = team.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Team"
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Team",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Team stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Strategy: ${team.strategy}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Win Rate: ${String.format("%.1f", team.winRate)}%",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Used: ${team.usageCount} times",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Create team dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTeamDialog(
    onDismiss: () -> Unit,
    onTeamCreated: (Team) -> Unit
) {
    var teamName by remember { mutableStateOf("") }
    var teamDescription by remember { mutableStateOf("") }
    var selectedStrategy by remember { mutableStateOf("Auto") }
    var supportClass by remember { mutableStateOf("Any") }
    
    val strategies = listOf("Auto", "Farming", "Challenge", "Custom")
    val servantClasses = listOf("Any", "Saber", "Archer", "Lancer", "Rider", "Caster", "Assassin", "Berserker")
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Create New Team",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Team name
                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Team Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Team description
                OutlinedTextField(
                    value = teamDescription,
                    onValueChange = { teamDescription = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                // Strategy selection
                var strategyExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = strategyExpanded,
                    onExpandedChange = { strategyExpanded = !strategyExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedStrategy,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Strategy") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = strategyExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = strategyExpanded,
                        onDismissRequest = { strategyExpanded = false }
                    ) {
                        strategies.forEach { strategy ->
                            DropdownMenuItem(
                                text = { Text(strategy) },
                                onClick = {
                                    selectedStrategy = strategy
                                    strategyExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Support class selection
                var supportExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = supportExpanded,
                    onExpandedChange = { supportExpanded = !supportExpanded }
                ) {
                    OutlinedTextField(
                        value = supportClass,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Preferred Support Class") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supportExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = supportExpanded,
                        onDismissRequest = { supportExpanded = false }
                    ) {
                        servantClasses.forEach { servantClass ->
                            DropdownMenuItem(
                                text = { Text(servantClass) },
                                onClick = {
                                    supportClass = servantClass
                                    supportExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            if (teamName.isNotBlank()) {
                                val newTeam = Team(
                                    id = System.currentTimeMillis(), // Simple ID generation
                                    name = teamName.trim(),
                                    description = teamDescription.trim(),
                                    servantIds = emptyList(), // Will be configured later
                                    craftEssenceIds = emptyList(), // Will be configured later
                                    supportServantClass = supportClass,
                                    strategy = selectedStrategy,
                                    createdAt = System.currentTimeMillis(),
                                    lastUpdated = System.currentTimeMillis()
                                )
                                onTeamCreated(newTeam)
                            }
                        },
                        enabled = teamName.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Create")
                    }
                }
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