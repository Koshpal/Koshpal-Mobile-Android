package com.koshpal_android.koshpalapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.koshpal_android.koshpalapp.databinding.ActivityLoginBinding
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import com.koshpal_android.koshpalapp.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    @Inject
    lateinit var userPreferences: UserPreferences


    private val allowedEmails = setOf(
        "muditsharmaanjana2203@gmail.com",
        "guptasankalp2004@gmail.com",
        "tushars7740@gmail.com",
        "akshatnahata05@gmail.com",
        "khandalakshit@gmail.com",
        "karanbankar54@gmail.com",
        "koshpal@gmail.com"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        // Clear email field when activity opens (fresh login every time)
        binding.etEmail.text?.clear()
        binding.etEmail.requestFocus()

        // Login button click listener
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            when {
                email.isEmpty() -> {
                    showToast("Please enter your email")
                    binding.etEmail.requestFocus()
                }
                !isValidEmail(email) -> {
                    showToast("Please enter a valid email address")
                    binding.etEmail.requestFocus()
                }
                !isEmailWhitelisted(email) -> {
                    showToast("Invalid email - Access denied")
                    binding.etEmail.text?.clear()
                    binding.etEmail.requestFocus()
                }
                else -> {
                    // Email is valid and whitelisted
                    userPreferences.saveEmail(email)
                    showToast("Welcome! Access granted")
                    navigateToHome()
                }
            }
        }
    }

    /**
     * Validates if the email format is correct
     */
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Checks if the email is in the whitelist
     */
    private fun isEmailWhitelisted(email: String): Boolean {
        val normalizedEmail = email.lowercase().trim()
        return allowedEmails.any { it.lowercase() == normalizedEmail }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
