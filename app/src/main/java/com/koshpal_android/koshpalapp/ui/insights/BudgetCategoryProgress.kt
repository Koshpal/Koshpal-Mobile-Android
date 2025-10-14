package com.koshpal_android.koshpalapp.ui.insights

data class BudgetCategoryProgress(
    val categoryName: String,
    val categoryId: String,
    val allocatedAmount: Double,
    val spentAmount: Double,
    val percentageUsed: Float, // 0.0 to 1.0
    val remainingAmount: Double,
    val isOverBudget: Boolean,
    val categoryColor: String,
    val categoryIcon: Int
)
