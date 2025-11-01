package com.koshpal_android.koshpalapp.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.service.DemoLoginService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val demoLoginService: DemoLoginService
) : ViewModel() {
    
    private val _isLoginInProgress = MutableLiveData<Boolean>()
    val isLoginInProgress: LiveData<Boolean> = _isLoginInProgress
    
    private val _loginResult = MutableLiveData<LoginResult?>()
    val loginResult: LiveData<LoginResult?> = _loginResult
    
    fun login(email: String, password: String) {
        Log.d("LoginViewModel", "🚀 Login function called with email: $email")
        
        viewModelScope.launch {
            Log.d("LoginViewModel", "🔄 Setting login in progress to true")
            _isLoginInProgress.value = true
            _loginResult.value = null
            
            try {
                Log.d("LoginViewModel", "📞 Calling demoLoginService.loginWithDemoCredentials()")
                
                // For demo purposes, always use demo credentials regardless of input
                // In production, you would validate the actual email/password
                val result = demoLoginService.loginWithDemoCredentials()
                
                Log.d("LoginViewModel", "📋 Login service result: $result")
                
                when (result) {
                    is DemoLoginService.LoginResult.Success -> {
                        Log.d("LoginViewModel", "✅ Login successful!")
                        _loginResult.value = LoginResult.Success("Login successful!")
                    }
                    is DemoLoginService.LoginResult.Error -> {
                        Log.e("LoginViewModel", "❌ Login failed: ${result.message}")
                        _loginResult.value = LoginResult.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "❌ Exception during login: ${e.message}", e)
                _loginResult.value = LoginResult.Error("Login error: ${e.message}")
            } finally {
                Log.d("LoginViewModel", "🔄 Setting login in progress to false")
                _isLoginInProgress.value = false
            }
        }
    }
    
    sealed class LoginResult {
        data class Success(val message: String) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }
}
