package com.koshpal_android.koshpalapp.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.model.Transaction
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker for background transaction sync
 * This worker runs periodically to sync new/updated transactions to the backend
 */
@HiltWorker
class TransactionSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionSyncService: TransactionSyncService
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "transaction_sync_worker"
        const val TAG = "TransactionSyncWorker"
        
        // Input data keys
        const val KEY_TRANSACTION_ID = "transaction_id"
        const val KEY_SYNC_TYPE = "sync_type"
        
        // Sync types
        const val SYNC_TYPE_SINGLE = "single"
        const val SYNC_TYPE_ALL = "all"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "🔄 Starting background transaction sync")
        
        return try {
            val syncType = inputData.getString(KEY_SYNC_TYPE) ?: SYNC_TYPE_ALL
            
            when (syncType) {
                SYNC_TYPE_SINGLE -> {
                    // Sync a specific transaction
                    val transactionId = inputData.getString(KEY_TRANSACTION_ID)
                    if (transactionId != null) {
                        syncSingleTransaction(transactionId)
                    } else {
                        Log.e(TAG, "❌ Transaction ID not provided for single sync")
                        Result.failure()
                    }
                }
                SYNC_TYPE_ALL -> {
                    // Sync all pending transactions
                    syncAllPendingTransactions()
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
                val success = transactionSyncService.syncSingleTransaction(transaction)
                if (success) {
                    Log.d(TAG, "✅ Transaction synced successfully: $transactionId")
                    Result.success()
                } else {
                    Log.e(TAG, "❌ Failed to sync transaction: $transactionId")
                    Result.retry()
                }
            } else {
                Log.e(TAG, "❌ Transaction not found: $transactionId")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error syncing single transaction: ${e.message}", e)
            Result.retry()
        }
    }
    
    private suspend fun syncAllPendingTransactions(): Result {
        Log.d(TAG, "🔄 Syncing all pending transactions")
        
        return try {
            // Get ONLY unsynced transactions from database
            val database = KoshpalDatabase.getDatabase(applicationContext)
            val unsyncedTransactions = database.transactionDao().getUnsyncedTransactions()
            
            if (unsyncedTransactions.isEmpty()) {
                Log.d(TAG, "✅ No unsynced transactions - all transactions are already backed up!")
                return Result.success()
            }
            
            Log.d(TAG, "📊 Found ${unsyncedTransactions.size} unsynced transactions to sync")
            
            var successCount = 0
            var failureCount = 0
            
            // Sync each unsynced transaction
            unsyncedTransactions.forEach { transaction ->
                val success = transactionSyncService.syncSingleTransaction(transaction)
                if (success) {
                    successCount++
                } else {
                    failureCount++
                }
            }
            
            Log.d(TAG, "✅ Background sync completed: $successCount succeeded, $failureCount failed out of ${unsyncedTransactions.size} unsynced")
            
            // Return success if at least some transactions were synced OR if all are already synced
            if (successCount > 0 || unsyncedTransactions.isEmpty()) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error syncing all transactions: ${e.message}", e)
            Result.retry()
        }
    }
}

