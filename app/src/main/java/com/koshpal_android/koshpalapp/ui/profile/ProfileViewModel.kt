package com.koshpal_android.koshpalapp.ui.profile

import android.util.Log
import androidx.lifecycle.*
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.service.TransactionSyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val transactionSyncService: TransactionSyncService,
    private val userPreferences: UserPreferences
) : ViewModel() {
    
    // Sync Status (IDLE, SYNCING, SUCCESS, ERROR)
    private val _syncStatus = MutableLiveData<SyncStatus>(SyncStatus.IDLE)
    val syncStatus: LiveData<SyncStatus> = _syncStatus
    
    // Total synced transaction count
    private val _totalSyncedCount = MutableLiveData<Long>(0L)
    val totalSyncedCount: LiveData<Long> = _totalSyncedCount
    
    // Last sync error message
    private val _lastSyncError = MutableLiveData<String?>(null)
    val lastSyncError: LiveData<String?> = _lastSyncError
    
    // Last sync time
    private val _lastSyncTime = MutableLiveData<Long>(0L)
    val lastSyncTime: LiveData<Long> = _lastSyncTime
    
    // Is initial sync completed
    private val _isInitialSyncCompleted = MutableLiveData<Boolean>(false)
    val isInitialSyncCompleted: LiveData<Boolean> = _isInitialSyncCompleted
    
    // Sync progress (0-100)
    val syncProgress: StateFlow<Int> = transactionSyncService.syncProgress
    
    init {
        // Load sync data from preferences
        loadSyncData()
        
        // Observe sync service state
        viewModelScope.launch {
            transactionSyncService.syncState.collect { state ->
                _syncStatus.value = when (state) {
                    TransactionSyncService.SyncState.IDLE -> SyncStatus.IDLE
                    TransactionSyncService.SyncState.SYNCING -> SyncStatus.SYNCING
                    TransactionSyncService.SyncState.SUCCESS -> SyncStatus.SUCCESS
                    TransactionSyncService.SyncState.ERROR -> SyncStatus.ERROR
                }
            }
        }
    }
    
    /**
     * Load sync data from SharedPreferences
     */
    private fun loadSyncData() {
        _totalSyncedCount.value = userPreferences.getTotalSyncedCount()
        _lastSyncError.value = userPreferences.getLastSyncError()
        _lastSyncTime.value = userPreferences.getLastSyncTime()
        _isInitialSyncCompleted.value = userPreferences.isInitialSyncCompleted()
        
        Log.d("ProfileViewModel", "📊 Loaded sync data - Count: ${_totalSyncedCount.value}, Error: ${_lastSyncError.value}")
    }
    
    /**
     * Refresh sync data from preferences (call this after returning from other screens)
     */
    fun refreshSyncData() {
        loadSyncData()
    }
    
    /**
     * Perform initial bulk sync
     */
    fun performInitialSync() {
        Log.d("ProfileViewModel", "🔄 Starting initial bulk sync")
        
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            
            try {
                val result = transactionSyncService.performInitialSync()
                
                if (result.success) {
                    Log.d("ProfileViewModel", "✅ Sync completed successfully: ${result.syncedCount} transactions")
                    _syncStatus.value = SyncStatus.SUCCESS
                    
                    // Refresh data from preferences
                    loadSyncData()
                } else {
                    Log.e("ProfileViewModel", "❌ Sync failed: ${result.error}")
                    _syncStatus.value = SyncStatus.ERROR
                    _lastSyncError.value = result.error ?: "Unknown error"
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "❌ Sync exception: ${e.message}", e)
                _syncStatus.value = SyncStatus.ERROR
                _lastSyncError.value = e.message ?: "Unknown exception"
            }
        }
    }
    
    /**
     * Force re-sync all transactions
     */
    fun forceSyncAll() {
        Log.d("ProfileViewModel", "🔄 Force syncing all transactions")
        performInitialSync()
    }
    
    /**
     * Clear sync error
     */
    fun clearSyncError() {
        _lastSyncError.value = null
        userPreferences.setLastSyncError(null)
    }
    
    enum class SyncStatus {
        IDLE,      // No sync in progress
        SYNCING,   // Sync in progress
        SUCCESS,   // Sync completed successfully
        ERROR      // Sync failed with error
    }
}

