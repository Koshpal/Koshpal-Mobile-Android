/**
 * Component-Level Shimmer Implementation
 * For: CategoriesFragment & SetMonthlyBudgetFragment
 * 
 * Copy these methods to your fragment files
 */

// ==================== CATEGORIES FRAGMENT ====================

/**
 * CategoriesFragment.kt - Add these methods
 */

// Shimmer Control Methods

private fun showChartShimmer() {
    _binding?.let { binding ->
        binding.shimmerChart.visibility = View.VISIBLE
        binding.shimmerChart.startShimmer()
        binding.chartContainer.visibility = View.GONE
        binding.chartContainer.alpha = 0f
        android.util.Log.d("CategoriesFragment", "✨ Chart shimmer started")
    }
}

private fun hideChartShimmer() {
    _binding?.let { binding ->
        if (binding.shimmerChart.visibility == View.VISIBLE) {
            android.util.Log.d("CategoriesFragment", "✅ Chart data loaded")
            
            binding.shimmerChart.stopShimmer()
            binding.shimmerChart.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.shimmerChart.visibility = View.GONE
                    binding.shimmerChart.alpha = 1f
                }
                .start()

            binding.chartContainer.visibility = View.VISIBLE
            binding.chartContainer.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }
}

private fun showCategoriesListShimmer() {
    _binding?.let { binding ->
        binding.shimmerCategoriesList.visibility = View.VISIBLE
        binding.shimmerCategoriesList.startShimmer()
        binding.rvCategories.visibility = View.GONE
        binding.rvCategories.alpha = 0f
        android.util.Log.d("CategoriesFragment", "✨ Categories list shimmer started")
    }
}

private fun hideCategoriesListShimmer() {
    _binding?.let { binding ->
        if (binding.shimmerCategoriesList.visibility == View.VISIBLE) {
            android.util.Log.d("CategoriesFragment", "✅ Categories loaded")
            
            binding.shimmerCategoriesList.stopShimmer()
            binding.shimmerCategoriesList.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.shimmerCategoriesList.visibility = View.GONE
                    binding.shimmerCategoriesList.alpha = 1f
                }
                .start()

            binding.rvCategories.visibility = View.VISIBLE
            binding.rvCategories.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }
}

// Update onViewCreated()

override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // Show shimmer initially
    showChartShimmer()
    showCategoriesListShimmer()

    setupRecyclerView()
    setupClickListeners()
    observeCategoryData()
}

// Data Observation

private fun observeCategoryData() {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Replace with your actual data source
            viewModel.categorySpendingData.collect { data ->
                updateCategoryChart(data)
                updateCategoriesList(data)
            }
        }
    }
}

private fun updateCategoryChart(data: CategorySpendingData) {
    if (data.totalSpending > 0) {
        // Update chart
        setupPieChart(data.categories)
        binding.tvTotalSpending.text = "₹${String.format("%.2f", data.totalSpending)}"
        
        // Hide shimmer
        hideChartShimmer()
    }
}

private fun updateCategoriesList(data: CategorySpendingData) {
    when {
        data.categories.isNotEmpty() -> {
            categoryAdapter.submitList(data.categories)
            hideCategoriesListShimmer()
        }
        else -> {
            hideCategoriesListShimmer()
            showEmptyState()
        }
    }
}

// Refresh Handler

fun refreshData() {
    android.util.Log.d("CategoriesFragment", "🔄 Refreshing...")
    
    showChartShimmer()
    showCategoriesListShimmer()
    
    viewModel.refreshCategoryData()
}

// Cleanup

override fun onDestroyView() {
    super.onDestroyView()
    
    _binding?.let { binding ->
        binding.shimmerChart.stopShimmer()
        binding.shimmerCategoriesList.stopShimmer()
    }
    
    _binding = null
}

// ==================== SET BUDGET FRAGMENT ====================

/**
 * SetMonthlyBudgetFragment.kt - Add these methods
 */

// Shimmer Control Methods

