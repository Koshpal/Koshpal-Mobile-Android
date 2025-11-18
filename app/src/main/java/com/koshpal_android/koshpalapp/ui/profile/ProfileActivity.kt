package com.koshpal_android.koshpalapp.ui.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.koshpal_android.koshpalapp.databinding.ActivityProfileBinding
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.ui.sync.SyncActivity
import com.koshpal_android.koshpalapp.ui.splash.SplashActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityProfileBinding
    private val profileViewModel: ProfileViewModel by viewModels()
    
    @Inject
    lateinit var userPreferences: UserPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ProfileActivity", "🚀 ProfileActivity onCreate started")
        
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        observeViewModel()
        loadUserInfo()
    }
    
    private fun setupUI() {
        Log.d("ProfileActivity", "🔧 Setting up UI components")
        
        binding.apply {
            ivBack.setOnClickListener {
                Log.d("ProfileActivity", "⬅️ Back button clicked")
                finish()
            }
            
            btnLogout.setOnClickListener {
                Log.d("ProfileActivity", "🚪 Logout button clicked")
                logout()
            }
        }
        
        Log.d("ProfileActivity", "✅ UI setup completed")
    }
    
    private fun observeViewModel() {
        Log.d("ProfileActivity", "👀 Setting up ViewModel observers")
        
        profileViewModel.apply {
            // Observe sync status
            syncStatus.observe(this@ProfileActivity) { status ->
                Log.d("ProfileActivity", "🔄 Sync status: $status")
                val inProgress = status == ProfileViewModel.SyncStatus.SYNCING
                binding.apply {
                    progressBar.visibility = if (inProgress) View.VISIBLE else View.GONE
                }
                
                // Show toast on completion
                when (status) {
                    ProfileViewModel.SyncStatus.SUCCESS -> {
                        Toast.makeText(this@ProfileActivity, "Sync completed successfully!", Toast.LENGTH_SHORT).show()
                    }
                    ProfileViewModel.SyncStatus.ERROR -> {
                        val error = lastSyncError.value ?: "Unknown error"
                        Toast.makeText(this@ProfileActivity, "Sync failed: $error", Toast.LENGTH_LONG).show()
                    }
                    else -> {} // IDLE or SYNCING
                }
            }
            
            // Observe total synced count
            totalSyncedCount.observe(this@ProfileActivity) { count ->
                Log.d("ProfileActivity", "📊 Total synced count: $count")
            }
        }
        
        Log.d("ProfileActivity", "✅ ViewModel observers setup completed")
    }
    
    private fun loadUserInfo() {
        Log.d("ProfileActivity", "👤 Loading user information")
        
        try {
            // Use static employee ID (no login required)
            val staticEmployeeId = "68ee28ce2f3fd392ea436576"
            val email = userPreferences.getEmail()?.ifEmpty { "koshpal.user@app.com" } ?: "koshpal.user@app.com"
            val isLoggedIn = userPreferences.isLoggedIn()
            val isSyncCompleted = userPreferences.isInitialSyncCompleted()
            
            Log.d("ProfileActivity", "📊 User info - Email: $email, EmployeeId: $staticEmployeeId, LoggedIn: $isLoggedIn, SyncCompleted: $isSyncCompleted")
            
            binding.apply {
                tvPhone.text = "+919307879687"
                tvEmail.text = email
                tvVersion.text = "Version 8.8.8v888r59f163d126rdt3"
            }
        } catch (e: Exception) {
            Log.e("ProfileActivity", "❌ Error loading user info: ${e.message}", e)
            Toast.makeText(this, "Error loading user info: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    
    private fun logout() {
        Log.d("ProfileActivity", "🚪 Logging out user")
        try {
            // Clear user preferences
            userPreferences.setLoggedIn(false)
            userPreferences.setInitialSyncCompleted(false)
            userPreferences.saveUserId("")
            userPreferences.saveEmail("")
            userPreferences.saveUserToken("")
            
            Log.d("ProfileActivity", "✅ User logged out successfully")
            
            // Navigate to splash screen
            val intent = Intent(this, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("ProfileActivity", "❌ Error during logout: ${e.message}", e)
            Toast.makeText(this, "Error during logout: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
