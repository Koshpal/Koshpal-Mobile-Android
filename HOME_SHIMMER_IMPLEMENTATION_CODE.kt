/**
 * HomeFragment - Component-Level Shimmer Implementation
 * 
 * Add these methods to your HomeFragment.kt
 * Replace the global shimmer show/hide methods with these individual component methods
 */

// ==================== SHIMMER CONTROL METHODS ====================

/**
 * Show shimmer for financial overview card (Income + Expense)
 */
private fun showFinancialOverviewShimmer() {
    _binding?.let { binding ->
        binding.shimmerFinancialOverview.visibility = View.VISIBLE
        binding.shimmerFinancialOverview.startShimmer()
        binding.layoutFinancialOverview.visibility = View.GONE
        binding.layoutFinancialOverview.alpha = 0f
        android.util.Log.d("HomeFragment", "✨ Financial overview shimmer started")
    }
}

/**
 * Hide shimmer and show financial overview with smooth fade-in animation
 */
private fun hideFinancialOverviewShimmer() {
    _binding?.let { binding ->
        if (binding.shimmerFinancialOverview.visibility == View.VISIBLE) {
            android.util.Log.d("HomeFragment", "✅ Financial overview loaded - hiding shimmer")
            
            // Stop and fade out shimmer
            binding.shimmerFinancialOverview.stopShimmer()
            binding.shimmerFinancialOverview.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.shimmerFinancialOverview.visibility = View.GONE
                    binding.shimmerFinancialOverview.alpha = 1f
                }
                .start()

            // Show and fade in actual content
            binding.layoutFinancialOverview.visibility = View.VISIBLE
            binding.layoutFinancialOverview.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }
}

/**
 * Show shimmer for recent transactions section
 */
private fun showRecentTransactionsShimmer() {
    _binding?.let { binding ->
        binding.shimmerRecentTransactions.visibility = View.VISIBLE
        binding.shimmerRecentTransactions.startShimmer()
        binding.cardRecentTransactions.visibility = View.GONE
        binding.cardRecentTransactions.alpha = 0f
        android.util.Log.d("HomeFragment", "✨ Recent transactions shimmer started")
    }
}

/**
 * Hide shimmer and show recent transactions with smooth fade-in animation
 */
private fun hideRecentTransactionsShimmer() {
    _binding?.let { binding ->
        if (binding.shimmerRecentTransactions.visibility == View.VISIBLE) {
            android.util.Log.d("HomeFragment", "✅ Recent transactions loaded - hiding shimmer")
            
            // Stop and fade out shimmer
            binding.shimmerRecentTransactions.stopShimmer()
            binding.shimmerRecentTransactions.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.shimmerRecentTransactions.visibility = View.GONE
                    binding.shimmerRecentTransactions.alpha = 1f
                }
                .start()

            // Show and fade in actual content
            binding.cardRecentTransactions.visibility = View.VISIBLE
            binding.cardRecentTransactions.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }
}

// ==================== UPDATE onViewCreated ====================

/**
 * Update your onViewCreated to show component-level shimmers initially
 */
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // Show shimmer on each component initially
    showFinancialOverviewShimmer()
    showRecentTransactionsShimmer()

    setupRecyclerView()
    setupClickListeners()
    observeUIState()
}

// ==================== UPDATE observeUIState ====================

/**
 * Update your UI state observation to hide shimmers when data loads
 */
private fun observeUIState() {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiState.collect { state ->
                updateFinancialOverview(state)
                updateRecentTransactions(state)
            }
        }
    }
}

/**
 * Update financial overview and hide shimmer when data is ready
 */
