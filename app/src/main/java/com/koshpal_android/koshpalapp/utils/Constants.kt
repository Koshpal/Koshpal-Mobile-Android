package com.koshpal_android.koshpalapp.utils

object Constants {
    const val PHONE_NUMBER = "phone_number"
    const val VERIFICATION_ID = "verification_id"
    const val SMS_PERMISSION_REQUEST = 100
    const val RESEND_TOKEN = "resend_token"
    const val OTP_TIMEOUT = 60L

    // API Constants
    const val API_BASE_URL = "http://10.195.166.107:5000/" // Updated to use localhost for email endpoint
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}