/**
 * FGO Bot - Welcome Screen
 * 
 * This screen provides the initial user onboarding experience for new users.
 * It introduces the app's features, guides users through initial setup,
 * and ensures they understand the core functionality before proceeding.
 * 
 * Features:
 * - App introduction with feature highlights
 * - Step-by-step setup wizard
 * - Accessibility service setup guide
 * - Interactive tutorials
 * - Terms of service and privacy policy
 */

package com.fgobot.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fgobot.R

/**
 * WelcomeScreen - Main onboarding screen for new users
 * 
 * This composable provides a comprehensive introduction to the FGO Bot
 * application, including feature highlights, setup instructions, and
 * interactive tutorials to help users get started.
 * 
 * @param onNavigateToMain Callback to navigate to the main app after onboarding
 * @param onSkipOnboarding Callback to skip the onboarding process
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onNavigateToMain: () -> Unit,
    onSkipOnboarding: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 5
    
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        // Top App Bar with Progress
        TopAppBar(
            title = {
                Text(
                    text = "Welcome to FGO Bot",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                TextButton(onClick = onSkipOnboarding) {
                    Text("Skip")
                }
            }
        )
        
        // Progress Indicator
        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / totalSteps },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Content based on current step
        AnimatedVisibility(
            visible = true,
            enter = slideInHorizontally() + fadeIn(),
            exit = slideOutHorizontally() + fadeOut()
        ) {
            when (currentStep) {
                0 -> WelcomeIntroStep()
                1 -> FeaturesOverviewStep()
                2 -> SetupRequirementsStep()
                3 -> AccessibilitySetupStep()
                4 -> FinalSetupStep(onNavigateToMain)
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Navigation Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back Button
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back")
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Next/Finish Button
            Button(
                onClick = {
                    if (currentStep < totalSteps - 1) {
                        currentStep++
                    } else {
                        onNavigateToMain()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (currentStep < totalSteps - 1) "Next" else "Get Started")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    if (currentStep < totalSteps - 1) Icons.Default.ArrowForward else Icons.Default.Check,
                    contentDescription = null
                )
            }
        }
    }
}

/**
 * WelcomeIntroStep - Introduction step with app overview
 */
@Composable
private fun WelcomeIntroStep() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // App Logo and Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // TODO: Add app logo
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = "FGO Bot Logo",
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "FGO Bot",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Intelligent Automation for Fate/Grand Order",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Welcome to the Future of FGO Gaming!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "FGO Bot is an advanced automation tool designed to enhance your Fate/Grand Order experience. " +
                                "With intelligent battle strategies, team optimization, and seamless automation, " +
                                "you can focus on enjoying the story while the bot handles the grinding.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        item {
            Text(
                text = "Key Benefits",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        items(
            listOf(
                "⚡ Intelligent battle automation with strategic decision making",
                "🎯 Optimized team compositions for maximum efficiency",
                "📊 Real-time performance monitoring and analytics",
                "🛡️ Advanced anti-detection with human-like behavior",
                "🔧 Fully customizable automation settings",
                "📱 Beautiful, intuitive user interface"
            )
        ) { benefit ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = benefit,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

/**
 * FeaturesOverviewStep - Detailed features overview
 */
@Composable
private fun FeaturesOverviewStep() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Core Features",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        items(getFeaturesList()) { feature ->
            FeatureCard(feature = feature)
        }
    }
}

/**
 * SetupRequirementsStep - System requirements and setup information
 */
@Composable
private fun SetupRequirementsStep() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Setup Requirements",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Important Notice",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "This app requires accessibility service permissions to function. " +
                                "It will interact with your device screen to automate FGO gameplay. " +
                                "Please ensure you understand and consent to this functionality.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        items(getRequirementsList()) { requirement ->
            RequirementCard(requirement = requirement)
        }
    }
}

/**
 * AccessibilitySetupStep - Guide for setting up accessibility service
 */
