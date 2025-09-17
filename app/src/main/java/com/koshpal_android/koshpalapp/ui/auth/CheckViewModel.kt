package com.koshpal_android.koshpalapp.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.data.remote.dto.CreateUserRequest
import com.koshpal_android.koshpalapp.network.ApiService
import com.koshpal_android.koshpalapp.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CheckViewModel : ViewModel() {
    private val apiService: ApiService = RetrofitClient.instance

    private val _uiState = MutableStateFlow(CheckUiState())
    val uiState: StateFlow<CheckUiState> = _uiState

    fun storeMobileNumber(phoneNumber: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                Log.d("CheckViewModel", "=== API REQUEST DEBUG ===")
                Log.d("CheckViewModel", "URL: http://10.147.39.107:5000/create-user")
                Log.d("CheckViewModel", "Method: POST")
                Log.d("CheckViewModel", "Input phone number: $phoneNumber")
                
                val request = CreateUserRequest(phoneNumber)
                Log.d("CheckViewModel", "Request object: $request")
                Log.d("CheckViewModel", "JSON will be: {\"phoneNumber\":\"$phoneNumber\"}")
                
                val response = apiService.createUser(request)

                Log.d("CheckViewModel", "=== API RESPONSE DEBUG ===")
                Log.d("CheckViewModel", "Response Code: ${response.code()}")
                Log.d("CheckViewModel", "Response Success: ${response.isSuccessful}")
                Log.d("CheckViewModel", "Response Headers: ${response.headers()}")
                
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    Log.e("CheckViewModel", "Error Response Body: $errorBody")
                }

                if (response.isSuccessful) {
                    response.body()?.let { createUserResponse ->
                        Log.d("CheckViewModel", "Mobile number stored successfully: ${createUserResponse.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = true,
                            message = "Mobile number registered successfully!"
                        )
                    } ?: run {
                        Log.e("CheckViewModel", "Empty response body from server")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Empty response from server"
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("CheckViewModel", "API Error: ${response.code()}, Body: $errorBody")
                    
                    val errorMessage = when (response.code()) {
                        400 -> "Invalid phone number format"
                        409 -> "Phone number already registered"
                        500 -> "Server error. Please try again."
                        else -> "Failed to register mobile number: ${response.code()}"
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
            } catch (e: Exception) {
                Log.e("CheckViewModel", "Network error storing mobile number", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Network error: ${e.localizedMessage}"
                )
            }
        }
    }

    fun clearState() {
        _uiState.value = CheckUiState()
    }
}

data class CheckUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
