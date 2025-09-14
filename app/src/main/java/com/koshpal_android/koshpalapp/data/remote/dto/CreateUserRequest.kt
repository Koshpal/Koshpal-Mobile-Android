package com.koshpal_android.koshpalapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateUserRequest(
    @SerializedName("phoneNumber")
    val phoneNumber: String
)