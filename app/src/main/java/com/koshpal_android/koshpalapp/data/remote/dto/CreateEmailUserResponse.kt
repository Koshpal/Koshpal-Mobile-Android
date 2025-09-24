package com.koshpal_android.koshpalapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateEmailUserResponse(
    @SerializedName("user")
    val user: ApiEmailUser,
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("message")
    val message: String? = null
)

data class ApiEmailUser(
    @SerializedName("_id")
    val id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)
