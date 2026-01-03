package com.koshpal_android.koshpalapp.ui.goals

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.auth.SessionManager
import com.koshpal_android.koshpalapp.data.remote.dto.FinancialGoalDto
import com.koshpal_android.koshpalapp.network.NetworkResult
import com.koshpal_android.koshpalapp.repository.GoalsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalsRepository: GoalsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val TAG = "GoalsViewModel"

    private val _financialGoals = MutableLiveData<List<FinancialGoalDto>>()
    val financialGoals: LiveData<List<FinancialGoalDto>> = _financialGoals

    private val _isLoadingGoals = MutableLiveData<Boolean>()
    val isLoadingGoals: LiveData<Boolean> = _isLoadingGoals

    private val _goalsError = MutableLiveData<String?>()
    val goalsError: LiveData<String?> = _goalsError

    private val _isCreatingGoal = MutableLiveData<Boolean>()
    val isCreatingGoal: LiveData<Boolean> = _isCreatingGoal

    private val _createGoalResult = MutableLiveData<CreateGoalResult?>()
    val createGoalResult: LiveData<CreateGoalResult?> = _createGoalResult

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    init {
        // Auto-load goals if user is logged in
        if (sessionManager.isLoggedIn.value) {
            loadFinancialGoals()
        }
    }

    /**
     * Load financial goals from server
     */
    fun loadFinancialGoals() {
        if (!sessionManager.isValidSession()) {
            Log.d(TAG, "⏭️ Skipping goals load: User not logged in")
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "🎯 Loading financial goals")

            _isLoadingGoals.value = true
            _goalsError.value = null

            goalsRepository.getFinancialGoals().collectLatest { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        Log.d(TAG, "⏳ Loading goals...")
                        _isLoadingGoals.value = true
                    }
                    is NetworkResult.Success -> {
                        val goalsResponse = result.data
                        val goals = goalsResponse?.financialGoals ?: emptyList()
                        Log.d(TAG, "✅ Loaded ${goals.size} financial goals")
                        _financialGoals.value = goals
                        _goalsError.value = null
                    }
                    is NetworkResult.Error -> {
                        Log.e(TAG, "❌ Failed to load goals: ${result.message}")
                        _goalsError.value = result.message
                        _financialGoals.value = emptyList()
                    }
                }
                _isLoadingGoals.value = false
            }
        }
    }

    /**
     * Create a new financial goal
     */
    fun createFinancialGoal(
        goalName: String,
        goalAmount: Double,
        goalDate: String,
        saving: Double = 0.0,
        icon: String = "🚗"
    ) {
        if (!sessionManager.isValidSession()) {
            _createGoalResult.value = CreateGoalResult.Error("Authentication required. Please login first.")
            return
        }

        // Basic validation
        if (goalName.isBlank()) {
            _createGoalResult.value = CreateGoalResult.Error("Goal name cannot be empty")
            return
        }

        if (goalAmount <= 0) {
            _createGoalResult.value = CreateGoalResult.Error("Goal amount must be greater than 0")
            return
        }

        if (goalDate.isBlank()) {
            _createGoalResult.value = CreateGoalResult.Error("Goal date is required")
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "🎯 Creating financial goal: $goalName")

            _isCreatingGoal.value = true
            _createGoalResult.value = null

            goalsRepository.createFinancialGoal(
                goalName = goalName,
                icon = icon,
                goalAmount = goalAmount,
                saving = saving,
                goalDate = goalDate
            ).collectLatest { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        Log.d(TAG, "⏳ Creating goal...")
                        _isCreatingGoal.value = true
                    }
                    is NetworkResult.Success -> {
                        val createdGoal = result.data?.data
                        if (createdGoal != null) {
                            Log.d(TAG, "✅ Goal created successfully: ${createdGoal.goalName}")
                            _createGoalResult.value = CreateGoalResult.Success(createdGoal)

                            // Refresh goals list
                            loadFinancialGoals()
                        } else {
                            _createGoalResult.value = CreateGoalResult.Error("Goal creation failed: Empty response")
                        }
                    }
                    is NetworkResult.Error -> {
                        Log.e(TAG, "❌ Goal creation failed: ${result.message}")
                        _createGoalResult.value = CreateGoalResult.Error(result.message)
                    }
                }
                _isCreatingGoal.value = false
            }
        }
    }

    /**
     * Clear create goal result (for UI state management)
     */
    fun clearCreateGoalResult() {
        _createGoalResult.value = null
    }

    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean = sessionManager.isLoggedIn.value

    /**
     * Get formatted target date for display
     */
    fun formatTargetDate(targetDateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(targetDateString)
            dateFormatter.format(date ?: Date())
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: $targetDateString", e)
            targetDateString // Return original if parsing fails
        }
    }

    /**
     * Calculate progress percentage for a goal
     */
    fun calculateProgress(current: Double, target: Double): Int {
        if (target <= 0) return 0
        val progress = (current / target * 100).toInt()
        return progress.coerceIn(0, 100)
    }

    /**
     * Get goals summary for display
     */
    fun getGoalsSummary(): GoalsSummary {
        val goals = _financialGoals.value ?: emptyList()
        val totalGoals = goals.size
        val completedGoals = goals.count { it.saving >= it.goalAmount }
        val totalTargetAmount = goals.sumOf { it.goalAmount }
        val totalSavedAmount = goals.sumOf { it.saving }

        return GoalsSummary(
            totalGoals = totalGoals,
            completedGoals = completedGoals,
            totalTargetAmount = totalTargetAmount,
            totalSavedAmount = totalSavedAmount
        )
    }

    sealed class CreateGoalResult {
        data class Success(val goal: FinancialGoalDto) : CreateGoalResult()
        data class Error(val message: String) : CreateGoalResult()
    }

    data class GoalsSummary(
        val totalGoals: Int,
        val completedGoals: Int,
        val totalTargetAmount: Double,
        val totalSavedAmount: Double
    )
}
