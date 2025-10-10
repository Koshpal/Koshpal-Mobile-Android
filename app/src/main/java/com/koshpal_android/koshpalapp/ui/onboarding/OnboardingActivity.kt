package com.koshpal_android.koshpalapp.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.koshpal_android.koshpalapp.databinding.ActivityOnboardingBinding
import com.koshpal_android.koshpalapp.model.OnboardingSection
import com.koshpal_android.koshpalapp.ui.sms.SmsProcessingActivity
import com.koshpal_android.koshpalapp.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private val viewModel: OnboardingViewModel by viewModels()
    private lateinit var onboardingAdapter: OnboardingPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        observeViewModel()
        
        // Get email from intent
        val email = intent.getStringExtra("email") ?: ""
        viewModel.setUserEmail(email)
    }

    private fun setupViewPager() {
        onboardingAdapter = OnboardingPagerAdapter(this)
        binding.viewPager.adapter = onboardingAdapter
        binding.viewPager.isUserInputEnabled = false // Disable swipe navigation
        
        // Setup progress indicator
        updateProgressIndicator(0)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading)
                    android.view.View.VISIBLE else android.view.View.GONE

                state.error?.let { 
                    showToast(it)
                    viewModel.clearError()
                }

                if (state.isOnboardingComplete) {
                    navigateToHome()
                }
            }
        }
    }

    fun nextSection() {
        val currentItem = binding.viewPager.currentItem
        if (currentItem < onboardingAdapter.itemCount - 1) {
            binding.viewPager.currentItem = currentItem + 1
            updateProgressIndicator(currentItem + 1)
        }
    }

    fun completeOnboarding() {
        viewModel.submitOnboardingData()
    }

    fun updateAnswer(questionId: String, answer: String, sectionNumber: Int) {
        viewModel.updateAnswer(questionId, answer, sectionNumber)
    }

    fun updateRatingAnswer(questionId: String, rating: Int, sectionNumber: Int) {
        viewModel.updateRatingAnswer(questionId, rating, sectionNumber)
    }

    private fun updateProgressIndicator(position: Int) {
        val progress = ((position + 1) * 100) / onboardingAdapter.itemCount
        binding.progressIndicator.progress = progress
        binding.progressText.text = "${position + 1}/${onboardingAdapter.itemCount}"
    }

    private fun navigateToHome() {
        // After onboarding, go to SMS Processing to extract transaction data
        val intent = Intent(this, SmsProcessingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        val currentItem = binding.viewPager.currentItem
        if (currentItem > 0) {
            binding.viewPager.currentItem = currentItem - 1
            updateProgressIndicator(currentItem - 1)
        } else {
            super.onBackPressed()
        }
    }
}

class OnboardingPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    
    override fun getItemCount(): Int = 4 // Welcome + 3 sections

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> WelcomeFragment()
            1 -> QuickProfileFragment()
            2 -> FinancialHealthFragment()
            3 -> GoalsPrioritiesFragment()
            else -> WelcomeFragment()
        }
    }
}
