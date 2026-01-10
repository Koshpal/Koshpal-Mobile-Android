package com.koshpal_android.koshpalapp.auth

import android.content.Context
import android.content.SharedPreferences
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.data.remote.dto.UserDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
) {

    // TODO: Replace with EncryptedSharedPreferences for production security
    // Requires adding androidx.security:security-crypto dependency
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        // Restore session on initialization
        restoreSession()
    }

    companion object {
        private const val PREFS_NAME = "koshpal_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_NAME = "name"
        private const val KEY_ROLE = "role"
        private const val KEY_COMPANY_ID = "company_id"
        private const val KEY_IS_ACTIVE = "is_active"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }

    /**
     * Save user session after successful login
     */
    fun saveSession(user: UserDto, accessToken: String? = null, refreshToken: String? = null) {
        prefs.edit().apply {
            putString(KEY_USER_ID, user.id)
            putString(KEY_EMAIL, user.email)
            putString(KEY_NAME, user.name)
            putString(KEY_ROLE, user.role)
            putString(KEY_COMPANY_ID, user.companyId)
            putBoolean(KEY_IS_ACTIVE, user.isActive)
            putBoolean(KEY_IS_LOGGED_IN, true)
            accessToken?.let { putString(KEY_ACCESS_TOKEN, it) }
            refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
            apply()
        }

        _currentUser.value = user
        _isLoggedIn.value = true

        // Synchronize with UserPreferences
        userPreferences.setLoggedIn(true)
        userPreferences.saveUserId(user.id)
        userPreferences.saveEmail(user.email)
        userPreferences.saveUserToken(accessToken ?: "")
    }

    /**
     * Restore session from SharedPreferences
     */
    private fun restoreSession() {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val isActive = prefs.getBoolean(KEY_IS_ACTIVE, false)

        android.util.Log.d("SessionManager", "🔄 Restoring session: loggedIn=$isLoggedIn, hasToken=${accessToken != null}, isActive=$isActive")

        if (isLoggedIn) {
            val user = UserDto(
                id = prefs.getString(KEY_USER_ID, "") ?: "",
                email = prefs.getString(KEY_EMAIL, "") ?: "",
                name = prefs.getString(KEY_NAME, "") ?: "",
                role = prefs.getString(KEY_ROLE, "") ?: "",
                companyId = prefs.getString(KEY_COMPANY_ID, "") ?: "",
                isActive = prefs.getBoolean(KEY_IS_ACTIVE, false)
            )

            _currentUser.value = user
            _isLoggedIn.value = true

            // Synchronize with UserPreferences
            userPreferences.setLoggedIn(true)
            userPreferences.saveUserId(user.id)
            userPreferences.saveEmail(user.email)
            userPreferences.saveUserToken(accessToken ?: "")

            android.util.Log.d("SessionManager", "✅ Session restored: user=${user.email}, active=${user.isActive}, token=${accessToken?.take(10)}...")
        } else {
            android.util.Log.d("SessionManager", "❌ No saved session found")
        }
    }

    /**
     * Clear session on logout
     */
    fun clearSession() {
        prefs.edit().apply {
            remove(KEY_USER_ID)
            remove(KEY_EMAIL)
            remove(KEY_NAME)
            remove(KEY_ROLE)
            remove(KEY_COMPANY_ID)
            remove(KEY_IS_ACTIVE)
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }

        _currentUser.value = null
        _isLoggedIn.value = false

        // Synchronize with UserPreferences
        userPreferences.setLoggedIn(false)
        userPreferences.saveUserId("")
        userPreferences.saveEmail("")
        userPreferences.saveUserToken("")
        userPreferences.setInitialSyncCompleted(false)
    }

    /**
     * Get current user ID
     */
    fun getUserId(): String? = _currentUser.value?.id

    /**
     * Get current company ID
     */
    fun getCompanyId(): String? = _currentUser.value?.companyId

    /**
     * Get current user email
     */
    fun getUserEmail(): String? = _currentUser.value?.email

    /**
     * Get current user name
     */
    fun getUserName(): String? = _currentUser.value?.name

    /**
     * Get current user role
     */
    fun getUserRole(): String? = _currentUser.value?.role

    /**
     * Check if user is active
     */
    fun isUserActive(): Boolean = _currentUser.value?.isActive == true

    /**
     * Check if session is valid (user logged in with valid token)
     */
    fun isValidSession(): Boolean {
        val isLoggedIn = _isLoggedIn.value
        val hasToken = getAccessToken() != null
        val isUserActive = _currentUser.value?.isActive == true

        val result = isLoggedIn && hasToken && isUserActive

        android.util.Log.d("SessionManager", "🔐 Session validation: loggedIn=$isLoggedIn, hasToken=$hasToken, isActive=$isUserActive, valid=$result")

        return result
    }

    /**
     * Get access token for API authentication
     */
    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    /**
     * Get refresh token for token renewal
     */
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    /**
     * Save tokens after login
     */
    fun saveTokens(accessToken: String, refreshToken: String? = null) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
            apply()
        }
    }

    /**
     * Check if user has valid access token
     */
    fun hasValidToken(): Boolean = getAccessToken() != null
}
