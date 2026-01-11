package com.koshpal_android.koshpalapp.service

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Scheduler for background transaction sync using WorkManager
 */
object TransactionSyncScheduler {

    private const val TAG = "TransactionSyncScheduler"
    private const val PERIODIC_SYNC_WORK_NAME = "periodic_transaction_sync"

    // Sync types (matching TransactionSyncWorker constants)
    private const val SYNC_TYPE_SINGLE = "single"
    private const val SYNC_TYPE_BULK = "bulk"
    
    /**
     * Schedule periodic sync every 15 minutes (minimum allowed by Android)
     */
    fun schedulePeriodicSync(context: Context) {
        Log.d(TAG, "📅 Scheduling periodic transaction sync")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val periodicWorkRequest = PeriodicWorkRequestBuilder<TransactionSyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(TransactionSyncWorker.TAG)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
        
        Log.d(TAG, "✅ Periodic sync scheduled successfully")
    }
    
    /**
     * Schedule one-time sync for a specific transaction
     */
    fun scheduleSingleTransactionSync(context: Context, transactionId: String) {
        Log.d(TAG, "📤 Scheduling single transaction sync: $transactionId")
        
        val inputData = workDataOf(
            TransactionSyncWorker.KEY_TRANSACTION_ID to transactionId,
            TransactionSyncWorker.KEY_SYNC_TYPE to TransactionSyncWorker.SYNC_TYPE_SINGLE
        )
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<TransactionSyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(TransactionSyncWorker.TAG)
            .build()
        
        WorkManager.getInstance(context).enqueueUniqueWork(
            "sync_transaction_$transactionId",
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )
        
        Log.d(TAG, "✅ Single transaction sync scheduled: $transactionId")
    }
    
    /**
     * Schedule immediate sync of all transactions
     */
    fun scheduleImmediateSync(context: Context) {
        Log.d(TAG, "🚀 Scheduling immediate sync of all transactions")
        
        val inputData = workDataOf(
            TransactionSyncWorker.KEY_SYNC_TYPE to SYNC_TYPE_BULK
        )
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<TransactionSyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(TransactionSyncWorker.TAG)
            .build()
        
        WorkManager.getInstance(context).enqueue(oneTimeWorkRequest)
        
        Log.d(TAG, "✅ Immediate sync scheduled")
    }
    
    /**
     * Cancel all scheduled sync work
     */
    fun cancelAllSync(context: Context) {
        Log.d(TAG, "🛑 Canceling all scheduled sync work")
        WorkManager.getInstance(context).cancelAllWorkByTag(TransactionSyncWorker.TAG)
        Log.d(TAG, "✅ All sync work canceled")
    }
    
    /**
     * Cancel periodic sync
     */
    fun cancelPeriodicSync(context: Context) {
        Log.d(TAG, "🛑 Canceling periodic sync")
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
        Log.d(TAG, "✅ Periodic sync canceled")
    }
}

