package com.koshpal_android.koshpalapp.model

data class PaymentSms(
    val id: String = "",
    val smsBody: String = "",
    val sender: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isProcessed: Boolean = false,
    val amount: Double? = null,
    val merchant: String? = null,
    val transactionType: String? = null,
    val accountNumber: String? = null,
    val balance: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)