private fun showBudgetSummaryShimmer() {
    _binding?.let { binding ->
        binding.shimmerBudgetSummary.visibility = View.VISIBLE
        binding.shimmerBudgetSummary.startShimmer()
        binding.layoutTotalBudget.visibility = View.GONE
        binding.layoutTotalBudget.alpha = 0f
        android.util.Log.d("SetBudgetFragment", "✨ Budget summary shimmer started")
    }
}

private fun hideBudgetSummaryShimmer() {
    _binding?.let { binding ->
        if (binding.shimmerBudgetSummary.visibility == View.VISIBLE) {
            android.util.Log.d("SetBudgetFragment", "✅ Budget summary loaded")
            
            binding.shimmerBudgetSummary.stopShimmer()
            binding.shimmerBudgetSummary.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.shimmerBudgetSummary.visibility = View.GONE
                    binding.shimmerBudgetSummary.alpha = 1f
                }
                .start()

            binding.layoutTotalBudget.visibility = View.VISIBLE
            binding.layoutTotalBudget.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }
}

private fun showBudgetCategoriesShimmer() {
    _binding?.let { binding ->
        binding.shimmerBudgetCategories.visibility = View.VISIBLE
        binding.shimmerBudgetCategories.startShimmer()
        binding.rvCategories.visibility = View.GONE
        binding.rvCategories.alpha = 0f
        android.util.Log.d("SetBudgetFragment", "✨ Budget categories shimmer started")
    }
}

private fun hideBudgetCategoriesShimmer() {
    _binding?.let { binding ->
        if (binding.shimmerBudgetCategories.visibility == View.VISIBLE) {
            android.util.Log.d("SetBudgetFragment", "✅ Budget categories loaded")
            
            binding.shimmerBudgetCategories.stopShimmer()
            binding.shimmerBudgetCategories.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.shimmerBudgetCategories.visibility = View.GONE
                    binding.shimmerBudgetCategories.alpha = 1f
                }
                .start()

            binding.rvCategories.visibility = View.VISIBLE
            binding.rvCategories.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }
}

// Update onViewCreated()

override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // Show shimmer initially
    showBudgetSummaryShimmer()
    showBudgetCategoriesShimmer()

    setupRecyclerView()
    setupClickListeners()
    observeBudgetData()
}

// Data Observation

private fun observeBudgetData() {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Replace with your actual data source
            viewModel.budgetData.collect { data ->
                updateBudgetSummary(data)
                updateBudgetCategories(data)
            }
        }
    }
}

private fun updateBudgetSummary(data: BudgetData) {
    if (data.totalBudget >= 0) {
        binding.tvTotalBudget.text = "₹${String.format("%.2f", data.totalBudget)}"
        hideBudgetSummaryShimmer()
    }
}

private fun updateBudgetCategories(data: BudgetData) {
    if (data.categoryBudgets != null && data.categoryBudgets.isNotEmpty()) {
        budgetAdapter.submitList(data.categoryBudgets)
        hideBudgetCategoriesShimmer()
    } else {
        hideBudgetCategoriesShimmer()
        // Show empty state if needed
    }
}

// Refresh Handler

fun refreshData() {
    android.util.Log.d("SetBudgetFragment", "🔄 Refreshing...")
    
    showBudgetSummaryShimmer()
    showBudgetCategoriesShimmer()
    
    viewModel.refreshBudgetData()
}

// Cleanup

override fun onDestroyView() {
    super.onDestroyView()
    
    _binding?.let { binding ->
        binding.shimmerBudgetSummary.stopShimmer()
        binding.shimmerBudgetCategories.stopShimmer()
    }
    
    _binding = null
}

// ==================== NOTES ====================

/*
IMPORTANT:
1. Replace "viewModel.categorySpendingData" with your actual data source
2. Replace "viewModel.budgetData" with your actual data source
3. Adjust data class names (CategorySpendingData, BudgetData) to match your models
4. Update binding IDs to match your XML after adding FrameLayout wrappers
5. Test all scenarios: initial load, refresh, empty state, navigation

BINDING IDs REQUIRED:
- CategoriesFragment: shimmerChart, shimmerCategoriesList
- SetBudgetFragment: shimmerBudgetSummary, shimmerBudgetCategories

All shimmer controls are data-driven - shimmer stops when data arrives!
*/
