package com.koshpal_android.koshpalapp.repository

import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.data.remote.dto.CreateUserRequest
import com.koshpal_android.koshpalapp.network.ApiService
import com.koshpal_android.koshpalapp.network.NetworkResult
import com.koshpal_android.koshpalapp.network.RetrofitClient

class UserRepository(
    private val apiService: ApiService = RetrofitClient.instance,
    private val userPreferences: UserPreferences
) {

    suspend fun createUser(phoneNumber: String): NetworkResult<String> {
        return try {
            val request = CreateUserRequest(phoneNumber)
            val response = apiService.createUser(request)

            if (response.isSuccessful) {
                response.body()?.let { createUserResponse ->
                    // Save user data locally
                    userPreferences.saveUserToken(createUserResponse.token)
                    userPreferences.saveUserId(createUserResponse.user.id)
                    userPreferences.savePhoneNumber(createUserResponse.user.phoneNumber)
                    userPreferences.setLoggedIn(true)

                    NetworkResult.Success(createUserResponse.token)
                } ?: NetworkResult.Error("Empty response from server")
            } else {
                when (response.code()) {
                    409 -> {
                        // User already exists, try to get existing user data
                        handleExistingUser(phoneNumber)
                    }
                    400 -> NetworkResult.Error("Invalid phone number")
                    else -> NetworkResult.Error("Server error: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    private suspend fun handleExistingUser(phoneNumber: String): NetworkResult<String> {
        // If user exists, you might want to have a login endpoint
        // For now, we'll create a simple response
        return NetworkResult.Success("User already exists, logged in successfully")
    }

    suspend fun getUserProfile(userId: String): NetworkResult<Any> {
        return try {
            val response = apiService.getUserProfile(userId)
            if (response.isSuccessful) {
                response.body()?.let { userResponse ->
                    NetworkResult.Success(userResponse)
                } ?: NetworkResult.Error("Empty response")
            } else {
                NetworkResult.Error("Failed to get user profile: ${response.code()}")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }
}