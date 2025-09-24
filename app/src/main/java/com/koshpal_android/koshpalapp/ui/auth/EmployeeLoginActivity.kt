package com.koshpal_android.koshpalapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.koshpal_android.koshpalapp.databinding.ActivityEmployeeLoginBinding
import com.koshpal_android.koshpalapp.ui.onboarding.OnboardingActivity
import com.koshpal_android.koshpalapp.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EmployeeLoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmployeeLoginBinding
    private val viewModel: EmployeeLoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmployeeLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (isValidEmail(email)) {
                viewModel.loginWithEmail(email)
            } else {
                showToast("Please enter a valid email address")
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading)
                    android.view.View.VISIBLE else android.view.View.GONE
                binding.btnLogin.isEnabled = !state.isLoading
                binding.etEmail.isEnabled = !state.isLoading

                state.error?.let { 
                    showToast(it)
                    viewModel.clearError()
                }

                if (state.isLoginSuccess) {
                    showToast("Login successful!")
                    val email = binding.etEmail.text.toString().trim()
                    navigateToOnboarding(email)
                }
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun navigateToOnboarding(email: String) {
        val intent = Intent(this, OnboardingActivity::class.java)
        intent.putExtra("email", email)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
