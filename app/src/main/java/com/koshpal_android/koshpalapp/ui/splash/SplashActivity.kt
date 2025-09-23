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
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
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
        // Regardless of grant/deny, continue splash flow
        startSplash()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeNavigation()
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
            startSplash()
        }
    }

    private fun startSplash() {
        viewModel.startSplashTimer()
    }
}