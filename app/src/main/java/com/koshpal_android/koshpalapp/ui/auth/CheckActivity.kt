package com.koshpal_android.koshpalapp.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.koshpal_android.koshpalapp.databinding.ActivityCheckBinding
import com.koshpal_android.koshpalapp.utils.showToast
import kotlinx.coroutines.launch

class CheckActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCheckBinding
    private val viewModel: CheckViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnSubmit.setOnClickListener {
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                viewModel.storeMobileNumber(phoneNumber)
            } else {
                showToast("Please enter your mobile number")
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Update UI based on loading state
                binding.progressBar.visibility = if (state.isLoading)
                    android.view.View.VISIBLE else android.view.View.GONE
                binding.btnSubmit.isEnabled = !state.isLoading

                // Handle success state
                if (state.isSuccess) {
                    showToast(state.message ?: "Mobile number stored successfully!")
                    navigateToLogin()
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

    private fun navigateToLogin() {
        // Add a small delay to show success message
        binding.root.postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }, 1500)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearState()
    }
}
