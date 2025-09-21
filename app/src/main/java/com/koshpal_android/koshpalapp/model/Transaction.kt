package com.koshpal_android.koshpalapp.model

data class Transaction(
    val id: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val merchant: String = "",
    val categoryId: String = "",
    val type: TransactionType = TransactionType.DEBIT,
    val date: Long = System.currentTimeMillis(),
    val smsId: String? = null,
    val isProcessed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
