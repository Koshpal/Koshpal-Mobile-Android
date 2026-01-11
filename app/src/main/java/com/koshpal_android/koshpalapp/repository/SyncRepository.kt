package com.koshpal_android.koshpalapp.repository

import android.util.Log
import com.koshpal_android.koshpalapp.auth.SessionManager
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.data.local.dao.TransactionDao
import com.koshpal_android.koshpalapp.data.remote.dto.*
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.network.ApiService
import com.koshpal_android.koshpalapp.network.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val transactionDao: TransactionDao
) {

    private val TAG = "SyncRepository"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Sync a single transaction to the server
     */
    fun syncSingleTransaction(transaction: Transaction): Flow<NetworkResult<SyncTransactionResponse>> = flow {
        try {
            Log.d(TAG, "🔄 Starting single transaction sync: ${transaction.id}")

            // Check if user is logged in
            if (!sessionManager.isValidSession()) {
                Log.e(TAG, "❌ Cannot sync: User not logged in")
                emit(NetworkResult.Error("Authentication required. Please login first."))
                return@flow
            }

            emit(NetworkResult.Loading())

            // Convert single transaction to bulk format (array with one item)
            val syncRequest = transactionToSyncRequest(transaction)
            val bulkRequest = BulkSyncRequest(listOf(syncRequest))

            Log.d(TAG, "📤 Syncing transaction: ${transaction.merchant} - ₹${transaction.amount}")

            val response = apiService.syncBulkTransactions(bulkRequest)

            if (response.isSuccessful) {
                val bulkResponse = response.body()
                if (bulkResponse != null && bulkResponse.count > 0) {
                    // Mark transaction as synced in database
                    transactionDao.markTransactionAsSynced(transaction.id, System.currentTimeMillis())

                    Log.d(TAG, "✅ Single transaction synced successfully via bulk API")

                    // Convert bulk response to single response format for compatibility
                    val singleResponse = SyncTransactionResponse(
                        success = true,
                        message = "Transaction synced successfully",
                        data = SyncTransactionData(bulkResponse.transactions.firstOrNull()?.id ?: "")
                    )

                    emit(NetworkResult.Success(singleResponse))
                } else {
                    val error = "Empty response from bulk sync API"
                    Log.e(TAG, "❌ Sync failed: $error")
                    emit(NetworkResult.Error(error))
                }
            } else {
                val error = getSyncErrorMessage(response)
                Log.e(TAG, "❌ Sync API error: $error")

                // Handle 401 Unauthorized - clear session and redirect to login
                if (response.code() == 401) {
                    Log.w(TAG, "🚪 401 Unauthorized - clearing session and redirecting to login")
                    sessionManager.clearSession()
                    // Note: Navigation to login should be handled by the UI layer
                }

                // Log full request body on 400 Bad Request errors for debugging
                if (response.code() == 400) {
                    Log.e(TAG, "📋 400 Bad Request - Full request body: $bulkRequest")
                    Log.e(TAG, "📋 400 Bad Request - Response body: ${response.errorBody()?.string()}")
                }

                emit(NetworkResult.Error(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync exception: ${e.message}", e)
            emit(NetworkResult.Error("Network error: ${e.localizedMessage ?: e.message}"))
        }
    }

    /**
     * Bulk sync multiple transactions with chunking and delays
     */
    fun syncBulkTransactions(transactions: List<Transaction>): Flow<NetworkResult<BulkSyncResponse>> = flow {
        try {
            Log.d(TAG, "🔄 Starting bulk sync of ${transactions.size} transactions")

            // Check if user is logged in
            if (!sessionManager.isValidSession()) {
                Log.e(TAG, "❌ Cannot bulk sync: User not logged in")
                emit(NetworkResult.Error("Authentication required. Please login first."))
                return@flow
            }

            emit(NetworkResult.Loading())

            // Split transactions into chunks of 10
            val chunks = transactions.chunked(10)
            Log.d(TAG, "📦 Split into ${chunks.size} chunks of 10 transactions each")

            var totalSyncedCount = 0
            var totalFailedCount = 0
            var hasErrors = false

            // Process each chunk sequentially with 2-second delay between chunks
            chunks.forEachIndexed { index, chunk ->
                Log.d(TAG, "📦 Processing chunk ${index + 1}/${chunks.size} with ${chunk.size} transactions")

                try {
                    // Convert chunk to sync requests
                    val syncRequests = chunk.map { transactionToSyncRequest(it) }
                    val bulkRequest = BulkSyncRequest(syncRequests)

                    Log.d(TAG, "📤 Sending chunk ${index + 1} with ${syncRequests.size} transactions")

                    val response = apiService.syncBulkTransactions(bulkRequest)

                    if (response.isSuccessful) {
                        val apiResponse: BulkSyncApiResponse? = response.body()
                        if (apiResponse != null) {
                            val chunkSyncedCount = apiResponse.count
                            totalSyncedCount += chunkSyncedCount

                            Log.d(TAG, "✅ Chunk ${index + 1} synced: $chunkSyncedCount/${chunk.size} transactions")

                            // Mark chunk transactions as synced
                            val chunkTransactionIds = chunk.map { it.id }
                            transactionDao.markTransactionsAsSynced(chunkTransactionIds, System.currentTimeMillis())
                        } else {
                            val error = "Invalid response format from server"
                            Log.e(TAG, "❌ Chunk ${index + 1} failed: $error")
                            totalFailedCount += chunk.size
                            hasErrors = true
                        }
                    } else {
                        val error = getSyncErrorMessage(response)
                        Log.e(TAG, "❌ Chunk ${index + 1} API error: $error")

                        // Handle 401 Unauthorized - clear session and redirect to login
                        if (response.code() == 401) {
                            Log.w(TAG, "🚪 401 Unauthorized - clearing session and redirecting to login")
                            sessionManager.clearSession()
                            // Note: Navigation to login should be handled by the UI layer
                            hasErrors = true
                            return@forEachIndexed
                        }

                        // Log full request body on 400 Bad Request errors for debugging
                        if (response.code() == 400) {
                            Log.e(TAG, "📋 400 Bad Request - Full request body: $bulkRequest")
                            Log.e(TAG, "📋 400 Bad Request - Response body: ${response.errorBody()?.string()}")
                        }

                        totalFailedCount += chunk.size
                        hasErrors = true
                    }

                    // Add 2-second delay between chunks (except for the last chunk)
                    if (index < chunks.size - 1) {
                        Log.d(TAG, "⏳ Waiting 2 seconds before next chunk...")
                        kotlinx.coroutines.delay(2000L)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Chunk ${index + 1} exception: ${e.message}", e)
                    totalFailedCount += chunk.size
                    hasErrors = true
                }
            }

            // Create final compatible response
            val finalResponse = com.koshpal_android.koshpalapp.data.remote.dto.BulkSyncResponse(
                success = !hasErrors,
                message = if (hasErrors) {
                    "Bulk sync completed with errors: $totalSyncedCount synced, $totalFailedCount failed"
                } else {
                    "Bulk sync completed successfully: $totalSyncedCount transactions synced"
                },
                data = com.koshpal_android.koshpalapp.data.remote.dto.BulkSyncData(
                    syncedCount = totalSyncedCount,
                    failedCount = totalFailedCount,
                    transactionIds = emptyList()
                )
            )

            Log.d(TAG, "✅ Bulk sync completed: $totalSyncedCount synced, $totalFailedCount failed")
            emit(NetworkResult.Success(finalResponse))

        } catch (e: Exception) {
            Log.e(TAG, "❌ Bulk sync exception: ${e.message}", e)
            emit(NetworkResult.Error("Network error: ${e.localizedMessage ?: e.message}"))
        }
    }

    /**
     * Get unsynced transactions for bulk sync
     */
    suspend fun getUnsyncedTransactions(): List<Transaction> {
        return transactionDao.getUnsyncedTransactions()
    }

    /**
     * Get count of unsynced transactions
     */
    suspend fun getUnsyncedTransactionCount(): Int {
        return transactionDao.getUnsyncedTransactionCount()
    }

    /**
     * Convert Transaction entity to SyncTransactionRequest
     */
    private fun transactionToSyncRequest(transaction: Transaction): SyncTransactionRequest {
        return SyncTransactionRequest(
            accountId = null, // Not available in current model
            amount = transaction.amount,
            type = if (transaction.type == TransactionType.CREDIT) "INCOME" else "EXPENSE",
            category = transaction.categoryId,
            subCategory = null, // Not available in current model
            source = "BANK", // Default to BANK as per requirements
            description = transaction.description,
            transactionDate = dateFormat.format(Date(transaction.date))
        )
    }

    /**
     * Get user-friendly error message from API response
     */
    private fun getSyncErrorMessage(response: Response<*>): String {
        return when (response.code()) {
            400 -> "Invalid transaction data. Please check transaction details."
            401 -> "Authentication failed. Please login again."
            403 -> "Access denied. Your account may not be active."
            409 -> "Transaction already exists on server."
            429 -> "Too many requests. Please wait and try again."
            500 -> "Server error. Please try again later."
            else -> "Sync failed: ${response.message()}"
        }
    }
}
