/**
 * FGO Bot - Animated UI Components
 * 
 * This file contains animated UI components that enhance the user experience
 * with smooth transitions, loading states, progress indicators, and micro-interactions.
 * These components provide visual feedback and make the app feel more responsive.
 * 
 * Features:
 * - Screen transition animations
 * - Loading states and progress indicators
 * - Micro-interactions and feedback
 * - Animated icons and buttons
 * - Smooth state transitions
 */

package com.fgobot.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * AnimatedLoadingButton - Button with loading animation
 * 
 * A button that shows a loading spinner when in loading state,
 * with smooth transitions between states.
 * 
 * @param onClick Callback when button is clicked
 * @param modifier Modifier for styling
 * @param enabled Whether the button is enabled
 * @param isLoading Whether to show loading state
 * @param text Button text
 * @param icon Optional icon to display
 */
@Composable
fun AnimatedLoadingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    text: String,
    icon: ImageVector? = null
) {
    val buttonScale by animateFloatAsState(
        targetValue = if (enabled && !isLoading) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )
    
    Button(
        onClick = onClick,
        modifier = modifier.scale(buttonScale),
        enabled = enabled && !isLoading
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
            },
            label = "buttonContent"
        ) { loading ->
            if (loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Loading...")
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text)
                }
            }
        }
    }
}

/**
 * PulsingIcon - Icon with pulsing animation
 * 
 * An icon that pulses to draw attention or indicate activity.
 * 
 * @param icon The icon to display
 * @param modifier Modifier for styling
 * @param tint Icon tint color
 * @param isActive Whether the pulsing animation is active
 */
@Composable
fun PulsingIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    isActive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsingIcon")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 0.6f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconAlpha"
    )
    
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = modifier.scale(scale),
        tint = tint.copy(alpha = alpha)
    )
}

/**
 * AnimatedCounter - Counter with smooth number transitions
 * 
 * Displays a number with smooth animation when the value changes.
 * 
 * @param count The current count value
 * @param modifier Modifier for styling
 * @param style Text style
 */
@Composable
fun AnimatedCounter(
    count: Int,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium
) {
    var oldCount by remember { mutableIntStateOf(count) }
    
    LaunchedEffect(count) {
        oldCount = count
    }
    
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            if (targetState > initialState) {
                slideInVertically { height -> height } + fadeIn() togetherWith
                        slideOutVertically { height -> -height } + fadeOut()
            } else {
                slideInVertically { height -> -height } + fadeIn() togetherWith
                        slideOutVertically { height -> height } + fadeOut()
            }.using(SizeTransform(clip = false))
        },
        label = "counterAnimation"
    ) { targetCount ->
        Text(
            text = targetCount.toString(),
            style = style,
            modifier = modifier
        )
    }
}

/**
 * WaveLoadingIndicator - Wave-style loading animation
 * 
 * A custom loading indicator with wave animation for a unique look.
 * 
 * @param modifier Modifier for styling
 * @param color Wave color
 * @param isAnimating Whether the animation is active
 */
@Composable
fun WaveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    isAnimating: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveLoading")
    
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isAnimating) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )
    
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isAnimating) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, delayMillis = 200, easing = EaseInOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )
    
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isAnimating) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, delayMillis = 400, easing = EaseInOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave3"
    )
    
    Canvas(
        modifier = modifier.size(60.dp)
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.minDimension / 2
        
        drawCircle(
            color = color.copy(alpha = 0.3f * (1f - wave1)),
            radius = maxRadius * wave1,
            center = center
        )
        
        drawCircle(
            color = color.copy(alpha = 0.3f * (1f - wave2)),
            radius = maxRadius * wave2,
            center = center
        )
        
        drawCircle(
            color = color.copy(alpha = 0.3f * (1f - wave3)),
            radius = maxRadius * wave3,
            center = center
        )
    }
}

/**
 * SlideInCard - Card with slide-in animation
 * 
 * A card that slides in from the specified direction when it becomes visible.
 * 
 * @param visible Whether the card is visible
 * @param direction Slide direction
 * @param modifier Modifier for styling
 * @param content Card content
 */
