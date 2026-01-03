package com.koshpal_android.koshpalapp.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.koshpal_android.koshpalapp.auth.SessionManager
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.network.NetworkResult
import com.koshpal_android.koshpalapp.repository.SyncRepository
import com.koshpal_android.koshpalapp.service.NewTransactionSyncService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker for background transaction sync
 */
@HiltWorker
class TransactionSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val sessionManager: SessionManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "transaction_sync_worker"
        const val TAG = "TransactionSyncWorker"

        // Input data keys
        const val KEY_TRANSACTION_ID = "transaction_id"
        const val KEY_SYNC_TYPE = "sync_type"

        // Sync types
        const val SYNC_TYPE_SINGLE = "single"
        const val SYNC_TYPE_BULK = "bulk"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "🔄 Starting background transaction sync")

        return try {
            // Check if user is logged in
            if (!sessionManager.isValidSession()) {
                Log.d(TAG, "⏭️ Skipping sync: User not logged in")
                return Result.success() // Don't retry if not logged in
            }

            val syncType = inputData.getString(KEY_SYNC_TYPE) ?: SYNC_TYPE_BULK

            when (syncType) {
                SYNC_TYPE_SINGLE -> {
                    val transactionId = inputData.getString(KEY_TRANSACTION_ID)
                    if (transactionId != null) {
                        syncSingleTransaction(transactionId)
                    } else {
                        Log.e(TAG, "❌ Transaction ID not provided for single sync")
                        Result.failure()
                    }
                }
                SYNC_TYPE_BULK -> {
                    syncBulkTransactions()
                }
                else -> {
                    Log.e(TAG, "❌ Unknown sync type: $syncType")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Background sync failed: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun syncSingleTransaction(transactionId: String): Result {
        Log.d(TAG, "🔄 Syncing single transaction: $transactionId")

        return try {
            // Get transaction from database
            val database = KoshpalDatabase.getDatabase(applicationContext)
            val transaction = database.transactionDao().getTransactionById(transactionId)

            if (transaction != null) {
                var syncResult: Result = Result.retry()

                syncRepository.syncSingleTransaction(transaction).collect { result ->
                    when (result) {
                        is NetworkResult.Loading -> {
                            Log.d(TAG, "⏳ Single sync in progress...")
                        }
                        is NetworkResult.Success -> {
                            Log.d(TAG, "✅ Single transaction synced successfully")
                            syncResult = Result.success()
                        }
                        is NetworkResult.Error -> {
                            Log.e(TAG, "❌ Single transaction sync failed: ${result.message}")
                            syncResult = Result.retry()
                        }
                    }
                }

                syncResult
            } else {
                Log.e(TAG, "❌ Transaction not found: $transactionId")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error syncing single transaction: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun syncBulkTransactions(): Result {
        Log.d(TAG, "🔄 Starting bulk background sync")

        return try {
            // Get all unsynced transactions
            val unsyncedTransactions = syncRepository.getUnsyncedTransactions()

            if (unsyncedTransactions.isEmpty()) {
                Log.d(TAG, "✅ No unsynced transactions - all synced!")
                return Result.success()
            }

            Log.d(TAG, "📊 Found ${unsyncedTransactions.size} unsynced transactions")

            // Use the repository's chunked bulk sync method
            var syncResult: Result = Result.retry()

            syncRepository.syncBulkTransactions(unsyncedTransactions).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        Log.d(TAG, "⏳ Bulk sync in progress...")
                    }
                    is NetworkResult.Success -> {
                        val syncedCount = result.data?.data?.syncedCount ?: 0
                        val failedCount = result.data?.data?.failedCount ?: 0
                        Log.d(TAG, "✅ Bulk sync completed: $syncedCount synced, $failedCount failed")
                        syncResult = if (syncedCount > 0) Result.success() else Result.retry()
                    }
                    is NetworkResult.Error -> {
                        Log.e(TAG, "❌ Bulk sync failed: ${result.message}")
                        syncResult = Result.retry()
                    }
                }
            }

            syncResult
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in bulk background sync: ${e.message}", e)
            Result.retry()
        }
    }
}
