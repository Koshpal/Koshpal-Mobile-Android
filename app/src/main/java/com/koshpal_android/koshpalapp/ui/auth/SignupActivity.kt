package com.koshpal_android.koshpalapp.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.koshpal_android.koshpalapp.databinding.ActivitySignupBinding
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import com.koshpal_android.koshpalapp.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private val viewModel: SignupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Sign up button click listener
        binding.btnSignup.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val name = binding.etName.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            // Validate inputs
            when {
                name.isEmpty() -> {
                    showToast("Please enter your name")
                    binding.etName.requestFocus()
                }
                email.isEmpty() -> {
                    showToast("Please enter your email")
                    binding.etEmail.requestFocus()
                }
                password.isEmpty() -> {
                    showToast("Please enter a password")
                    binding.etPassword.requestFocus()
                }
                confirmPassword.isEmpty() -> {
                    showToast("Please confirm your password")
                    binding.etConfirmPassword.requestFocus()
                }
                password != confirmPassword -> {
                    showToast("Passwords do not match")
                    binding.etConfirmPassword.requestFocus()
                }
                password.length < 6 -> {
                    showToast("Password must be at least 6 characters")
                    binding.etPassword.requestFocus()
                }
                else -> {
                    viewModel.signup(email, name, password)
                }
            }
        }

        // Login link click listener
        binding.tvLoginLink.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Update UI based on loading state
                binding.progressBar.visibility = if (state.isLoading)
                    android.view.View.VISIBLE else android.view.View.GONE
                binding.btnSignup.isEnabled = !state.isLoading

                // Handle success state
                if (state.isSuccess) {
                    showToast(state.message ?: "Signup successful!")
                    navigateToHome()
                }

                // Handle error state
                state.error?.let { error ->
                    showToast(error)
                    binding.tvMessage.apply {
                        text = error
                        setTextColor(getColor(android.R.color.holo_red_dark))
                        visibility = android.view.View.VISIBLE
                    }
                }

                // Show success message
                state.message?.let { message ->
                    if (state.isSuccess) {
                        binding.tvMessage.apply {
                            text = message
                            setTextColor(getColor(android.R.color.holo_green_dark))
                            visibility = android.view.View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun navigateToHome() {
        binding.root.postDelayed({
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }, 1500)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearState()
    }
}
