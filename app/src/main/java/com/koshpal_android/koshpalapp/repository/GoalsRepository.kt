package com.koshpal_android.koshpalapp.repository

import android.util.Log
import com.koshpal_android.koshpalapp.auth.SessionManager
import com.koshpal_android.koshpalapp.data.remote.dto.*
import com.koshpal_android.koshpalapp.network.ApiService
import com.koshpal_android.koshpalapp.network.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalsRepository @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {

    private val TAG = "GoalsRepository"

    /**
     * Get financial goals for the current employee
     */
    fun getFinancialGoals(): Flow<NetworkResult<FinancialGoalsResponse>> = flow {
        try {
            Log.d(TAG, "🎯 Fetching financial goals")

            // Check if user is logged in
            if (!sessionManager.isValidSession()) {
                Log.e(TAG, "❌ Cannot get goals: User not logged in")
                emit(NetworkResult.Error("Authentication required. Please login first."))
                return@flow
            }

            emit(NetworkResult.Loading())

            val response = apiService.getFinancialGoals()

            if (response.isSuccessful) {
                val goalsResponse = response.body()
                if (goalsResponse != null) {
                    Log.d(TAG, "✅ Retrieved ${goalsResponse.financialGoals.size} financial goals")
                    emit(NetworkResult.Success(goalsResponse))
                } else {
                    Log.e(TAG, "❌ Goals response body is null")
                    emit(NetworkResult.Error("Failed to load goals: Empty response"))
                }
            } else {
                val error = getGoalsErrorMessage(response)
                Log.e(TAG, "❌ Goals API error: $error")

                // Handle 401 Unauthorized - clear session and redirect to login
                if (response.code() == 401) {
                    Log.w(TAG, "🚪 401 Unauthorized - clearing session and redirecting to login")
                    sessionManager.clearSession()
                    // Note: Navigation to login should be handled by the UI layer
                }

                emit(NetworkResult.Error(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Goals exception: ${e.message}", e)
            emit(NetworkResult.Error("Network error: ${e.localizedMessage ?: e.message}"))
        }
    }

    /**
     * Create a new financial goal
     */
    fun createFinancialGoal(
        goalName: String,
        icon: String,
        goalAmount: Double,
        saving: Double,
        goalDate: String
    ): Flow<NetworkResult<CreateGoalResponse>> = flow {
        try {
            Log.d(TAG, "🎯 Creating financial goal: $goalName")

            // Check if user is logged in
            if (!sessionManager.isValidSession()) {
                Log.e(TAG, "❌ Cannot create goal: User not logged in")
                emit(NetworkResult.Error("Authentication required. Please login first."))
                return@flow
            }

            // Validate inputs
            if (goalName.isBlank()) {
                emit(NetworkResult.Error("Goal name cannot be empty"))
                return@flow
            }

            if (goalAmount <= 0) {
                emit(NetworkResult.Error("Goal amount must be greater than 0"))
                return@flow
            }

            if (goalDate.isBlank()) {
                emit(NetworkResult.Error("Goal date is required"))
                return@flow
            }

            emit(NetworkResult.Loading())

            val createRequest = CreateGoalRequest(
                goalName = goalName,
                icon = icon,
                goalAmount = goalAmount,
                saving = saving,
                goalDate = goalDate
            )

            Log.d(TAG, "📤 Creating goal: $goalName, Amount: ₹$goalAmount, Goal Date: $goalDate")

            val response = apiService.createFinancialGoal(createRequest)

            if (response.isSuccessful) {
                val goalDto = response.body()
                if (goalDto != null) {
                    Log.d(TAG, "✅ Goal created successfully: ${goalDto.goalName}")
                    // Wrap in CreateGoalResponse for consistency with existing code
                    val wrappedResponse = CreateGoalResponse(
                        success = true,
                        message = "Goal created successfully",
                        data = goalDto
                    )
                    emit(NetworkResult.Success(wrappedResponse))
                } else {
                    Log.e(TAG, "❌ Goal creation failed: Empty response body")
                    emit(NetworkResult.Error("Goal creation failed: Empty response"))
                }
            } else {
                val error = getGoalsErrorMessage(response)
                Log.e(TAG, "❌ Goal creation API error: $error")

                // Handle 401 Unauthorized - clear session and redirect to login
                if (response.code() == 401) {
                    Log.w(TAG, "🚪 401 Unauthorized - clearing session and redirecting to login")
                    sessionManager.clearSession()
                    // Note: Navigation to login should be handled by the UI layer
                }

                // Log full request body on 400 Bad Request errors for debugging
                if (response.code() == 400) {
                    Log.e(TAG, "📋 400 Bad Request - Full request body: $createRequest")
                    Log.e(TAG, "📋 400 Bad Request - Response body: ${response.errorBody()?.string()}")
                }

                emit(NetworkResult.Error(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Goal creation exception: ${e.message}", e)
            emit(NetworkResult.Error("Network error: ${e.localizedMessage ?: e.message}"))
        }
    }

    /**
     * Get user-friendly error message from API response
     */
    private fun getGoalsErrorMessage(response: Response<*>): String {
        return when (response.code()) {
            400 -> "Invalid goal data. Please check your input."
            401 -> "Authentication failed. Please login again."
            403 -> "Access denied. Your account may not be active."
            409 -> "Goal with this name already exists."
            429 -> "Too many requests. Please wait and try again."
            500 -> "Server error. Please try again later."
            else -> "Failed to load goals: ${response.message()}"
        }
    }
}
