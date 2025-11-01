package com.koshpal_android.koshpalapp.ui.sync

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.koshpal_android.koshpalapp.databinding.ActivitySyncBinding
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SyncActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySyncBinding
    private val syncViewModel: SyncViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySyncBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        observeViewModel()
        
        // Start initial sync automatically
        lifecycleScope.launch {
            syncViewModel.performInitialSync()
        }
    }
    
    private fun setupUI() {
        binding.apply {
            btnRetry.setOnClickListener {
                lifecycleScope.launch {
                    syncViewModel.performInitialSync()
                }
            }
            
            btnSkip.setOnClickListener {
                // Skip sync and go to home
                navigateToHome()
            }
            
            btnContinue.setOnClickListener {
                navigateToHome()
            }
        }
    }
    
    private fun observeViewModel() {
        syncViewModel.apply {
            isInitialSyncCompleted.observe(this@SyncActivity) { completed ->
                if (completed) {
                    showSuccessState()
                }
            }
            
            isInitialSyncInProgress.observe(this@SyncActivity) { inProgress ->
                if (inProgress) {
                    showProgressState()
                }
            }
            
            syncProgress.observe(this@SyncActivity) { progress ->
                binding.progressBar.progress = progress
                binding.tvProgress.text = "Syncing transactions... $progress%"
            }
            
            errorMessage.observe(this@SyncActivity) { error ->
                if (error.isNotEmpty()) {
                    showErrorState(error)
                }
            }
        }
    }
    
    private fun showProgressState() {
        binding.apply {
            progressBar.visibility = View.VISIBLE
            tvStatus.text = "Syncing your transactions to cloud..."
            tvProgress.visibility = View.VISIBLE
            btnRetry.visibility = View.GONE
            btnSkip.visibility = View.VISIBLE
            btnContinue.visibility = View.GONE
        }
    }
    
    private fun showSuccessState() {
        binding.apply {
            progressBar.visibility = View.GONE
            tvStatus.text = "✅ Sync completed successfully!"
            tvProgress.text = "Your transactions are now backed up to the cloud"
            tvProgress.visibility = View.VISIBLE
            btnRetry.visibility = View.GONE
            btnSkip.visibility = View.GONE
            btnContinue.visibility = View.VISIBLE
        }
        
        // Auto-navigate to home after 2 seconds
        lifecycleScope.launch {
            kotlinx.coroutines.delay(2000)
            navigateToHome()
        }
    }
    
    private fun showErrorState(error: String) {
        binding.apply {
            progressBar.visibility = View.GONE
            tvStatus.text = "❌ Sync failed"
            tvProgress.text = error
            tvProgress.visibility = View.VISIBLE
            btnRetry.visibility = View.VISIBLE
            btnSkip.visibility = View.VISIBLE
            btnContinue.visibility = View.GONE
        }
        
        Toast.makeText(this, "Sync failed: $error", Toast.LENGTH_LONG).show()
        
        // If user is not logged in, automatically skip to home after a delay
        if (error.contains("not logged in") || error.contains("not registered")) {
            binding.apply {
                tvStatus.text = "ℹ️ User not logged in"
                tvProgress.text = "Skipping sync and going to home..."
                btnRetry.visibility = View.GONE
                btnSkip.visibility = View.GONE
            }
            
            // Auto-navigate to home after 2 seconds
            lifecycleScope.launch {
                kotlinx.coroutines.delay(2000)
                navigateToHome()
            }
        }
    }
    
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
