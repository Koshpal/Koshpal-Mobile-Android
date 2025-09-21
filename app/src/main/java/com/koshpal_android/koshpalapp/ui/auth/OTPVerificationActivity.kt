package com.koshpal_android.koshpalapp.ui.auth

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.koshpal_android.koshpalapp.databinding.ActivityOtpverificationBinding
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import com.koshpal_android.koshpalapp.utils.Constants
import com.koshpal_android.koshpalapp.utils.SMSReceiver
import com.koshpal_android.koshpalapp.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OTPVerificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOtpverificationBinding
    private val viewModel: OTPVerificationViewModel by viewModels()
    private var smsReceiver: SMSReceiver? = null

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            registerSMSReceiver()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpverificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val phoneNumber = intent.getStringExtra(Constants.PHONE_NUMBER) ?: ""
        val verificationId = intent.getStringExtra(Constants.VERIFICATION_ID) ?: ""

        setupUI(phoneNumber, verificationId)
        observeViewModel()
        checkSMSPermission()
        viewModel.startTimer()
    }

    private fun setupUI(phoneNumber: String, verificationId: String) {
        binding.tvPhoneNumber.text = "Enter OTP sent to $phoneNumber"

        binding.btnVerifyOTP.setOnClickListener {
            val otp = binding.etOTP.text.toString().trim()
            if (otp.length == 6) {
                // Updated method call with phoneNumber
                viewModel.verifyOTPAndCreateUser(verificationId, otp, phoneNumber)
            } else {
                showToast("Please enter 6-digit OTP")
            }
        }

        binding.btnResendOTP.setOnClickListener {
            showToast("OTP resent successfully")
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading)
                    android.view.View.VISIBLE else android.view.View.GONE
                binding.btnVerifyOTP.isEnabled = !state.isLoading

                binding.tvTimer.text = "Resend OTP in ${state.timerSeconds}s"
                binding.btnResendOTP.isEnabled = state.timerSeconds == 0

                state.error?.let { showToast(it) }

                if (state.isVerified && state.user != null) {
                    showToast("Welcome! Account created successfully.")
                    startActivity(Intent(this@OTPVerificationActivity, HomeActivity::class.java))
                    finishAffinity()
                }
            }
        }
    }

    // ... rest of the SMS receiver code remains the same ...

    private fun checkSMSPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            == PackageManager.PERMISSION_GRANTED) {
            registerSMSReceiver()
        } else {
            smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
        }
    }

    private fun registerSMSReceiver() {
        smsReceiver = SMSReceiver().apply {
            onOTPReceived = { otp ->
                runOnUiThread {
                    binding.etOTP.setText(otp)
                }
            }
        }
        val filter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
        registerReceiver(smsReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        smsReceiver?.let { unregisterReceiver(it) }
    }
}