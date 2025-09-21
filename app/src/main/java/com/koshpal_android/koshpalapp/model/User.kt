package com.koshpal_android.koshpalapp.model

data class User(
    val uid: String = "",
    val phoneNumber: String = "",
    val isVerified: Boolean = false,
    val apiToken: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)