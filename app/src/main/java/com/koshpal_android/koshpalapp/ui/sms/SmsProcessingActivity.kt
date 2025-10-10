package com.koshpal_android.koshpalapp.ui.sms

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.koshpal_android.koshpalapp.databinding.ActivitySmsProcessingBinding
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SmsProcessingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySmsProcessingBinding
    private val viewModel: SmsProcessingViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySmsProcessingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupBackPressHandler()
        setupUI()
        observeProcessingState()
        
        // Start processing automatically
        viewModel.startProcessing()
    }
    
    private fun setupBackPressHandler() {
        // Prevent back press during processing - user can only skip
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing - user must use Skip button
            }
        })
    }
    
    private fun setupUI() {
        binding.btnSkip.setOnClickListener {
            navigateToHome()
        }
        
        binding.btnRetry.setOnClickListener {
            viewModel.retryProcessing()
        }
        
        binding.btnContinue.setOnClickListener {
            navigateToHome()
        }
    }
    
    private fun observeProcessingState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.processingState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }
    
    private fun updateUI(state: SmsProcessingState) {
        when (state) {
            is SmsProcessingState.Idle -> {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "Ready to process"
                binding.btnSkip.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.GONE
                binding.btnContinue.visibility = View.GONE
            }
            
            is SmsProcessingState.Processing -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.tvStatus.text = state.message
                binding.tvDetails.text = state.details
                binding.btnSkip.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.GONE
                binding.btnContinue.visibility = View.GONE
                
                // Update stats
                binding.tvSmsFound.text = "${state.smsFound}"
                binding.tvTransactionSms.text = "${state.transactionSms}"
                binding.tvTransactionsCreated.text = "${state.transactionsCreated}"
            }
            
            is SmsProcessingState.Success -> {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "✅ Processing Complete!"
                binding.tvDetails.text = state.summary
                binding.btnSkip.visibility = View.GONE
                binding.btnRetry.visibility = View.GONE
                binding.btnContinue.visibility = View.VISIBLE
                
                // Show final stats
                binding.tvSmsFound.text = "${state.smsFound}"
                binding.tvTransactionSms.text = "${state.transactionSms}"
                binding.tvTransactionsCreated.text = "${state.transactionsCreated}"
                
                // Auto-navigate after 2 seconds
                binding.root.postDelayed({
                    navigateToHome()
                }, 2000)
            }
            
            is SmsProcessingState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "❌ Error"
                binding.tvDetails.text = state.message
                binding.btnSkip.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.VISIBLE
                binding.btnContinue.visibility = View.GONE
            }
            
            is SmsProcessingState.PermissionDenied -> {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "⚠️ Permission Required"
                binding.tvDetails.text = "SMS permissions are needed to extract transaction data. You can skip and use the app with manual entry."
                binding.btnSkip.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.GONE
                binding.btnContinue.visibility = View.GONE
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

