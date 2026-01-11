package com.koshpal_android.koshpalapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.auth.SessionManager
import com.koshpal_android.koshpalapp.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OTPVerificationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OTPUiState())
    val uiState: StateFlow<OTPUiState> = _uiState

    fun verifyOTPAndCreateUser(verificationId: String, otp: String, phoneNumber: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // TODO: Implement phone number OTP verification
            // This functionality is not implemented in the current email/password auth system
            delay(1000) // Simulate network call

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Phone number authentication is not implemented. Please use email login."
            )
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