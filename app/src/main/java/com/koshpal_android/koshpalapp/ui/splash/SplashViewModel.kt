package com.koshpal_android.koshpalapp.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.repository.AuthRepository
import com.koshpal_android.koshpalapp.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val authRepository = AuthRepository(userRepository, userPreferences)

    private val _navigationEvent = MutableSharedFlow<NavigationDestination>()
    val navigationEvent: SharedFlow<NavigationDestination> = _navigationEvent

    fun startSplashTimer() {
        viewModelScope.launch {
            delay(2000) // 2 seconds delay

            // Check if user is logged in and verified
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null && currentUser.isVerified) {
                _navigationEvent.emit(NavigationDestination.HOME)
            } else {
                // Navigate to CheckActivity for new users
                _navigationEvent.emit(NavigationDestination.CHECK)
            }
        }
    }

    enum class NavigationDestination {
        CHECK,
        LOGIN,
        HOME
    }
}