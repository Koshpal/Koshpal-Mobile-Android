package com.koshpal_android.koshpalapp.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.repository.AuthRepository
import com.koshpal_android.koshpalapp.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OTPVerificationViewModel(context: Context) : ViewModel() {
    private val userPreferences = UserPreferences(context)
    private val userRepository = UserRepository(userPreferences = userPreferences)
    private val authRepository = AuthRepository(userRepository, userPreferences)

    private val _uiState = MutableStateFlow(OTPUiState())
    val uiState: StateFlow<OTPUiState> = _uiState

    fun verifyOTPAndCreateUser(verificationId: String, otp: String, phoneNumber: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = authRepository.verifyOTPAndCreateUser(verificationId, otp, phoneNumber)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isVerified = true,
                    user = result.getOrNull()
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Verification failed"
                )
            }
        }
    }

    fun startTimer() {
        viewModelScope.launch {
            for (i in 30 downTo 0) {
                _uiState.value = _uiState.value.copy(timerSeconds = i)
                delay(1000)
            }
        }
    }
}

data class OTPUiState(
    val isLoading: Boolean = false,
    val isVerified: Boolean = false,
    val timerSeconds: Int = 30,
    val error: String? = null,
    val user: com.koshpal_android.koshpalapp.model.User? = null
)