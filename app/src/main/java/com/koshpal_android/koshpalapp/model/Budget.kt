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

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = TransactionCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class Budget(
    @PrimaryKey
    val id: String,
    val categoryId: String,
    val monthlyLimit: Double,
    val spent: Double = 0.0,
    val month: Int,
    val year: Int,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val remaining: Double get() = monthlyLimit - spent
    val progressPercentage: Float get() = (spent / monthlyLimit * 100).toFloat()
    val status: BudgetStatus get() = when {
        progressPercentage <= 50 -> BudgetStatus.SAFE
        progressPercentage <= 80 -> BudgetStatus.WARNING
        progressPercentage <= 100 -> BudgetStatus.CRITICAL
        else -> BudgetStatus.EXCEEDED
    }
    
    fun getFormattedLimit(): String {
        return "₹${String.format("%.2f", monthlyLimit)}"
    }
    
    fun getFormattedSpent(): String {
        return "₹${String.format("%.2f", spent)}"
    }
    
    fun getFormattedRemaining(): String {
        return "₹${String.format("%.2f", remaining)}"
    }
}
