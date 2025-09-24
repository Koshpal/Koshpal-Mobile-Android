package com.koshpal_android.koshpalapp.network

import com.koshpal_android.koshpalapp.data.remote.dto.CreateUserRequest
import com.koshpal_android.koshpalapp.data.remote.dto.CreateUserResponse
import com.koshpal_android.koshpalapp.data.remote.dto.CreateEmailUserRequest
import com.koshpal_android.koshpalapp.data.remote.dto.CreateEmailUserResponse
import com.koshpal_android.koshpalapp.model.OnboardingResponse
import com.koshpal_android.koshpalapp.data.remote.dto.OnboardingApiResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("create-user")
    suspend fun createUser(
        @Body request: CreateUserRequest
    ): Response<CreateUserResponse>

    @POST("create-email-user")
    suspend fun createEmailUser(
        @Body request: CreateEmailUserRequest
    ): Response<CreateEmailUserResponse>

    @POST("submit-onboarding")
    suspend fun submitOnboardingData(
        @Body request: OnboardingResponse
    ): Response<OnboardingApiResponse>

    @GET("users/{userId}")
    suspend fun getUserProfile(
        @Path("userId") userId: String
    ): Response<CreateUserResponse>

    @PUT("users/{userId}")
    suspend fun updateUserProfile(
        @Path("userId") userId: String,
        @Body request: Map<String, Any>
    ): Response<CreateUserResponse>
}