package com.koshpal_android.koshpalapp.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.koshpal_android.koshpalapp.ui.theme.KoshpalTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DemoLoginActivity : AppCompatActivity() {

    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("DemoLoginActivity", "🚀 DemoLoginActivity onCreate started")

        setContent {
            KoshpalTheme {
                var email by remember { mutableStateOf("chaitnykakde517@gmail.com") }
                var password by remember { mutableStateOf("&7KmvTvIANXa") }
                var isLoading by remember { mutableStateOf(false) }

                LoginScreen(
                    email = email,
                    onEmailChange = { email = it },
                    password = password,
                    onPasswordChange = { password = it },
                    onLoginClick = {
                        Log.d("DemoLoginActivity", "🔘 Login button clicked")

                        val trimmedEmail = email.trim()
                        val trimmedPassword = password.trim()

                        Log.d("DemoLoginActivity", "📧 Email: $trimmedEmail")
                        Log.d("DemoLoginActivity", "🔑 Password: $trimmedPassword")

                        if (trimmedEmail.isEmpty() || trimmedPassword.isEmpty()) {
                            Log.w("DemoLoginActivity", "⚠️ Empty email or password")
                            Toast.makeText(this@DemoLoginActivity, "Please enter email and password", Toast.LENGTH_SHORT).show()
                            return@LoginScreen
                        }

                        Log.d("DemoLoginActivity", "🚀 Starting login process")
                        isLoading = true

                        lifecycleScope.launch {
                            try {
                                loginViewModel.login(trimmedEmail, trimmedPassword)
                                Log.d("DemoLoginActivity", "✅ Login request sent to ViewModel")
                            } catch (e: Exception) {
                                Log.e("DemoLoginActivity", "❌ Error calling login: ${e.message}", e)
                                isLoading = false
                                Toast.makeText(this@DemoLoginActivity, "Login error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    isLoading = isLoading
                )
            }
        }

        observeViewModel()
        Log.d("DemoLoginActivity", "✅ DemoLoginActivity setup completed")
    }

    private fun observeViewModel() {
        Log.d("DemoLoginActivity", "👀 Setting up ViewModel observers")

        loginViewModel.apply {
            isLoginInProgress.observe(this@DemoLoginActivity) { inProgress ->
                Log.d("DemoLoginActivity", "🔄 Login in progress: $inProgress")
                // Loading state is now handled in Compose
            }

            loginResult.observe(this@DemoLoginActivity) { result ->
                Log.d("DemoLoginActivity", "📋 Login result received: $result")
                when (result) {
                    is LoginViewModel.LoginResult.Success -> {
                        Log.d("DemoLoginActivity", "✅ Login successful!")
                        Toast.makeText(this@DemoLoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                        navigateToSmsProcessing()
                    }
                    is LoginViewModel.LoginResult.Error -> {
                        Log.e("DemoLoginActivity", "❌ Login failed: ${result.message}")
                        setContent {
                            KoshpalTheme {
                                var email by remember { mutableStateOf("demo@koshpal.com") }
                                var password by remember { mutableStateOf("demo@123") }
                                LoginScreen(
                                    email = email,
                                    onEmailChange = { email = it },
                                    password = password,
                                    onPasswordChange = { password = it },
                                    onLoginClick = { /* Handle login */ },
                                    isLoading = false
                                )
                            }
                        }
                        Toast.makeText(this@DemoLoginActivity, "Login failed: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                    null -> {
                        Log.d("DemoLoginActivity", "⏳ No login result yet")
                    }
                }
            }
        }

        Log.d("DemoLoginActivity", "✅ ViewModel observers setup completed")
    }

    private fun navigateToSmsProcessing() {
        Log.d("DemoLoginActivity", "📱 Navigating to SMS Processing Activity")
        try {
            val intent = Intent(this, com.koshpal_android.koshpalapp.ui.sms.SmsProcessingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            Log.d("DemoLoginActivity", "✅ Successfully navigated to SMS Processing Activity")
        } catch (e: Exception) {
            Log.e("DemoLoginActivity", "❌ Error navigating to SMS Processing: ${e.message}", e)
            Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
