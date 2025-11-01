package com.koshpal_android.koshpalapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from single transaction upload API
 */
data class SingleTransactionResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: SingleTransactionData? = null
)

data class SingleTransactionData(
    @SerializedName("employeeId")
    val employeeId: String,
    
    @SerializedName("transactionId")
    val transactionId: String,
    
    @SerializedName("_id")
    val _id: String
)

