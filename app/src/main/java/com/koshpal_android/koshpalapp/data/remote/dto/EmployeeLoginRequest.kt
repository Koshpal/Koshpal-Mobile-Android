package com.koshpal_android.koshpalapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EmployeeLoginRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String
)
