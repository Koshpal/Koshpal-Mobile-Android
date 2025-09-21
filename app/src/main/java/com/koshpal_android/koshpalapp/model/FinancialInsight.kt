package com.koshpal_android.koshpalapp.model

enum class InsightType {
    SPENDING_TREND,
    BUDGET_ALERT,
    SAVING_TIP,
    CATEGORY_ANALYSIS,
    MONTHLY_SUMMARY
}

data class FinancialInsight(
    val id: String = "",
    val type: InsightType = InsightType.SPENDING_TREND,
    val title: String = "",
    val description: String = "",
    val value: Double = 0.0,
    val percentage: Float = 0f,
    val isPositive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
