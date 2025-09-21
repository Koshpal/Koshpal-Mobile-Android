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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    // Use Hilt for ViewModel injection
    private val viewModel: LoginViewModel by viewModels()

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