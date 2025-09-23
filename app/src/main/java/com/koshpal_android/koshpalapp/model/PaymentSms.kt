package com.koshpal_android.koshpalapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "payment_sms")
data class PaymentSms(
    @PrimaryKey
    val id: String = "",
    @ColumnInfo(name = "smsBody")
    val smsBody: String = "",
    @ColumnInfo(name = "sender")
    val sender: String = "",
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isProcessed")
    val isProcessed: Boolean = false,
    @ColumnInfo(name = "amount")
    val amount: Double? = null,
    @ColumnInfo(name = "merchant")
    val merchant: String? = null,
    @ColumnInfo(name = "transactionType")
    val transactionType: String? = null,
    @ColumnInfo(name = "accountNumber")
    val accountNumber: String? = null,
    @ColumnInfo(name = "balance")
    val balance: Double? = null,
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
) {
    val address: String
        get() = sender
    
    val body: String
        get() = smsBody
    
    val date: Long
        get() = timestamp
}
