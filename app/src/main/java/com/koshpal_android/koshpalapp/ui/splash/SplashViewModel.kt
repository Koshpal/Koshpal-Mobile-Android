package com.koshpal_android.koshpalapp.ui.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.repository.AuthRepository
import com.koshpal_android.koshpalapp.repository.UserRepository
import com.koshpal_android.koshpalapp.ui.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
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
            delay(1000) // 1 second delay for splash screen
            
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