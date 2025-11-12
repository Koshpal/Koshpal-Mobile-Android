package com.koshpal_android.koshpalapp.ui.home.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.koshpal_android.koshpalapp.ui.theme.AppColors

/**
 * Data class representing a navigation item
 */
data class NavigationItem(
    val id: Int,
    val iconResId: Int,
    val title: String
)

/**
 * Custom Bottom Navigation Bar with animated blue line and blur effect
 */
@Composable
fun CustomBottomNavigation(
    items: List<NavigationItem>,
    selectedItemId: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Gray gradient mixed with black for sophisticated look - more gray
    val lightGray = Color(0xFF2A2A2A) // Lighter gray
    val darkGrayBase = Color(0xFF1F1F1F) // Dark gray base (more gray)
    val darkerGray = Color(0xFF1A1A1A) // Darker gray (still gray, not black)
    val veryDarkGray = Color(0xFF0F0F0F) // Very dark gray (minimal black)
    
    NavigationBar(
        modifier = modifier
            .drawBehind {
                // Sophisticated gray gradient - more gray, less black
                // Vertical gradient from lighter gray at top to darker gray at bottom
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lightGray, // Top - lighter gray
                            darkGrayBase, // Upper mid - dark gray
                            darkerGray, // Lower mid - darker gray
                            veryDarkGray // Bottom - very dark gray (minimal black)
                        )
                    )
                )
                
                // Subtle horizontal variation for depth
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            darkerGray.copy(alpha = 0.2f),
                            Color.Transparent,
                            darkerGray.copy(alpha = 0.2f)
                        )
                    )
                )
            },
        containerColor = Color.Transparent, // Transparent to show gradient
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val isSelected = item.id == selectedItemId
            
            // Custom navigation item
            CustomNavigationItem(
                item = item,
                isSelected = isSelected,
                onClick = { onItemSelected(item.id) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Custom Navigation Item with animated blue line
 */
@Composable
private fun CustomNavigationItem(
    item: NavigationItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(
                onClick = onClick,
                indication = null, // No ripple effect
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated blue line above icon
        // Fixed height Box to prevent icon jumping when line appears/disappears
        Box(
            modifier = Modifier
                .height(4.dp)
                .width(30.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300)),
                label = "line_visibility"
            ) {
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(2.dp)
                        .drawBehind {
                            // Smooth, subtle blue glow effect - less intense, more proper
                            val glowSize = 2.dp.toPx() // Smaller glow size
                            
                            // Subtle outer glow (very soft)
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        AppColors.AccentBlue.copy(alpha = 0.0f),
                                        AppColors.AccentBlue.copy(alpha = 0.15f), // Much softer
                                        AppColors.AccentBlue.copy(alpha = 0.15f),
                                        AppColors.AccentBlue.copy(alpha = 0.0f)
                                    ),
                                    start = Offset(0f, size.height / 2f),
                                    end = Offset(size.width, size.height / 2f)
                                ),
                                topLeft = Offset(-glowSize, -glowSize),
                                size = androidx.compose.ui.geometry.Size(
                                    size.width + glowSize * 2,
                                    size.height + glowSize * 2
                                )
                            )
                            
                            // Main blue line (solid, clean)
                            drawRect(
                                color = AppColors.AccentBlue,
                                topLeft = Offset(0f, 0f),
                                size = size
                            )
                        }
                )
            }
        }
        
        // Small spacer between line and icon
        Spacer(modifier = Modifier.height(6.dp))
        
        // Icon with animated color using Crossfade
        Crossfade(
            targetState = isSelected,
            animationSpec = tween(durationMillis = 300),
            label = "icon_color_animation"
        ) { selected ->
            Icon(
                painter = painterResource(id = item.iconResId),
                contentDescription = item.title,
                tint = if (selected) {
                    AppColors.AccentBlue // Active item - blue
                } else {
                    Color.White // Inactive items - white (not gray)
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

