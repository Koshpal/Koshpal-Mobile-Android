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
import com.github.mikephil.charting.utils.MPPointF

@AndroidEntryPoint
class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var transactionRepository: TransactionRepository

    private lateinit var categorySpendingAdapter: CategorySpendingAdapter

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
        setupPieChart()
        setupClickListeners()
        loadCategoryData()
    }

    private fun setupRecyclerView() {
        categorySpendingAdapter = CategorySpendingAdapter { categorySpending ->
            // Handle set budget click
            val category = TransactionCategory.getDefaultCategories().find { it.id == categorySpending.categoryId }
            val categoryName = category?.name ?: "Unknown Category"
            android.widget.Toast.makeText(
                requireContext(),
                "Set budget for $categoryName",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categorySpendingAdapter
        }
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(false)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            legend.isEnabled = false
            setEntryLabelColor(Color.TRANSPARENT)
        }
    }

    private fun setupClickListeners() {
        // Tab layout click handling
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        // Transactions tab - navigate back to transactions
                        (activity as? HomeActivity)?.showTransactionsFragment()
                    }
                    1 -> {
                        // Categories tab - already showing
                    }
                    2 -> {
                        // Merchants tab - placeholder
                        android.widget.Toast.makeText(requireContext(), "Merchants view coming soon!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        // Set Categories tab as selected by default
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(1))

        binding.btnSetBudget.setOnClickListener {
            // For debugging - manually refresh data
            android.util.Log.d("CategoriesFragment", "🔄 Manual refresh triggered by Set Budget button")
            loadCategoryData()
        }

        binding.tvMonth.setOnClickListener {
            // Show month picker
            android.widget.Toast.makeText(
                requireContext(),
                "Month picker coming soon!",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun loadCategoryData() {
        lifecycleScope.launch {
            try {
                // Get all time data first to see if there are any categorized transactions
                val startOfAllTime = 0L
                val endOfAllTime = System.currentTimeMillis()
                
                // Also get current month data
                val calendar = Calendar.getInstance()
                val startOfMonth = calendar.apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val endOfMonth = calendar.apply {
                    add(Calendar.MONTH, 1)
                    add(Calendar.MILLISECOND, -1)
                }.timeInMillis

                // Update month display
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                binding.tvMonth.text = monthFormat.format(Date())

                android.util.Log.d("CategoriesFragment", "📅 Querying category spending from ${Date(startOfMonth)} to ${Date(endOfMonth)}")

                // Debug: Check all transactions first
                val allTransactions = transactionRepository.getAllTransactionsOnce()
                android.util.Log.d("CategoriesFragment", "🔍 Total transactions in DB: ${allTransactions.size}")
                
                if (allTransactions.isEmpty()) {
                    android.util.Log.e("CategoriesFragment", "❌ NO TRANSACTIONS FOUND IN DATABASE!")
                    android.util.Log.e("CategoriesFragment", "❌ Database might have been recreated and data lost")
                } else {
                    android.util.Log.d("CategoriesFragment", "✅ Found ${allTransactions.size} transactions")
                    // Debug: Check transaction IDs and categoryIds
                    allTransactions.take(10).forEach { txn ->
                        android.util.Log.d("CategoriesFragment", "🔍 Transaction: ID='${txn.id}', CategoryId='${txn.categoryId}', Merchant='${txn.merchant}', Amount=${txn.amount}")
                    }
                }
                
                val categorizedTransactions = allTransactions.filter { 
                    !it.categoryId.isNullOrEmpty() && it.categoryId != "uncategorized" 
                }
                val uncategorizedTransactions = allTransactions.filter { 
                    it.categoryId.isNullOrEmpty() || it.categoryId == "uncategorized" 
                }
                
                android.util.Log.d("CategoriesFragment", "🔍 Categorized transactions: ${categorizedTransactions.size}")
                android.util.Log.d("CategoriesFragment", "🔍 Uncategorized transactions: ${uncategorizedTransactions.size}")
                
                android.util.Log.d("CategoriesFragment", "📋 DETAILED CATEGORIZED TRANSACTIONS:")
                categorizedTransactions.forEach { txn ->
                    android.util.Log.d("CategoriesFragment", "✅ Categorized: ${txn.id}, Category: '${txn.categoryId}', Amount: ${txn.amount}, Type: ${txn.type}, Date: ${java.util.Date(txn.date)}")
                }
                
                // Show unique category IDs found in database
                val uniqueCategoryIds = categorizedTransactions.map { it.categoryId }.distinct()
                android.util.Log.d("CategoriesFragment", "🏷️ UNIQUE CATEGORY IDs IN DB: $uniqueCategoryIds")
                
                // Specific check for "food" category
                val foodTransactions = categorizedTransactions.filter { it.categoryId == "food" }
                android.util.Log.d("CategoriesFragment", "🍔 FOOD TRANSACTIONS: Found ${foodTransactions.size} transactions")
                foodTransactions.forEach { txn ->
                    android.util.Log.d("CategoriesFragment", "🍔 Food txn: ${txn.id}, Amount: ${txn.amount}, Type: ${txn.type}, Date: ${java.util.Date(txn.date)}")
                }
                
                
                // Show available category definitions
                val availableCategories = TransactionCategory.getDefaultCategories()
                android.util.Log.d("CategoriesFragment", "📚 AVAILABLE CATEGORY DEFINITIONS:")
                availableCategories.forEach { cat ->
                    android.util.Log.d("CategoriesFragment", "   📂 ${cat.id} -> ${cat.name}")
                }
                
                // Check for mismatches
                uniqueCategoryIds.forEach { dbCategoryId ->
                    val matchingCategory = availableCategories.find { it.id == dbCategoryId }
                    if (matchingCategory == null) {
                        android.util.Log.e("CategoriesFragment", "❌ MISMATCH: Category ID '$dbCategoryId' in DB but not in definitions!")
                    } else {
                        android.util.Log.d("CategoriesFragment", "✅ MATCH: Category ID '$dbCategoryId' -> '${matchingCategory.name}'")
                    }
                }
                
                uncategorizedTransactions.take(5).forEach { txn ->
                    android.util.Log.d("CategoriesFragment", "❌ Uncategorized: ${txn.id}, Category: '${txn.categoryId}', Amount: ${txn.amount}, Type: ${txn.type}")
                }

                // First try to get category spending data for all time (simpler query)
                android.util.Log.d("CategoriesFragment", "📅 Checking all-time category spending...")
                val allTimeCategorySpending = transactionRepository.getAllTimeCategorySpending()
                android.util.Log.d("CategoriesFragment", "📊 All-time category spending: ${allTimeCategorySpending.size} categories")
                
                // Also try the date-filtered query for comparison
                val monthCategorySpending = transactionRepository.getCategoryWiseSpending(startOfMonth, endOfMonth)
                android.util.Log.d("CategoriesFragment", "📊 Month category spending: ${monthCategorySpending.size} categories")
                
                // Use all-time data, but if empty, calculate manually
                val categorySpending = if (allTimeCategorySpending.isNotEmpty()) {
                    allTimeCategorySpending
                } else {
                    // Fallback: Calculate manually from categorized transactions
                    android.util.Log.d("CategoriesFragment", "📊 SQL query returned empty, calculating manually...")
                    calculateCategorySpendingManually()
                }
                
                android.util.Log.d("CategoriesFragment", "📊 Category spending data received: ${categorySpending.size} categories")
                android.util.Log.d("CategoriesFragment", "📊 DETAILED CATEGORY SPENDING RESULTS:")
                categorySpending.forEach { spending ->
                    val categoryName = TransactionCategory.getDefaultCategories().find { it.id == spending.categoryId }?.name ?: "Unknown"
                    android.util.Log.d("CategoriesFragment", "   💰 ${spending.categoryId} ('$categoryName') -> ₹${spending.totalAmount}")
                }
                
                // Specific check if "food" is missing from results
                val foodInResults = categorySpending.find { it.categoryId == "food" }
                if (foodInResults != null) {
                    android.util.Log.d("CategoriesFragment", "🍔 FOOD FOUND in results: ₹${foodInResults.totalAmount}")
                } else {
                    android.util.Log.e("CategoriesFragment", "🍔 FOOD MISSING from SQL results!")
                    if (foodTransactions.isNotEmpty()) {
                        val totalFoodAmount = foodTransactions.filter { it.type == com.koshpal_android.koshpalapp.model.TransactionType.DEBIT }.sumOf { it.amount }
                        android.util.Log.e("CategoriesFragment", "🍔 Expected food amount: ₹$totalFoodAmount from ${foodTransactions.size} transactions")
                    }
                }
                
                // Compare with what we expect to see
                android.util.Log.d("CategoriesFragment", "🔍 EXPECTED vs ACTUAL:")
                uniqueCategoryIds.forEach { expectedCategoryId ->
                    val foundInResults = categorySpending.find { it.categoryId == expectedCategoryId }
                    if (foundInResults != null) {
                        android.util.Log.d("CategoriesFragment", "   ✅ Expected '$expectedCategoryId' -> Found ₹${foundInResults.totalAmount}")
                    } else {
                        android.util.Log.e("CategoriesFragment", "   ❌ Expected '$expectedCategoryId' -> NOT FOUND in query results!")
                    }
                }
                
                if (categorySpending.isNotEmpty()) {
                    updatePieChart(categorySpending)
                    updateCategoryList(categorySpending)
                    updateTotalSpending(categorySpending)
                    showDataViews()
                } else {
                    android.util.Log.d("CategoriesFragment", "📊 No category spending data found")
                    showEmptyState()
                }

            } catch (e: Exception) {
                android.util.Log.e("CategoriesFragment", "Failed to load category data: ${e.message}")
                showEmptyState()
            }
        }
    }

    private fun updatePieChart(categorySpending: List<CategorySpending>) {
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        categorySpending.forEach { spending ->
            // Get category name from default categories
            val category = TransactionCategory.getDefaultCategories().find { it.id == spending.categoryId }
            val categoryName = category?.name ?: "Unknown"
            entries.add(PieEntry(spending.totalAmount.toFloat(), categoryName))
            
            // Get category color
            val color = try {
                Color.parseColor(category?.color ?: "#6750A4")
            } catch (e: Exception) {
                Color.parseColor("#6750A4")
            }
            colors.add(color)
        }

        val dataSet = PieDataSet(entries, "Categories").apply {
            setDrawIcons(false)
            sliceSpace = 3f
            iconsOffset = MPPointF.getInstance(0f, 40f)
            selectionShift = 5f
            setColors(colors)
            valueTextColor = Color.TRANSPARENT
            valueTextSize = 0f
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter())
            setValueTextSize(0f)
            setValueTextColor(Color.TRANSPARENT)
        }

        binding.pieChart.data = data
        binding.pieChart.highlightValues(null)
        binding.pieChart.invalidate()
    }

    private fun updateCategoryList(categorySpending: List<CategorySpending>) {
        categorySpendingAdapter.submitList(categorySpending)
    }

    private fun updateTotalSpending(categorySpending: List<CategorySpending>) {
        val total = categorySpending.sumOf { it.totalAmount }
        binding.tvTotalSpending.text = "₹${String.format("%.0f", total)}"
    }

    private fun showDataViews() {
        binding.pieChart.visibility = View.VISIBLE
        binding.layoutTotalSpending.visibility = View.VISIBLE
        binding.btnSetBudget.visibility = View.VISIBLE
        binding.rvCategories.visibility = View.VISIBLE
        binding.layoutEmptyState.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.pieChart.visibility = View.GONE
        binding.layoutTotalSpending.visibility = View.GONE
        binding.btnSetBudget.visibility = View.GONE
        binding.rvCategories.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible
        loadCategoryData()
    }
    
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            // Fragment became visible - refresh data
            loadCategoryData()
        }
    }
    
    private suspend fun calculateCategorySpendingManually(): List<CategorySpending> {
        return try {
            android.util.Log.d("CategoriesFragment", "🔧 Calculating category spending manually...")
            
            // Get all categorized transactions directly
            val categorizedTransactions = transactionRepository.getAllCategorizedTransactions()
            android.util.Log.d("CategoriesFragment", "🔧 Found ${categorizedTransactions.size} categorized transactions")
            
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
            android.util.Log.e("CategoriesFragment", "❌ Manual calculation failed: ${e.message}")
            emptyList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
