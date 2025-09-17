package com.koshpal_android.koshpalapp.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.koshpal_android.koshpalapp.databinding.ActivitySplashBinding
import com.koshpal_android.koshpalapp.ui.auth.CheckActivity
import com.koshpal_android.koshpalapp.ui.auth.LoginActivity
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.network.RetrofitClient
import com.koshpal_android.koshpalapp.repository.UserRepository
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    // Create dependencies
    private val userPreferences by lazy {
        UserPreferences(this)
    }

    private val userRepository by lazy {
        UserRepository(
            apiService = RetrofitClient.instance,
            userPreferences = userPreferences
        )
    }

    // Option 1: Using ViewModels with factory
    private val viewModel: SplashViewModel by viewModels {
        SplashViewModelFactory(
            userRepository = userRepository,
            userPreferences = userPreferences
        )
    }

    // Alternative Option 2: Manual ViewModel creation
    // private val viewModel: SplashViewModel by lazy {
    //     SplashViewModel(userRepository, userPreferences)
    // }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeNavigation()
        viewModel.startSplashTimer()
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
}