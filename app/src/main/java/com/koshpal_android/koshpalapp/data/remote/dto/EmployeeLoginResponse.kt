package com.koshpal_android.koshpalapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EmployeeLoginResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("employeeId")
    val employeeId: String? = null,
    
    @SerializedName("token")
    val token: String? = null,
    
    @SerializedName("email")
    val email: String? = null
)
