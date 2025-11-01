package com.koshpal_android.koshpalapp.ui.insights

data class MonthComparisonInsight(
    val insightText: String,
    val topIncreases: List<MonthComparisonData>,
    val topDecreases: List<MonthComparisonData>,
    val overallSavingChange: Double, // Positive = saved more, Negative = saved less
    val totalCurrentMonthSpending: Double,
    val totalPreviousMonthSpending: Double
)
