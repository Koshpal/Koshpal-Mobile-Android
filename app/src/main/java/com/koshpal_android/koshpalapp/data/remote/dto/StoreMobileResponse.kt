package com.koshpal_android.koshpalapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StoreMobileResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("phoneNumber")
    val phoneNumber: String? = null,
    @SerializedName("id")
    val id: String? = null
)
