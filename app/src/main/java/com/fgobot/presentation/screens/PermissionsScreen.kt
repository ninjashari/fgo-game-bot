/**
 * FGO Bot - Permissions Screen
 * 
 * This screen provides a comprehensive permission management interface for the FGO Bot application.
 * Users can easily view and grant all required permissions from a single location.
 * 
 * Features:
 * - Permission status overview
 * - Direct permission granting
 * - Step-by-step guidance
 * - Permission explanations
 */

package com.fgobot.presentation.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fgobot.presentation.components.*
import com.fgobot.presentation.theme.FGOBotTheme
import com.fgobot.presentation.viewmodel.AutomationViewModel
import com.fgobot.presentation.viewmodel.AutomationAction

/**
 * Permission item data class
 */
data class PermissionItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isGranted: Boolean,
    val isRequired: Boolean = true,
    val actionText: String = "Grant",
    val onAction: () -> Unit
)

/**
 * Permissions screen composable
 * 
 * @param viewModel AutomationViewModel for permission management
 * @param onNavigateBack Callback for back navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    viewModel: AutomationViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Create permission items list
    val permissionItems = remember(uiState) {
        listOf(
            PermissionItem(
                title = "Accessibility Service",
                description = "Required for automating game interactions and reading screen content. This allows the bot to tap buttons, select cards, and navigate menus automatically.",
                icon = Icons.Default.Accessibility,
                isGranted = uiState.isAccessibilityServiceEnabled,
                actionText = "Enable",
                onAction = { viewModel.handleAction(AutomationAction.OpenAccessibilitySettings) }
            ),
            PermissionItem(
                title = "Screen Capture",
                description = "Required for taking screenshots to analyze the game state. This allows the bot to see what's happening on screen and make intelligent decisions.",
                icon = Icons.Default.Screenshot,
                isGranted = uiState.isScreenCapturePermissionGranted,
                actionText = "Grant",
                onAction = { viewModel.handleAction(AutomationAction.RequestScreenCapturePermission) }
            ),
            PermissionItem(
                title = "Display Over Other Apps",
                description = "Optional permission that allows the bot to show overlay controls while the game is running. Useful for monitoring automation status.",
                icon = Icons.Default.Layers,
                isGranted = Settings.canDrawOverlays(context),
                isRequired = false,
                actionText = "Enable",
                onAction = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )
        )
    }
    
    val allRequiredGranted = permissionItems.filter { it.isRequired }.all { it.isGranted }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Header
            item {
                PermissionsHeader(allRequiredGranted = allRequiredGranted)
            }
            
            // Permission items
            items(permissionItems) { permission ->
                PermissionCard(permission = permission)
            }
            
            // Setup complete card
            if (allRequiredGranted) {
                item {
                    SetupCompleteCard(onNavigateBack = onNavigateBack)
                }
            }
            
            // Help section
            item {
                PermissionsHelpCard()
            }
        }
    }
}

/**
 * Permissions header with overall status
 */
@Composable
private fun PermissionsHeader(allRequiredGranted: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (allRequiredGranted) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (allRequiredGranted) Icons.Default.CheckCircle else Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (allRequiredGranted) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (allRequiredGranted) "All Set!" else "Permissions Required",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (allRequiredGranted) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (allRequiredGranted) 
                    "FGO Bot is ready to automate your gameplay! You can now start using the automation features." 
                else 
                    "FGO Bot needs certain permissions to function properly. Please grant the required permissions below.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = if (allRequiredGranted) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Individual permission card
 */
@Composable
private fun PermissionCard(permission: PermissionItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (permission.isGranted) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.surface
        )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = permission.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (permission.isGranted) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = permission.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            if (!permission.isRequired) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "Optional",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (permission.isGranted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (permission.isGranted) Color.Green else Color.Red
                            )
                            
                            Text(
                                text = if (permission.isGranted) "Granted" else "Not Granted",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (permission.isGranted) Color.Green else Color.Red,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                if (!permission.isGranted) {
                    FGOBotPrimaryButton(
                        text = permission.actionText,
                        onClick = permission.onAction
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = permission.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Setup complete card
 */
@Composable
private fun SetupCompleteCard(onNavigateBack: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Celebration,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Setup Complete!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "All required permissions have been granted. You can now use FGO Bot to automate your gameplay!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            FGOBotPrimaryButton(
                text = "Start Automating",
                onClick = onNavigateBack,
                fillMaxWidth = true
            )
        }
    }
}

/**
 * Permissions help card
 */
@Composable
private fun PermissionsHelpCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Help,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Need Help?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "If you're having trouble granting permissions:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val helpSteps = listOf(
                "Make sure you're following the on-screen instructions carefully",
                "Some permissions require navigating through multiple settings screens",
                "You may need to restart the app after granting certain permissions",
                "Check your device's notification settings if permissions aren't working"
            )
            
            helpSteps.forEach { step ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "• ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
} 