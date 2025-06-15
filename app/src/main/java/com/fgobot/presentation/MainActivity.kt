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
 */

package com.fgobot.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fgobot.R
import com.fgobot.presentation.theme.FGOBotTheme

/**
 * MainActivity - Primary activity for the FGO Bot application
 * 
 * This activity serves as the main entry point and container for the
 * Jetpack Compose UI. It handles the overall app navigation and
 * lifecycle management.
 */
class MainActivity : ComponentActivity() {
    
    /**
     * Called when the activity is first created.
     * Sets up the Compose UI with the FGO Bot theme.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            FGOBotTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

/**
 * MainScreen - Primary composable for the main screen
 * 
 * This composable displays the main interface of the FGO Bot application.
 * Currently shows a welcome message and app information.
 */
@Composable
fun MainScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.main_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = stringResource(id = R.string.main_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Text(
            text = "Welcome to FGO Bot! This application will help you automate your Fate/Grand Order gameplay with intelligent decision making.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp)
        )
        
        Text(
            text = "Version 1.0.0 - Android 14 Ready",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

/**
 * Preview function for the MainScreen composable
 * Allows viewing the UI in Android Studio's design preview
 */
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    FGOBotTheme {
        MainScreen()
    }
} 