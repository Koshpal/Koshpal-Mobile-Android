package com.koshpal_android.koshpalapp.utils

import android.content.Context
import android.widget.Toast
import java.util.regex.Pattern

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun String.isValidPhoneNumber(): Boolean {
    val pattern = Pattern.compile("^[6-9]\\d{9}$")
    return pattern.matcher(this).matches()
}

fun String.extractOTP(): String? {
    val pattern = Pattern.compile("\\d{6}")
    val matcher = pattern.matcher(this)
    return if (matcher.find()) matcher.group() else null
}