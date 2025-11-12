package com.koshpal_android.koshpalapp.model

data class BankSpending(
    val bankName: String,
    val totalSpending: Double,
    val transactionCount: Int,
    val isCash: Boolean = false,
    val accountNumber: String? = null, // Last 4 digits of account (e.g., "3695")
    val balance: Double? = null, // Current balance from latest transaction
    val lastUpdated: Long? = null // Timestamp of most recent transaction
)
