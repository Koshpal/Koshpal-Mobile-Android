package com.koshpal_android.koshpalapp.data.remote.dto

import com.google.gson.annotations.SerializedName

// ============ FINANCIAL GOALS DTOs ============

data class FinancialGoalsResponse(
    val employeeId: String,
    val financialGoals: List<FinancialGoalDto>
)

data class FinancialGoalDto(
    @SerializedName("_id")
    val id: String,
    val goalName: String,
    val icon: String,
    val goalAmount: Double,
    val saving: Double,
    val goalDate: String // ISO date format
)

data class CreateGoalRequest(
    val goalName: String,
    val icon: String,
    val goalAmount: Double,
    val saving: Double,
    val goalDate: String // Format: "2026-12-31"
)

data class CreateGoalResponse(
    val success: Boolean,
    val message: String,
    val data: FinancialGoalDto?
)
