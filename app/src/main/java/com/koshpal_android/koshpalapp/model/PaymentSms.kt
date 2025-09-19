package com.koshpal_android.koshpalapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_sms")
data class PaymentSms(
    @PrimaryKey
    val id: String,
    val address: String,
    val body: String,
    val timestamp: Long,
    val date: String,
    val isProcessed: Boolean = false
)
