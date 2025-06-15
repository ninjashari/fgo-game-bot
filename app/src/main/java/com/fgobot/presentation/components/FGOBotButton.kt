/*
 * FGO Bot - Custom Button Components
 * 
 * This file defines reusable button components with FGO theming.
 * Provides consistent styling and behavior across the application.
 */

package com.fgobot.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fgobot.presentation.theme.FGOBotTheme

/**
 * Primary button component with FGO theming
 * 
 * @param text Button text
 * @param onClick Click handler
 * @param modifier Modifier for customization
 * @param enabled Whether the button is enabled
 * @param fillMaxWidth Whether to fill maximum width
 */
@Composable
fun FGOBotPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .let { if (fillMaxWidth) it.fillMaxWidth() else it }
            .height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Secondary button component with FGO theming
 * 
 * @param text Button text
 * @param onClick Click handler
 * @param modifier Modifier for customization
 * @param enabled Whether the button is enabled
 * @param fillMaxWidth Whether to fill maximum width
 */
@Composable
fun FGOBotSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .let { if (fillMaxWidth) it.fillMaxWidth() else it }
            .height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Text button component with FGO theming
 * 
 * @param text Button text
 * @param onClick Click handler
 * @param modifier Modifier for customization
 * @param enabled Whether the button is enabled
 */
@Composable
fun FGOBotTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Danger button component for destructive actions
 * 
 * @param text Button text
 * @param onClick Click handler
 * @param modifier Modifier for customization
 * @param enabled Whether the button is enabled
 * @param fillMaxWidth Whether to fill maximum width
 */
@Composable
fun FGOBotDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .let { if (fillMaxWidth) it.fillMaxWidth() else it }
            .height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Success button component for positive actions
 * 
 * @param text Button text
 * @param onClick Click handler
 * @param modifier Modifier for customization
 * @param enabled Whether the button is enabled
 * @param fillMaxWidth Whether to fill maximum width
 */
@Composable
fun FGOBotSuccessButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .let { if (fillMaxWidth) it.fillMaxWidth() else it }
            .height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4CAF50), // Green color
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FGOBotButtonsPreview() {
    FGOBotTheme {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            FGOBotPrimaryButton(
                text = "Primary Button",
                onClick = { },
                fillMaxWidth = true
            )
            
            FGOBotSecondaryButton(
                text = "Secondary Button",
                onClick = { },
                fillMaxWidth = true
            )
            
            FGOBotTextButton(
                text = "Text Button",
                onClick = { }
            )
            
            FGOBotDangerButton(
                text = "Danger Button",
                onClick = { },
                fillMaxWidth = true
            )
            
            FGOBotSuccessButton(
                text = "Success Button",
                onClick = { },
                fillMaxWidth = true
            )
        }
    }
} 