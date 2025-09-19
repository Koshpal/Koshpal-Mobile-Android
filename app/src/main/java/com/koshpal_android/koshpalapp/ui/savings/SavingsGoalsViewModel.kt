package com.koshpal_android.koshpalapp.ui.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.repository.SavingsGoalRepository
import com.koshpal_android.koshpalapp.model.SavingsGoal
import com.koshpal_android.koshpalapp.model.GoalCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavingsGoalsViewModel @Inject constructor(
    private val savingsGoalRepository: SavingsGoalRepository
) : ViewModel() {
    
    private val _activeGoals = MutableStateFlow<List<SavingsGoal>>(emptyList())
    val activeGoals: StateFlow<List<SavingsGoal>> = _activeGoals.asStateFlow()
    
    private val _completedGoals = MutableStateFlow<List<SavingsGoal>>(emptyList())
    val completedGoals: StateFlow<List<SavingsGoal>> = _completedGoals.asStateFlow()
    
    private val _savingsOverview = MutableStateFlow(SavingsOverview())
    val savingsOverview: StateFlow<SavingsOverview> = _savingsOverview.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private var allActiveGoals: List<SavingsGoal> = emptyList()
    private var currentFilter: GoalFilter = GoalFilter.ALL
    
    fun loadSavingsGoals() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Load active goals
                savingsGoalRepository.getActiveGoals().collect { goals ->
                    allActiveGoals = goals
                    applyCurrentFilter()
                    _isLoading.value = false
                }
                
                // Load completed goals in parallel
                launch {
                    savingsGoalRepository.getCompletedGoals().collect { goals ->
                        _completedGoals.value = goals
                    }
                }
                
                // Update overview
                updateSavingsOverview()
                
            } catch (e: Exception) {
                _activeGoals.value = emptyList()
                _completedGoals.value = emptyList()
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun updateSavingsOverview() {
        try {
            val totalTarget = savingsGoalRepository.getTotalTargetAmount()
            val totalSaved = savingsGoalRepository.getTotalSavedAmount()
            val completedCount = savingsGoalRepository.getCompletedGoalsCount()
            val totalGoals = allActiveGoals.size + completedCount
            
            _savingsOverview.value = SavingsOverview(
                totalTarget = totalTarget,
                totalSaved = totalSaved,
                completedGoals = completedCount,
                totalGoals = totalGoals
            )
        } catch (e: Exception) {
            _savingsOverview.value = SavingsOverview()
        }
    }
    
    fun createGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            try {
                savingsGoalRepository.createGoal(
                    goal.name,
                    goal.targetAmount,
                    goal.category,
                    goal.targetDate
                )
                loadSavingsGoals() // Refresh
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun updateGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            try {
                savingsGoalRepository.updateGoal(goal)
                loadSavingsGoals() // Refresh
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            try {
                savingsGoalRepository.deleteSavingsGoal(goal)
                loadSavingsGoals() // Refresh
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun addMoneyToGoal(goalId: String, amount: Double) {
        viewModelScope.launch {
            try {
                savingsGoalRepository.addToGoal(goalId, amount)
                loadSavingsGoals() // Refresh
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun pauseGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            try {
                val pausedGoal = goal.copy(isActive = false)
                savingsGoalRepository.updateGoal(pausedGoal)
                loadSavingsGoals() // Refresh
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun applyFilter(filter: GoalFilter) {
        currentFilter = filter
        applyCurrentFilter()
    }
    
    private fun applyCurrentFilter() {
        val filteredGoals = when (currentFilter) {
            GoalFilter.ALL -> allActiveGoals
            GoalFilter.EMERGENCY_FUND -> allActiveGoals.filter { it.category == GoalCategory.EMERGENCY_FUND }
            GoalFilter.VACATION -> allActiveGoals.filter { it.category == GoalCategory.VACATION }
            GoalFilter.GADGET -> allActiveGoals.filter { it.category == GoalCategory.GADGET }
            GoalFilter.EDUCATION -> allActiveGoals.filter { it.category == GoalCategory.EDUCATION }
            GoalFilter.INVESTMENT -> allActiveGoals.filter { it.category == GoalCategory.INVESTMENT }
            GoalFilter.NEAR_DEADLINE -> allActiveGoals.filter { goal ->
                goal.targetDate?.let { targetDate ->
                    val daysRemaining = goal.getDaysRemaining() ?: Long.MAX_VALUE
                    daysRemaining <= 30 // Goals with deadline within 30 days
                } ?: false
            }
            GoalFilter.HIGH_PROGRESS -> allActiveGoals.filter { it.progressPercentage >= 75f }
        }
        
        _activeGoals.value = filteredGoals.sortedByDescending { it.progressPercentage }
    }
}

data class SavingsOverview(
    val totalTarget: Double = 0.0,
    val totalSaved: Double = 0.0,
    val completedGoals: Int = 0,
    val totalGoals: Int = 0
)

enum class GoalFilter {
    ALL,
    EMERGENCY_FUND,
    VACATION,
    GADGET,
    EDUCATION,
    INVESTMENT,
    NEAR_DEADLINE,
    HIGH_PROGRESS
}
