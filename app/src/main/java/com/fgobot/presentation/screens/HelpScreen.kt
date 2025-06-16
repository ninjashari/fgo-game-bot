/**
 * FGO Bot - Help Screen
 * 
 * This screen provides comprehensive help documentation for users.
 * It includes searchable content, FAQ sections, troubleshooting guides,
 * and links to additional resources and tutorials.
 * 
 * Features:
 * - Searchable help content
 * - Categorized FAQ sections
 * - Step-by-step troubleshooting guides
 * - Video tutorial links
 * - Contact and support information
 */

package com.fgobot.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * HelpScreen - Comprehensive help and documentation screen
 * 
 * This composable provides users with searchable help content,
 * frequently asked questions, troubleshooting guides, and
 * additional resources to help them use the FGO Bot effectively.
 * 
 * @param onNavigateBack Callback to navigate back to the previous screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(HelpCategory.ALL) }
    
    val helpItems = remember { getHelpItems() }
    val filteredItems = remember(searchQuery, selectedCategory) {
        helpItems.filter { item ->
            val matchesSearch = searchQuery.isEmpty() || 
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.content.contains(searchQuery, ignoreCase = true) ||
                    item.tags.any { it.contains(searchQuery, ignoreCase = true) }
            
            val matchesCategory = selectedCategory == HelpCategory.ALL || item.category == selectedCategory
            
            matchesSearch && matchesCategory
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Help & Support",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search help topics...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true
        )
        
        // Category Filter
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(HelpCategory.values()) { category ->
                FilterChip(
                    onClick = { selectedCategory = category },
                    label = { Text(category.displayName) },
                    selected = selectedCategory == category,
                    leadingIcon = {
                        Icon(
                            category.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Help Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredItems.isEmpty()) {
                item {
                    EmptySearchResults(searchQuery = searchQuery)
                }
            } else {
                items(filteredItems) { helpItem ->
                    HelpItemCard(helpItem = helpItem)
                }
            }
            
            // Additional Resources Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                AdditionalResourcesSection()
            }
            
            // Contact Support Section
            item {
                ContactSupportSection()
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * HelpItemCard - Expandable card for help content
 */
@Composable
private fun HelpItemCard(helpItem: HelpItem) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        helpItem.category.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = helpItem.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (helpItem.subtitle.isNotEmpty()) {
                            Text(
                                text = helpItem.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Divider()
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = helpItem.content,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                    
                    if (helpItem.steps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Steps:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        helpItem.steps.forEachIndexed { index, step ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(20.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (index + 1).toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    if (helpItem.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(helpItem.tags) { tag ->
                                AssistChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * EmptySearchResults - Display when no search results found
 */
@Composable
private fun EmptySearchResults(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No results found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        if (searchQuery.isNotEmpty()) {
            Text(
                text = "No help topics match \"$searchQuery\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Try searching for different keywords or browse all categories",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * AdditionalResourcesSection - Links to external resources
 */
@Composable
private fun AdditionalResourcesSection() {
    val uriHandler = LocalUriHandler.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Additional Resources",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val resources = listOf(
                Resource("Video Tutorials", "Watch step-by-step guides", Icons.Default.PlayArrow, "https://example.com/tutorials"),
                Resource("User Manual", "Complete documentation", Icons.Default.MenuBook, "https://example.com/manual"),
                Resource("Community Forum", "Get help from other users", Icons.Default.Forum, "https://example.com/forum"),
                Resource("GitHub Repository", "Source code and issues", Icons.Default.Code, "https://github.com/example/fgo-bot")
            )
            
            resources.forEach { resource ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri(resource.url) }
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            resource.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = resource.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = resource.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = "Open link",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * ContactSupportSection - Contact information and support options
 */
@Composable
private fun ContactSupportSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Need More Help?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "If you can't find the answer you're looking for, don't hesitate to reach out for support.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        // TODO: Open email client
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Email Support")
                }
                
                OutlinedButton(
                    onClick = {
                        // TODO: Generate bug report
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Report Bug")
                }
            }
        }
    }
}

// Data classes for help content
data class HelpItem(
    val title: String,
    val subtitle: String = "",
    val content: String,
    val steps: List<String> = emptyList(),
    val category: HelpCategory,
    val tags: List<String> = emptyList()
)

data class Resource(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val url: String
)

enum class HelpCategory(val displayName: String, val icon: ImageVector) {
    ALL("All", Icons.Default.Help),
    GETTING_STARTED("Getting Started", Icons.Default.PlayArrow),
    AUTOMATION("Automation", Icons.Default.SmartToy),
    TEAMS("Teams", Icons.Default.Group),
    SETTINGS("Settings", Icons.Default.Settings),
    TROUBLESHOOTING("Troubleshooting", Icons.Default.Build),
    FAQ("FAQ", Icons.Default.QuestionAnswer)
}

// Helper function to get help content
private fun getHelpItems(): List<HelpItem> = listOf(
    HelpItem(
        title = "Getting Started with FGO Bot",
        subtitle = "Basic setup and first automation",
        content = "Learn how to set up FGO Bot for the first time and run your first automated battle. This guide covers the essential steps to get you started quickly and safely.",
        steps = listOf(
            "Complete the welcome tutorial",
            "Enable accessibility service",
            "Create your first team",
            "Configure basic automation settings",
            "Start with a simple quest"
        ),
        category = HelpCategory.GETTING_STARTED,
        tags = listOf("setup", "tutorial", "first time", "beginner")
    ),
    
    HelpItem(
        title = "How to Create and Manage Teams",
        subtitle = "Team composition and strategy",
        content = "Teams are the foundation of successful automation. Learn how to create effective team compositions, assign servants and craft essences, and configure battle strategies.",
        steps = listOf(
            "Go to the Teams screen",
            "Tap 'Create New Team'",
            "Select your servants for each position",
            "Assign craft essences",
            "Configure skill priorities",
            "Set NP usage strategy",
            "Save your team"
        ),
        category = HelpCategory.TEAMS,
        tags = listOf("team", "servants", "craft essence", "strategy")
    ),
    
    HelpItem(
        title = "Automation Not Starting",
        subtitle = "Common startup issues",
        content = "If automation fails to start, this is usually due to missing permissions or incorrect setup. Follow these troubleshooting steps to resolve the issue.",
        steps = listOf(
            "Check if accessibility service is enabled",
            "Verify FGO is running and visible",
            "Ensure you're on a supported screen",
            "Check if a team is selected",
            "Restart the app if needed"
        ),
        category = HelpCategory.TROUBLESHOOTING,
        tags = listOf("not starting", "permissions", "accessibility", "troubleshooting")
    ),
    
    HelpItem(
        title = "Understanding Automation Settings",
        subtitle = "Customize automation behavior",
        content = "FGO Bot offers extensive customization options to match your playstyle. Learn about each setting and how to optimize them for your needs.",
        category = HelpCategory.SETTINGS,
        tags = listOf("settings", "customization", "configuration", "optimization")
    ),
    
    HelpItem(
        title = "Battle Strategy Configuration",
        subtitle = "Advanced automation tactics",
        content = "Configure advanced battle strategies including card priority, skill usage timing, NP management, and enemy targeting to maximize your automation effectiveness.",
        steps = listOf(
            "Open team configuration",
            "Set card selection priority",
            "Configure skill usage conditions",
            "Set NP timing preferences",
            "Define enemy targeting rules",
            "Test with different quest types"
        ),
        category = HelpCategory.AUTOMATION,
        tags = listOf("strategy", "cards", "skills", "NP", "targeting")
    ),
    
    HelpItem(
        title = "Performance Optimization",
        subtitle = "Improve speed and efficiency",
        content = "Optimize FGO Bot's performance for your device. Learn about settings that affect speed, battery usage, and automation reliability.",
        category = HelpCategory.SETTINGS,
        tags = listOf("performance", "speed", "battery", "optimization")
    ),
    
    HelpItem(
        title = "Error Recovery and Handling",
        subtitle = "What happens when things go wrong",
        content = "FGO Bot includes sophisticated error recovery mechanisms. Understand how the bot handles unexpected situations and how to configure error handling behavior.",
        category = HelpCategory.AUTOMATION,
        tags = listOf("errors", "recovery", "handling", "unexpected")
    ),
    
    HelpItem(
        title = "Statistics and Analytics",
        subtitle = "Monitor your automation performance",
        content = "Track your automation performance with detailed statistics. Learn how to interpret the data and use it to improve your setup.",
        category = HelpCategory.AUTOMATION,
        tags = listOf("statistics", "analytics", "performance", "monitoring")
    ),
    
    HelpItem(
        title = "Frequently Asked Questions",
        subtitle = "Common questions and answers",
        content = "Find answers to the most commonly asked questions about FGO Bot, including safety, compatibility, and usage guidelines.",
        category = HelpCategory.FAQ,
        tags = listOf("FAQ", "common", "questions", "answers")
    ),
    
    HelpItem(
        title = "Safety and Anti-Detection",
        subtitle = "Using FGO Bot safely",
        content = "Learn about FGO Bot's anti-detection features and best practices for safe automation. Understand the risks and how to minimize them.",
        category = HelpCategory.FAQ,
        tags = listOf("safety", "detection", "ban", "risk", "best practices")
    ),
    
    HelpItem(
        title = "Supported Game Versions",
        subtitle = "Compatibility information",
        content = "Check which versions of Fate/Grand Order are supported by FGO Bot and what to do when the game updates.",
        category = HelpCategory.FAQ,
        tags = listOf("compatibility", "versions", "updates", "support")
    ),
    
    HelpItem(
        title = "Backup and Restore Settings",
        subtitle = "Protect your configuration",
        content = "Learn how to backup your teams, settings, and automation configurations, and how to restore them when needed.",
        steps = listOf(
            "Go to Settings",
            "Find 'Backup & Restore'",
            "Create a backup file",
            "Store it safely",
            "Use 'Restore' to recover settings"
        ),
        category = HelpCategory.SETTINGS,
        tags = listOf("backup", "restore", "settings", "configuration")
    )
) 