@Composable
private fun AccessibilitySetupStep() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Accessibility Service Setup",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Why Accessibility Service?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "The accessibility service allows FGO Bot to interact with your screen, " +
                                "detect game elements, and perform automated actions. This is essential " +
                                "for the automation functionality to work properly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
        
        items(getAccessibilitySteps()) { step ->
            SetupStepCard(step = step)
        }
        
        item {
            Button(
                onClick = {
                    // TODO: Open accessibility settings
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Accessibility Settings")
            }
        }
    }
}

/**
 * FinalSetupStep - Final setup and completion
 */
@Composable
private fun FinalSetupStep(onNavigateToMain: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Setup Complete!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "You're all set to start using FGO Bot. The app is now ready to help you " +
                    "automate your Fate/Grand Order gameplay with intelligent strategies and " +
                    "optimized performance.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
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
                    text = "Quick Tips to Get Started:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                listOf(
                    "1. Set up your teams in the Teams section",
                    "2. Configure automation settings to your preference",
                    "3. Start with simple quests to test the automation",
                    "4. Monitor performance in the Statistics dashboard"
                ).forEach { tip ->
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * FeatureCard - Card component for displaying feature information
 */
@Composable
private fun FeatureCard(feature: Feature) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                feature.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * RequirementCard - Card component for displaying system requirements
 */
@Composable
private fun RequirementCard(requirement: Requirement) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (requirement.isMet) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (requirement.isMet) Color(0xFF4CAF50) else Color(0xFFFF9800)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = requirement.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = requirement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * SetupStepCard - Card component for displaying setup steps
 */
@Composable
private fun SetupStepCard(step: SetupStep) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = step.stepNumber.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Data classes for onboarding content
data class Feature(
    val icon: ImageVector,
    val title: String,
    val description: String
)

data class Requirement(
    val title: String,
    val description: String,
    val isMet: Boolean
)

data class SetupStep(
    val stepNumber: Int,
    val title: String,
    val description: String
)

// Helper functions to get content data
private fun getFeaturesList(): List<Feature> = listOf(
    Feature(
        Icons.Default.SmartToy,
        "Intelligent Automation",
        "Advanced AI-powered battle strategies with optimal card selection and skill usage"
    ),
    Feature(
        Icons.Default.Group,
        "Team Optimization",
        "Automatic team composition analysis and optimization for maximum efficiency"
    ),
    Feature(
        Icons.Default.Analytics,
        "Performance Analytics",
        "Real-time monitoring with detailed statistics and performance insights"
    ),
    Feature(
        Icons.Default.Security,
        "Anti-Detection",
        "Human-like behavior patterns to ensure safe and undetectable automation"
    ),
    Feature(
        Icons.Default.Settings,
        "Customizable Settings",
        "Fully configurable automation parameters to match your playstyle"
    ),
    Feature(
        Icons.Default.Speed,
        "High Performance",
        "Optimized for speed and efficiency with minimal battery impact"
    )
)

private fun getRequirementsList(): List<Requirement> = listOf(
    Requirement(
        "Android 7.0 or higher",
        "Required for accessibility service functionality",
        true // TODO: Check actual device version
    ),
    Requirement(
        "Accessibility Service Permission",
        "Allows the app to interact with your screen for automation",
        false // TODO: Check if permission is granted
    ),
    Requirement(
        "Fate/Grand Order Installed",
        "The target game must be installed and accessible",
        true // TODO: Check if FGO is installed
    ),
    Requirement(
        "Sufficient Storage Space",
        "At least 100MB free space for templates and logs",
        true // TODO: Check available storage
    ),
    Requirement(
        "Stable Internet Connection",
        "Required for game data synchronization and updates",
        true // TODO: Check network connectivity
    )
)

private fun getAccessibilitySteps(): List<SetupStep> = listOf(
    SetupStep(
        1,
        "Open Settings",
        "Go to your device's main Settings app"
    ),
    SetupStep(
        2,
        "Find Accessibility",
        "Navigate to Accessibility or Accessibility Services"
    ),
    SetupStep(
        3,
        "Locate FGO Bot",
        "Find 'FGO Bot' in the list of available services"
    ),
    SetupStep(
        4,
        "Enable Service",
        "Toggle the switch to enable FGO Bot accessibility service"
    ),
    SetupStep(
        5,
        "Confirm Permission",
        "Accept the permission dialog to allow screen interaction"
    )
) 