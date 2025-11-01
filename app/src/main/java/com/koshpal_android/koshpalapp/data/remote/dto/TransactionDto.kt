package com.koshpal_android.koshpalapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TransactionDto(
    @SerializedName("employeeId")
    val employeeId: String,
    
    @SerializedName("transaction_id")
    val transaction_id: String,
    
    @SerializedName("sender")
    val sender: String,
    
    @SerializedName("message_body")
    val message_body: String,
    
    @SerializedName("amount")
    val amount: Double,
    
    @SerializedName("currency")
    val currency: String = "INR",
    
    @SerializedName("txn_type")
    val txn_type: String,
    
    @SerializedName("timestamp_ms")
    val timestamp_ms: Long,
    
    @SerializedName("account_last4")
    val account_last4: String? = null,
    
    @SerializedName("merchant")
    val merchant: String,
    
    @SerializedName("category_id")
    val category_id: String,
    
    @SerializedName("category_name")
    val category_name: String? = null,
    
    @SerializedName("upi_ref")
    val upi_ref: String? = null,
    
    @SerializedName("bank")
    val bank: String? = null,
    
    @SerializedName("is_starred")
    val is_starred: Boolean = false,
    
    @SerializedName("include_in_cash_flow")
    val include_in_cash_flow: Boolean = false,
    
    @SerializedName("source")
    val source: String = "sms",
    
    @SerializedName("app_version")
    val app_version: String = "1.0.0",
    
    @SerializedName("device_id")
    val device_id: String
)

data class TransactionSyncResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("transaction_id")
    val transactionId: String? = null,
    
    @SerializedName("synced_at")
    val syncedAt: Long? = null
)

