package com.koshpal_android.koshpalapp.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.repository.AuthRepository
import com.koshpal_android.koshpalapp.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val authRepository = AuthRepository(userRepository, userPreferences)

    private val _navigationEvent = MutableSharedFlow<NavigationDestination>()
    val navigationEvent: SharedFlow<NavigationDestination> = _navigationEvent

    fun startSplashTimer() {
        viewModelScope.launch {
            delay(1000) // 1 second delay for splash screen
            
            // 🧪 TESTING MODE: Check if initial SMS processing is done
            if (!userPreferences.isInitialSmsProcessed()) {
                // First time - process all SMS
                _navigationEvent.emit(NavigationDestination.SMS_PROCESSING)
            } else {
                // Already processed - go directly to HOME
                // Background service will handle new SMS automatically
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
        SMS_PROCESSING
    }
}