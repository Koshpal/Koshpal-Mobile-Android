package com.koshpal_android.koshpalapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateUserResponse(
    @SerializedName("user")
    val user: ApiUser,
    @SerializedName("token")
    val token: String,
    @SerializedName("message")
    val message: String? = null
)

data class ApiUser(
    @SerializedName("_id")
    val id: String,
    @SerializedName("phoneNumber")
    val phoneNumber: String,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)