package com.koshpal_android.koshpalapp.network

import com.koshpal_android.koshpalapp.data.remote.dto.*
import com.koshpal_android.koshpalapp.model.OnboardingResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ============ AUTHENTICATION ============

    /**
     * Employee login with email and password
     */
    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    // ============ TRANSACTION SYNC ============

    /**
     * Sync single transaction (incremental sync)
     */
    @POST("api/v1/transactions")
    suspend fun syncSingleTransaction(
        @Body request: SyncTransactionRequest
    ): Response<SyncTransactionResponse>

    /**
     * Bulk sync multiple transactions
     */
    @POST("api/v1/transactions/bulk")
    suspend fun syncBulkTransactions(
        @Body request: BulkSyncRequest
    ): Response<BulkSyncApiResponse>

    // ============ FINANCIAL GOALS ============

    /**
     * Get employee's financial goals
     */
    @GET("api/v1/employee/goals")
    suspend fun getFinancialGoals(): Response<FinancialGoalsResponse>

    /**
     * Create new financial goal
     */
    @POST("api/v1/employee/goals")
    suspend fun createFinancialGoal(
        @Body request: CreateGoalRequest
    ): Response<FinancialGoalDto>

    // ============ LEGACY ENDPOINTS (for backward compatibility) ============

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

    // Old Transaction Sync Endpoints

    /**
     * Bulk upload transactions (first-time sync)
     */
    @POST("api/v1/transactions/bulk")
    suspend fun uploadBulkTransactions(
        @Body request: BulkTransactionRequest
    ): Response<BulkTransactionResponse>

    /**
     * Upload single transaction (new or updated)
     */
    @POST("api/v1/transactions/")
    suspend fun uploadSingleTransaction(
        @Body transaction: TransactionDto
    ): Response<SingleTransactionResponse>

    @PUT("api/v1/transactions/{transactionId}")
    suspend fun updateTransaction(
        @Path("transactionId") transactionId: String,
        @Body transaction: TransactionDto
    ): Response<TransactionSyncResponse>

    @DELETE("api/v1/transactions/{transactionId}")
    suspend fun deleteTransaction(
        @Path("transactionId") transactionId: String
    ): Response<TransactionSyncResponse>

    @GET("api/v1/transactions/sync-status")
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