package com.koshpal_android.koshpalapp.ui.sync

import android.content.Context
import android.util.Log
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.service.TransactionSyncService
import com.koshpal_android.koshpalapp.service.DemoLoginService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncService: TransactionSyncService,
    private val userPreferences: UserPreferences,
    private val demoLoginService: DemoLoginService
) {
    
    private val _isInitialSyncCompleted = MutableStateFlow(false)
    val isInitialSyncCompleted: StateFlow<Boolean> = _isInitialSyncCompleted.asStateFlow()
    
    private val _isInitialSyncInProgress = MutableStateFlow(false)
    val isInitialSyncInProgress: StateFlow<Boolean> = _isInitialSyncInProgress.asStateFlow()
    
    private val _syncProgress = MutableStateFlow(0)
    val syncProgress: StateFlow<Int> = _syncProgress.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()
    
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()
    
    init {
        // Check if initial sync has been completed
        checkInitialSyncStatus()
        
        // Observe sync service state
        observeSyncService()
    }
    
    private fun checkInitialSyncStatus() {
        try {
            val isCompleted = userPreferences.isInitialSyncCompleted()
            _isInitialSyncCompleted.value = isCompleted
            Log.d("SyncManager", "Initial sync status: $isCompleted")
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to check initial sync status: ${e.message}")
        }
    }
    
    private fun observeSyncService() {
        // Observe sync service progress
        // Note: In a real implementation, you might want to use a proper observer pattern
        // For now, we'll update the progress when sync operations are triggered
    }
    
    /**
     * Perform initial sync of all transactions to MongoDB
     */
    suspend fun performInitialSync(): Boolean {
        if (_isInitialSyncCompleted.value) {
            Log.d("SyncManager", "Initial sync already completed, skipping")
            return true
        }
        
        if (_isInitialSyncInProgress.value) {
            Log.d("SyncManager", "Initial sync already in progress, skipping")
            return false
        }
        
        _isInitialSyncInProgress.value = true
        _errorMessage.value = ""
        
        return try {
            Log.d("SyncManager", "🚀 Starting initial sync...")
            
            // Check if user is logged in (should be done via login screen)
            if (!userPreferences.isLoggedIn()) {
                val errorMsg = "User not logged in. Please login first."
                Log.e("SyncManager", "❌ $errorMsg")
                _errorMessage.value = errorMsg
                return false
            }
            
            Log.d("SyncManager", "✅ User is logged in, proceeding with sync")
            
            val result = syncService.performInitialSync()
            
            if (result.success) {
                // Mark initial sync as completed
                userPreferences.setInitialSyncCompleted(true)
                _isInitialSyncCompleted.value = true
                _lastSyncTime.value = System.currentTimeMillis()
                
                Log.d("SyncManager", "✅ Initial sync completed successfully: ${result.syncedCount} transactions synced")
                true
            } else {
                val errorMsg = result.error ?: "Unknown sync error"
                Log.e("SyncManager", "❌ Initial sync failed: $errorMsg")
                _errorMessage.value = errorMsg
                false
            }
        } catch (e: Exception) {
            val errorMsg = "Initial sync exception: ${e.message}"
            Log.e("SyncManager", "❌ $errorMsg", e)
            _errorMessage.value = errorMsg
            false
        } finally {
            _isInitialSyncInProgress.value = false
        }
    }
    
    /**
     * Force sync all transactions (for manual sync)
     */
    suspend fun forceSyncAll(): Boolean {
        return try {
            Log.d("SyncManager", "🔄 Force syncing all transactions...")
            
            val result = syncService.forceSyncAll()
            
            if (result.success) {
                _lastSyncTime.value = System.currentTimeMillis()
                Log.d("SyncManager", "✅ Force sync completed: ${result.syncedCount} transactions synced")
                true
            } else {
                Log.e("SyncManager", "❌ Force sync failed: ${result.error}")
                false
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "❌ Force sync exception: ${e.message}", e)
            false
        }
    }
    
    /**
     * Check if sync is needed (for periodic checks)
     */
    suspend fun checkSyncStatus(): Boolean {
        return try {
            syncService.checkSyncStatus()
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to check sync status: ${e.message}")
            false
        }
    }
    
    /**
     * Get sync statistics
     */
    fun getSyncStats(): SyncStats {
        return SyncStats(
            isInitialSyncCompleted = _isInitialSyncCompleted.value,
            isInitialSyncInProgress = _isInitialSyncInProgress.value,
            lastSyncTime = _lastSyncTime.value,
            syncProgress = _syncProgress.value
        )
    }
    
    /**
     * Reset sync status (for testing or user request)
     */
    suspend fun resetSyncStatus() {
        try {
            userPreferences.setInitialSyncCompleted(false)
            _isInitialSyncCompleted.value = false
            _lastSyncTime.value = 0L
            _syncProgress.value = 0
            Log.d("SyncManager", "Sync status reset")
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to reset sync status: ${e.message}")
        }
    }
}

data class SyncStats(
    val isInitialSyncCompleted: Boolean,
    val isInitialSyncInProgress: Boolean,
    val lastSyncTime: Long,
    val syncProgress: Int
)
