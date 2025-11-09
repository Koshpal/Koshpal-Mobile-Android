package com.koshpal_android.koshpalapp.ui.categories.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.koshpal_android.koshpalapp.model.CategorySpending
import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.ui.theme.AppColors

/**
 * Donut Chart Composable
 * Displays category spending breakdown as a donut chart with blue accent colors
 * No labels around the chart - only center text is shown
 */
@Composable
fun DonutChart(
    categorySpending: List<CategorySpending>,
    categoriesById: Map<String, TransactionCategory>,
    modifier: Modifier = Modifier
) {
    if (categorySpending.isEmpty()) {
        return
    }
    
    val total = categorySpending.sumOf { it.totalAmount }
    if (total == 0.0) {
        return
    }
    
    // Generate blue accent colors with varying shades
    val blueShades = generateBlueShades(categorySpending.size)
    
    // Prepare data with colors
    val chartData = categorySpending.mapIndexed { index, spending ->
        val category = categoriesById[spending.categoryId]
        ChartSegment(
            amount = spending.totalAmount,
            percentage = (spending.totalAmount / total).toFloat(),
            color = try {
                Color(android.graphics.Color.parseColor(category?.color ?: "#007BFF"))
            } catch (e: Exception) {
                blueShades[index % blueShades.size]
            }
        )
    }
    
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.15f
        val radius = (size.minDimension - strokeWidth) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        
        var startAngle = -90f // Start from top
        
        chartData.forEach { segment ->
            val sweepAngle = segment.percentage * 360f
            
            // Draw arc segment
            drawArc(
                color = segment.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
            
            startAngle += sweepAngle
        }
    }
}

private fun generateBlueShades(count: Int): List<Color> {
    val baseBlue = AppColors.AccentBlue
    val shades = mutableListOf<Color>()
    
    // Generate varying shades of blue
    val baseHue = 0.58f // Blue hue in HSV
    val baseSaturation = 1.0f
    val baseValue = 1.0f
    
    for (i in 0 until count) {
        val value = 0.6f + (i * 0.4f / count.coerceAtLeast(1))
        val saturation = 0.7f + (i * 0.3f / count.coerceAtLeast(1))
        
        // Convert HSV to RGB
        val color = android.graphics.Color.HSVToColor(
            floatArrayOf(
                baseHue * 360f,
                saturation,
                value
            )
        )
        shades.add(Color(color))
    }
    
    // Fallback colors if generation fails
    if (shades.isEmpty()) {
        return listOf(
            Color(0xFF007BFF), // Primary blue
            Color(0xFF3399FF), // Light blue
            Color(0xFF0056B3), // Dark blue
            Color(0xFF66B3FF), // Lighter blue
            Color(0xFF003D7A)  // Darker blue
        )
    }
    
    return shades
}

private data class ChartSegment(
    val amount: Double,
    val percentage: Float,
    val color: Color
)
