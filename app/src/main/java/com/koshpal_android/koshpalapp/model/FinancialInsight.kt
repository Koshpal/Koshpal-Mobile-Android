package com.koshpal_android.koshpalapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_insights")
data class FinancialInsight(
    @PrimaryKey
    val id: String,
    val month: Int,
    val year: Int,
    val totalIncome: Double,
    val totalExpense: Double,
    val savingsRate: Float,
    val healthScore: Int,
    val recommendations: List<String>,
    val topSpendingCategory: String,
    val expenseGrowth: Float, // Percentage change from previous month
    val incomeGrowth: Float,
    val createdAt: Long = System.currentTimeMillis()
) {
    val netSavings: Double get() = totalIncome - totalExpense
    val expenseRatio: Float get() = (totalExpense / totalIncome * 100).toFloat()
    
    fun getFormattedIncome(): String {
        return "₹${String.format("%.2f", totalIncome)}"
    }
    
    fun getFormattedExpense(): String {
        return "₹${String.format("%.2f", totalExpense)}"
    }
    
    fun getFormattedSavings(): String {
        return "₹${String.format("%.2f", netSavings)}"
    }
    
    fun getHealthScoreColor(): String {
        return when {
            healthScore >= 80 -> "#4CAF50" // Green
            healthScore >= 60 -> "#FF9800" // Orange
            healthScore >= 40 -> "#FF5722" // Red-Orange
            else -> "#F44336" // Red
        }
    }
}
