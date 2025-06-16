/**
 * FGO Bot - Main Activity
 * 
 * This is the main entry point of the FGO Bot application.
 * It sets up the Jetpack Compose UI and handles the primary user interface.
 * 
 * Features:
 * - Jetpack Compose UI setup
 * - Material Design 3 theming
 * - Navigation framework ready
 * - Accessibility service integration
 * - Runtime permission handling
 */

package com.fgobot.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.fgobot.presentation.navigation.FGOBotNavigation
import com.fgobot.presentation.theme.FGOBotTheme
import com.fgobot.presentation.viewmodel.AutomationViewModel
import com.fgobot.data.repository.TeamRepository
import com.fgobot.data.repository.BattleLogRepository
import com.fgobot.core.logging.FGOBotLogger

/**
 * MainActivity - Primary activity for the FGO Bot application
 * 
 * This activity serves as the main entry point and container for the
 * Jetpack Compose UI. It handles the overall app navigation and
 * lifecycle management with the new navigation system.
 */
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    // TODO: Implement proper dependency injection
    private lateinit var automationViewModel: AutomationViewModel
    private val logger = FGOBotLogger
    
    // Permission request launchers
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { (permission, isGranted) ->
            logger.info(FGOBotLogger.Category.GENERAL, "Permission $permission: ${if (isGranted) "granted" else "denied"}")
        }
    }
    
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Settings.canDrawOverlays(this)) {
            logger.info(FGOBotLogger.Category.GENERAL, "Overlay permission granted")
        } else {
            logger.warn(FGOBotLogger.Category.GENERAL, "Overlay permission denied")
        }
    }
    
    /**
     * Called when the activity is first created.
     * Sets up the Compose UI with the FGO Bot theme and navigation.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        logger.info(FGOBotLogger.Category.GENERAL, "MainActivity onCreate")
        
        // Request necessary permissions
        requestRequiredPermissions()
        
        // TODO: Replace with proper dependency injection
        // For now, we'll create placeholder repositories
        val teamRepository = createPlaceholderTeamRepository()
        val battleLogRepository = createPlaceholderBattleLogRepository()
        
        // Create ViewModel with factory
        automationViewModel = ViewModelProvider(
            this,
            AutomationViewModelFactory(application, teamRepository, battleLogRepository)
        )[AutomationViewModel::class.java]
        
        setContent {
            FGOBotTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FGOBotNavigation(automationViewModel = automationViewModel)
                }
            }
        }
    }
    
    /**
     * Request all required permissions for the app to function properly
     */
    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        // Storage permissions (for Android 12 and below)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        // Media permissions for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            
            // Notification permission for Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Camera permission (some devices require this for screen capture)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        
        // Request dangerous permissions
        if (permissionsToRequest.isNotEmpty()) {
            logger.info(FGOBotLogger.Category.GENERAL, "Requesting permissions: ${permissionsToRequest.joinToString()}")
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
        
        // Request overlay permission (SYSTEM_ALERT_WINDOW)
        if (!Settings.canDrawOverlays(this)) {
            logger.info(FGOBotLogger.Category.GENERAL, "Requesting overlay permission")
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }
    
    // TODO: Replace with proper dependency injection
    private fun createPlaceholderTeamRepository(): TeamRepository {
        // This is a placeholder - in a real implementation, this would be injected
        return object : TeamRepository {
            override fun getAllTeams() = kotlinx.coroutines.flow.flowOf(emptyList<com.fgobot.data.database.entities.Team>())
            override suspend fun getTeamById(teamId: Long) = null
            override suspend fun createTeam(team: com.fgobot.data.database.entities.Team) = Result.success(0L)
            override suspend fun updateTeam(team: com.fgobot.data.database.entities.Team) = Result.success(Unit)
            override suspend fun deleteTeam(teamId: Long) = Result.success(Unit)
            override suspend fun getTeamStats() = com.fgobot.data.repository.TeamStats(0)
        }
    }
    
    private fun createPlaceholderBattleLogRepository(): BattleLogRepository {
        // This is a placeholder - in a real implementation, this would be injected
        return object : BattleLogRepository {
            override fun getAllBattleLogs() = kotlinx.coroutines.flow.flowOf(emptyList<com.fgobot.data.database.entities.BattleLog>())
            override suspend fun getBattleLogById(battleLogId: Long) = null
            override fun getBattleLogsByQuest(questId: Int) = kotlinx.coroutines.flow.flowOf(emptyList<com.fgobot.data.database.entities.BattleLog>())
            override suspend fun recordBattleLog(battleLog: com.fgobot.data.database.entities.BattleLog) = Result.success(0L)
            override suspend fun getBattleAnalytics() = com.fgobot.data.repository.BattleAnalytics(0, 0, 0.0, 0L)
        }
    }
}

/**
 * ViewModel factory for AutomationViewModel
 * TODO: Replace with proper dependency injection framework
 */
class AutomationViewModelFactory(
    private val application: android.app.Application,
    private val teamRepository: TeamRepository,
    private val battleLogRepository: BattleLogRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AutomationViewModel::class.java)) {
            return AutomationViewModel(application, teamRepository, battleLogRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 