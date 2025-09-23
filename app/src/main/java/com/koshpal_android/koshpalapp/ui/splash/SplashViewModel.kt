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
            delay(1000) // 1 second delay for faster testing
            
            // Bypass authentication - directly navigate to HOME
            _navigationEvent.emit(NavigationDestination.HOME)
            
            // Original authentication logic commented out for testing:
            /*
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null && currentUser.isVerified) {
                _navigationEvent.emit(NavigationDestination.HOME)
            } else {
                _navigationEvent.emit(NavigationDestination.CHECK)
            }
            */
        }
    }

    enum class NavigationDestination {
        CHECK,
        LOGIN,
        HOME
    }
}