package com.koshpal_android.koshpalapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateEmailUserRequest(
    @SerializedName("email")
    val email: String
)
