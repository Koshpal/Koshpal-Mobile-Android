package com.koshpal_android.koshpalapp.network

import com.koshpal_android.koshpalapp.data.remote.dto.*
import com.koshpal_android.koshpalapp.model.OnboardingResponse
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

    // Transaction Sync Endpoints
    
    /**
     * Bulk upload transactions (first-time sync)
     */
    @POST("api/transactions/bulk")
    suspend fun uploadBulkTransactions(
        @Body request: BulkTransactionRequest
    ): Response<BulkTransactionResponse>
    
    /**
     * Upload single transaction (new or updated)
     */
    @POST("api/transactions/")
    suspend fun uploadSingleTransaction(
        @Body transaction: TransactionDto
    ): Response<SingleTransactionResponse>

    @PUT("api/transactions/{transactionId}")
    suspend fun updateTransaction(
        @Path("transactionId") transactionId: String,
        @Body transaction: TransactionDto
    ): Response<TransactionSyncResponse>

    @DELETE("api/transactions/{transactionId}")
    suspend fun deleteTransaction(
        @Path("transactionId") transactionId: String
    ): Response<TransactionSyncResponse>

    @GET("api/transactions/sync-status")
    suspend fun getSyncStatus(
        @Query("employeeId") employeeId: String,
        @Query("deviceId") deviceId: String
    ): Response<Map<String, Any>>

    // Employee Login Endpoint
    @POST("api/employee/login")
    suspend fun employeeLogin(
        @Body request: EmployeeLoginRequest
    ): Response<EmployeeLoginResponse>
}