package com.koshpal_android.koshpalapp.data.local

import android.content.Context
import android.content.SharedPreferences
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
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        
        // Alert preferences
        private const val KEY_ALERTS_ENABLED = "alerts_enabled"
        private const val KEY_THRESHOLD_50_ENABLED = "threshold_50_enabled"
        private const val KEY_THRESHOLD_80_ENABLED = "threshold_80_enabled"
        private const val KEY_THRESHOLD_100_ENABLED = "threshold_100_enabled"
        private const val KEY_DAILY_SUMMARY_ENABLED = "daily_summary_enabled"
        private const val KEY_WEEKLY_SUMMARY_ENABLED = "weekly_summary_enabled"
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

    fun setLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
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
}