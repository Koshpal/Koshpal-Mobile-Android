package com.koshpal_android.koshpalapp.ui.auth

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState

    // Whitelist of allowed emails
    private val allowedEmails = setOf(
        "muditsharmaanjana2203@gmail.com",
        "guptasankalp2004@gmail.com",
        "tushars7740@gmail.com",
        "akshatnahata05@gmail.com",
        "khandalakshit@gmail.com",
        "karanbankar54@gmail.com",
        "koshpal@gmail.com"
    )

    fun signup(email: String, name: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                Log.d("SignupViewModel", "=== SIGNUP REQUEST ===")
                Log.d("SignupViewModel", "Email: $email")
                Log.d("SignupViewModel", "Name: $name")

                // Validate email format
                if (!isValidEmail(email)) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Please enter a valid email address"
                    )
                    return@launch
                }

                // Check if email is in whitelist
                if (!isEmailWhitelisted(email)) {
                    Log.w("SignupViewModel", "Email not in whitelist: $email")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "This email is not authorized to access the app. Please contact support."
                    )
                    return@launch
                }

                Log.d("SignupViewModel", "Email verified and whitelisted")

                // Store user data locally
                userPreferences.saveEmail(email)
                userPreferences.setLoggedIn(true)

                Log.d("SignupViewModel", "User data saved successfully")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true,
                    message = "Welcome $name! Signup successful!"
                )

            } catch (e: Exception) {
                Log.e("SignupViewModel", "Signup error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Signup failed: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Validates if the email format is correct
     */
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Checks if the email is in the whitelist
     */
    private fun isEmailWhitelisted(email: String): Boolean {
        val normalizedEmail = email.lowercase().trim()
        return allowedEmails.any { it.lowercase() == normalizedEmail }
    }

    fun clearState() {
        _uiState.value = SignupUiState()
    }
}

data class SignupUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
