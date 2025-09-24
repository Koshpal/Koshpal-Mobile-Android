package com.koshpal_android.koshpalapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OnboardingApiResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean = true,
    @SerializedName("onboardingId")
    val onboardingId: String? = null
)
