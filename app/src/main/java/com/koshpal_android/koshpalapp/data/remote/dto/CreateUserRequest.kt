package com.koshpal_android.koshpalapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateUserRequest(
    @SerializedName("mobile")
    val phoneNumber: String
)

// Alternative if backend expects "phone"
// data class CreateUserRequest(
//     @SerializedName("phone")
//     val phoneNumber: String
// )