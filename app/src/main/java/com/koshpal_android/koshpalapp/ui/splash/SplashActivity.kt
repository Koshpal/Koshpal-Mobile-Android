package com.koshpal_android.koshpalapp.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.koshpal_android.koshpalapp.databinding.ActivitySplashBinding
import com.koshpal_android.koshpalapp.ui.auth.LoginActivity
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeNavigation()
        viewModel.startSplashTimer()
    }

    private fun observeNavigation() {
        lifecycleScope.launch {
            viewModel.navigationEvent.collect { destination ->
                when (destination) {
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