@Composable
fun SlideInCard(
    visible: Boolean,
    direction: SlideDirection = SlideDirection.LEFT,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth ->
                when (direction) {
                    SlideDirection.LEFT -> -fullWidth
                    SlideDirection.RIGHT -> fullWidth
                }
            },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth ->
                when (direction) {
                    SlideDirection.LEFT -> -fullWidth
                    SlideDirection.RIGHT -> fullWidth
                }
            }
        ) + fadeOut(),
        modifier = modifier
    ) {
        Card {
            content()
        }
    }
}

/**
 * BouncyButton - Button with bouncy press animation
 * 
 * A button that bounces when pressed for tactile feedback.
 * 
 * @param onClick Callback when button is clicked
 * @param modifier Modifier for styling
 * @param enabled Whether the button is enabled
 * @param content Button content
 */
@Composable
fun BouncyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "bouncyScale"
    )
    
    Button(
        onClick = {
            if (enabled) {
                onClick()
            }
        },
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                enabled = enabled
            ) {
                isPressed = true
            },
        enabled = enabled,
        content = content
    )
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}

/**
 * GradientProgressBar - Progress bar with gradient colors
 * 
 * A progress bar with smooth gradient colors and animation.
 * 
 * @param progress Current progress (0.0 to 1.0)
 * @param modifier Modifier for styling
 * @param colors Gradient colors
 * @param height Progress bar height
 */
@Composable
fun GradientProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color(0xFF4CAF50),
        Color(0xFF8BC34A),
        Color(0xFFCDDC39)
    ),
    height: Dp = 8.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "progressAnimation"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(height / 2))
                .background(
                    Brush.horizontalGradient(colors)
                )
        )
    }
}

/**
 * FloatingActionButtonWithLabel - FAB with animated label
 * 
 * A floating action button that shows/hides a label with animation.
 * 
 * @param onClick Callback when FAB is clicked
 * @param icon FAB icon
 * @param label FAB label text
 * @param modifier Modifier for styling
 * @param expanded Whether the label is expanded
 */
@Composable
fun FloatingActionButtonWithLabel(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    expanded: Boolean = false
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        expanded = expanded,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        },
        text = {
            Text(text = label)
        }
    )
}

/**
 * ShimmerEffect - Shimmer loading effect
 * 
 * Creates a shimmer effect for loading placeholders.
 * 
 * @param modifier Modifier for styling
 * @param isLoading Whether to show the shimmer effect
 * @param content Content to show when not loading
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    isLoading: Boolean = true,
    content: @Composable () -> Unit = {}
) {
    if (isLoading) {
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        
        val shimmerTranslateAnim by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerTranslate"
        )
        
        Box(
            modifier = modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        startX = shimmerTranslateAnim - 300f,
                        endX = shimmerTranslateAnim
                    )
                )
                .clip(RoundedCornerShape(8.dp))
        )
    } else {
        content()
    }
}

/**
 * StatusIndicator - Animated status indicator
 * 
 * Shows status with color-coded animation.
 * 
 * @param status Current status
 * @param modifier Modifier for styling
 */
@Composable
fun StatusIndicator(
    status: Status,
    modifier: Modifier = Modifier
) {
    val color = when (status) {
        Status.ACTIVE -> Color(0xFF4CAF50)
        Status.INACTIVE -> Color(0xFF9E9E9E)
        Status.ERROR -> Color(0xFFF44336)
        Status.WARNING -> Color(0xFFFF9800)
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "statusIndicator")
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == Status.ACTIVE) 0.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statusAlpha"
    )
    
    Box(
        modifier = modifier
            .size(12.dp)
            .background(
                color = color.copy(alpha = alpha),
                shape = CircleShape
            )
    )
}

// Enums and data classes
enum class SlideDirection {
    LEFT, RIGHT
}

enum class Status {
    ACTIVE, INACTIVE, ERROR, WARNING
} 