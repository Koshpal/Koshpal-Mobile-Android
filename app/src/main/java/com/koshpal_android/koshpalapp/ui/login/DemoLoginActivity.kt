package com.koshpal_android.koshpalapp.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.koshpal_android.koshpalapp.databinding.ActivityDemoLoginBinding
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DemoLoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDemoLoginBinding
    private val loginViewModel: LoginViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("DemoLoginActivity", "🚀 DemoLoginActivity onCreate started")
        
        binding = ActivityDemoLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        Log.d("DemoLoginActivity", "✅ UI binding completed")
        
        setupUI()
        observeViewModel()
        
        Log.d("DemoLoginActivity", "✅ DemoLoginActivity setup completed")
    }
    
    private fun setupUI() {
        Log.d("DemoLoginActivity", "🔧 Setting up UI components")
        
        binding.apply {
            // Pre-fill demo credentials - REMOVED for production-like feel
            
            Log.d("DemoLoginActivity", "✅ UI components initialized")
            
            btnLogin.setOnClickListener {
                Log.d("DemoLoginActivity", "🔘 Login button clicked")
                
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()
                
                Log.d("DemoLoginActivity", "📧 Email: $email")
                Log.d("DemoLoginActivity", "🔑 Password: $password")
                
                if (email.isEmpty() || password.isEmpty()) {
                    Log.w("DemoLoginActivity", "⚠️ Empty email or password")
                    Toast.makeText(this@DemoLoginActivity, "Please enter email and password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                Log.d("DemoLoginActivity", "🚀 Starting login process")
                lifecycleScope.launch {
                    try {
                        loginViewModel.login(email, password)
                        Log.d("DemoLoginActivity", "✅ Login request sent to ViewModel")
                    } catch (e: Exception) {
                        Log.e("DemoLoginActivity", "❌ Error calling login: ${e.message}", e)
                        Toast.makeText(this@DemoLoginActivity, "Login error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            
            // Remove skip button - login is now required
            btnSkip.visibility = View.GONE

            tvForgotPassword.setOnClickListener {
                Toast.makeText(this@DemoLoginActivity, "Forgot password clicked", Toast.LENGTH_SHORT).show()
            }
        }
        
        Log.d("DemoLoginActivity", "✅ UI setup completed")
    }
    
    private fun observeViewModel() {
        Log.d("DemoLoginActivity", "👀 Setting up ViewModel observers")
        
        loginViewModel.apply {
            isLoginInProgress.observe(this@DemoLoginActivity) { inProgress ->
                Log.d("DemoLoginActivity", "🔄 Login in progress: $inProgress")
                binding.apply {
                    progressBar.visibility = if (inProgress) View.VISIBLE else View.GONE
                    btnLogin.isEnabled = !inProgress
                }
            }
            
            loginResult.observe(this@DemoLoginActivity) { result ->
                Log.d("DemoLoginActivity", "📋 Login result received: $result")
                when (result) {
                    is LoginViewModel.LoginResult.Success -> {
                        Log.d("DemoLoginActivity", "✅ Login successful!")
                        Toast.makeText(this@DemoLoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                        navigateToNextScreen()
                    }
                    is LoginViewModel.LoginResult.Error -> {
                        Log.e("DemoLoginActivity", "❌ Login failed: ${result.message}")
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
    
    private fun navigateToNextScreen() {
        Log.d("DemoLoginActivity", "📱 Navigating to next screen after login")
        try {
            val userPreferences = com.koshpal_android.koshpalapp.data.local.UserPreferences(this)
            
            val intent = if (!userPreferences.isOnboardingCompleted()) {
                Log.d("DemoLoginActivity", "➡️ Onboarding not complete, navigating to OnboardingActivity")
                Intent(this, com.koshpal_android.koshpalapp.ui.onboarding.OnboardingActivity::class.java).apply {
                    putExtra("email", loginViewModel.getCurrentUserEmail() ?: userPreferences.getEmail() ?: "")
                }
            } else {
                Log.d("DemoLoginActivity", "➡️ Onboarding complete, navigating to SmsProcessingActivity")
                Intent(this, com.koshpal_android.koshpalapp.ui.sms.SmsProcessingActivity::class.java)
            }
            
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            Log.d("DemoLoginActivity", "✅ Successfully navigated to next screen")
        } catch (e: Exception) {
            Log.e("DemoLoginActivity", "❌ Error navigating: ${e.message}", e)
            Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
