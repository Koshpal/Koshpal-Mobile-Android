package com.koshpal_android.koshpalapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

enum class BudgetStatus {
    SAFE,
    WARNING,
    CRITICAL,
    EXCEEDED
}

data class Budget(
    val id: String = "",
    val categoryId: String = "",
    val amount: Double = 0.0,
    val spentAmount: Double = 0.0,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getRemainingAmount(): Double {
        return amount - spentAmount
    }
    
    fun getSpentPercentage(): Float {
        return if (amount > 0) (spentAmount / amount * 100).toFloat() else 0f
    }
    
    fun getStatus(): BudgetStatus {
        val percentage = getSpentPercentage()
        return when {
            percentage >= 100 -> BudgetStatus.EXCEEDED
            percentage >= 80 -> BudgetStatus.CRITICAL
            percentage >= 50 -> BudgetStatus.WARNING
            else -> BudgetStatus.SAFE
        }
    }
}

enum class BudgetPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY
}
