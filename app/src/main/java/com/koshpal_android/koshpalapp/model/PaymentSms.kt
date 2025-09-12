package com.koshpal_android.koshpalapp.model

data class PaymentSms(
    val id: String,
    val address: String,
    val body: String,
    val timestamp: Long,
    val date: String
)
