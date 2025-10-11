package com.koshpal_android.koshpalapp.model

data class BankSpending(
    val bankName: String,
    val totalSpending: Double,
    val transactionCount: Int,
    val isCash: Boolean = false
)