private fun updateFinancialOverview(state: HomeUiState) {
    // Check if financial data is loaded
    if (state.totalIncome != null && state.totalExpense != null) {
        // Update UI
        binding.tvTotalIncome.text = "₹${NumberFormat.getInstance(Locale("en", "IN"))
            .apply { maximumFractionDigits = 2 }
            .format(state.totalIncome)}"
        
        binding.tvTotalExpense.text = "₹${NumberFormat.getInstance(Locale("en", "IN"))
            .apply { maximumFractionDigits = 2 }
            .format(state.totalExpense)}"
        
        // Hide shimmer - data is loaded
        hideFinancialOverviewShimmer()
        
        android.util.Log.d("HomeFragment", "💰 Financial overview updated: " +
            "Income=₹${state.totalIncome}, Expense=₹${state.totalExpense}")
    }
}

/**
 * Update recent transactions and hide shimmer when data is ready
 */
private fun updateRecentTransactions(state: HomeUiState) {
    when {
        // Data loaded and not empty
        state.recentTransactions != null && state.recentTransactions.isNotEmpty() -> {
            recentTransactionAdapter.submitList(state.recentTransactions)
            hideRecentTransactionsShimmer()
            
            android.util.Log.d("HomeFragment", "📋 Recent transactions updated: " +
                "${state.recentTransactions.size} transactions")
        }
        
        // Data loaded but empty
        state.recentTransactions != null && state.recentTransactions.isEmpty() -> {
            hideRecentTransactionsShimmer()
            showEmptyTransactionsState()
            
            android.util.Log.d("HomeFragment", "📭 No recent transactions")
        }
        
        // Still loading (shimmer remains visible)
        else -> {
            android.util.Log.d("HomeFragment", "⏳ Still loading transactions...")
        }
    }
}

/**
 * Show empty state for transactions (no shimmer, no data)
 */
private fun showEmptyTransactionsState() {
    // Show empty state UI
    // binding.layoutEmptyTransactions.visibility = View.VISIBLE
    // OR set empty message in the card
}

// ==================== REFRESH HANDLING ====================

/**
 * Handle pull-to-refresh or manual refresh
 * Shows shimmer only on components being refreshed
 */
fun refreshData() {
    android.util.Log.d("HomeFragment", "🔄 Refreshing data...")
    
    // Show shimmer on components being refreshed
    showFinancialOverviewShimmer()
    showRecentTransactionsShimmer()

    // Trigger data reload from ViewModel
    viewModel.refreshData()
}

// ==================== OPTIONAL: Smart Caching ====================

/**
 * Conditional shimmer based on data freshness
 * Show shimmer only if data is stale
 */
private fun loadDataWithSmartShimmer() {
    // Check if financial data is stale
    if (viewModel.isFinancialDataStale()) {
        showFinancialOverviewShimmer()
    } else {
        // Show cached data immediately, no shimmer
        val cachedData = viewModel.getCachedFinancialData()
        binding.tvTotalIncome.text = "₹${cachedData.income}"
        binding.tvTotalExpense.text = "₹${cachedData.expense}"
        binding.layoutFinancialOverview.visibility = View.VISIBLE
        binding.layoutFinancialOverview.alpha = 1f
    }

    // Check if transactions are stale
    if (viewModel.isTransactionsDataStale()) {
        showRecentTransactionsShimmer()
    } else {
        // Show cached transactions immediately
        val cachedTransactions = viewModel.getCachedTransactions()
        recentTransactionAdapter.submitList(cachedTransactions)
        binding.cardRecentTransactions.visibility = View.VISIBLE
        binding.cardRecentTransactions.alpha = 1f
    }

    // Load fresh data in background
    viewModel.loadFreshData()
}

// ==================== CLEANUP ====================

/**
 * Stop shimmers when fragment is destroyed
 */
override fun onDestroyView() {
    super.onDestroyView()
    
    // Stop any running shimmers
    _binding?.let { binding ->
        binding.shimmerFinancialOverview.stopShimmer()
        binding.shimmerRecentTransactions.stopShimmer()
    }
    
    _binding = null
}

// ==================== REMOVE THESE OLD METHODS ====================

/*
❌ REMOVE: Global shimmer methods (no longer needed)

private fun showShimmer() { ... }
private fun hideShimmer() { ... }
private var isDataLoaded = false

These are replaced by component-specific methods above
*/
