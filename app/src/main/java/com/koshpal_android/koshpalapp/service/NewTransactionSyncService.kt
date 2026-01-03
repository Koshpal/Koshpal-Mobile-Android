package com.koshpal_android.koshpalapp.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.*
import com.koshpal_android.koshpalapp.auth.SessionManager
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.network.NetworkResult
import com.koshpal_android.koshpalapp.repository.SyncRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewTransactionSyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncRepository: SyncRepository,
    private val sessionManager: SessionManager
) {

    private val TAG = "NewTransactionSyncService"

    private val database = KoshpalDatabase.getDatabase(context)
    private val transactionDao = database.transactionDao()

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _syncProgress = MutableStateFlow(0)
    val syncProgress: StateFlow<Int> = _syncProgress.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private var syncJob: Job? = null

    enum class SyncState {
        IDLE, SYNCING, SUCCESS, ERROR
    }

    data class SyncResult(
        val success: Boolean,
        val syncedCount: Int,
        val failedCount: Int,
        val error: String? = null
    )

    /**
     * Check if device has internet connectivity
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    /**
     * Perform bulk sync of all unsynced transactions
     */
    suspend fun performBulkSync(): SyncResult {
        Log.d(TAG, "🚀 Starting bulk sync")

        // Check authentication
        if (!sessionManager.isValidSession()) {
            val error = "Authentication required. Please login first."
            Log.e(TAG, "❌ $error")
            return SyncResult(false, 0, 0, error)
        }

        // Check network
        if (!isNetworkAvailable()) {
            val error = "No network connection"
            Log.d(TAG, "❌ $error")
            return SyncResult(false, 0, 0, error)
        }

        _syncState.value = SyncState.SYNCING
        _syncProgress.value = 0

        return try {
            // Get all unsynced transactions
            val unsyncedTransactions = syncRepository.getUnsyncedTransactions()
            Log.d(TAG, "📊 Found ${unsyncedTransactions.size} unsynced transactions")

            if (unsyncedTransactions.isEmpty()) {
                Log.d(TAG, "✅ No unsynced transactions")
                _syncState.value = SyncState.SUCCESS
                _lastSyncTime.value = System.currentTimeMillis()
                return SyncResult(true, 0, 0)
            }

            // Split into batches of 10 for bulk sync
            val batches = unsyncedTransactions.chunked(10)
            Log.d(TAG, "📦 Split into ${batches.size} batches")

            var totalSynced = 0
            var totalFailed = 0

            // Process each batch
            batches.forEachIndexed { index, batch ->
                val progress = ((index + 1) * 90) / batches.size // 0-90% progress
                _syncProgress.value = progress

                Log.d(TAG, "📤 Processing batch ${index + 1}/${batches.size} (${batch.size} transactions)")

                syncRepository.syncBulkTransactions(batch).collect { result ->
                    when (result) {
                        is NetworkResult.Loading -> {
                            Log.d(TAG, "⏳ Batch sync in progress...")
                        }
                        is NetworkResult.Success -> {
                            val batchResult = result.data
                            val syncedInBatch = batchResult?.data?.syncedCount ?: 0
                            totalSynced += syncedInBatch
                            Log.d(TAG, "✅ Batch ${index + 1}: $syncedInBatch/${batch.size} synced")
                        }
                        is NetworkResult.Error -> {
                            totalFailed += batch.size
                            Log.e(TAG, "❌ Batch ${index + 1} failed: ${result.message}")
                        }
                    }
                }

                // Small delay between batches to avoid overwhelming server
                if (index < batches.size - 1) {
                    delay(2000)
                }
            }

            _syncProgress.value = 95
            _lastSyncTime.value = System.currentTimeMillis()

            val isSuccess = totalFailed == 0
            if (isSuccess) {
                Log.d(TAG, "✅ Bulk sync completed: $totalSynced synced, $totalFailed failed")
                _syncState.value = SyncState.SUCCESS
                _syncProgress.value = 100
                SyncResult(true, totalSynced, totalFailed)
            } else {
                Log.e(TAG, "❌ Bulk sync partially failed: $totalSynced synced, $totalFailed failed")
                _syncState.value = SyncState.ERROR
                _syncProgress.value = 100
                SyncResult(false, totalSynced, totalFailed, "Partial sync failure")
            }

        } catch (e: Exception) {
            val error = "Bulk sync failed: ${e.message}"
            Log.e(TAG, error, e)
            _syncState.value = SyncState.ERROR
            SyncResult(false, 0, 0, error)
        }
    }

    /**
     * Sync a single transaction (called when new SMS is processed)
     */
    fun syncSingleTransaction(transaction: Transaction) {
        if (syncJob?.isActive == true) {
            Log.d(TAG, "⏳ Sync already in progress, queuing transaction")
            return
        }

        syncJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🔄 Syncing single transaction: ${transaction.id}")

                // Wait a bit for transaction to be saved locally first
                delay(1000)

                syncRepository.syncSingleTransaction(transaction).collect { result ->
                    when (result) {
                        is NetworkResult.Loading -> {
                            Log.d(TAG, "⏳ Single sync in progress...")
                        }
                        is NetworkResult.Success -> {
                            Log.d(TAG, "✅ Single transaction synced successfully")
                            _lastSyncTime.value = System.currentTimeMillis()
                        }
                        is NetworkResult.Error -> {
                            Log.e(TAG, "❌ Single transaction sync failed: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Single sync exception: ${e.message}", e)
            }
        }
    }

    /**
     * Auto-sync new transactions (called when new SMS is processed)
     * Only syncs if user is authenticated
     */
    fun autoSyncNewTransaction(transaction: Transaction) {
        // Check if user is authenticated before attempting sync
        if (!sessionManager.isValidSession()) {
            Log.d(TAG, "⏭️ User not authenticated, skipping auto-sync")
            return
        }

        if (syncJob?.isActive == true) {
            Log.d(TAG, "⏳ Sync already in progress, queuing transaction")
            return
        }

        syncJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                delay(2000) // Small delay to ensure transaction is saved locally first
                syncSingleTransaction(transaction)
                Log.d(TAG, "✅ Auto-sync completed for transaction: ${transaction.id}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Auto-sync failed: ${e.message}", e)
            } finally {
                syncJob = null
            }
        }
    }

    /**
     * Cancel ongoing sync
     */
    fun cancelSync() {
        Log.d(TAG, "🛑 Canceling sync")
        syncJob?.cancel()
        _syncState.value = SyncState.IDLE
        _syncProgress.value = 0
    }

    /**
     * Get current sync statistics
     */
    suspend fun getSyncStats(): SyncStats {
        val unsyncedCount = syncRepository.getUnsyncedTransactionCount()
        return SyncStats(
            unsyncedCount = unsyncedCount,
            lastSyncTime = _lastSyncTime.value,
            isSyncing = _syncState.value == SyncState.SYNCING
        )
    }
}

data class SyncStats(
    val unsyncedCount: Int,
    val lastSyncTime: Long,
    val isSyncing: Boolean
)
