package com.koshpal_android.koshpalapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StoreMobileRequest(
    @SerializedName("phoneNumber")
    val phoneNumber: String
)
