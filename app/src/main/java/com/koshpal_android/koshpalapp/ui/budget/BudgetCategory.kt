package com.koshpal_android.koshpalapp.ui.budget

data class BudgetCategory(
    val id: String,
    val name: String,
    val iconRes: Int,
    val isSelected: Boolean = false
)
