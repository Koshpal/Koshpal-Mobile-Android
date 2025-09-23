package com.koshpal_android.koshpalapp.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budget_categories",
    foreignKeys = [
        ForeignKey(
            entity = Budget::class,
            parentColumns = ["id"],
            childColumns = ["budgetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("budgetId")]
)
data class BudgetCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val budgetId: Int,
    val name: String,
    val allocatedAmount: Double,
    val spentAmount: Double = 0.0
)


