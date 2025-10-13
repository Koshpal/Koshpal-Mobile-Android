package com.koshpal_android.koshpalapp.ui.splash

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.koshpal_android.koshpalapp.databinding.ActivitySplashBinding
import com.koshpal_android.koshpalapp.ui.auth.CheckActivity
import com.koshpal_android.koshpalapp.ui.auth.LoginActivity
import com.koshpal_android.koshpalapp.ui.auth.EmployeeLoginActivity
import com.koshpal_android.koshpalapp.ui.onboarding.OnboardingActivity
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import com.koshpal_android.koshpalapp.ui.sms.SmsProcessingActivity
import com.koshpal_android.koshpalapp.utils.NotificationPermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    // Use Hilt for ViewModel injection
    private val viewModel: SplashViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // After SMS permissions result, proceed to notifications or splash
        requestNotificationPermissionIfNeeded()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Continue splash flow regardless of notification permission result
        startSplashOnce()
    }

    private var hasStartedSplash = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeNavigation()
        // Request permissions in sequence to avoid overlapping dialogs
        requestSmsPermissionsIfNeeded()
    }

    private fun observeNavigation() {
        // Better lifecycle-aware approach
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvent.collect { destination ->
                    when (destination) {
                        SplashViewModel.NavigationDestination.CHECK -> {
                            startActivity(Intent(this@SplashActivity, CheckActivity::class.java))
                            finish()
                        }
                        SplashViewModel.NavigationDestination.LOGIN -> {
                            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                            finish()
                        }
                        SplashViewModel.NavigationDestination.HOME -> {
                            startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
                            finish()
                        }
                        SplashViewModel.NavigationDestination.EMPLOYEE_LOGIN -> {
                            startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
                            finish()
                        }
                        SplashViewModel.NavigationDestination.ONBOARDING -> {
                            val intent = Intent(this@SplashActivity, OnboardingActivity::class.java)
                            // Get email from preferences for onboarding
                            val userPreferences = com.koshpal_android.koshpalapp.data.local.UserPreferences(this@SplashActivity)
                            val email = userPreferences.getEmail() ?: ""
                            intent.putExtra("email", email)
                            startActivity(intent)
                            finish()
                        }
                        SplashViewModel.NavigationDestination.SMS_PROCESSING -> {
                            startActivity(Intent(this@SplashActivity, SmsProcessingActivity::class.java))
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun requestSmsPermissionsIfNeeded() {
        val readSms = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
        val receiveSms = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
        val toRequest = mutableListOf<String>()
        if (readSms != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            toRequest.add(Manifest.permission.READ_SMS)
        }
        if (receiveSms != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            toRequest.add(Manifest.permission.RECEIVE_SMS)
        }
        if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest.toTypedArray())
        } else {
            // No SMS permission needed, proceed to notifications
            requestNotificationPermissionIfNeeded()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (!NotificationPermissionHelper.hasNotificationPermission(this)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // For Android 12 and below, notifications are enabled by default
                startSplashOnce()
            }
        } else {
            startSplashOnce()
        }
    }

    private fun startSplashOnce() {
        if (hasStartedSplash) return
        hasStartedSplash = true
        viewModel.startSplashTimer()
    }
}