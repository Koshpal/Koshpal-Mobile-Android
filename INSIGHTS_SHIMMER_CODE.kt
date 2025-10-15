/**
 * InsightsFragment - Component-Level Shimmer Implementation Code
 * 
 * Copy these methods to InsightsFragment.kt
 */

// ==================== SHIMMER CONTROL METHODS ====================

/**
 * Show shimmer for recurring payments card
 */
private fun showRecurringPaymentsShimmer() {
    _binding?.let { binding ->
        binding.shimmerRecurringPayments.visibility = View.VISIBLE
        binding.shimmerRecurringPayments.startShimmer()
        binding.cardRecurringPayments.visibility = View.GONE
        binding.cardRecurringPayments.alpha = 0f
        android.util.Log.d("InsightsFragment", "✨ Recurring payments shimmer started")
    }
}

/**
 * Hide shimmer and show recurring payments with fade-in animation
 */
private fun hideRecurringPaymentsShimmer() {
    _binding?.let { binding ->
        if (binding.shimmerRecurringPayments.visibility == View.VISIBLE) {
            android.util.Log.d("InsightsFragment", "✅ Recurring payments loaded")
            
            // Stop and fade out shimmer
            binding.shimmerRecurringPayments.stopShimmer()
            binding.shimmerRecurringPayments.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.shimmerRecurringPayments.visibility = View.GONE
                    binding.shimmerRecurringPayments.alpha = 1f
                }
                .start()

            // Show and fade in card
            binding.cardRecurringPayments.visibility = View.VISIBLE
            binding.cardRecurringPayments.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }
}

/**
 * Show shimmer for credit merchants card (Money Received From)
 */
private fun showCreditMerchantsShimmer() {
    _binding?.let { binding ->
        binding.shimmerCreditMerchants.visibility = View.VISIBLE
        binding.shimmerCreditMerchants.startShimmer()
        binding.cardCreditMerchants.visibility = View.GONE
        binding.cardCreditMerchants.alpha = 0f
        android.util.Log.d("InsightsFragment", "✨ Credit merchants shimmer started")
    }
}

/**
 * Hide shimmer and show credit merchants with fade-in animation
 */
private fun hideCreditMerchantsShimmer() {
    _binding?.let { binding ->
        if (binding.shimmerCreditMerchants.visibility == View.VISIBLE) {
            android.util.Log.d("InsightsFragment", "✅ Credit merchants loaded")
            
            binding.shimmerCreditMerchants.stopShimmer()
            binding.shimmerCreditMerchants.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.shimmerCreditMerchants.visibility = View.GONE
                    binding.shimmerCreditMerchants.alpha = 1f
                }
                .start()

            binding.cardCreditMerchants.visibility = View.VISIBLE
            binding.cardCreditMerchants.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }
}

/**
 * Show shimmer for debit merchants card (Money Spent On)
 */
private fun showDebitMerchantsShimmer() {
    _binding?.let { binding ->
        binding.shimmerDebitMerchants.visibility = View.VISIBLE
        binding.shimmerDebitMerchants.startShimmer()
        binding.cardDebitMerchants.visibility = View.GONE
        binding.cardDebitMerchants.alpha = 0f
        android.util.Log.d("InsightsFragment", "✨ Debit merchants shimmer started")
    }
}

/**
 * Hide shimmer and show debit merchants with fade-in animation
 */
private fun hideDebitMerchantsShimmer() {
    _binding?.let { binding ->
        if (binding.shimmerDebitMerchants.visibility == View.VISIBLE) {
            android.util.Log.d("InsightsFragment", "✅ Debit merchants loaded")
            
            binding.shimmerDebitMerchants.stopShimmer()
            binding.shimmerDebitMerchants.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.shimmerDebitMerchants.visibility = View.GONE
                    binding.shimmerDebitMerchants.alpha = 1f
                }
                .start()

            binding.cardDebitMerchants.visibility = View.VISIBLE
            binding.cardDebitMerchants.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }
}

// ==================== UPDATE onViewCreated ====================

override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    
    // Show shimmer on all components initially
    showRecurringPaymentsShimmer()
    showCreditMerchantsShimmer()
    showDebitMerchantsShimmer()
    
    setupUI()
    
    // Auto-refresh when transactions flow emits updates
    viewLifecycleOwner.lifecycleScope.launch {
        transactionRepository.getAllTransactions().collect {
            // Clear cache and reload insights when any transaction changes
            cachedTransactions = null
            lastDataLoadTime = 0
            
            // Show shimmer again during refresh
            showRecurringPaymentsShimmer()
            showCreditMerchantsShimmer()
            showDebitMerchantsShimmer()
            
            loadInsightsData()
        }
    }
    
    loadInsightsData()
}

// ==================== UPDATE loadInsightsData ====================

