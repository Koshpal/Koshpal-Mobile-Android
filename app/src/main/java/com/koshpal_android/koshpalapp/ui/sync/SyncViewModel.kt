package com.koshpal_android.koshpalapp.ui.sync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncManager: SyncManager
) : ViewModel() {
    
    private val _isInitialSyncCompleted = MutableLiveData<Boolean>()
    val isInitialSyncCompleted: LiveData<Boolean> = _isInitialSyncCompleted
    
    private val _isInitialSyncInProgress = MutableLiveData<Boolean>()
    val isInitialSyncInProgress: LiveData<Boolean> = _isInitialSyncInProgress
    
    private val _syncProgress = MutableLiveData<Int>()
    val syncProgress: LiveData<Int> = _syncProgress
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    init {
        // Observe sync manager state
        viewModelScope.launch {
            syncManager.isInitialSyncCompleted.collect { completed ->
                _isInitialSyncCompleted.value = completed
            }
        }
        
        viewModelScope.launch {
            syncManager.isInitialSyncInProgress.collect { inProgress ->
                _isInitialSyncInProgress.value = inProgress
            }
        }
        
        viewModelScope.launch {
            syncManager.syncProgress.collect { progress ->
                _syncProgress.value = progress
            }
        }
        
        viewModelScope.launch {
            syncManager.errorMessage.collect { error ->
                _errorMessage.value = error
            }
        }
    }
    
    fun performInitialSync() {
        viewModelScope.launch {
            try {
                syncManager.performInitialSync()
                // Error messages are now handled by SyncManager and observed via errorMessage flow
            } catch (e: Exception) {
                _errorMessage.value = "Sync error: ${e.message}"
            }
        }
    }
    
    fun forceSyncAll() {
        viewModelScope.launch {
            try {
                val success = syncManager.forceSyncAll()
                if (!success) {
                    _errorMessage.value = "Failed to sync transactions. Please try again."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Sync error: ${e.message}"
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = ""
    }
}
