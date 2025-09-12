package com.koshpal_android.koshpalapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OTPVerificationViewModel : ViewModel() {
    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(OTPUiState())
    val uiState: StateFlow<OTPUiState> = _uiState

    fun verifyOTP(verificationId: String, otp: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = authRepository.verifyOTP(verificationId, otp)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, isVerified = true)
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
    val error: String? = null
)