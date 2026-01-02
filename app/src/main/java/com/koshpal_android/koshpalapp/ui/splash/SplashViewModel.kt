package com.koshpal_android.koshpalapp.ui.splash

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.repository.AuthRepository
import com.koshpal_android.koshpalapp.repository.UserRepository
import com.koshpal_android.koshpalapp.ui.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences,
    private val syncManager: SyncManager
) : ViewModel() {

    private val authRepository = AuthRepository(userRepository, userPreferences)

    private val _navigationEvent = MutableSharedFlow<NavigationDestination>()
    val navigationEvent: SharedFlow<NavigationDestination> = _navigationEvent

    fun startSplashTimer() {
        viewModelScope.launch {
            Log.d("SplashViewModel", "🚀 Starting splash timer")

            // ============================================
            // ADDED LOTTIE ANIMATION: Increased delay to allow animation to complete
            // Animation duration is approximately 2.6 seconds
            // 2.5s delay ensures at least one full cycle of the Lottie animation
            // ============================================
            delay(2500) // 2.5 seconds delay for splash screen

            // 🔄 FRESH INSTALL DETECTION: Reset preferences if this is a fresh install
            // Check if app was freshly installed by looking for a version-specific flag
            val currentVersionCode = getCurrentVersionCode()
            val storedVersionCode = userPreferences.getStoredVersionCode()

            if (storedVersionCode == 0L || storedVersionCode != currentVersionCode) {
                Log.d("SplashViewModel", "🔄 Fresh install detected - resetting preferences")
                // This is a fresh install or app update, reset SMS processing flag
                userPreferences.resetForFreshInstall()
                userPreferences.setStoredVersionCode(currentVersionCode)
            }

            // 🔐 AUTO-LOGIN: Always use static employee ID (no login required)
            val staticEmployeeId = "68ee28ce2f3fd392ea436576"
            if (!userPreferences.isLoggedIn()) {
                Log.d("SplashViewModel", "🔐 Auto-logging in with static employee ID: $staticEmployeeId")
                userPreferences.setLoggedIn(true)
                userPreferences.saveUserId(staticEmployeeId)
                userPreferences.saveEmail("koshpal.user@app.com") // Default email
            }

            val isSmsProcessed = userPreferences.isInitialSmsProcessed()
            val isLoggedIn = userPreferences.isLoggedIn()
            val isSyncCompleted = userPreferences.isInitialSyncCompleted()

            Log.d("SplashViewModel", "📊 User state - SMS Processed: $isSmsProcessed, Logged In: $isLoggedIn, Sync Completed: $isSyncCompleted")

            // 🧪 SIMPLIFIED FLOW: Auto-login enabled, just check SMS processing
            if (!isSmsProcessed) {
                Log.d("SplashViewModel", "📱 SMS not processed - navigating to SMS_PROCESSING")
                // First time - process all SMS
                _navigationEvent.emit(NavigationDestination.SMS_PROCESSING)
            } else {
                Log.d("SplashViewModel", "🏠 SMS processed and auto-logged in - navigating to HOME")
                // User is auto-logged in - go to home
                _navigationEvent.emit(NavigationDestination.HOME)
            }
            
            /* PRODUCTION FLOW (Uncomment for production):
            // Check if user is already logged in
            if (userPreferences.isLoggedIn()) {
                // Check if onboarding is completed
                if (userPreferences.isOnboardingCompleted()) {
                    // Check if initial SMS processing is done
                    if (!userPreferences.isInitialSmsProcessed()) {
                        // First time after onboarding - process SMS
                        _navigationEvent.emit(NavigationDestination.SMS_PROCESSING)
                    } else {
                        // User is logged in, onboarded, and SMS processed - go to HOME
                        _navigationEvent.emit(NavigationDestination.HOME)
                    }
                } else {
                    // User is logged in but onboarding not completed, go to ONBOARDING
                    val email = userPreferences.getEmail() ?: ""
                    if (email.isNotEmpty()) {
                        _navigationEvent.emit(NavigationDestination.ONBOARDING)
                    } else {
                        // No email found, go to employee login
                        _navigationEvent.emit(NavigationDestination.EMPLOYEE_LOGIN)
                    }
                }
            } else {
                // User not logged in, go to Employee Login
                _navigationEvent.emit(NavigationDestination.EMPLOYEE_LOGIN)
            }
            */
        }
    }

    private fun getCurrentVersionCode(): Long {
        return try {
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            Log.e("SplashViewModel", "❌ Error getting version code: ${e.message}", e)
            1L // Default fallback
        }
    }

    enum class NavigationDestination {
        CHECK,
        LOGIN,
        HOME,
        EMPLOYEE_LOGIN,
        ONBOARDING,
        SMS_PROCESSING,
        SYNC
    }
}