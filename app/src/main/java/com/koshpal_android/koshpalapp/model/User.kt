package com.koshpal_android.koshpalapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val uid: String = "",
    @ColumnInfo(name = "phoneNumber")
    val phoneNumber: String = "",
    @ColumnInfo(name = "isVerified")
    val isVerified: Boolean = false,
    @ColumnInfo(name = "apiToken")
    val apiToken: String? = null,
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
)