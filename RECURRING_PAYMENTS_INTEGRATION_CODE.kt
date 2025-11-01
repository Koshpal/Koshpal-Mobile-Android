// ==================== InsightsFragment Integration Code ====================
// Add these methods to InsightsFragment.kt to integrate the refactored recurring payments

// 1. UPDATE ADAPTER DECLARATION (replace old adapter)
private lateinit var recurringPaymentEnhancedAdapter: RecurringPaymentEnhancedAdapter

// 2. UPDATE setupUI() method
private fun setupUI() {
    binding.apply {
        // ... existing code ...
        
        // Setup enhanced recurring payments adapter
        recurringPaymentEnhancedAdapter = RecurringPaymentEnhancedAdapter()
        rvRecurringPayments.layoutManager = LinearLayoutManager(requireContext())
        rvRecurringPayments.adapter = recurringPaymentEnhancedAdapter
        
        // ... rest of existing code ...
    }
    
    // Observe ViewModel data
    observeViewModel()
    observeRecurringPayments() // NEW
}

// 3. ADD NEW METHOD: observeRecurringPayments()
private fun observeRecurringPayments() {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Observe recurring payments list
            launch {
                viewModel.recurringPayments.collect { payments ->
                    recurringPaymentEnhancedAdapter.submitList(payments)
                    
                    // Update count badge
                    binding.tvRecurringCount.text = "${payments.size} found"
                }
            }
            
            // Observe recurring insights
            launch {
                viewModel.recurringInsights.collect { insights ->
                    insights?.let { renderRecurringInsights(it) }
                }
            }
            
            // Observe loading state
            launch {
                viewModel.isLoadingRecurring.collect { isLoading ->
                    if (isLoading) {
                        showRecurringShimmer()
                    } else {
                        hideRecurringShimmer()
                    }
                }
            }
        }
    }
}

// 4. ADD NEW METHOD: renderRecurringInsights()
private fun renderRecurringInsights(insights: RecurringPaymentsInsight) {
    // Find views (assumes you've added card_recurring_insights to layout)
    val insightsCard = binding.root.findViewById<View>(R.id.cardRecurringInsights) ?: return
    val tvSummary = insightsCard.findViewById<TextView>(R.id.tvInsightSummary)
    val tvCount = insightsCard.findViewById<TextView>(R.id.tvTotalCount)
    val tvSpend = insightsCard.findViewById<TextView>(R.id.tvTotalSpend)
    val cardSavings = insightsCard.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSavingsSuggestion)
    val tvSavings = insightsCard.findViewById<TextView>(R.id.tvSavingsSuggestion)
    
    // Set data
    tvSummary.text = insights.insightText
    tvCount.text = insights.totalRecurringCount.toString()
    tvSpend.text = "₹${String.format("%.0f", insights.totalMonthlySpend)}"
    
    // Show/hide savings suggestion
    if (insights.potentialSavings > 0 && insights.savingsSuggestion.isNotEmpty()) {
        cardSavings.visibility = View.VISIBLE
        tvSavings.text = insights.savingsSuggestion
        
        // Animate in
        cardSavings.alpha = 0f
        cardSavings.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    } else {
        cardSavings.visibility = View.GONE
    }
}

// 5. ADD SHIMMER METHODS (using existing shimmer pattern)
private fun showRecurringShimmer() {
    // Assumes you have shimmer_recurring_payments in FrameLayout
    val shimmer = binding.root.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(
        R.id.shimmerRecurringPayments
    ) ?: return
    
    shimmer.visibility = View.VISIBLE
    shimmer.startShimmer()
    binding.rvRecurringPayments.visibility = View.GONE
}

private fun hideRecurringShimmer() {
    val shimmer = binding.root.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(
        R.id.shimmerRecurringPayments
    ) ?: return
    
    shimmer.stopShimmer()
    shimmer.animate()
        .alpha(0f)
        .setDuration(200)
        .withEndAction {
            shimmer.visibility = View.GONE
            shimmer.alpha = 1f
            
            binding.rvRecurringPayments.alpha = 0f
            binding.rvRecurringPayments.visibility = View.VISIBLE
            binding.rvRecurringPayments.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
        .start()
}

// 6. UPDATE onViewCreated() - add ViewModel load call
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    
    setupUI()
    
    // Existing loads
    viewLifecycleOwner.lifecycleScope.launch {
        transactionRepository.getAllTransactions().collect {
            cachedTransactions = null
            lastDataLoadTime = 0
            loadInsightsData()
        }
    }
    
    loadInsightsData()
    viewModel.loadMonthComparisonData()
    viewModel.loadRecurringPayments() // NEW - Load recurring payments
}

// 7. UPDATE refreshAllData() to reload recurring payments
private fun refreshAllData() {
    cachedTransactions = null
    lastDataLoadTime = 0
    loadInsightsData()
    viewModel.loadMonthComparisonData()
    viewModel.loadRecurringPayments() // NEW
    binding.swipeRefresh?.isRefreshing = false
}

// 8. REMOVE OLD METHODS (if they exist)
// - detectRecurringPayments() - now in ViewModel
// - normalizeMerchantName() - now in ViewModel
// - calculateFrequency() - now in ViewModel
// - calculateSubscriptionScore() - now in ViewModel
// - getTimelineData() - replaced with month comparison
// - markRecurringAsEssential() - remove if not needed
// - showCancelSuggestion() - remove if not needed
// - markRecurringAsReimbursable() - remove if not needed

// ==================== END OF INTEGRATION CODE ====================

// LAYOUT UPDATES NEEDED in fragment_insights.xml:
/*
Replace the old recurring payments section with:

<!-- Smart Insights Card -->
<include
    android:id="@+id/cardRecurringInsights"
    layout="@layout/card_recurring_insights"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="16dp" />

<!-- Recurring Payments List (wrapped in FrameLayout for shimmer) -->
<FrameLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="24dp">
    
    <!-- Shimmer Placeholder -->
    <com.facebook.shimmer.ShimmerFrameLayout
        android:id="@+id/shimmerRecurringPayments"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:visibility="visible"
        app:shimmer_auto_start="true"
        app:shimmer_base_alpha="0.7"
        app:shimmer_direction="left_to_right"
        app:shimmer_duration="1500">
        
        <include layout="@layout/shimmer_recurring_payments" />
    </com.facebook.shimmer.ShimmerFrameLayout>
    
    <!-- Actual RecyclerView -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvRecurringPayments"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:nestedScrollingEnabled="false"
        android:clipToPadding="false"
        android:visibility="gone" />
</FrameLayout>
*/
