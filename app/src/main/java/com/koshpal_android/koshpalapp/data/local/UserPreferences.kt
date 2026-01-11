package com.koshpal_android.koshpalapp.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(@ApplicationContext context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "user_prefs", Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_USER_TOKEN = "user_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_PHONE_NUMBER = "phone_number"
        private const val KEY_EMAIL = "email"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LOGIN_TYPE = "login_type" // "phone" or "email"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_INITIAL_SMS_PROCESSED = "initial_sms_processed"
        private const val KEY_INITIAL_SYNC_COMPLETED = "initial_sync_completed"
        
        // Alert preferences
        private const val KEY_ALERTS_ENABLED = "alerts_enabled"
        private const val KEY_THRESHOLD_50_ENABLED = "threshold_50_enabled"
        private const val KEY_THRESHOLD_80_ENABLED = "threshold_80_enabled"
        private const val KEY_THRESHOLD_100_ENABLED = "threshold_100_enabled"
        private const val KEY_DAILY_SUMMARY_ENABLED = "daily_summary_enabled"
        private const val KEY_WEEKLY_SUMMARY_ENABLED = "weekly_summary_enabled"
        
        // Transaction sync preferences
        private const val KEY_TOTAL_SYNCED_COUNT = "total_synced_count"
        private const val KEY_LAST_SYNC_ERROR = "last_sync_error"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"

        // Fresh install detection
        private const val KEY_STORED_VERSION_CODE = "stored_version_code"
    }

    fun saveUserToken(token: String) {
        sharedPreferences.edit().putString(KEY_USER_TOKEN, token).apply()
    }

    fun getUserToken(): String? {
        return sharedPreferences.getString(KEY_USER_TOKEN, null)
    }

    fun saveUserId(userId: String) {
        sharedPreferences.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    fun savePhoneNumber(phoneNumber: String) {
        sharedPreferences.edit().putString(KEY_PHONE_NUMBER, phoneNumber).apply()
    }

    fun getPhoneNumber(): String? {
        return sharedPreferences.getString(KEY_PHONE_NUMBER, null)
    }

    fun saveEmail(email: String) {
        sharedPreferences.edit().putString(KEY_EMAIL, email).apply()
    }

    fun getEmail(): String? {
        return sharedPreferences.getString(KEY_EMAIL, null)
    }

    fun saveLoginType(loginType: String) {
        sharedPreferences.edit().putString(KEY_LOGIN_TYPE, loginType).apply()
    }

    fun getLoginType(): String? {
        return sharedPreferences.getString(KEY_LOGIN_TYPE, null)
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun clearUserData() {
        sharedPreferences.edit().clear().apply()
    }
    
    // Alert preferences methods
    fun getAlertsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_ALERTS_ENABLED, true)
    }
    
    fun setAlertsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_ALERTS_ENABLED, enabled).apply()
    }
    
    fun getThreshold50Enabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_THRESHOLD_50_ENABLED, true)
    }
    
    fun setThreshold50Enabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_THRESHOLD_50_ENABLED, enabled).apply()
    }
    
    fun getThreshold80Enabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_THRESHOLD_80_ENABLED, true)
    }
    
    fun setThreshold80Enabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_THRESHOLD_80_ENABLED, enabled).apply()
    }
    
    fun getThreshold100Enabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_THRESHOLD_100_ENABLED, true)
    }
    
    fun setThreshold100Enabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_THRESHOLD_100_ENABLED, enabled).apply()
    }
    
    fun getDailySummaryEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_DAILY_SUMMARY_ENABLED, true)
    }
    
    fun setDailySummaryEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_DAILY_SUMMARY_ENABLED, enabled).apply()
    }
    
    fun getWeeklySummaryEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_WEEKLY_SUMMARY_ENABLED, false)
    }
    
    fun setWeeklySummaryEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_WEEKLY_SUMMARY_ENABLED, enabled).apply()
    }
    
    // SMS Processing preference
    fun isInitialSmsProcessed(): Boolean {
        return sharedPreferences.getBoolean(KEY_INITIAL_SMS_PROCESSED, false)
    }
    
    fun setInitialSmsProcessed(processed: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_INITIAL_SMS_PROCESSED, processed).apply()
    }
    
    // Sync preferences
    fun isInitialSyncCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_INITIAL_SYNC_COMPLETED, false)
    }
    
    fun setInitialSyncCompleted(completed: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_INITIAL_SYNC_COMPLETED, completed).apply()
    }
    
    // Transaction sync count methods
    fun getTotalSyncedCount(): Long {
        return sharedPreferences.getLong(KEY_TOTAL_SYNCED_COUNT, 0L)
    }
    
    fun setTotalSyncedCount(count: Long) {
        sharedPreferences.edit().putLong(KEY_TOTAL_SYNCED_COUNT, count).apply()
    }
    
    fun incrementSyncedCount(count: Int = 1) {
        val currentCount = getTotalSyncedCount()
        setTotalSyncedCount(currentCount + count)
    }
    
    fun getLastSyncError(): String? {
        return sharedPreferences.getString(KEY_LAST_SYNC_ERROR, null)
    }
    
    fun setLastSyncError(error: String?) {
        sharedPreferences.edit().putString(KEY_LAST_SYNC_ERROR, error).apply()
    }
    
    fun getLastSyncTime(): Long {
        return sharedPreferences.getLong(KEY_LAST_SYNC_TIME, 0L)
    }
    
    fun setLastSyncTime(time: Long) {
        sharedPreferences.edit().putLong(KEY_LAST_SYNC_TIME, time).apply()
    }

    // Fresh install detection methods
    fun getStoredVersionCode(): Long {
        return sharedPreferences.getLong(KEY_STORED_VERSION_CODE, 0L)
    }

    fun setStoredVersionCode(versionCode: Long) {
        sharedPreferences.edit().putLong(KEY_STORED_VERSION_CODE, versionCode).apply()
    }

    fun resetForFreshInstall() {
        Log.d("UserPreferences", "🔄 Resetting preferences for fresh install")
        // Reset SMS and sync flags, but keep login state
        setInitialSmsProcessed(false)
        setInitialSyncCompleted(false)
        setTotalSyncedCount(0L)
        setLastSyncError(null)
        setLastSyncTime(0L)
        // Don't clear login state, email, etc. as they should persist
    }
}