package com.koshpal_android.koshpalapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import com.koshpal_android.koshpalapp.repository.BudgetRepository
import com.koshpal_android.koshpalapp.repository.SavingsGoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val userName: String = "Chaitany Kakde",
    val userEmail: String = "chaitany@example.com",
    val totalTransactions: Int = 0,
    val activeBudgets: Int = 0,
    val savingsGoals: Int = 0,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val savingsGoalRepository: SavingsGoalRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    init {
        loadProfileData()
    }
    
    fun loadProfileData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Get transaction count
                val transactionCount = transactionRepository.getTransactionCount()
                
                // For now, set default values - will be updated when data is available
                val activeBudgetsCount = 0
                val savingsGoalsCount = 0
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    totalTransactions = transactionCount,
                    activeBudgets = activeBudgetsCount,
                    savingsGoals = savingsGoalsCount
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load profile data: ${e.message}"
                )
            }
        }
    }
    
    fun updateUserName(newName: String) {
        _uiState.value = _uiState.value.copy(userName = newName)
    }
    
    fun updateUserEmail(newEmail: String) {
        _uiState.value = _uiState.value.copy(userEmail = newEmail)
    }
    
    fun logout() {
        viewModelScope.launch {
            try {
                // Clear user session data
                // This would typically involve clearing shared preferences, tokens, etc.
                _uiState.value = ProfileUiState() // Reset to default state
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to logout: ${e.message}"
                )
            }
        }
    }
}
