package com.koshpal_android.koshpalapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.repository.AuthRepository
import com.koshpal_android.koshpalapp.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val authRepository = AuthRepository(userRepository, userPreferences)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadUserData()
    }

    private fun loadUserData() {
        val user = authRepository.getCurrentUser()
        val welcomeMessage = if (user != null) {
            "Welcome! ${user.phoneNumber}"
        } else {
            "Welcome to KoshpalApp!"
        }

        _uiState.value = _uiState.value.copy(welcomeMessage = welcomeMessage)
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = _uiState.value.copy(isLoggedOut = true)
        }
    }
}

data class HomeUiState(
    val welcomeMessage: String = "Welcome to KoshpalApp!",
    val isLoggedOut: Boolean = false
)