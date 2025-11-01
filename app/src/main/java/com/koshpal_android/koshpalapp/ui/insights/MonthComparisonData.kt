package com.koshpal_android.koshpalapp.ui.insights

data class MonthComparisonData(
    val categoryId: String,
    val categoryName: String,
    val currentMonthAmount: Double,
    val previousMonthAmount: Double,
    val percentageChange: Float, // Positive = increase, Negative = decrease
    val absoluteChange: Double,
    val categoryColor: String,
    val categoryIcon: Int
) {
    val isIncrease: Boolean
        get() = percentageChange > 0
    
    val isDecrease: Boolean
        get() = percentageChange < 0
    
    val hasSignificantChange: Boolean
        get() = kotlin.math.abs(percentageChange) >= 5f // 5% threshold
}
