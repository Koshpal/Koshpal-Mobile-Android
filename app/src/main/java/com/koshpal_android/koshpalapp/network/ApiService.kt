package com.koshpal_android.koshpalapp.network

import com.koshpal_android.koshpalapp.data.remote.dto.CreateUserRequest
import com.koshpal_android.koshpalapp.data.remote.dto.CreateUserResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("create-user")
    suspend fun createUser(
        @Body request: CreateUserRequest
    ): Response<CreateUserResponse>

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