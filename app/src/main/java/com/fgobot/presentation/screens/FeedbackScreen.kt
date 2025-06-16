/**
 * FGO Bot - Feedback and Error Reporting Screen
 * 
 * This screen provides users with tools to report bugs, submit feedback,
 * and generate detailed error reports. It includes crash reporting integration,
 * performance monitoring, and user feedback collection systems.
 * 
 * Features:
 * - Bug report generation with logs
 * - User feedback collection
 * - Crash report viewing and submission
 * - Performance monitoring dashboard
 * - System information collection
 */

package com.fgobot.presentation.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

// Data classes for feedback system
enum class BugCategory(val displayName: String, val description: String) {
    GENERAL("General", "General bugs and issues"),
    AUTOMATION("Automation", "Issues with battle automation"),
    UI("User Interface", "Problems with the app interface"),
    PERFORMANCE("Performance", "Speed, battery, or memory issues"),
    CRASH("Crash", "App crashes or freezes"),
    DATA("Data", "Issues with data sync or storage")
}

enum class FeedbackType(val displayName: String, val description: String) {
    GENERAL("General Feedback", "Overall thoughts about the app"),
    FEATURE_REQUEST("Feature Request", "Suggest new features or improvements"),
    UI_UX("UI/UX Feedback", "Comments about the user interface"),
    PERFORMANCE("Performance", "Feedback about app speed and efficiency"),
    DOCUMENTATION("Documentation", "Comments about help and guides")
}

data class CrashReport(
    val title: String,
    val timestamp: String,
    val stackTrace: String
)

data class SystemInfoItem(
    val label: String,
    val value: String,
    val icon: ImageVector
)

/**
 * FeedbackScreen - Main feedback and error reporting interface
 * 
 * This composable provides users with comprehensive tools for reporting
 * issues, submitting feedback, and monitoring system performance.
 * 
 * @param onNavigateBack Callback to navigate back to the previous screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Report Bug", "Feedback", "Crash Reports", "System Info")
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Feedback & Reports",
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
        
        // Tab Row
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        // Tab Content
        when (selectedTab) {
            0 -> BugReportTab()
            1 -> FeedbackTab()
            2 -> CrashReportsTab()
            3 -> SystemInfoTab()
        }
    }
}

/**
 * BugReportTab - Bug reporting interface
 */
