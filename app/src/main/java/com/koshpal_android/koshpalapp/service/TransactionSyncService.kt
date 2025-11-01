package com.koshpal_android.koshpalapp.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.data.remote.dto.BulkTransactionItem
import com.koshpal_android.koshpalapp.data.remote.dto.BulkTransactionRequest
import com.koshpal_android.koshpalapp.data.remote.dto.TransactionDto
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.network.ApiService
import com.koshpal_android.koshpalapp.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionSyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) {
    
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
     * Get static employee ID (no login required)
     */
    private fun getEmployeeId(): String {
        // Always return the static employee ID
        return Constants.STATIC_EMPLOYEE_ID
    }
    
    /**
     * Get device ID (unique identifier for this device)
     */
    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: UUID.randomUUID().toString()
    }
    
    /**
     * Convert Transaction entity to TransactionDto (for single transaction API)
     */
    private fun transactionToDto(transaction: Transaction, employeeId: String): TransactionDto {
        return TransactionDto(
            employeeId = employeeId,
            transaction_id = transaction.id,
            sender = transaction.bankName ?: "Unknown",
            message_body = transaction.smsBody ?: transaction.description,
            amount = transaction.amount,
            currency = "INR",
            txn_type = transaction.type.name,
            timestamp_ms = transaction.date,
            account_last4 = null, // Not available in current model
            merchant = transaction.merchant,
            category_id = transaction.categoryId,
            category_name = transaction.categoryId, // Use categoryId as name for now
            upi_ref = null, // Not available in current model
            bank = transaction.bankName,
            is_starred = transaction.isStarred,
            include_in_cash_flow = false, // Will be determined by cash flow logic
            source = "sms",
            app_version = "1.0.0",
            device_id = getDeviceId()
        )
    }
    
    /**
     * Convert Transaction entity to BulkTransactionItem (for bulk upload API)
     * Note: No employeeId at item level - it's at the request root level
     */
    private fun transactionToBulkItem(transaction: Transaction): BulkTransactionItem {
        return BulkTransactionItem(
            transaction_id = transaction.id,
            sender = transaction.bankName ?: "Unknown",
            message_body = transaction.smsBody ?: transaction.description,
            amount = transaction.amount,
            currency = "INR",
            txn_type = transaction.type.name,
            timestamp_ms = transaction.date,
            account_last4 = null, // Not available in current model
            merchant = transaction.merchant,
            category_id = transaction.categoryId,
            category_name = transaction.categoryId, // Use categoryId as name for now
            upi_ref = null, // Not available in current model
            bank = transaction.bankName,
            is_starred = transaction.isStarred,
            include_in_cash_flow = false, // Will be determined by cash flow logic
            source = "sms",
            app_version = "1.0.0",
            device_id = getDeviceId()
        )
    }
    
    /**
     * Sync a single transaction to backend (for new or updated transactions)
     */
    suspend fun syncSingleTransaction(transaction: Transaction): Boolean {
        if (!isNetworkAvailable()) {
            Log.d("TransactionSyncService", "No network available, skipping sync")
            return false
        }
        
        // Check if transaction is already synced
        if (transaction.isSynced) {
            Log.d("TransactionSyncService", "⏭️ Transaction already synced, skipping: ${transaction.id}")
            return true
        }
        
        return try {
            val employeeId = getEmployeeId()
            val transactionDto = transactionToDto(transaction, employeeId)
            
            Log.d("TransactionSyncService", "🔄 Syncing single transaction: ${transaction.merchant} - ₹${transaction.amount}")
            
            val response = apiService.uploadSingleTransaction(transactionDto)
            
            if (response.isSuccessful) {
                val syncResponse = response.body()
                if (syncResponse?.success == true) {
                    Log.d("TransactionSyncService", "✅ Transaction synced successfully: ${transaction.id}")
                    _lastSyncTime.value = System.currentTimeMillis()
                    
                    // Mark transaction as synced in database
                    transactionDao.markTransactionAsSynced(transaction.id, System.currentTimeMillis())
                    
                    // Update sync count and time in preferences
                    userPreferences.incrementSyncedCount(1)
                    userPreferences.setLastSyncTime(System.currentTimeMillis())
                    userPreferences.setLastSyncError(null)
                    
                    true
                } else {
                    Log.e("TransactionSyncService", "❌ Sync failed: ${syncResponse?.message}")
                    userPreferences.setLastSyncError(syncResponse?.message ?: "Unknown error")
                    
                    // Update last sync attempt time even if failed
                    transactionDao.updateLastSyncAttempt(transaction.id, System.currentTimeMillis())
                    false
                }
            } else if (response.code() == 409) {
                // 409 Conflict = Transaction already exists on server (already backed up!)
                Log.d("TransactionSyncService", "✅ Transaction already exists on server (already synced): ${transaction.id}")
                _lastSyncTime.value = System.currentTimeMillis()
                
                // Mark as synced since it's already on the server
                transactionDao.markTransactionAsSynced(transaction.id, System.currentTimeMillis())
                
                userPreferences.setLastSyncTime(System.currentTimeMillis())
                userPreferences.setLastSyncError(null)
                true // Treat as success - transaction is already backed up
            } else {
                val error = "API error: ${response.code()} - ${response.message()}"
                Log.e("TransactionSyncService", "❌ $error")
                userPreferences.setLastSyncError(error)
                
                // Update last sync attempt time
                transactionDao.updateLastSyncAttempt(transaction.id, System.currentTimeMillis())
                false
            }
        } catch (e: Exception) {
            val error = "Exception during sync: ${e.message}"
            Log.e("TransactionSyncService", "❌ $error", e)
            userPreferences.setLastSyncError(error)
            
            // Update last sync attempt time
            try {
                transactionDao.updateLastSyncAttempt(transaction.id, System.currentTimeMillis())
            } catch (dbError: Exception) {
                Log.e("TransactionSyncService", "Failed to update sync attempt: ${dbError.message}")
            }
            false
        }
    }
    
    /**
     * Bulk sync all transactions (first-time sync) - Uses bulk upload API
     */
    suspend fun performInitialSync(): SyncResult {
        if (!isNetworkAvailable()) {
            val error = "No network connection"
            Log.d("TransactionSyncService", "No network available, skipping initial sync")
            userPreferences.setLastSyncError(error)
            return SyncResult(false, 0, 0, error)
        }
        
        _syncState.value = SyncState.SYNCING
        _syncProgress.value = 0
        
        return try {
            val employeeId = getEmployeeId()
            Log.d("TransactionSyncService", "🚀 Starting bulk sync for employee: $employeeId")
            
            // Get all transactions from local database
            val allTransactions = transactionDao.getAllTransactionsOnce()
            Log.d("TransactionSyncService", "📊 Found ${allTransactions.size} transactions to sync")
            
            if (allTransactions.isEmpty()) {
                _syncState.value = SyncState.SUCCESS
                userPreferences.setInitialSyncCompleted(true)
                userPreferences.setLastSyncTime(System.currentTimeMillis())
                userPreferences.setLastSyncError(null)
                return SyncResult(true, 0, 0)
            }
            
            // Convert to bulk transaction items (without employeeId in each item)
            val transactionItems = allTransactions.map { transactionToBulkItem(it) }
            
            Log.d("TransactionSyncService", "📦 Chunking ${transactionItems.size} transactions into batches of ${Constants.SYNC_BATCH_SIZE}...")
            
            // Split into chunks to avoid server overload (max 50 per request)
            val chunks = transactionItems.chunked(Constants.SYNC_BATCH_SIZE)
            Log.d("TransactionSyncService", "📦 Created ${chunks.size} chunks")
            
            var totalInserted = 0
            var currentChunk = 0
            var hasError = false
            var lastError: String? = null
            
            // Upload each chunk separately
            for (chunk in chunks) {
                currentChunk++
                val progress = 50 + ((currentChunk * 40) / chunks.size) // 50-90% progress
                _syncProgress.value = progress
                
                Log.d("TransactionSyncService", "📤 Uploading chunk $currentChunk/${chunks.size} (${chunk.size} transactions)...")
                
                try {
                    val bulkRequest = BulkTransactionRequest(
                        employeeId = employeeId,
                        transactions = chunk
                    )
                    
                    val response = apiService.uploadBulkTransactions(bulkRequest)
                    
                    if (response.isSuccessful) {
                        val bulkResponse = response.body()
                        if (bulkResponse?.success == true) {
                            val insertedCount = bulkResponse.data?.insertedCount ?: 0
                            totalInserted += insertedCount
                            Log.d("TransactionSyncService", "✅ Chunk $currentChunk/${ chunks.size}: $insertedCount/${ chunk.size} transactions inserted")
                        } else {
                            hasError = true
                            lastError = bulkResponse?.message ?: "Unknown error"
                            Log.e("TransactionSyncService", "❌ Chunk $currentChunk failed: $lastError")
                        }
                    } else {
                        hasError = true
                        lastError = "HTTP ${response.code()}: ${response.message()}"
                        Log.e("TransactionSyncService", "❌ Chunk $currentChunk API error: $lastError")
                        
                        // Log response body for debugging
                        val errorBody = response.errorBody()?.string()
                        if (errorBody != null) {
                            Log.e("TransactionSyncService", "Error body: $errorBody")
                        }
                    }
                } catch (e: Exception) {
                    hasError = true
                    lastError = "Exception: ${e.message}"
                    Log.e("TransactionSyncService", "❌ Chunk $currentChunk exception: ${e.message}", e)
                }
                
                // Small delay between chunks to avoid overwhelming server
                if (currentChunk < chunks.size) {
                    delay(500)
                }
            }
            
            _syncProgress.value = 90
            _lastSyncTime.value = System.currentTimeMillis()
            
            // Final result
            // Consider it successful if:
            // 1. Some transactions were inserted, OR
            // 2. No transactions inserted BUT no errors (means all already exist - which is OK!)
            val isSuccess = !hasError
            
            if (isSuccess) {
                if (totalInserted > 0) {
                    Log.d("TransactionSyncService", "✅ Bulk sync completed: $totalInserted/${transactionItems.size} transactions inserted")
                } else {
                    Log.d("TransactionSyncService", "✅ Bulk sync completed: 0/${transactionItems.size} inserted (all transactions already exist on server)")
                }
                
                // Mark ALL transactions as synced in database (whether new or already existing)
                Log.d("TransactionSyncService", "📝 Marking all ${allTransactions.size} transactions as synced...")
                val allTransactionIds = allTransactions.map { it.id }
                transactionDao.markTransactionsAsSynced(allTransactionIds, System.currentTimeMillis())
                Log.d("TransactionSyncService", "✅ All transactions marked as synced")
                
                // Update preferences
                userPreferences.setInitialSyncCompleted(true)
                // Update count only if new transactions were inserted
                if (totalInserted > 0) {
                    val currentCount = userPreferences.getTotalSyncedCount()
                    userPreferences.setTotalSyncedCount(currentCount + totalInserted)
                }
                userPreferences.setLastSyncTime(System.currentTimeMillis())
                userPreferences.setLastSyncError(null)
                
                _syncState.value = SyncState.SUCCESS
                _syncProgress.value = 100
                
                SyncResult(
                    success = true,
                    syncedCount = totalInserted,
                    failedCount = 0,
                    error = null
                )
            } else {
                Log.e("TransactionSyncService", "❌ Bulk sync failed: $lastError")
                _syncState.value = SyncState.ERROR
                userPreferences.setLastSyncError(lastError ?: "Sync failed with errors")
                _syncProgress.value = 100
                SyncResult(false, totalInserted, transactionItems.size - totalInserted, lastError)
            }
            
        } catch (e: Exception) {
            val error = e.message ?: "Unknown exception"
            Log.e("TransactionSyncService", "❌ Initial sync failed: $error", e)
            _syncState.value = SyncState.ERROR
            userPreferences.setLastSyncError(error)
            SyncResult(false, 0, 0, error)
        }
    }
    
    /**
     * Auto-sync new transactions (called when new SMS is processed)
     */
    fun autoSyncNewTransaction(transaction: Transaction) {
        if (syncJob?.isActive == true) {
            Log.d("TransactionSyncService", "Sync already in progress, queuing transaction")
            return
        }
        
        syncJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                delay(2000) // Small delay to ensure transaction is saved locally first
                syncSingleTransaction(transaction)
            } catch (e: Exception) {
                Log.e("TransactionSyncService", "Auto-sync failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Auto-sync transaction updates (called when transaction is modified)
     */
    fun autoSyncTransactionUpdate(transaction: Transaction) {
        if (syncJob?.isActive == true) {
            Log.d("TransactionSyncService", "Sync already in progress, queuing update")
            return
        }
        
        syncJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                delay(1000) // Small delay
                syncSingleTransaction(transaction)
            } catch (e: Exception) {
                Log.e("TransactionSyncService", "Auto-sync update failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Check sync status with server
     */
    suspend fun checkSyncStatus(): Boolean {
        if (!isNetworkAvailable()) return false
        
        return try {
            val employeeId = getEmployeeId()
            val response = apiService.getSyncStatus(employeeId, getDeviceId())
            
            if (response.isSuccessful) {
                val status = response.body()
                Log.d("TransactionSyncService", "Sync status: $status")
                true
            } else {
                Log.e("TransactionSyncService", "Failed to get sync status: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e("TransactionSyncService", "Exception getting sync status: ${e.message}", e)
            false
        }
    }
    
    /**
     * Force sync all pending transactions
     */
    suspend fun forceSyncAll(): SyncResult {
        return performInitialSync()
    }
    
    /**
     * Cancel ongoing sync
     */
    fun cancelSync() {
        syncJob?.cancel()
        _syncState.value = SyncState.IDLE
        _syncProgress.value = 0
    }
}
