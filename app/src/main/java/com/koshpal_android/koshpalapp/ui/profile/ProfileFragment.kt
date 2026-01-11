package com.koshpal_android.koshpalapp.ui.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.koshpal_android.koshpalapp.auth.SessionManager
import com.koshpal_android.koshpalapp.databinding.FragmentProfileBinding
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val profileViewModel: ProfileViewModel by viewModels()
    
    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeViewModel()
        loadUserInfo()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh sync data when returning to this fragment
        profileViewModel.refreshSyncData()
    }
    
    private fun setupUI() {
        Log.d("ProfileFragment", "🔧 Setting up UI components")
        
        binding.apply {
            btnSyncSms.setOnClickListener {
                Log.d("ProfileFragment", "🔄 Sync button clicked")
                performSync()
            }
            
            btnLogout.setOnClickListener {
                Log.d("ProfileFragment", "🚪 Logout button clicked")
                logout()
            }
        }
        
        Log.d("ProfileFragment", "✅ UI setup completed")
    }
    
    private fun observeViewModel() {
        Log.d("ProfileFragment", "👀 Setting up ViewModel observers")
        
        profileViewModel.apply {
            // Observe sync status
            syncStatus.observe(viewLifecycleOwner) { status ->
                Log.d("ProfileFragment", "🔄 Sync status changed: $status")
                updateSyncStatusUI(status)
            }
            
            // Observe total synced count
            totalSyncedCount.observe(viewLifecycleOwner) { count ->
                Log.d("ProfileFragment", "📊 Total synced count: $count")
                binding.tvTotalSyncedCount.text = "Total Messages Stored: $count"
            }
            
            // Observe last sync error
            lastSyncError.observe(viewLifecycleOwner) { error ->
                Log.d("ProfileFragment", "❌ Last sync error: $error")
                if (error != null) {
                    binding.tvSyncError.text = "Error: $error"
                    binding.tvSyncError.visibility = View.VISIBLE
                } else {
                    binding.tvSyncError.visibility = View.GONE
                }
            }
            
            // Observe last sync time
            lastSyncTime.observe(viewLifecycleOwner) { time ->
                Log.d("ProfileFragment", "⏰ Last sync time: $time")
                val formattedTime = if (time > 0) {
                    val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                    sdf.format(Date(time))
                } else {
                    "Never"
                }
                binding.tvLastSyncTime.text = "Last Sync: $formattedTime"
            }
            
            // Observe sync progress
            lifecycleScope.launch {
                syncProgress.collect { progress ->
                    Log.d("ProfileFragment", "📈 Sync progress: $progress%")
                    binding.progressBarSync.progress = progress
                }
            }
        }
        
        Log.d("ProfileFragment", "✅ ViewModel observers setup completed")
    }
    
    private fun updateSyncStatusUI(status: ProfileViewModel.SyncStatus) {
        binding.apply {
            when (status) {
                ProfileViewModel.SyncStatus.IDLE -> {
                    tvSyncStatus.text = "Status: Idle"
                    btnSyncSms.isEnabled = true
                    btnSyncSms.text = "Sync to Cloud"
                    progressBar.visibility = View.GONE
                    progressBarSync.visibility = View.GONE
                }
                ProfileViewModel.SyncStatus.SYNCING -> {
                    tvSyncStatus.text = "Status: Syncing..."
                    btnSyncSms.isEnabled = false
                    btnSyncSms.text = "Syncing..."
                    progressBar.visibility = View.VISIBLE
                    progressBarSync.visibility = View.VISIBLE
                }
                ProfileViewModel.SyncStatus.SUCCESS -> {
                    tvSyncStatus.text = "Status: ✅ All transactions backed up"
                    btnSyncSms.isEnabled = true
                    btnSyncSms.text = "Re-sync to Cloud"
                    progressBar.visibility = View.GONE
                    progressBarSync.visibility = View.GONE
                    Toast.makeText(requireContext(), "Sync completed successfully!", Toast.LENGTH_SHORT).show()
                }
                ProfileViewModel.SyncStatus.ERROR -> {
                    tvSyncStatus.text = "Status: ❌ Sync failed"
                    btnSyncSms.isEnabled = true
                    btnSyncSms.text = "Retry Sync"
                    progressBar.visibility = View.GONE
                    progressBarSync.visibility = View.GONE
                }
            }
        }
    }
    
    private fun loadUserInfo() {
        Log.d("ProfileFragment", "👤 Loading user information")
        
        try {
            val email = userPreferences.getEmail() ?: "koshpal.user@app.com"
            val userId = Constants.STATIC_EMPLOYEE_ID
            
            Log.d("ProfileFragment", "📊 User info - Email: $email, UserId: $userId")
            
            binding.apply {
                tvEmail.text = "Email: $email"
                tvUserId.text = "Employee ID: ${userId.take(8)}..."
                tvLoginStatus.text = "✅ App Registered"
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "❌ Error loading user info: ${e.message}", e)
            Toast.makeText(requireContext(), "Error loading user info: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun performSync() {
        Log.d("ProfileFragment", "🔄 Performing sync")
        
        // Clear previous error
        profileViewModel.clearSyncError()
        
        // Start sync
        profileViewModel.performInitialSync()
    }
    
    private fun logout() {
        Log.d("ProfileFragment", "🚪 Logging out user")
        try {
            // Clear session (this will synchronize both SessionManager and UserPreferences)
            sessionManager.clearSession()

            Log.d("ProfileFragment", "✅ User logged out successfully")

            // Restart app to go to splash screen
            val intent = requireActivity().packageManager.getLaunchIntentForPackage(requireActivity().packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            intent?.let { startActivity(it) }
            requireActivity().finish()

            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("ProfileFragment", "❌ Error during logout: ${e.message}", e)
            Toast.makeText(requireContext(), "Error during logout: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
