package com.koshpal_android.koshpalapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GoalCategory {
    EMERGENCY_FUND,
    VACATION,
    GADGET,
    EDUCATION,
    INVESTMENT,
    HEALTH,
    HOME,
    VEHICLE,
    OTHER
}

data class SavingsGoal(
    val id: String = "",
    val name: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val targetDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getProgress(): Float {
        return if (targetAmount > 0) (currentAmount / targetAmount * 100).toFloat() else 0f
    }
    
    fun getRemainingAmount(): Double {
        return targetAmount - currentAmount
    }
}
