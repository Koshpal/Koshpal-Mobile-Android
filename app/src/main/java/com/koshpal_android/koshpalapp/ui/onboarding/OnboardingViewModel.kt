package com.koshpal_android.koshpalapp.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.model.OnboardingResponse
import com.koshpal_android.koshpalapp.repository.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState

    private var userEmail: String = ""
    private val answers = mutableMapOf<String, String>()
    private var moneyWorryRating: Int = 1

    fun setUserEmail(email: String) {
        userEmail = email
    }

    fun updateAnswer(questionId: String, answer: String, sectionNumber: Int) {
        answers[questionId] = answer
    }

    fun updateRatingAnswer(questionId: String, rating: Int, sectionNumber: Int) {
        if (questionId == "money_worry_level") {
            moneyWorryRating = rating
        }
    }

    fun submitOnboardingData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val onboardingResponse = OnboardingResponse(
                    email = userEmail,
                    ageGroup = answers["age_group"] ?: "",
                    monthlySalary = answers["monthly_salary"] ?: "",
                    financialSituation = answers["financial_situation"] ?: "",
                    expenseTracking = answers["expense_tracking"] ?: "",
                    moneyWorryLevel = moneyWorryRating,
                    highestExpenseCategory = answers["highest_expense_category"] ?: "",
                    primaryFinancialGoal = answers["primary_financial_goal"] ?: "",
                    goalTimeframe = answers["goal_timeframe"] ?: "",
                    financialStressArea = answers["financial_stress_area"] ?: ""
                )

                val result = onboardingRepository.submitOnboardingData(onboardingResponse)
                if (result.isSuccess) {
                    // Mark onboarding as completed
                    userPreferences.setOnboardingCompleted(true)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isOnboardingComplete = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to save onboarding data"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.localizedMessage}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun getAnswers(): Map<String, String> = answers.toMap()
    fun getMoneyWorryRating(): Int = moneyWorryRating
}

data class OnboardingUiState(
    val isLoading: Boolean = false,
    val isOnboardingComplete: Boolean = false,
    val error: String? = null
)
