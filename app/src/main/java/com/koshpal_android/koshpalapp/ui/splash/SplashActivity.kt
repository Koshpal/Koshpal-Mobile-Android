package com.koshpal_android.koshpalapp.ui.splash

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.koshpal_android.koshpalapp.ui.login.DemoLoginActivity
import com.koshpal_android.koshpalapp.ui.auth.EmployeeLoginActivity
import com.koshpal_android.koshpalapp.ui.onboarding.OnboardingActivity
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import com.koshpal_android.koshpalapp.ui.sms.SmsProcessingActivity
import com.koshpal_android.koshpalapp.ui.sync.SyncActivity
import com.koshpal_android.koshpalapp.utils.NotificationPermissionHelper
import com.koshpal_android.koshpalapp.ml.SmsClassifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    // Use Hilt for ViewModel injection
    private val viewModel: SplashViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if SMS permissions were actually granted
        val readSmsGranted = permissions[Manifest.permission.READ_SMS] == true
        val receiveSmsGranted = permissions[Manifest.permission.RECEIVE_SMS] == true
        
        if (readSmsGranted && receiveSmsGranted) {
            Log.d("SplashActivity", "✅ SMS permissions granted")
            // Initialize SMS Classifier early now that permissions are granted
            initializeSmsClassifier()
        } else {
            Log.w("SplashActivity", "⚠️ SMS permissions denied - READ_SMS: $readSmsGranted, RECEIVE_SMS: $receiveSmsGranted")
        }
        
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
    private var isLottieAnimationReady = false
    private var lottieAnimationStartTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ============================================
        // ADDED LOTTIE ANIMATION: Initialize and start Lottie animation
        // Uses Lottie library to display animated logo on splash screen
        // Animation is loaded from raw resources (splasg_anim.json) - 2 seconds duration
        // ============================================
        setupLottieAnimation()
        
        observeNavigation()
        // Request permissions in sequence to avoid overlapping dialogs
        requestSmsPermissionsIfNeeded()
        
        // Check for app updates
        checkForAppUpdate()
    }
    
    /**
     * Check for app updates in background
     */
    private fun checkForAppUpdate() {
        lifecycleScope.launch {
            try {
                val updateManager = com.koshpal_android.koshpalapp.utils.UpdateManager(this@SplashActivity)
                val updateInfo = updateManager.checkForUpdate()
                
                if (updateInfo != null) {
                    // Show update dialog after a short delay to not interfere with splash
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (!isFinishing && !isDestroyed) {
                            val updateDialog = com.koshpal_android.koshpalapp.ui.update.UpdateDialog.newInstance(updateInfo)
                            updateDialog.show(supportFragmentManager, "UpdateDialog")
                        }
                    }, 2000) // Show after 2 seconds
                }
            } catch (e: Exception) {
                Log.e("SplashActivity", "❌ Error checking for updates: ${e.message}", e)
            }
        }
    }
    
    /**
     * ADDED LOTTIE ANIMATION: Setup and configure Lottie animation for splash screen
     * 
     * This function:
     * - Loads Lottie animation from raw resources (splasg_anim.json)
     * - Configures animation properties (plays once, 2 seconds duration, speed, rendering mode)
     * - Handles animation lifecycle (play, pause, resume)
     * - Provides fallback handling if animation fails to load
     */
    private fun setupLottieAnimation() {
        try {
            // Step 1: Verify resource exists
            val resourceId = R.raw.splasg_anim
            Log.d("SplashActivity", "🔍 Checking Lottie resource ID: $resourceId")
            
            // Step 2: Configure animation view properties first
            binding.lottieAnimation.apply {
                // Set visibility explicitly
                visibility = android.view.View.VISIBLE
                
                // Set background color to match screen background
                setBackgroundColor(ContextCompat.getColor(this@SplashActivity, R.color.primary_darkest))
                
                // Configure animation properties
                repeatCount = 0 // Play once (2 seconds duration)
                speed = 1.0f
                
                // Enable merge paths support (required for complex animations)
                enableMergePathsForKitKatAndAbove(true)
                
                // Try software rendering first (hardware can sometimes cause issues)
                // If needed, can switch to HARDWARE later
                setRenderMode(com.airbnb.lottie.RenderMode.AUTOMATIC)
                
                Log.d("SplashActivity", "✅ LottieAnimationView configured")
            }
            
            // Step 3: Load composition asynchronously using LottieCompositionFactory
            Log.d("SplashActivity", "📥 Loading Lottie composition from raw resource...")
            
            com.airbnb.lottie.LottieCompositionFactory.fromRawRes(this, resourceId)
                .addListener(object : com.airbnb.lottie.LottieListener<com.airbnb.lottie.LottieComposition> {
                    override fun onResult(composition: com.airbnb.lottie.LottieComposition?) {
                        if (composition != null) {
                            Log.d("SplashActivity", "✅ Lottie composition loaded successfully")
                            Log.d("SplashActivity", "   - Duration: ${composition.duration}ms")
                            Log.d("SplashActivity", "   - Frame rate: ${composition.frameRate} fps")
                            Log.d("SplashActivity", "   - Start frame: ${composition.startFrame}")
                            Log.d("SplashActivity", "   - End frame: ${composition.endFrame}")
                            
                            // Check for warnings
                            val warnings = composition.warnings
                            if (warnings.isNotEmpty()) {
                                Log.w("SplashActivity", "⚠️ Lottie composition warnings:")
                                warnings.forEach { warning ->
                                    Log.w("SplashActivity", "   - $warning")
                                }
                            }
                            
                            // Set composition and play animation
                            binding.lottieAnimation.apply {
                                // Ensure visibility first
                                visibility = android.view.View.VISIBLE
                                
                                // Set composition
                                setComposition(composition)
                                
                                // Set to first frame immediately to show something right away
                                val startFrame = composition.startFrame.toInt()
                                frame = startFrame
                                Log.d("SplashActivity", "📸 Set to first frame: $startFrame")
                                
                                // Post to ensure view is fully laid out before playing
                                post {
                                    // Double-check visibility
                                    visibility = android.view.View.VISIBLE
                                    
                                    // Start playing animation
                                    playAnimation()
                                    
                                    // Mark animation as ready
                                    isLottieAnimationReady = true
                                    lottieAnimationStartTime = System.currentTimeMillis()
                                    
                                    // Log animation state
                                    Log.d("SplashActivity", "🎬 Lottie animation started playing")
                                    Log.d("SplashActivity", "   - Current frame: $frame")
                                    Log.d("SplashActivity", "   - Is animating: $isAnimating")
                                    Log.d("SplashActivity", "   - Progress: $progress")
                                    Log.d("SplashActivity", "   - Speed: $speed")
                                    Log.d("SplashActivity", "   - Repeat count: $repeatCount")
                                    
                                    // If splash timer hasn't started yet, trigger it now
                                    if (!hasStartedSplash) {
                                        Log.d("SplashActivity", "🚀 Lottie ready, triggering splash timer")
                                        startSplashOnce()
                                    }
                                    
                                    // Verify animation is actually playing after a short delay
                                    postDelayed({
                                        Log.d("SplashActivity", "🔍 Animation check after 500ms:")
                                        Log.d("SplashActivity", "   - Is animating: $isAnimating")
                                        Log.d("SplashActivity", "   - Current frame: $frame")
                                        Log.d("SplashActivity", "   - Progress: $progress")
                                        if (!isAnimating) {
                                            Log.w("SplashActivity", "⚠️ Animation is not playing, trying to restart...")
                                            playAnimation()
                                        }
                                    }, 500)
                                }
                            }
                        } else {
                            Log.e("SplashActivity", "❌ Lottie composition is null")
                            handleAnimationLoadFailure("Composition is null")
                        }
                    }
                })
                .addFailureListener(object : com.airbnb.lottie.LottieListener<Throwable> {
                    override fun onResult(result: Throwable) {
                        Log.e("SplashActivity", "❌ Failed to load Lottie composition", result)
                        handleAnimationLoadFailure(result.message ?: "Unknown error")
                    }
                })
                
        } catch (e: Exception) {
            Log.e("SplashActivity", "❌ Exception during Lottie setup: ${e.message}", e)
            handleAnimationLoadFailure(e.message ?: "Exception during setup")
        }
    }
    
    private fun handleAnimationLoadFailure(errorMessage: String) {
        Log.e("SplashActivity", "❌ Animation load failed: $errorMessage")
        // Keep view visible but log the error
        // Animation view will remain visible but empty
        binding.lottieAnimation.visibility = android.view.View.VISIBLE
        
        // Optionally, you could show a placeholder image here
        // For now, we'll just log the error and keep the view visible
    }

    private fun observeNavigation() {
        // Better lifecycle-aware approach
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvent.collect { destination ->
                    Log.d("SplashActivity", "🧭 Navigation destination: $destination")
                    when (destination) {
                        SplashViewModel.NavigationDestination.CHECK -> {
                            Log.d("SplashActivity", "➡️ Navigating to CheckActivity")
                            startActivity(Intent(this@SplashActivity, CheckActivity::class.java))
                            finish()
                        }
                        SplashViewModel.NavigationDestination.LOGIN -> {
                            Log.d("SplashActivity", "➡️ Navigating to DemoLoginActivity")
                            startActivity(Intent(this@SplashActivity, DemoLoginActivity::class.java))
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
                        SplashViewModel.NavigationDestination.SYNC -> {
                            startActivity(Intent(this@SplashActivity, SyncActivity::class.java))
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
            Log.d("SplashActivity", "📱 Requesting SMS permissions: ${toRequest.joinToString()}")
            permissionLauncher.launch(toRequest.toTypedArray())
        } else {
            // Permissions already granted, initialize SMS Classifier
            Log.d("SplashActivity", "✅ SMS permissions already granted")
            initializeSmsClassifier()
            // No SMS permission needed, proceed to notifications
            requestNotificationPermissionIfNeeded()
        }
    }
    
    /**
     * Initialize SMS Classifier model early so it's ready when SMS arrives
     */
    private fun initializeSmsClassifier() {
        try {
            Log.d("SplashActivity", "🤖 Initializing SMS Classifier model...")
            val classifier = SmsClassifier(this)
            Log.d("SplashActivity", "✅ SMS Classifier initialized and ready")
        } catch (e: Exception) {
            Log.e("SplashActivity", "❌ Failed to initialize SMS Classifier: ${e.message}", e)
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
        
        // Wait for Lottie animation to be ready before starting timer
        waitForLottieAndStartTimer()
    }
    
    private fun waitForLottieAndStartTimer() {
        // Check if Lottie is already ready
        if (isLottieAnimationReady) {
            Log.d("SplashActivity", "✅ Lottie already ready, starting splash timer")
            viewModel.startSplashTimer()
            return
        }
        
        // Wait for Lottie animation to be ready (max 3 seconds)
        val startTime = System.currentTimeMillis()
        val maxWaitTime = 3000L // 3 seconds max wait
        
        // Check periodically if Lottie becomes ready
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val checkRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                if (isLottieAnimationReady) {
                    Log.d("SplashActivity", "✅ Lottie ready after ${elapsed}ms, starting splash timer")
                    viewModel.startSplashTimer()
                } else if (elapsed < maxWaitTime) {
                    // Keep checking every 100ms until max wait time
                    handler.postDelayed(this, 100)
                } else {
                    // Max wait time reached, start timer anyway
                    Log.w("SplashActivity", "⚠️ Lottie not ready after ${elapsed}ms, starting timer anyway")
                    viewModel.startSplashTimer()
                }
            }
        }
        handler.post(checkRunnable)
    }


    // Background color is set to dark blue in XML, no transition needed
    
    override fun onResume() {
        super.onResume()
        // Resume animation when activity resumes
        binding.lottieAnimation.resumeAnimation()
    }
    
    override fun onPause() {
        super.onPause()
        // Pause animation when activity pauses
        binding.lottieAnimation.pauseAnimation()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up animation resources
        binding.lottieAnimation.cancelAnimation()
    }
}