private fun loadInsightsData() {
    viewLifecycleOwner.lifecycleScope.launch {
        try {
            android.util.Log.d("InsightsFragment", "🚀 Loading insights data...")
            
            // Load data on IO dispatcher
            val insightsData = withContext(Dispatchers.IO) {
                val transactions = getAllTransactions()
                
                // Process all insights data
                val recurring = detectRecurringPayments(transactions)
                val creditMerchants = getTopMerchantsByType(transactions, TransactionType.CREDIT)
                val debitMerchants = getTopMerchantsByType(transactions, TransactionType.DEBIT)
                
                Triple(recurring, creditMerchants, debitMerchants)
            }
            
            val (recurring, creditMerchants, debitMerchants) = insightsData
            
            // Update UI on Main thread with shimmer control
            updateRecurringPayments(recurring)
            updateCreditMerchants(creditMerchants)
            updateDebitMerchants(debitMerchants)
            
            android.util.Log.d("InsightsFragment", "✅ Insights data loaded successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("InsightsFragment", "❌ Failed to load insights: ${e.message}", e)
            
            // Hide all shimmers on error
            hideRecurringPaymentsShimmer()
            hideCreditMerchantsShimmer()
            hideDebitMerchantsShimmer()
            
            Toast.makeText(requireContext(), "Failed to load insights", Toast.LENGTH_SHORT).show()
        }
    }
}

// ==================== DATA UPDATE METHODS ====================

/**
 * Update recurring payments list and hide shimmer when data is ready
 */
private fun updateRecurringPayments(items: List<RecurringPaymentItem>) {
    // Update count badge
    binding.tvRecurringCount.text = "${items.size} found"
    
    when {
        items.isNotEmpty() -> {
            recurringPaymentAdapter.submitList(items)
            hideRecurringPaymentsShimmer()
            android.util.Log.d("InsightsFragment", "📋 Recurring payments: ${items.size} items")
        }
        else -> {
            // Show empty state
            hideRecurringPaymentsShimmer()
            recurringPaymentAdapter.submitList(emptyList())
            android.util.Log.d("InsightsFragment", "📭 No recurring payments found")
        }
    }
}

/**
 * Update credit merchants (Money Received From) and hide shimmer when data is ready
 */
private fun updateCreditMerchants(merchants: List<MerchantProgressItem>) {
    when {
        merchants.isNotEmpty() -> {
            topCreditMerchantAdapter.submitList(merchants)
            hideCreditMerchantsShimmer()
            android.util.Log.d("InsightsFragment", "💚 Credit merchants: ${merchants.size} items")
        }
        else -> {
            // Show empty state
            hideCreditMerchantsShimmer()
            topCreditMerchantAdapter.submitList(emptyList())
            android.util.Log.d("InsightsFragment", "📭 No credit merchants found")
        }
    }
}

/**
 * Update debit merchants (Money Spent On) and hide shimmer when data is ready
 */
private fun updateDebitMerchants(merchants: List<MerchantProgressItem>) {
    when {
        merchants.isNotEmpty() -> {
            topDebitMerchantAdapter.submitList(merchants)
            hideDebitMerchantsShimmer()
            android.util.Log.d("InsightsFragment", "🔴 Debit merchants: ${merchants.size} items")
        }
        else -> {
            // Show empty state
            hideDebitMerchantsShimmer()
            topDebitMerchantAdapter.submitList(emptyList())
            android.util.Log.d("InsightsFragment", "📭 No debit merchants found")
        }
    }
}

// ==================== REFRESH HANDLING ====================

/**
 * Refresh insights data (call when user manually refreshes)
 */
fun refreshInsightsData() {
    android.util.Log.d("InsightsFragment", "🔄 Refreshing insights data...")
    
    // Show shimmer on all components
    showRecurringPaymentsShimmer()
    showCreditMerchantsShimmer()
    showDebitMerchantsShimmer()
    
    // Clear cache
    cachedTransactions = null
    lastDataLoadTime = 0
    
    // Reload data
    loadInsightsData()
}

// ==================== CLEANUP ====================

override fun onDestroyView() {
    super.onDestroyView()
    
    // Stop all shimmers
    _binding?.let { binding ->
        binding.shimmerRecurringPayments.stopShimmer()
        binding.shimmerCreditMerchants.stopShimmer()
        binding.shimmerDebitMerchants.stopShimmer()
    }
    
    _binding = null
}

// ==================== OPTIONAL: SMART CACHING ====================

/**
 * Get transactions with smart caching
 */
private suspend fun getAllTransactions(): List<com.koshpal_android.koshpalapp.model.Transaction> {
    val currentTime = System.currentTimeMillis()
    
    // Check if cache is still valid (within 5 minutes)
    if (cachedTransactions != null && 
        (currentTime - lastDataLoadTime) < DATA_CACHE_DURATION) {
        android.util.Log.d("InsightsFragment", "📦 Using cached transactions")
        return cachedTransactions!!
    }
    
    // Fetch fresh data
    val database = KoshpalDatabase.getDatabase(requireContext())
    val transactions = database.transactionDao().getAllTransactionsOnce()
    
    // Update cache
    cachedTransactions = transactions
    lastDataLoadTime = currentTime
    
    android.util.Log.d("InsightsFragment", "🔄 Fetched ${transactions.size} transactions from database")
    
    return transactions
}

// ==================== NOTES ====================

/*
BINDING IDS REQUIRED IN XML:
- shimmerRecurringPayments (ShimmerFrameLayout)
- shimmerCreditMerchants (ShimmerFrameLayout)
- shimmerDebitMerchants (ShimmerFrameLayout)

EXISTING BINDING IDS TO KEEP:
- cardRecurringPayments (MaterialCardView)
- cardCreditMerchants (MaterialCardView)
- cardDebitMerchants (MaterialCardView)
- rvRecurringPayments (RecyclerView)
- rvTopCreditMerchants (RecyclerView)
- rvTopDebitMerchants (RecyclerView)
- tvRecurringCount (TextView)

DATA FLOW:
1. Fragment opens → Show shimmer on all 3 cards
2. Fetch data asynchronously
3. Recurring payments ready → hideRecurringPaymentsShimmer()
4. Credit merchants ready → hideCreditMerchantsShimmer()
5. Debit merchants ready → hideDebitMerchantsShimmer()
6. Each card fades in independently (300ms)

REFRESH BEHAVIOR:
- When transaction changes are detected (Flow emission)
- Shimmer reappears on all cards
- Data reloads
- Cards fade in as data becomes available

All shimmer controls are data-driven - shimmer stops when data arrives!
*/
