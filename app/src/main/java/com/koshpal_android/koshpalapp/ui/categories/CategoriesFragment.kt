package com.koshpal_android.koshpalapp.ui.categories

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.koshpal_android.koshpalapp.ui.budget.BudgetDetailsChartHelper
import com.koshpal_android.koshpalapp.databinding.FragmentCategoriesBinding
import com.koshpal_android.koshpalapp.model.CategorySpending
import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import com.koshpal_android.koshpalapp.ui.categories.adapter.CategorySpendingAdapter
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.github.mikephil.charting.utils.MPPointF

@AndroidEntryPoint
class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var transactionRepository: TransactionRepository

    private lateinit var categorySpendingAdapter: CategorySpendingAdapter
    
    // Month selection properties
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) // 0-based (0=Jan, 11=Dec)
    
    // Flag to track if refresh is pending (view not created yet)
    private var pendingRefresh = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupMonthPicker()
        setupSetBudgetButton()
        updateMonthDisplay()
        
        // Check if refresh was requested before view was created
        if (pendingRefresh) {
            android.util.Log.d("CategoriesFragment", "🔄 View created with pending refresh - loading data now")
            pendingRefresh = false
            loadCategoryData()
        } else if (!isHidden) {
            // Don't load data immediately if fragment is hidden - wait until it becomes visible
            // This prevents loading stale data before refresh can happen
            android.util.Log.d("CategoriesFragment", "🔄 Fragment visible on create - loading data")
            loadCategoryData()
        } else {
            android.util.Log.d("CategoriesFragment", "⏸️ Fragment hidden on create - skipping initial load")
        }
    }

    private fun setupRecyclerView() {
        categorySpendingAdapter = CategorySpendingAdapter(
            onSetBudgetClick = { categorySpending ->
                // Handle set budget click
                val category = TransactionCategory.getDefaultCategories()
                    .find { it.id == categorySpending.categoryId }
                val categoryName = category?.name ?: "Unknown Category"
                android.widget.Toast.makeText(
                    requireContext(),
                    "Set budget for $categoryName",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            onCategoryClick = { categorySpending ->
                // Navigate to category details
                val category = TransactionCategory.getDefaultCategories()
                    .find { it.id == categorySpending.categoryId }
                if (category != null) {
                    (activity as? HomeActivity)?.showCategoryDetailsFragment(
                        categoryId = category.id,
                        categoryName = category.name,
                        categoryIcon = category.icon,
                        month = selectedMonth,
                        year = selectedYear
                    )
                }
            }
        )

        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categorySpendingAdapter
        }
    }

    private fun setupPieChart() {
        // Chart setup is now handled by BudgetDetailsChartHelper
        // This method is kept for backward compatibility but does nothing
    }


    private fun loadCategoryData() {
        android.util.Log.d("CategoriesFragment", "🚀 ===== loadCategoryData() STARTED =====")
        android.util.Log.d("CategoriesFragment", "🔍 Context: context=${context != null}, view=${view != null}, lifecycle=${lifecycle.currentState}")
        
        // Critical check: Don't try to load if view isn't created yet
        if (_binding == null) {
            android.util.Log.w("CategoriesFragment", "⚠️ Binding is null! View not created yet. Skipping load.")
            return
        }
        
        // DEBUG: Check all transactions in database
        lifecycleScope.launch {
            try {
                val allTransactions = transactionRepository.getAllTransactionsOnce()
                android.util.Log.d("CategoriesFragment", "📊 DEBUG: Total transactions in DB: ${allTransactions.size}")
                
                val categorizedTxns = allTransactions.filter { !it.categoryId.isNullOrEmpty() && it.categoryId != "" }
                android.util.Log.d("CategoriesFragment", "📊 DEBUG: Categorized transactions: ${categorizedTxns.size}")
                
                val byCategoryMap = categorizedTxns.groupBy { it.categoryId }
                byCategoryMap.forEach { (catId, txns) ->
                    val totalAmount = txns.filter { it.type == com.koshpal_android.koshpalapp.model.TransactionType.DEBIT }.sumOf { it.amount }
                    android.util.Log.d("CategoriesFragment", "   📌 Category '$catId': ${txns.size} txns, ₹$totalAmount")
                }
            } catch (e: Exception) {
                android.util.Log.e("CategoriesFragment", "❌ Debug check failed: ${e.message}")
            }
        }
        
        // Use lifecycleScope but ensure it's actually running
        lifecycleScope.launch {
            android.util.Log.d("CategoriesFragment", "🏃 Coroutine started in lifecycleScope")
            try {
                android.util.Log.d("CategoriesFragment", "🔄 Loading category data...")

                // Debug: Check what transactions are actually in the database
                transactionRepository.debugCategorizedTransactions()

                // Clean up any test/dummy data first
                val deletedTestTransactions = transactionRepository.deleteTestTransactions()
                if (deletedTestTransactions > 0) {
                    android.util.Log.d(
                        "CategoriesFragment",
                        "🧹 Cleaned up $deletedTestTransactions test transactions"
                    )
                }
                
                // Only reset on first load, not every time Categories screen opens
                // This prevents overriding user categorizations

                // Calculate selected month date range
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.YEAR, selectedYear)
                calendar.set(Calendar.MONTH, selectedMonth)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis

                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val endOfMonth = calendar.timeInMillis

                android.util.Log.d(
                    "CategoriesFragment",
                    "📅 Querying selected month ($selectedYear-${selectedMonth + 1}) category spending from ${Date(startOfMonth)} to ${Date(endOfMonth)}"
                )

                // Get SELECTED MONTH category spending only
                android.util.Log.d("CategoriesFragment", "📊 ===== QUERYING CATEGORY SPENDING =====")
                android.util.Log.d("CategoriesFragment", "📅 Date Range: $startOfMonth to $endOfMonth")
                
                val selectedMonthCategorySpending = transactionRepository.getCurrentMonthCategorySpending(startOfMonth, endOfMonth)
                android.util.Log.d("CategoriesFragment", "📊 Selected month category spending: ${selectedMonthCategorySpending.size} categories")
                
                if (selectedMonthCategorySpending.isEmpty()) {
                    android.util.Log.w("CategoriesFragment", "⚠️ NO CATEGORY SPENDING DATA FOR SELECTED MONTH!")
                    android.util.Log.w("CategoriesFragment", "⚠️ This could mean:")
                    android.util.Log.w("CategoriesFragment", "   1. No transactions in this month")
                    android.util.Log.w("CategoriesFragment", "   2. Transactions exist but categoryId is null/empty")
                    android.util.Log.w("CategoriesFragment", "   3. Database query is not finding the data")
                }
                
                // Log the amounts for debugging
                selectedMonthCategorySpending.forEach { spending ->
                    val categoryName = TransactionCategory.getDefaultCategories().find { it.id == spending.categoryId }?.name ?: "Unknown"
                    android.util.Log.d("CategoriesFragment", "   💰 ${spending.categoryId} ('$categoryName') -> ₹${spending.totalAmount}")
                }
                
                var categorySpending = selectedMonthCategorySpending

                // FALLBACK: If current month has no data, check all-time spending for debugging
                if (categorySpending.isEmpty()) {
                    android.util.Log.w("CategoriesFragment", "⚠️ No data for current month, checking all-time spending...")
                    val allTimeSpending = transactionRepository.getAllTimeCategorySpending()
                    android.util.Log.d("CategoriesFragment", "📊 All-time category spending: ${allTimeSpending.size} categories")
                    
                    if (allTimeSpending.isNotEmpty()) {
                        android.util.Log.d("CategoriesFragment", "✅ Using all-time data instead")
                        allTimeSpending.forEach { spending ->
                            val categoryName = TransactionCategory.getDefaultCategories().find { it.id == spending.categoryId }?.name ?: "Unknown"
                            android.util.Log.d("CategoriesFragment", "   💰 ${spending.categoryId} ('$categoryName') -> ₹${spending.totalAmount}")
                        }
                        categorySpending = allTimeSpending
                    }
                }

                // Show ALL categories including "others" - this gives users a complete view
                android.util.Log.d("CategoriesFragment", "📊 Final categories to display: ${categorySpending.size}")
                
                if (categorySpending.isNotEmpty()) {
                    android.util.Log.d("CategoriesFragment", "✅ Showing category data")
                    updatePieChart(categorySpending)
                    updateCategoryList(categorySpending)
                    updateTotalSpending(categorySpending)
                    showDataViews()
                } else {
                    android.util.Log.d("CategoriesFragment", "📊 No transactions found in database")
                    showEmptyState()
                }

            } catch (e: Exception) {
                android.util.Log.e("CategoriesFragment", "Failed to load category data: ${e.message}")
                showEmptyState()
            }
        }
    }

    private fun updatePieChart(categorySpending: List<CategorySpending>) {
        // Convert to modern chart data format
        val chartData = categorySpending.map { spending ->
            // Get category from default categories
            val category = TransactionCategory.getDefaultCategories()
                .find { it.id == spending.categoryId }
            val categoryName = category?.name ?: "Unknown"
            
            // Get category color
            val color = try {
                Color.parseColor(category?.color ?: "#6750A4")
            } catch (e: Exception) {
                Color.parseColor("#6750A4")
            }
            
            BudgetDetailsChartHelper.CategoryData(
                label = categoryName,
                amount = spending.totalAmount,
                color = color
            )
        }
        
        // Use modern donut chart with outside labels
        BudgetDetailsChartHelper.setupModernDonutChart(
            chart = binding.pieChart,
            data = chartData
        )
    }

    private fun updateCategoryList(categorySpending: List<CategorySpending>) {
        lifecycleScope.launch {
            try {
                // Get budget categories if budget exists
                val budget = transactionRepository.getSingleBudget()
                val budgetCategories = if (budget != null) {
                    transactionRepository.getBudgetCategoriesForBudget(budget.id)
                } else {
                    emptyList()
                }
                
                // Get transaction counts for each category
                val calendar = Calendar.getInstance()
                calendar.set(selectedYear, selectedMonth, 1, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis
                
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val endOfMonth = calendar.timeInMillis
                
                // Combine spending with budget info and transaction counts
                val combinedData = categorySpending.map { spending ->
                    // Find matching budget category
                    val budgetCat = budgetCategories.find { 
                        it.name.equals(
                            TransactionCategory.getDefaultCategories()
                                .find { cat -> cat.id == spending.categoryId }?.name,
                            ignoreCase = true
                        )
                    }
                    
                    // Get transaction count for this category
                    val transactionCount = transactionRepository.getTransactionCountByCategory(
                        categoryId = spending.categoryId,
                        startDate = startOfMonth,
                        endDate = endOfMonth
                    )
                    
                    com.koshpal_android.koshpalapp.ui.categories.adapter.CategorySpendingWithBudget(
                        categorySpending = spending,
                        budgetCategory = budgetCat,
                        transactionCount = transactionCount
                    )
                }
                
                categorySpendingAdapter.submitList(combinedData)
            } catch (e: Exception) {
                android.util.Log.e("CategoriesFragment", "Error updating category list: ${e.message}", e)
                // Fallback to showing spending without budget data
                val fallbackData = categorySpending.map {
                    com.koshpal_android.koshpalapp.ui.categories.adapter.CategorySpendingWithBudget(
                        categorySpending = it,
                        budgetCategory = null,
                        transactionCount = 1
                    )
                }
                categorySpendingAdapter.submitList(fallbackData)
            }
        }
    }

    private fun updateTotalSpending(categorySpending: List<CategorySpending>) {
        val total = categorySpending.sumOf { it.totalAmount }
        binding.tvTotalSpending.text = "₹${String.format("%.0f", total)}"
    }

    private fun showDataViews() {
        binding.chartContainer.visibility = View.VISIBLE
        binding.btnSetBudget.visibility = View.VISIBLE
        binding.rvCategories.visibility = View.VISIBLE
        binding.layoutEmptyState.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.chartContainer.visibility = View.GONE
        binding.btnSetBudget.visibility = View.GONE
        binding.rvCategories.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.VISIBLE
    }


    override fun onResume() {
        super.onResume()
        android.util.Log.d("CategoriesFragment", "🔄 onResume - refreshing category data")
        // Refresh data when fragment becomes visible
        loadCategoryData()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            android.util.Log.d("CategoriesFragment", "👁️ Fragment became visible - refreshing data")
            android.util.Log.d("CategoriesFragment", "🔍 Pending refresh flag: $pendingRefresh")
            // Fragment became visible - refresh data
            if (pendingRefresh) {
                android.util.Log.d("CategoriesFragment", "✅ Processing pending refresh")
                pendingRefresh = false
            }
            loadCategoryData()
        }
    }
    
    /**
     * Public method to force refresh category data
     * Call this after transactions are categorized
     */
    fun refreshCategoryData() {
        android.util.Log.d("CategoriesFragment", "🔄 Manual refresh requested - reloading category data")
        android.util.Log.d("CategoriesFragment", "🔍 Fragment state: isAdded=${isAdded}, isVisible=${isVisible}, isHidden=${isHidden}, view=${view != null}, binding=${_binding != null}")
        
        // If view isn't created yet, set pending flag
        if (_binding == null) {
            android.util.Log.w("CategoriesFragment", "⏸️ View not created yet - setting pending refresh flag")
            pendingRefresh = true
            return
        }
        
        // Force reload
        loadCategoryData()
    }

    private fun setupMonthPicker() {
        binding.tvMonth.setOnClickListener {
            showMonthPickerDialog()
        }
    }

    private fun setupSetBudgetButton() {
        binding.btnSetBudget.setOnClickListener {
            (activity as? HomeActivity)?.showSetMonthlyBudgetFragment()
        }
    }

    private fun updateMonthDisplay() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, selectedYear)
        calendar.set(Calendar.MONTH, selectedMonth)
        
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        binding.tvMonth.text = monthFormat.format(calendar.time)
        
        android.util.Log.d("CategoriesFragment", "📅 Month display updated: ${binding.tvMonth.text}")
    }

    private fun showMonthPickerDialog() {
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        
        val years = (2020..2030).map { it.toString() }.toTypedArray()
        
        // Create month-year options
        val monthYearOptions = mutableListOf<String>()
        val currentCalendar = Calendar.getInstance()
        val currentYear = currentCalendar.get(Calendar.YEAR)
        val currentMonth = currentCalendar.get(Calendar.MONTH)
        
        // Add months from 2023 to current date
        for (year in 2023..currentYear) {
            val endMonth = if (year == currentYear) currentMonth else 11
            for (month in 0..endMonth) {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                val monthYearText = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
                monthYearOptions.add(monthYearText)
            }
        }
        
        // Find current selection index
        val currentSelectionText = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(
            Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth)
            }.time
        )
        val selectedIndex = monthYearOptions.indexOf(currentSelectionText).takeIf { it >= 0 } ?: (monthYearOptions.size - 1)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Month")
            .setSingleChoiceItems(monthYearOptions.toTypedArray(), selectedIndex) { dialog, which ->
                // Parse selected month and year
                val selectedText = monthYearOptions[which]
                val calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                try {
                    val date = dateFormat.parse(selectedText)
                    calendar.time = date!!
                    selectedYear = calendar.get(Calendar.YEAR)
                    selectedMonth = calendar.get(Calendar.MONTH)
                    
                    android.util.Log.d("CategoriesFragment", "📅 Selected: $selectedText (Year: $selectedYear, Month: $selectedMonth)")
                    
                    updateMonthDisplay()
                    loadCategoryData() // Reload data for selected month
                    
                } catch (e: Exception) {
                    android.util.Log.e("CategoriesFragment", "Failed to parse selected date: $selectedText", e)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private suspend fun calculateCategorySpendingManually(): List<CategorySpending> {
        return try {
            android.util.Log.d(
                "CategoriesFragment",
                "🔧 Calculating category spending manually..."
            )

            // Get all transactions with any categoryId (including "others")
            val allTransactions = transactionRepository.getAllTransactionsOnce()
            val categorizedTransactions =
                allTransactions.filter { !it.categoryId.isNullOrEmpty() }
            android.util.Log.d(
                "CategoriesFragment",
                "🔧 Found ${categorizedTransactions.size} categorized transactions (including 'others')"
            )

            // Show breakdown by category
            val categoryBreakdown = categorizedTransactions.groupBy { it.categoryId }
            categoryBreakdown.forEach { (categoryId, transactions) ->
                android.util.Log.d(
                    "CategoriesFragment",
                    "🔧 Category '$categoryId': ${transactions.size} transactions"
                )
            }

            // Group by category and sum amounts (only DEBIT transactions for expenses)
            val categoryTotals = categorizedTransactions
                .filter { it.type == com.koshpal_android.koshpalapp.model.TransactionType.DEBIT }
                .groupBy { it.categoryId }
                .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
                .filter { (_, total) -> total > 0 }

            android.util.Log.d("CategoriesFragment", "🔧 Manual calculation results:")
            categoryTotals.forEach { (categoryId, total) ->
                android.util.Log.d("CategoriesFragment", "   🔧 $categoryId -> ₹$total")
            }

            // Convert to CategorySpending objects
            categoryTotals.map { (categoryId, total) ->
                CategorySpending(categoryId = categoryId, totalAmount = total)
            }

        } catch (e: Exception) {
            android.util.Log.e(
                "CategoriesFragment",
                "❌ Manual calculation failed: ${e.message}"
            )
            emptyList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


