package com.koshpal_android.koshpalapp.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.koshpal_android.koshpalapp.databinding.ActivityLoginBinding
import com.koshpal_android.koshpalapp.utils.Constants
import com.koshpal_android.koshpalapp.utils.isValidPhoneNumber
import com.koshpal_android.koshpalapp.utils.showToast
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.network.RetrofitClient
import com.koshpal_android.koshpalapp.repository.UserRepository
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    // Create dependencies
    private val userPreferences by lazy {
        UserPreferences(this)
    }

    private val userRepository by lazy {
        UserRepository(
            apiService = RetrofitClient.instance,
            userPreferences = userPreferences
        )
    }

    // Use factory to create ViewModel
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(
            userRepository = userRepository,
            userPreferences = userPreferences
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnSendOTP.setOnClickListener {
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()
            if (phoneNumber.isValidPhoneNumber()) {
                viewModel.sendOTP("+91$phoneNumber", this)
            } else {
                showToast("Please enter a valid phone number")
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading)
                    android.view.View.VISIBLE else android.view.View.GONE
                binding.btnSendOTP.isEnabled = !state.isLoading

                state.error?.let { showToast(it) }

                if (state.isOTPSent) {
                    val intent = Intent(this@LoginActivity, OTPVerificationActivity::class.java)
                    intent.putExtra(Constants.PHONE_NUMBER, "+91${binding.etPhoneNumber.text}")
                    intent.putExtra(Constants.VERIFICATION_ID, state.verificationId)
                    startActivity(intent)
                }
            }
        }
    }
}