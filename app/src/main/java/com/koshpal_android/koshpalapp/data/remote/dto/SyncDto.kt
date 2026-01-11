package com.koshpal_android.koshpalapp.data.remote.dto

import com.google.gson.annotations.SerializedName

// ============ TRANSACTION SYNC DTOs ============

data class SyncTransactionRequest(
    val accountId: String? = null,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String,
    val subCategory: String? = null,
    val source: String = "BANK", // "BANK" or "MOBILE"
    val description: String,
    val transactionDate: String // ISO format: "2025-12-01T00:00:00.000Z"
)

data class SyncTransactionResponse(
    val success: Boolean,
    val message: String,
    val data: SyncTransactionData?
)

data class SyncTransactionData(
    val transactionId: String
)

data class BulkSyncRequest(
    val transactions: List<SyncTransactionRequest>
)

// API Response DTO (matches actual API response)
data class BulkSyncApiResponse(
    val message: String,
    val count: Int,
    val transactions: List<BulkSyncTransactionData>
)

data class BulkSyncTransactionData(
    val id: String,
    val userId: String,
    val companyId: String,
    val accountId: String?,
    val amount: String,
    val type: String,
    val category: String,
    val subCategory: String?,
    val source: String,
    val description: String,
    val merchant: String?,
    val bank: String?,
    val maskedAccountNo: String?,
    val transactionDate: String,
    val createdAt: String,
    val deletedAt: String?
)

// Internal Response DTO (used by repository)
data class BulkSyncResponse(
    val success: Boolean,
    val message: String,
    val data: BulkSyncData?
)

data class BulkSyncData(
    val syncedCount: Int,
    val failedCount: Int,
    val transactionIds: List<String>?
)
