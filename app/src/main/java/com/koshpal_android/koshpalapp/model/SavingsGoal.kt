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

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey
    val id: String,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val category: GoalCategory,
    val targetDate: Long?,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val progressPercentage: Float get() = (currentAmount / targetAmount * 100).toFloat()
    val remainingAmount: Double get() = targetAmount - currentAmount
    val isCompleted: Boolean get() = currentAmount >= targetAmount
    
    fun getFormattedTarget(): String {
        return "₹${String.format("%.2f", targetAmount)}"
    }
    
    fun getFormattedCurrent(): String {
        return "₹${String.format("%.2f", currentAmount)}"
    }
    
    fun getFormattedRemaining(): String {
        return "₹${String.format("%.2f", remainingAmount)}"
    }
    
    fun getDaysRemaining(): Long? {
        return targetDate?.let { 
            val currentTime = System.currentTimeMillis()
            if (it > currentTime) {
                (it - currentTime) / (24 * 60 * 60 * 1000)
            } else {
                0
            }
        }
    }
}
