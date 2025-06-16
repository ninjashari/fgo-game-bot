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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fgobot.R
import com.fgobot.data.database.entities.Team
import com.fgobot.data.repository.TeamRepositoryImpl
import com.fgobot.presentation.components.*
import com.fgobot.presentation.viewmodel.TeamsViewModel
import com.fgobot.presentation.viewmodel.TeamsAction
import java.util.*

/**
 * Teams screen composable
 * 
 * @param onNavigateBack Callback for back navigation
 * @param onNavigateToHome Callback for home navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    
    // Create ViewModel with repository
    val database = com.fgobot.data.database.FGOBotDatabase.getDatabase(context)
    val teamDao = database.teamDao()
    val logger = com.fgobot.core.logging.FGOLoggerImpl()
    val repository = TeamRepositoryImpl(teamDao, logger)
    
    val viewModel: TeamsViewModel = viewModel { TeamsViewModel(repository) }
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.team_management)) },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.handleAction(TeamsAction.ShowCreateDialog) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Team")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Error message
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.handleAction(TeamsAction.ClearError) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear Error",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            
            // Loading indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // Teams list
            if (uiState.teams.isEmpty() && !uiState.isLoading) {
                // Empty state
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No teams created yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Create your first team to start automating battles",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        FGOBotPrimaryButton(
                            text = "Create Team",
                            onClick = { viewModel.handleAction(TeamsAction.ShowCreateDialog) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.teams) { team ->
                        TeamCard(
                            team = team,
                            onEdit = { /* TODO: Implement edit */ },
                            onDelete = { viewModel.handleAction(TeamsAction.DeleteTeam(team.id)) }
                        )
                    }
                }
            }
        }
    }
    
    // Create team dialog
    if (uiState.showCreateDialog) {
        CreateTeamDialog(
            onDismiss = { viewModel.handleAction(TeamsAction.HideCreateDialog) },
            onCreateTeam = { teamName ->
                val newTeam = Team(
                    id = 0, // Will be auto-generated by database
                    name = teamName,
                    description = "Created team",
                    servantIds = emptyList(),
                    craftEssenceIds = emptyList(),
                    strategy = "Auto"
                )
                viewModel.handleAction(TeamsAction.CreateTeam(newTeam))
            }
        )
    }
}

/**
 * Team card component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamCard(
    team: Team,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Team")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Team",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Team info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Servants: ${team.servantIds.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Strategy: ${team.strategy}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (team.isDefault) "Default" else "Custom",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (team.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
    onCreateTeam: (String) -> Unit
) {
    var teamName by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Team") },
        text = {
            Column {
                Text(
                    text = "Enter a name for your new team:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = teamName,
                    onValueChange = { 
                        teamName = it
                        isError = it.isBlank()
                    },
                    label = { Text("Team Name") },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Team name cannot be empty") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            FGOBotPrimaryButton(
                text = "Create",
                onClick = {
                    if (teamName.isNotBlank()) {
                        onCreateTeam(teamName.trim())
                    } else {
                        isError = true
                    }
                },
                enabled = teamName.isNotBlank()
            )
        },
        dismissButton = {
            FGOBotSecondaryButton(
                text = "Cancel",
                onClick = onDismiss
            )
        }
    )
}

 