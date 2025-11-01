package com.koshpal_android.koshpalapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from bulk transaction upload API
 */
data class BulkTransactionResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: BulkTransactionData? = null
)

data class BulkTransactionData(
    @SerializedName("insertedCount")
    val insertedCount: Int,
    
    @SerializedName("insertedIds")
    val insertedIds: List<String>? = null
)

