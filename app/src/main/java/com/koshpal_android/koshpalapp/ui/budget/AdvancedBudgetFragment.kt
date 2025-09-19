package com.koshpal_android.koshpalapp.ui.budget

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.koshpal_android.koshpalapp.databinding.FragmentAdvancedBudgetBinding
import com.koshpal_android.koshpalapp.ui.budget.model.*
import com.koshpal_android.koshpalapp.ui.budget.custom.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Revolutionary Advanced Budget Fragment - The most intuitive budgeting experience
 */
@AndroidEntryPoint
class AdvancedBudgetFragment : Fragment() {

    private var _binding: FragmentAdvancedBudgetBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AdvancedBudgetViewModel by viewModels()
    
    // Animation controllers
    private var heroAnimator: ValueAnimator? = null
    private var celebrationAnimator: ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdvancedBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupInteractions()
        observeViewModel()
    }

    private fun setupUI() {
        // Setup Financial Universe
        setupFinancialUniverse()
        
        // Setup Tier Cards
        setupTierCards()
        
        // Setup Hero Section
        setupHeroSection()
        
        // Setup What-If Scenario Planner
        setupScenarioPlanner()
        
        // Setup Quick Actions
        setupQuickActions()
    }

    private fun setupFinancialUniverse() {
        binding.financialUniverse.apply {
            onPlanetClickListener = { category ->
                showCategoryDrillDown(category)
            }
            
            onPlanetLongClickListener = { category ->
                showQuickExpenseEntry(category)
            }
        }
        
        // Universe controls
        binding.btnUniverseReset.setOnClickListener {
            binding.financialUniverse.setUniverseData(FinancialUniverseData())
            animateUniverseReset()
        }
        
        binding.btnUniverseSettings.setOnClickListener {
            // Show universe customization options
            showUniverseSettings()
        }
    }

    private fun setupTierCards() {
        // Setup tier card interactions
        binding.essentialsTier.onTierClickListener = { tier ->
            expandTierDetails(tier)
        }
        
        binding.wantsTier.onTierClickListener = { tier ->
            expandTierDetails(tier)
        }
        
        binding.goalsTier.onTierClickListener = { tier ->
            expandTierDetails(tier)
        }
    }

    private fun setupHeroSection() {
        // Hero section will be updated via data binding
        // Add click listener for detailed breakdown
        binding.heroSection.setOnClickListener {
            showDetailedBreakdown()
        }
    }

    private fun setupScenarioPlanner() {
        binding.btnStartScenario.setOnClickListener {
            showWhatIfScenarioModal()
        }
        
        // Expand/collapse insights
        binding.btnExpandInsights.setOnClickListener {
            toggleInsightsExpansion()
        }
    }

    private fun setupQuickActions() {
        binding.fabQuickExpense.setOnClickListener {
            showQuickExpenseModal()
        }
        
        // Extended FAB animation
        binding.fabQuickExpense.extend()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: AdvancedBudgetUiState) {
        // Handle loading state
        if (state.isLoading) {
            showLoadingState()
            return
        }
        
        // Handle error state
        state.errorMessage?.let { error ->
            showError(error)
            return
        }
        
        // Update Hero Section
        updateHeroSection(state)
        
        // Update Financial Universe
        updateFinancialUniverse(state.universeData)
        
        // Update Tier Breakdown
        updateTierBreakdown(state.tierBreakdown)
        
        // Update Smart Insights
        updateSmartInsights(state.insights)
        
        // Update Predictive Alerts
        updatePredictiveAlerts(state.predictiveAlerts)
        
        // Update Subscription Tracker
        updateSubscriptionTracker(state.detectedSubscriptions, state.upcomingPayments)
        
        // Handle animations
        handleAnimations(state.animationStates)
        
        // Handle scenario mode
        if (state.isScenarioMode) {
            showScenarioMode(state.activeScenario)
        }
    }

    private fun updateHeroSection(state: AdvancedBudgetUiState) {
        // Update remaining budget with animation
        val remainingText = "₹${String.format("%.0f", state.remainingBudget)}"
        binding.tvRemainingBudget.text = remainingText
        
        // Update health score
        val healthText = when {
            state.budgetHealthScore >= 0.8f -> "Excellent"
            state.budgetHealthScore >= 0.6f -> "Good"
            state.budgetHealthScore >= 0.4f -> "Fair"
            state.budgetHealthScore >= 0.2f -> "Poor"
            else -> "Critical"
        }
        binding.tvHealthScore.text = healthText
        
        // Update month progress
        val daysLeft = ((1f - state.monthProgress) * 30f).toInt()
        binding.tvMonthProgress.text = "$daysLeft days left"
        
        // Update liquid progress bar
        binding.liquidProgress.setProgress(state.monthProgress, true)
        
        // Animate hero section entrance
        animateHeroSection()
    }

    private fun updateFinancialUniverse(universeData: FinancialUniverseData) {
        binding.financialUniverse.setUniverseData(universeData)
    }

    private fun updateTierBreakdown(tierBreakdown: Map<BudgetTier, TierData>) {
        tierBreakdown[BudgetTier.ESSENTIALS]?.let { tierData ->
            binding.essentialsTier.setTierData(tierData)
        }
        
        tierBreakdown[BudgetTier.WANTS]?.let { tierData ->
            binding.wantsTier.setTierData(tierData)
        }
        
        tierBreakdown[BudgetTier.GOALS]?.let { tierData ->
            binding.goalsTier.setTierData(tierData)
        }
    }

    private fun updateSmartInsights(insights: List<SmartInsight>) {
        // Update insights RecyclerView
        // For now, show count
        if (insights.isNotEmpty()) {
            binding.insightsCard.visibility = View.VISIBLE
            // Setup insights adapter here
        } else {
            binding.insightsCard.visibility = View.GONE
        }
    }

    private fun updatePredictiveAlerts(alerts: List<PredictiveAlert>) {
        // Show alerts as snackbars or in a dedicated section
        alerts.forEach { alert ->
            if (alert.alertType == AlertType.BUDGET_EXCEEDED) {
                showCriticalAlert(alert)
            }
        }
    }

    private fun updateSubscriptionTracker(
        subscriptions: List<DetectedSubscription>,
        upcomingPayments: List<UpcomingPayment>
    ) {
        binding.tvSubscriptionCount.text = "${subscriptions.size} active"
        
        // Update subscriptions RecyclerView
        // Setup subscription adapter here
    }

    private fun handleAnimations(animationStates: AnimationStates) {
        // Handle celebration animations
        animationStates.celebrationAnimation?.let { celebration ->
            when (celebration.type) {
                CelebrationType.GOAL_COMPLETED -> showGoalCompletedCelebration()
                CelebrationType.BUDGET_SAVED -> showBudgetSavedCelebration()
                CelebrationType.MILESTONE_REACHED -> showMilestoneCelebration()
            }
        }
        
        // Handle other animations
        if (animationStates.isHeroAnimating) {
            animateHeroSection()
        }
    }

    private fun animateHeroSection() {
        heroAnimator?.cancel()
        heroAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                binding.heroSection.alpha = progress
                binding.heroSection.translationY = (1f - progress) * 50f
            }
            start()
        }
    }

    private fun showGoalCompletedCelebration() {
        // Create confetti animation
        celebrationAnimator?.cancel()
        celebrationAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3000L
            addUpdateListener { animation ->
                // Implement confetti particle system
                val progress = animation.animatedValue as Float
                // Update confetti positions and alpha
            }
            start()
        }
        
        // Show congratulations message
        Snackbar.make(
            binding.root,
            "🎉 Congratulations! You've reached a savings goal!",
            Snackbar.LENGTH_LONG
        ).setAction("View Details") {
            // Show goal details
        }.show()
    }

    private fun showBudgetSavedCelebration() {
        // Animate savings achievement
        binding.liquidProgress.setLiquidColor(
            resources.getColor(android.R.color.holo_green_light, null)
        )
    }

    private fun showMilestoneCelebration() {
        // Show milestone achievement animation
    }

    private fun showCategoryDrillDown(category: BudgetCategory) {
        // Show detailed category analysis
        binding.categoryDrillDown.visibility = View.VISIBLE
        
        // Animate slide up
        binding.categoryDrillDown.translationY = binding.categoryDrillDown.height.toFloat()
        binding.categoryDrillDown.animate()
            .translationY(0f)
            .setDuration(300L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun showQuickExpenseEntry(category: BudgetCategory) {
        // Show quick expense entry modal
        binding.quickExpenseModal.visibility = View.VISIBLE
        
        // Animate modal appearance
        binding.quickExpenseModal.alpha = 0f
        binding.quickExpenseModal.animate()
            .alpha(1f)
            .setDuration(200L)
            .start()
    }

    private fun expandTierDetails(tier: BudgetTier) {
        // Expand tier to show detailed breakdown
        when (tier) {
            BudgetTier.ESSENTIALS -> {
                // Show essentials breakdown
            }
            BudgetTier.WANTS -> {
                // Show wants breakdown
            }
            BudgetTier.GOALS -> {
                // Show goals breakdown
            }
        }
    }

    private fun showWhatIfScenarioModal() {
        binding.scenarioModal.visibility = View.VISIBLE
        
        // Animate modal slide up
        binding.scenarioModal.translationY = binding.scenarioModal.height.toFloat()
        binding.scenarioModal.animate()
            .translationY(0f)
            .setDuration(400L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun showQuickExpenseModal() {
        binding.quickExpenseModal.visibility = View.VISIBLE
        
        // Animate modal appearance with scale
        binding.quickExpenseModal.scaleX = 0.8f
        binding.quickExpenseModal.scaleY = 0.8f
        binding.quickExpenseModal.alpha = 0f
        
        binding.quickExpenseModal.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(250L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun toggleInsightsExpansion() {
        val isExpanded = binding.rvInsights.visibility == View.VISIBLE
        
        if (isExpanded) {
            // Collapse insights
            binding.rvInsights.animate()
                .alpha(0f)
                .setDuration(200L)
                .withEndAction {
                    binding.rvInsights.visibility = View.GONE
                }
                .start()
            
            binding.btnExpandInsights.animate()
                .rotation(0f)
                .setDuration(200L)
                .start()
        } else {
            // Expand insights
            binding.rvInsights.visibility = View.VISIBLE
            binding.rvInsights.alpha = 0f
            binding.rvInsights.animate()
                .alpha(1f)
                .setDuration(300L)
                .start()
            
            binding.btnExpandInsights.animate()
                .rotation(180f)
                .setDuration(200L)
                .start()
        }
    }

    private fun showDetailedBreakdown() {
        // Show detailed budget breakdown modal
    }

    private fun showUniverseSettings() {
        // Show universe customization options
    }

    private fun animateUniverseReset() {
        // Animate universe reset
        binding.financialUniverse.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .alpha(0.5f)
            .setDuration(300L)
            .withEndAction {
                binding.financialUniverse.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(300L)
                    .start()
            }
            .start()
    }

    private fun showScenarioMode(scenario: WhatIfScenario?) {
        // Update UI for scenario mode
        scenario?.let {
            // Show scenario overlay
            // Update values with scenario projections
        }
    }

    private fun showLoadingState() {
        // Show loading indicators
        binding.heroSection.alpha = 0.5f
        binding.financialUniverse.alpha = 0.5f
    }

    private fun showError(error: String) {
        Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
            .setAction("Retry") {
                // Retry loading data
                viewModel.clearError()
            }
            .show()
    }

    private fun showCriticalAlert(alert: PredictiveAlert) {
        Snackbar.make(binding.root, alert.message, Snackbar.LENGTH_INDEFINITE)
            .setAction("View Details") {
                showCategoryDrillDown(alert.category)
            }
            .setBackgroundTint(resources.getColor(android.R.color.holo_red_light, null))
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        heroAnimator?.cancel()
        celebrationAnimator?.cancel()
        _binding = null
    }
}
