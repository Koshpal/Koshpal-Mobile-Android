package com.koshpal_android.koshpalapp.service

import android.util.Log
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.data.remote.dto.EmployeeLoginRequest
import com.koshpal_android.koshpalapp.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoLoginService @Inject constructor(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) {
    
    companion object {
        private const val DEMO_EMAIL = "demo@koshpal.com"
        private const val DEMO_PASSWORD = "demo@123"
    }
    
    suspend fun loginWithDemoCredentials(): LoginResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("DemoLoginService", "🔐 Starting demo login process...")
                Log.d("DemoLoginService", "📧 Demo email: $DEMO_EMAIL")
                Log.d("DemoLoginService", "🔑 Demo password: $DEMO_PASSWORD")
                
                val loginRequest = EmployeeLoginRequest(
                    email = DEMO_EMAIL,
                    password = DEMO_PASSWORD
                )
                
                Log.d("DemoLoginService", "📤 Sending login request: $loginRequest")
                
                val response = apiService.employeeLogin(loginRequest)
                
                Log.d("DemoLoginService", "📥 Received response - Code: ${response.code()}, Success: ${response.isSuccessful}")
                
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    Log.d("DemoLoginService", "📋 Response body: $loginResponse")
                    
                    if (loginResponse?.success == true) {
                        Log.d("DemoLoginService", "✅ Login response indicates success")
                        
                        // Save user data
                        loginResponse.employeeId?.let { employeeId ->
                            userPreferences.saveUserId(employeeId)
                            Log.d("DemoLoginService", "💾 Saved employeeId: $employeeId")
                        }
                        
                        loginResponse.token?.let { token ->
                            userPreferences.saveUserToken(token)
                            Log.d("DemoLoginService", "💾 Saved user token")
                        }
                        
                        userPreferences.setLoggedIn(true)
                        userPreferences.saveLoginType("email")
                        
                        Log.d("DemoLoginService", "✅ Demo login completed successfully")
                        LoginResult.Success(loginResponse.employeeId ?: "")
                    } else {
                        Log.e("DemoLoginService", "❌ Login response indicates failure: ${loginResponse?.message}")
                        LoginResult.Error(loginResponse?.message ?: "Login failed")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("DemoLoginService", "❌ API error - Code: ${response.code()}, Body: $errorBody")
                    LoginResult.Error("API error: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                Log.e("DemoLoginService", "❌ Demo login exception: ${e.message}", e)
                LoginResult.Error("Network error: ${e.message}")
            }
        }
    }
    
    sealed class LoginResult {
        data class Success(val employeeId: String) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }
}
