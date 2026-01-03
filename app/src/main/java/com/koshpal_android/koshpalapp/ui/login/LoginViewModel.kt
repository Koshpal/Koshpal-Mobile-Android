package com.koshpal_android.koshpalapp.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.auth.SessionManager
import com.koshpal_android.koshpalapp.network.NetworkResult
import com.koshpal_android.koshpalapp.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val TAG = "LoginViewModel"

    private val _isLoginInProgress = MutableLiveData<Boolean>()
    val isLoginInProgress: LiveData<Boolean> = _isLoginInProgress

    private val _loginResult = MutableLiveData<LoginResult?>()
    val loginResult: LiveData<LoginResult?> = _loginResult

    init {
        // Check if already logged in
        if (sessionManager.isLoggedIn.value) {
            Log.d(TAG, "🔄 User already logged in, setting success result")
            _loginResult.value = LoginResult.Success("Already logged in as ${sessionManager.getUserName()}")
        }
    }

    fun login(email: String, password: String) {
        Log.d(TAG, "🚀 Login function called with email: $email")

        // Basic validation
        if (email.isBlank() || password.isBlank()) {
            _loginResult.value = LoginResult.Error("Please enter both email and password")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _loginResult.value = LoginResult.Error("Please enter a valid email address")
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "🔄 Setting login in progress to true")
            _isLoginInProgress.value = true
            _loginResult.value = null

            try {
                Log.d(TAG, "📞 Calling authRepository.login()")

                authRepository.login(email, password).collectLatest { result ->
                    when (result) {
                        is NetworkResult.Loading -> {
                            Log.d(TAG, "⏳ Login in progress...")
                            _isLoginInProgress.value = true
                        }
                        is NetworkResult.Success -> {
                            val user = result.data.user
                            Log.d(TAG, "✅ Login successful for user: ${user.email} (${user.name})")

                            // Session is already saved in AuthRepository
                            _loginResult.value = LoginResult.Success("Welcome back, ${user.name}!")
                        }
                        is NetworkResult.Error -> {
                            Log.e(TAG, "❌ Login failed: ${result.message}")
                            _loginResult.value = LoginResult.Error(result.message)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during login: ${e.message}", e)
                _loginResult.value = LoginResult.Error("Login error: ${e.localizedMessage ?: e.message}")
            } finally {
                Log.d(TAG, "🔄 Setting login in progress to false")
                _isLoginInProgress.value = false
            }
        }
    }

    fun logout() {
        Log.d(TAG, "🚪 Logging out user")
        authRepository.logout()
        _loginResult.value = null
    }

    fun isUserLoggedIn(): Boolean = sessionManager.isLoggedIn.value

    fun getCurrentUserName(): String? = sessionManager.getUserName()

    sealed class LoginResult {
        data class Success(val message: String) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }
}
