package com.koshpal_android.koshpalapp.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.model.Reminder
import com.koshpal_android.koshpalapp.model.ReminderStatus
import com.koshpal_android.koshpalapp.model.ReminderType
import com.koshpal_android.koshpalapp.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository
) : ViewModel() {
    
    // All reminders
    val allReminders: StateFlow<List<Reminder>> = reminderRepository.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Pending reminders
    val pendingReminders: StateFlow<List<Reminder>> = reminderRepository.getPendingReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Overdue reminders
    val overdueReminders: StateFlow<List<Reminder>> = reminderRepository.getOverdueReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Completed reminders
    val completedReminders: StateFlow<List<Reminder>> = reminderRepository.getCompletedReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Next reminder (closest due)
    val nextReminder: StateFlow<Reminder?> = reminderRepository.getNextReminder()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    // Upcoming reminders (next 7 days)
    val upcomingReminders: StateFlow<List<Reminder>> = reminderRepository.getUpcomingReminders(7)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // UI State
    private val _uiState = MutableStateFlow(ReminderUiState())
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()
    
    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Filtered reminders based on search
    val filteredReminders: StateFlow<List<Reminder>> = combine(
        allReminders,
        _searchQuery
    ) { reminders, query ->
        if (query.isBlank()) {
            reminders
        } else {
            reminders.filter { reminder ->
                reminder.personName.contains(query, ignoreCase = true) ||
                reminder.purpose.contains(query, ignoreCase = true) ||
                reminder.contact?.contains(query, ignoreCase = true) == true
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    init {
        // Update overdue reminders on init
        updateOverdueReminders()
        
        // Load statistics
        loadStatistics()
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun insertReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                reminderRepository.insertReminder(reminder)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Reminder created successfully!",
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to create reminder: ${e.message}",
                    successMessage = null
                )
            }
        }
    }
    
    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                reminderRepository.updateReminder(reminder)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Reminder updated successfully!",
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update reminder: ${e.message}",
                    successMessage = null
                )
            }
        }
    }
    
    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                reminderRepository.deleteReminder(reminder)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Reminder deleted successfully!",
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete reminder: ${e.message}",
                    successMessage = null
                )
            }
        }
    }
    
    fun markReminderCompleted(reminderId: String) {
        viewModelScope.launch {
            try {
                reminderRepository.markReminderCompleted(reminderId)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Reminder marked as completed!",
                    errorMessage = null
                )
                loadStatistics()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update reminder: ${e.message}",
                    successMessage = null
                )
            }
        }
    }
    
    fun updateReminderStatus(reminderId: String, status: ReminderStatus) {
        viewModelScope.launch {
            try {
                reminderRepository.updateReminderStatus(reminderId, status)
                loadStatistics()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update status: ${e.message}"
                )
            }
        }
    }
    
    fun updateOverdueReminders() {
        viewModelScope.launch {
            try {
                reminderRepository.updateOverdueReminders()
            } catch (e: Exception) {
                android.util.Log.e("ReminderViewModel", "Failed to update overdue reminders", e)
            }
        }
    }
    
    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                val pendingCount = reminderRepository.getPendingReminderCount()
                val overdueCount = reminderRepository.getOverdueReminderCount()
                val totalToGive = reminderRepository.getTotalAmountToGive()
                val totalToReceive = reminderRepository.getTotalAmountToReceive()
                
                _uiState.value = _uiState.value.copy(
                    pendingCount = pendingCount,
                    overdueCount = overdueCount,
                    totalAmountToGive = totalToGive,
                    totalAmountToReceive = totalToReceive
                )
            } catch (e: Exception) {
                android.util.Log.e("ReminderViewModel", "Failed to load statistics", e)
            }
        }
    }
    
    fun getReminderById(reminderId: String): Flow<Reminder?> {
        return reminderRepository.getReminderByIdFlow(reminderId)
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }
    
    fun deleteAllCompletedReminders() {
        viewModelScope.launch {
            try {
                reminderRepository.deleteAllCompletedReminders()
                _uiState.value = _uiState.value.copy(
                    successMessage = "All completed reminders deleted!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete reminders: ${e.message}"
                )
            }
        }
    }
}

data class ReminderUiState(
    val pendingCount: Int = 0,
    val overdueCount: Int = 0,
    val totalAmountToGive: Double = 0.0,
    val totalAmountToReceive: Double = 0.0,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
