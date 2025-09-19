package com.koshpal_android.koshpalapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val uid: String = "",
    val phoneNumber: String = "",
    val isVerified: Boolean = false,
    val apiToken: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)