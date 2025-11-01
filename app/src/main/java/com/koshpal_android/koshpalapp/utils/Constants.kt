package com.koshpal_android.koshpalapp.utils

object Constants {
    const val PHONE_NUMBER = "phone_number"
    const val VERIFICATION_ID = "verification_id"
    const val SMS_PERMISSION_REQUEST = 100
    const val RESEND_TOKEN = "resend_token"
    const val OTP_TIMEOUT = 60L

    // API Constants
    const val API_BASE_URL = "https://koshpal-server-wc9o.onrender.com/" // Updated to new Koshpal server
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
    
    // Static Employee ID - No login required
    const val STATIC_EMPLOYEE_ID = "68ee28ce2f3fd392ea436576"
    
    // Sync Constants
    const val SYNC_BATCH_SIZE = 50
    const val SYNC_RETRY_COUNT = 3
    const val SYNC_RETRY_DELAY_MS = 5000L
}