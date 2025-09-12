package com.koshpal_android.koshpalapp.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {
    private val authRepository = AuthRepository()

    private val _navigationEvent = MutableSharedFlow<NavigationDestination>()
    val navigationEvent: SharedFlow<NavigationDestination> = _navigationEvent

    fun startSplashTimer() {
        viewModelScope.launch {
            delay(2000) // 2 seconds delay

            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null && currentUser.isVerified) {
                _navigationEvent.emit(NavigationDestination.HOME)
            } else {
                _navigationEvent.emit(NavigationDestination.LOGIN)
            }
        }
    }

    enum class NavigationDestination {
        LOGIN, HOME
    }
}