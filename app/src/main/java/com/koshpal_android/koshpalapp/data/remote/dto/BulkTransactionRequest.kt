package com.koshpal_android.koshpalapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request body for bulk transaction upload
 * Note: employeeId is at root level, transactions array does NOT contain employeeId
 */
data class BulkTransactionRequest(
    @SerializedName("employeeId")
    val employeeId: String,
    
    @SerializedName("transactions")
    val transactions: List<BulkTransactionItem>
)

