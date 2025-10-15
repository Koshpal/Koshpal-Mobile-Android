package com.koshpal_android.koshpalapp.ui.splash

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.koshpal_android.koshpalapp.R
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
        // Start animations immediately
        startPremiumAnimations()
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

    private fun startPremiumAnimations() {
        // Phase 1: Logo scale and fade in with bounce (0-900ms)
        animateLogoScaleAndFade()
        
        // Phase 2: Character-by-character "Koshpal" animation (1000-2200ms)
        animateKoshpalText()
        
        // Phase 3: Tagline fade in (2100-2600ms)
        animateTagline()
        
        // Phase 4: Background color transition (0-2800ms)
        animateBackgroundTransition()
    }

    private fun animateLogoScaleAndFade() {
        // Scale animation with overshoot: from 0.2 to 1.0 with slight bounce
        val scaleX = ObjectAnimator.ofFloat(binding.ivAppLogo, "scaleX", 0.2f, 1.1f, 1.0f).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
        }
        val scaleY = ObjectAnimator.ofFloat(binding.ivAppLogo, "scaleY", 0.2f, 1.1f, 1.0f).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
        }
        
        // Fade animation: from 0 to 1
        val fadeIn = ObjectAnimator.ofFloat(binding.ivAppLogo, "alpha", 0f, 1.0f).apply {
            duration = 900
            interpolator = DecelerateInterpolator()
        }
        
        // Add subtle rotation for extra flair
        val rotate = ObjectAnimator.ofFloat(binding.ivAppLogo, "rotation", -10f, 0f).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
        }
        
        scaleX.start()
        scaleY.start()
        fadeIn.start()
        rotate.start()
    }


    private fun animateKoshpalText() {
        try {
            // List of character TextViews
            val characters = listOf(
                binding.tvChar1,  // K
                binding.tvChar2,  // o
                binding.tvChar3,  // s
                binding.tvChar4,  // h
                binding.tvChar5,  // p
                binding.tvChar6,  // a
                binding.tvChar7   // l
            )
            
            // Animate each character with staggered delay
            characters.forEachIndexed { index, textView ->
                val delay = 1100L + (index * 120L) // Start at 1100ms, each character 120ms apart
                
                // Slide up animation with bounce
                val slideUp = ObjectAnimator.ofFloat(textView, "translationY", 30f, -5f, 0f).apply {
                    duration = 400
                    startDelay = delay
                    interpolator = DecelerateInterpolator()
                }
                
                // Fade in animation (from slightly visible to fully visible)
                val fadeIn = ObjectAnimator.ofFloat(textView, "alpha", 0.1f, 1.0f).apply {
                    duration = 400
                    startDelay = delay
                    interpolator = DecelerateInterpolator()
                }
                
                // Scale animation for bounce effect
                val scaleX = ObjectAnimator.ofFloat(textView, "scaleX", 0.5f, 1.2f, 1.0f).apply {
                    duration = 400
                    startDelay = delay
                    interpolator = DecelerateInterpolator()
                }
                
                val scaleY = ObjectAnimator.ofFloat(textView, "scaleY", 0.5f, 1.2f, 1.0f).apply {
                    duration = 400
                    startDelay = delay
                    interpolator = DecelerateInterpolator()
                }
                
                slideUp.start()
                fadeIn.start()
                scaleX.start()
                scaleY.start()
            }
        } catch (e: Exception) {
            // Binding not ready yet - will work after rebuild
            e.printStackTrace()
        }
    }
    
    private fun animateTagline() {
        try {
            // Fade in tagline after text animation
            val fadeIn = ObjectAnimator.ofFloat(binding.tvTagline, "alpha", 0f, 1.0f).apply {
                duration = 600
                startDelay = 2200
                interpolator = DecelerateInterpolator()
            }
            
            val slideUp = ObjectAnimator.ofFloat(binding.tvTagline, "translationY", 20f, 0f).apply {
                duration = 600
                startDelay = 2200
                interpolator = DecelerateInterpolator()
            }
            
            fadeIn.start()
            slideUp.start()
        } catch (e: Exception) {
            // Binding not ready yet - will work after rebuild
            e.printStackTrace()
        }
    }

    private fun animateBackgroundTransition() {
        // Transition from primary color to white
        val primaryColor = ContextCompat.getColor(this, R.color.primary)
        val whiteColor = ContextCompat.getColor(this, R.color.white)
        
        val colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), primaryColor, whiteColor).apply {
            duration = 2800
            addUpdateListener { animator ->
                binding.backgroundView.setBackgroundColor(animator.animatedValue as Int)
            }
            interpolator = AccelerateDecelerateInterpolator()
        }
        colorAnimator.start()
    }
}