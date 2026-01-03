package com.koshpal_android.koshpalapp.data.remote.dto

import com.google.gson.annotations.SerializedName

// ============ AUTHENTICATION DTOs ============

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val user: UserDto
)

data class UserDto(
    val id: String,
    val email: String,
    val role: String,
    val companyId: String,
    val name: String,
    @SerializedName("isActive")
    val isActive: Boolean
)
