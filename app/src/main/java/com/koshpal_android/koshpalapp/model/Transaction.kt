package com.koshpal_android.koshpalapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "transactions",
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
data class Transaction(
    @PrimaryKey
    val id: String,
    val amount: Double,
    val type: TransactionType,
    val merchant: String,
    val categoryId: String,
    val confidence: Float,
    val isManuallySet: Boolean = false,
    val timestamp: Long,
    val description: String,
    val smsBody: String,
    val accountNumber: String? = null,
    val balance: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getFormattedAmount(): String {
        return "₹${String.format("%.2f", amount)}"
    }
    
    fun isIncome(): Boolean {
        return type == TransactionType.CREDIT
    }
    
    fun isExpense(): Boolean {
        return type == TransactionType.DEBIT
    }
}
