package com.koshpal_android.koshpalapp.repository

import android.util.Log
import com.koshpal_android.koshpalapp.model.OnboardingResponse
import com.koshpal_android.koshpalapp.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun submitOnboardingData(onboardingResponse: OnboardingResponse): Result<String> {
        return try {
            Log.d("OnboardingRepository", "Submitting onboarding data for email: ${onboardingResponse.email}")
            val response = apiService.submitOnboardingData(onboardingResponse)

            Log.d("OnboardingRepository", "Onboarding API Response Code: ${response.code()}")
            Log.d("OnboardingRepository", "Onboarding API Response Success: ${response.isSuccessful}")

            if (response.isSuccessful) {
                response.body()?.let { onboardingApiResponse ->
                    Log.d("OnboardingRepository", "Onboarding data submitted successfully")
                    Result.success(onboardingApiResponse.message ?: "Onboarding completed successfully")
                } ?: run {
                    Log.e("OnboardingRepository", "Empty response body from server")
                    Result.failure(Exception("Empty response from server"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("OnboardingRepository", "Onboarding API Error: ${response.code()}, Body: $errorBody")
                
                when (response.code()) {
                    400 -> Result.failure(Exception("Invalid onboarding data"))
                    404 -> Result.failure(Exception("User not found"))
                    else -> Result.failure(Exception("Server error: ${response.code()} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            Log.e("OnboardingRepository", "Network error submitting onboarding data", e)
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }
}
