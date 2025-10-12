package com.koshpal_android.koshpalapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = TransactionCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class Transaction(
    @PrimaryKey
    val id: String = "",
    @ColumnInfo(name = "amount")
    val amount: Double = 0.0,
    @ColumnInfo(name = "description")
    val description: String = "",
    @ColumnInfo(name = "merchant")
    val merchant: String = "",
    @ColumnInfo(name = "categoryId")
    val categoryId: String = "others",
    @ColumnInfo(name = "type")
    val type: TransactionType = TransactionType.DEBIT,
    @ColumnInfo(name = "date")
    val date: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "smsId")
    val smsId: String? = null,
    @ColumnInfo(name = "isProcessed")
    val isProcessed: Boolean = false,
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "confidence")
    val confidence: Float = 1.0f,
    @ColumnInfo(name = "smsBody")
    val smsBody: String? = null,
    @ColumnInfo(name = "isManuallySet")
    val isManuallySet: Boolean = false,
    @ColumnInfo(name = "bankName")
    val bankName: String? = null,
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    @ColumnInfo(name = "attachmentPath")
    val attachmentPath: String? = null,
    @ColumnInfo(name = "tags")
    val tags: String? = null, // Comma-separated tags
    @ColumnInfo(name = "isStarred")
    val isStarred: Boolean = false,
    @ColumnInfo(name = "isBankEnabled")
    val isBankEnabled: Boolean = true,
    @ColumnInfo(name = "originalAmount")
    val originalAmount: Double? = null,
    @ColumnInfo(name = "originalMerchant")
    val originalMerchant: String? = null,
    @ColumnInfo(name = "isCashFlow")
    val isCashFlow: Boolean = false
) {
    val timestamp: Long
        get() = date
    
    val isExpense: Boolean
        get() = type == TransactionType.DEBIT
    
    val isIncome: Boolean
        get() = type == TransactionType.CREDIT
}
