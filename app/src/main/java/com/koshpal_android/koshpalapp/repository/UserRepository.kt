package com.koshpal_android.koshpalapp.repository

import android.util.Log
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.data.remote.dto.CreateUserRequest
import com.koshpal_android.koshpalapp.data.remote.dto.CreateEmailUserRequest
import com.koshpal_android.koshpalapp.data.remote.dto.CreateEmailUserResponse
import com.koshpal_android.koshpalapp.network.ApiService
import com.koshpal_android.koshpalapp.network.NetworkResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) {

    suspend fun createUser(phoneNumber: String): NetworkResult<String> {
        return try {
            Log.d("UserRepository", "Creating user with phone: $phoneNumber")
            val request = CreateUserRequest(phoneNumber)
            val response = apiService.createUser(request)

            Log.d("UserRepository", "API Response Code: ${response.code()}")
            Log.d("UserRepository", "API Response Success: ${response.isSuccessful}")

            if (response.isSuccessful) {
                response.body()?.let { createUserResponse ->
                    Log.d("UserRepository", "User created successfully: ${createUserResponse.user.id}")
                    // Save user data locally
                    userPreferences.saveUserToken(createUserResponse.token)
                    userPreferences.saveUserId(createUserResponse.user.id)
                    userPreferences.savePhoneNumber(createUserResponse.user.phoneNumber)
                    userPreferences.setLoggedIn(true)

                    NetworkResult.Success(createUserResponse.token)
                } ?: run {
                    Log.e("UserRepository", "Empty response body from server")
                    NetworkResult.Error("Empty response from server")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("UserRepository", "API Error: ${response.code()}, Body: $errorBody")
                
                when (response.code()) {
                    409 -> {
                        Log.d("UserRepository", "User already exists, handling existing user")
                        // User already exists, try to get existing user data
                        handleExistingUser(phoneNumber)
                    }
                    400 -> NetworkResult.Error("Invalid phone number")
                    else -> NetworkResult.Error("Server error: ${response.code()} - $errorBody")
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Network error creating user", e)
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    private suspend fun handleExistingUser(phoneNumber: String): NetworkResult<String> {
        // If user exists, you might want to have a login endpoint
        // For now, we'll create a simple response
        return try {
            Log.d("UserRepository", "Handling existing user with phone: $phoneNumber")
            NetworkResult.Success("User already exists, logged in successfully")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error handling existing user", e)
            NetworkResult.Error("Error handling existing user: ${e.localizedMessage}")
        }
    }

    suspend fun createEmailUser(email: String): Result<CreateEmailUserResponse> {
        return try {
            Log.d("UserRepository", "Creating email user with email: $email")
            val request = CreateEmailUserRequest(email)
            val response = apiService.createEmailUser(request)

            Log.d("UserRepository", "Email API Response Code: ${response.code()}")
            Log.d("UserRepository", "Email API Response Success: ${response.isSuccessful}")

            if (response.isSuccessful) {
                response.body()?.let { createEmailUserResponse ->
                    Log.d("UserRepository", "Email user created successfully: ${createEmailUserResponse.user.id}")
                    Result.success(createEmailUserResponse)
                } ?: run {
                    Log.e("UserRepository", "Empty response body from server")
                    Result.failure(Exception("Empty response from server"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("UserRepository", "Email API Error: ${response.code()}, Body: $errorBody")
                
                when (response.code()) {
                    409 -> Result.failure(Exception("User with this email already exists"))
                    400 -> Result.failure(Exception("Invalid email address"))
                    else -> Result.failure(Exception("Server error: ${response.code()} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Network error creating email user", e)
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }

    suspend fun getUserProfile(userId: String): NetworkResult<Any> {
        return try {
            Log.d("UserRepository", "Getting user profile for user: $userId")
            val response = apiService.getUserProfile(userId)
            Log.d("UserRepository", "API Response Code: ${response.code()}")
            Log.d("UserRepository", "API Response Success: ${response.isSuccessful}")

            if (response.isSuccessful) {
                response.body()?.let { userResponse ->
                    Log.d("UserRepository", "User profile retrieved successfully")
                    NetworkResult.Success(userResponse)
                } ?: run {
                    Log.e("UserRepository", "Empty response body from server")
                    NetworkResult.Error("Empty response")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("UserRepository", "API Error: ${response.code()}, Body: $errorBody")
                NetworkResult.Error("Failed to get user profile: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Network error getting user profile", e)
            NetworkResult.Error("Network error: ${e.localizedMessage}")
        }
    }
}