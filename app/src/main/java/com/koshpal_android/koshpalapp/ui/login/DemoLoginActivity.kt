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
import com.koshpal_android.koshpalapp.ui.sync.SyncActivity
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
            // Pre-fill demo credentials
            etEmail.setText("demo@koshpal.com")
            etPassword.setText("demo@123")
            
            Log.d("DemoLoginActivity", "✅ Demo credentials pre-filled")
            
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
            
            btnSkip.setOnClickListener {
                Log.d("DemoLoginActivity", "🔘 Skip button clicked")
                // Skip login and go to home
                navigateToHome()
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
                    btnSkip.isEnabled = !inProgress
                }
            }
            
            loginResult.observe(this@DemoLoginActivity) { result ->
                Log.d("DemoLoginActivity", "📋 Login result received: $result")
                when (result) {
                    is LoginViewModel.LoginResult.Success -> {
                        Log.d("DemoLoginActivity", "✅ Login successful!")
                        Toast.makeText(this@DemoLoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                        navigateToSync()
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
    
    private fun navigateToSync() {
        Log.d("DemoLoginActivity", "🔄 Navigating to Sync Activity")
        try {
            val intent = Intent(this, SyncActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            Log.d("DemoLoginActivity", "✅ Successfully navigated to Sync Activity")
        } catch (e: Exception) {
            Log.e("DemoLoginActivity", "❌ Error navigating to Sync: ${e.message}", e)
            Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun navigateToHome() {
        Log.d("DemoLoginActivity", "🏠 Navigating to Home Activity")
        try {
            val intent = Intent(this, com.koshpal_android.koshpalapp.ui.home.HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            Log.d("DemoLoginActivity", "✅ Successfully navigated to Home Activity")
        } catch (e: Exception) {
            Log.e("DemoLoginActivity", "❌ Error navigating to Home: ${e.message}", e)
            Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
