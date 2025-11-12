package com.koshpal_android.koshpalapp.ui.insights.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koshpal_android.koshpalapp.ui.insights.MonthComparisonData
import com.koshpal_android.koshpalapp.ui.theme.AppColors
import com.koshpal_android.koshpalapp.ui.insights.compose.SpendingBarChart

/**
 * Spending Trends Section with Bar Chart and Key Changes
 */
@Composable
fun SpendingTrendsSection(
    data: SpendingTrendsData,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPercentages by remember { mutableStateOf(data.showPercentages) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.DarkCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = AppColors.AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Spending Trends",
                        color = AppColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Month-over-month comparison",
                color = AppColors.TextSecondary,
                fontSize = 14.sp
            )

            // Toggle Buttons (₹ / %)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Rupee Button
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showPercentages = false },
                    shape = RoundedCornerShape(20.dp),
                    color = if (!showPercentages) AppColors.AccentBlue else AppColors.DarkCard.copy(alpha = 0.5f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "₹",
                            color = if (!showPercentages) AppColors.TextPrimary else AppColors.TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Percentage Button
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showPercentages = true },
                    shape = RoundedCornerShape(20.dp),
                    color = if (showPercentages) AppColors.AccentBlue else AppColors.DarkCard.copy(alpha = 0.5f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "%",
                            color = if (showPercentages) AppColors.TextPrimary else AppColors.TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bar Chart
            if (data.comparisonData.isNotEmpty()) {
                SpendingBarChart(
                    data = data.comparisonData,
                    showPercentages = showPercentages,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )
            }

            // Key Changes
            data.insights?.let { insights ->
                if (insights.topIncreases.isNotEmpty() || insights.topDecreases.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Top Increases
                        if (insights.topIncreases.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Top Increases",
                                    color = AppColors.TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                insights.topIncreases.forEach { change ->
                                    KeyChangeItem(
                                        change = change,
                                        isIncrease = true,
                                        onClick = { onCategoryClick(change.categoryId) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Top Decreases
                        if (insights.topDecreases.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Top Decreases",
                                    color = AppColors.TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                insights.topDecreases.forEach { change ->
                                    KeyChangeItem(
                                        change = change,
                                        isIncrease = false,
                                        onClick = { onCategoryClick(change.categoryId) },
                                        modifier = Modifier.fillMaxWidth()
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
 * Key Change Item (Top Increases/Decreases)
 */
@Composable
fun KeyChangeItem(
    change: MonthComparisonData,
    isIncrease: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon (using a colored circle as placeholder)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor(change.categoryColor))
                        } catch (e: Exception) {
                            AppColors.AccentBlue
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncrease) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = AppColors.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = change.categoryName,
                    color = AppColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isIncrease) {
                        "₹${kotlin.math.abs(change.absoluteChange).toInt()} more"
                    } else {
                        "₹${kotlin.math.abs(change.absoluteChange).toInt()} less"
                    },
                    color = AppColors.AccentBlue,
                    fontSize = 12.sp
                )
            }
        }

        // Percentage Badge
        val percentage = kotlin.math.abs(change.percentageChange).toInt()
        val badgeColor = if (isIncrease) {
            Color(0xFF4CAF50).copy(alpha = 0.2f)
        } else {
            Color(0xFFF44336).copy(alpha = 0.2f)
        }
        val textColor = if (isIncrease) {
            Color(0xFF4CAF50)
        } else {
            Color(0xFFF44336)
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = badgeColor
        ) {
            Text(
                text = "${if (isIncrease) "↑" else "↓"} $percentage%",
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

