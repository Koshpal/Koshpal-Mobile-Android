package com.koshpal_android.koshpalapp.repository

import android.util.Log
import com.koshpal_android.koshpalapp.auth.SessionManager
import com.koshpal_android.koshpalapp.data.remote.dto.LoginRequest
import com.koshpal_android.koshpalapp.data.remote.dto.LoginResponse
import com.koshpal_android.koshpalapp.network.ApiService
import com.koshpal_android.koshpalapp.network.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {

    private val TAG = "AuthRepository"

    /**
     * Login with email and password
     */
    fun login(email: String, password: String): Flow<NetworkResult<LoginResponse>> = flow {
        try {
            Log.d(TAG, "🚀 Starting login request for email: $email")

            emit(NetworkResult.Loading())

            val loginRequest = LoginRequest(email, password)
            val response = apiService.login(loginRequest)

            if (response.isSuccessful) {
                val loginResponse = response.body()
                if (loginResponse != null) {
                    Log.d(TAG, "✅ Login successful for user: ${loginResponse.user.email}")

                    // Extract tokens from Set-Cookie headers
                    var accessToken: String? = null
                    var refreshToken: String? = null

                    val setCookieHeaders = response.headers().values("Set-Cookie")
                    for (cookie in setCookieHeaders) {
                        Log.d(TAG, "🍪 Cookie received: $cookie")
                        if (cookie.startsWith("accessToken=")) {
                            accessToken = cookie.substringAfter("accessToken=").substringBefore(";")
                            Log.d(TAG, "🔑 Extracted accessToken: ${accessToken.take(20)}...")
                        } else if (cookie.startsWith("refreshToken=")) {
                            refreshToken = cookie.substringAfter("refreshToken=").substringBefore(";")
                            Log.d(TAG, "🔄 Extracted refreshToken: ${refreshToken.take(20)}...")
                        }
                    }

                    Log.d(TAG, "👤 User data: id=${loginResponse.user.id}, email=${loginResponse.user.email}, isActive=${loginResponse.user.isActive}")

                    // Save session with tokens
                    sessionManager.saveSession(loginResponse.user, accessToken, refreshToken)
                    Log.d(TAG, "✅ Session saved - checking validity: ${sessionManager.isValidSession()}")

                    emit(NetworkResult.Success(loginResponse))
                } else {
                    Log.e(TAG, "❌ Login response body is null")
                    emit(NetworkResult.Error("Login failed: Empty response"))
                }
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "Invalid email or password"
                    403 -> "Account is not active"
                    429 -> "Too many login attempts. Please try again later"
                    else -> "Login failed: ${response.message()}"
                }
                Log.e(TAG, "❌ Login failed with code ${response.code()}: ${response.message()}")
                emit(NetworkResult.Error(errorMessage))
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Login exception: ${e.message}", e)
            emit(NetworkResult.Error("Network error: ${e.localizedMessage ?: e.message}"))
        }
    }

    /**
     * Logout current user
     */
    fun logout() {
        Log.d(TAG, "🚪 Logging out user")
        sessionManager.clearSession()
    }

    /**
     * Check if user is currently logged in
     */
    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn.value

    /**
     * Get current user information
     */
    fun getCurrentUser() = sessionManager.currentUser.value

    /**
     * Check if session is valid
     */
    fun isValidSession(): Boolean = sessionManager.isValidSession()
}