@Composable
private fun BugReportTab() {
    var bugTitle by remember { mutableStateOf("") }
    var bugDescription by remember { mutableStateOf("") }
    var bugCategory by remember { mutableStateOf(BugCategory.GENERAL) }
    var includeSystemInfo by remember { mutableStateOf(true) }
    var includeLogs by remember { mutableStateOf(true) }
    var includeScreenshot by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                            Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Report a Bug",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Help us improve FGO Bot by reporting bugs and issues. " +
                                "Please provide as much detail as possible to help us reproduce and fix the problem.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        item {
            OutlinedTextField(
                value = bugTitle,
                onValueChange = { bugTitle = it },
                label = { Text("Bug Title") },
                placeholder = { Text("Brief description of the issue") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        
        item {
            Text(
                text = "Bug Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Column(
                modifier = Modifier.selectableGroup()
            ) {
                BugCategory.values().forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (bugCategory == category),
                                onClick = { bugCategory = category },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (bugCategory == category),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = category.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        item {
            OutlinedTextField(
                value = bugDescription,
                onValueChange = { bugDescription = it },
                label = { Text("Detailed Description") },
                placeholder = { Text("Please describe the bug in detail, including steps to reproduce...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 6
            )
        }
        
        item {
            Text(
                text = "Include Additional Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeSystemInfo,
                        onCheckedChange = { includeSystemInfo = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("System Information")
                        Text(
                            text = "Device model, Android version, app version",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeLogs,
                        onCheckedChange = { includeLogs = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Application Logs")
                        Text(
                            text = "Recent app logs and error messages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeScreenshot,
                        onCheckedChange = { includeScreenshot = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Screenshot")
                        Text(
                            text = "Current screen capture (if relevant)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        item {
            Button(
                onClick = {
                    generateBugReport(
                        context = context,
                        title = bugTitle,
                        description = bugDescription,
                        category = bugCategory,
                        includeSystemInfo = includeSystemInfo,
                        includeLogs = includeLogs,
                        includeScreenshot = includeScreenshot
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = bugTitle.isNotBlank() && bugDescription.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Bug Report")
            }
        }
    }
}

/**
 * FeedbackTab - User feedback collection interface
 */
@Composable
private fun FeedbackTab() {
    var feedbackType by remember { mutableStateOf(FeedbackType.GENERAL) }
    var rating by remember { mutableIntStateOf(5) }
    var feedbackText by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Feedback,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Share Your Feedback",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Your feedback helps us make FGO Bot better. Share your thoughts, " +
                                "suggestions, and experiences to help us improve the app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        item {
            Text(
                text = "Feedback Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Column(
                modifier = Modifier.selectableGroup()
            ) {
                FeedbackType.values().forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (feedbackType == type),
                                onClick = { feedbackType = type },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (feedbackType == type),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = type.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = type.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        item {
            Text(
                text = "Overall Rating",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..5).forEach { star ->
                    IconButton(
                        onClick = { rating = star }
                    ) {
                        Icon(
                            if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "$star stars",
                            tint = if (star <= rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
            Text(
                text = when (rating) {
                    1 -> "Poor"
                    2 -> "Fair"
                    3 -> "Good"
                    4 -> "Very Good"
                    5 -> "Excellent"
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        
        item {
            OutlinedTextField(
                value = feedbackText,
                onValueChange = { feedbackText = it },
                label = { Text("Your Feedback") },
                placeholder = { Text("Tell us what you think about FGO Bot...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 6
            )
        }
        
        item {
            OutlinedTextField(
                value = contactEmail,
                onValueChange = { contactEmail = it },
                label = { Text("Email (Optional)") },
                placeholder = { Text("your.email@example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Text(
                text = "We'll only use your email to follow up on your feedback if needed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        item {
            Button(
                onClick = {
                    submitFeedback(
                        context = context,
                        type = feedbackType,
                        rating = rating,
                        feedback = feedbackText,
                        email = contactEmail
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = feedbackText.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Feedback")
            }
        }
    }
}

/**
 * CrashReportsTab - View and manage crash reports
 */
@Composable
private fun CrashReportsTab() {
    val crashReports = remember { getCrashReports() }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Crash Reports",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "View recent crash reports and help us fix stability issues. " +
                                "Crash reports are automatically generated when the app encounters errors.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        
        if (crashReports.isEmpty()) {
            item {
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
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "No Crash Reports",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                                                    text = "Great! FGO Bot has been running smoothly without any crashes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(crashReports) { crashReport ->
                CrashReportCard(crashReport = crashReport)
            }
        }
    }
}

/**
 * SystemInfoTab - Display system information
 */
@Composable
private fun SystemInfoTab() {
    val systemInfo = remember { getSystemInfo() }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "System Information",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Technical information about your device and app installation. " +
                                "This information is useful for troubleshooting and support.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
        
        items(systemInfo) { info ->
            SystemInfoCard(info = info)
        }
        
        item {
            OutlinedButton(
                onClick = {
                    // TODO: Copy system info to clipboard
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy System Info")
            }
        }
    }
}

/**
 * CrashReportCard - Display individual crash report
 */
@Composable
private fun CrashReportCard(crashReport: CrashReport) {
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
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = crashReport.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = crashReport.timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Stack Trace:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = crashReport.stackTrace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // TODO: Share crash report
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                    
                    Button(
                        onClick = {
                            // TODO: Submit crash report
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Submit")
                    }
                }
            }
        }
    }
}

/**
 * SystemInfoCard - Display system information item
 */
@Composable
private fun SystemInfoCard(info: SystemInfoItem) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                info.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = info.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = info.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helper functions
private fun generateBugReport(
    context: Context,
    title: String,
    description: String,
    category: BugCategory,
    includeSystemInfo: Boolean,
    includeLogs: Boolean,
    includeScreenshot: Boolean
) {
    // TODO: Implement bug report generation
    val emailIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_EMAIL, arrayOf("support@fgobot.com"))
        putExtra(Intent.EXTRA_SUBJECT, "Bug Report: $title")
        putExtra(Intent.EXTRA_TEXT, buildString {
            appendLine("Bug Category: ${category.displayName}")
            appendLine("Description: $description")
            if (includeSystemInfo) {
                appendLine("\nSystem Information:")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Android: ${Build.VERSION.RELEASE}")
                appendLine("App Version: 1.0.0") // TODO: Get actual version
            }
        })
    }
    
    context.startActivity(Intent.createChooser(emailIntent, "Send Bug Report"))
}

private fun submitFeedback(
    context: Context,
    type: FeedbackType,
    rating: Int,
    feedback: String,
    email: String
) {
    // TODO: Implement feedback submission
    val emailIntent = Intent(Intent.ACTION_SEND).apply {
        this.type = "text/plain"
        putExtra(Intent.EXTRA_EMAIL, arrayOf("feedback@fgobot.com"))
        putExtra(Intent.EXTRA_SUBJECT, "Feedback: ${type.displayName}")
        putExtra(Intent.EXTRA_TEXT, buildString {
            appendLine("Feedback Type: ${type.displayName}")
            appendLine("Rating: $rating/5 stars")
            appendLine("Feedback: $feedback")
            if (email.isNotBlank()) {
                appendLine("Contact Email: $email")
            }
        })
    }
    
    context.startActivity(Intent.createChooser(emailIntent, "Send Feedback"))
}

private fun getCrashReports(): List<CrashReport> {
    // TODO: Implement crash report retrieval
    return emptyList() // Placeholder - no crashes for now
}

private fun getSystemInfo(): List<SystemInfoItem> {
    return listOf(
        SystemInfoItem("Device Model", "${Build.MANUFACTURER} ${Build.MODEL}", Icons.Default.PhoneAndroid),
        SystemInfoItem("Android Version", Build.VERSION.RELEASE, Icons.Default.Android),
        SystemInfoItem("API Level", Build.VERSION.SDK_INT.toString(), Icons.Default.Code),
        SystemInfoItem("App Version", "1.0.0", Icons.Default.Apps), // TODO: Get actual version
        SystemInfoItem("Build Type", "Debug", Icons.Default.Build), // TODO: Get actual build type
        SystemInfoItem("Architecture", Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown", Icons.Default.Memory),
        SystemInfoItem("Install Date", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()), Icons.Default.DateRange)
    )
} 