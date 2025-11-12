package com.koshpal_android.koshpalapp.ui.insights.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koshpal_android.koshpalapp.ui.insights.MonthComparisonData
import com.koshpal_android.koshpalapp.ui.theme.AppColors
import kotlin.math.max

/**
 * Modern Horizontally Scrollable Bar Chart for Spending Trends
 */
@Composable
fun SpendingBarChart(
    data: List<MonthComparisonData>,
    showPercentages: Boolean,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxValue = data.maxOfOrNull {
        max(it.currentMonthAmount, it.previousMonthAmount)
    } ?: 1.0

    // Calculate chart dimensions
    val chartHeight = 200.dp
    val barWidth = 20.dp
    val barGap = 4.dp // Small gap between bars in a pair
    val categorySpacing = 40.dp // Increased space between category groups to fit full names
    val categoryLabelWidth = 80.dp // Width for category labels to prevent wrapping
    val yAxisWidth = 55.dp
    val xAxisHeight = 40.dp // Increased height for labels
    val chartPadding = 16.dp
    
    // Calculate total width: categoryLabelWidth for each category + categorySpacing between groups
    val totalWidth = (data.size * (categoryLabelWidth + categorySpacing).value - categorySpacing.value + chartPadding.value * 2).dp

    Box(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Y-axis labels
            Column(
                modifier = Modifier
                    .width(yAxisWidth)
                    .fillMaxHeight()
                    .padding(top = chartPadding, bottom = xAxisHeight),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                // Generate Y-axis labels (5 levels)
                repeat(5) { i ->
                    val value = maxValue * (1 - i / 4f)
                    Text(
                        text = formatAmount(value),
                        color = AppColors.TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            // Scrollable chart area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val scrollState = rememberScrollState()
                
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Chart canvas - horizontally scrollable
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight + chartPadding)
                            .horizontalScroll(scrollState)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(totalWidth)
                                .fillMaxHeight()
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = chartPadding, top = chartPadding, end = chartPadding)
                            ) {
                                val canvasHeight = size.height - chartPadding.toPx() - xAxisHeight.toPx()
                                val baseY = canvasHeight

                                data.forEachIndexed { index, item ->
                                    // Calculate group X position: center bars within categoryLabelWidth
                                    val categoryStartX = index * (categoryLabelWidth + categorySpacing).toPx()
                                    val groupX = categoryStartX + (categoryLabelWidth - barWidth - barGap).toPx() / 2
                                    
                                    // Calculate bar heights
                                    val previousHeight = ((item.previousMonthAmount / maxValue) * (canvasHeight - 20.dp.toPx())).toFloat().coerceAtLeast(0f)
                                    val currentHeight = ((item.currentMonthAmount / maxValue) * (canvasHeight - 20.dp.toPx())).toFloat().coerceAtLeast(0f)

                                    // Previous Month Bar (Gray) - left bar in pair
                                    val previousBarX = groupX
                                    val previousBarY = baseY - previousHeight
                                    
                                    // Draw rounded rectangle for previous month
                                    drawRoundedBar(
                                        color = AppColors.TextSecondary.copy(alpha = 0.4f),
                                        topLeft = Offset(previousBarX, previousBarY),
                                        width = barWidth.toPx(),
                                        height = previousHeight,
                                        cornerRadius = 4.dp.toPx()
                                    )

                                    // Current Month Bar (Blue) - right bar in pair, close to previous
                                    val currentBarX = groupX + barWidth.toPx() + barGap.toPx()
                                    val currentBarY = baseY - currentHeight
                                    
                                    // Draw rounded rectangle for current month
                                    drawRoundedBar(
                                        color = AppColors.AccentBlue,
                                        topLeft = Offset(currentBarX, currentBarY),
                                        width = barWidth.toPx(),
                                        height = currentHeight,
                                        cornerRadius = 4.dp.toPx()
                                    )
                                }
                            }
                        }
                    }

                    // X-axis labels (categories) - horizontally scrollable, centered below bar pairs
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(xAxisHeight)
                            .horizontalScroll(scrollState)
                    ) {
                        Row(
                            modifier = Modifier
                                .width(totalWidth)
                                .padding(horizontal = chartPadding),
                            horizontalArrangement = Arrangement.spacedBy(categorySpacing)
                        ) {
                            data.forEach { item ->
                                // Center the label with enough width to prevent wrapping
                                Box(
                                    modifier = Modifier.width(categoryLabelWidth),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.categoryName,
                                        color = AppColors.TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper function to draw rounded rectangle bar
 */
private fun DrawScope.drawRoundedBar(
    color: Color,
    topLeft: Offset,
    width: Float,
    height: Float,
    cornerRadius: Float
) {
    if (height <= 0) return
    
    val path = Path().apply {
        val r = cornerRadius.coerceAtMost(width / 2).coerceAtMost(height / 2)
        
        // Start from top-left (after corner)
        moveTo(topLeft.x + r, topLeft.y)
        
        // Top edge
        lineTo(topLeft.x + width - r, topLeft.y)
        
        // Top-right corner
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                topLeft.x + width - r * 2,
                topLeft.y,
                topLeft.x + width,
                topLeft.y + r * 2
            ),
            startAngleDegrees = -90f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        
        // Right edge
        lineTo(topLeft.x + width, topLeft.y + height)
        
        // Bottom edge
        lineTo(topLeft.x, topLeft.y + height)
        
        // Left edge
        lineTo(topLeft.x, topLeft.y + r)
        
        // Bottom-left corner
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                topLeft.x,
                topLeft.y,
                topLeft.x + r * 2,
                topLeft.y + r * 2
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        
        close()
    }
    
    drawPath(path, color)
}

/**
 * Format amount for display
 */
private fun formatAmount(amount: Double): String {
    return when {
        amount >= 1000000 -> "₹${String.format("%.1f", amount / 1000000)}M"
        amount >= 1000 -> "₹${String.format("%.1f", amount / 1000)}K"
        else -> "₹${amount.toInt()}"
    }
}
