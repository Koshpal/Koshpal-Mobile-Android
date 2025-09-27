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
            android.widget.Toast.makeText(
                requireContext(),
                "Set monthly budget feature coming soon!",
                android.widget.Toast.LENGTH_SHORT
            ).show()
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
                
                val categorizedTransactions = allTransactions.filter { 
                    !it.categoryId.isNullOrEmpty() && it.categoryId != "uncategorized" 
                }
                val uncategorizedTransactions = allTransactions.filter { 
                    it.categoryId.isNullOrEmpty() || it.categoryId == "uncategorized" 
                }
                
                android.util.Log.d("CategoriesFragment", "🔍 Categorized transactions: ${categorizedTransactions.size}")
                android.util.Log.d("CategoriesFragment", "🔍 Uncategorized transactions: ${uncategorizedTransactions.size}")
                
                categorizedTransactions.forEach { txn ->
                    android.util.Log.d("CategoriesFragment", "✅ Categorized: ${txn.id}, Category: '${txn.categoryId}', Amount: ${txn.amount}, Type: ${txn.type}")
                }
                
                uncategorizedTransactions.take(5).forEach { txn ->
                    android.util.Log.d("CategoriesFragment", "❌ Uncategorized: ${txn.id}, Category: '${txn.categoryId}', Amount: ${txn.amount}, Type: ${txn.type}")
                }

                // First try to get category spending data for all time (simpler query)
                android.util.Log.d("CategoriesFragment", "📅 Checking all-time category spending...")
                val allTimeCategorySpending = transactionRepository.getAllTimeCategorySpending()
                android.util.Log.d("CategoriesFragment", "📊 All-time category spending: ${allTimeCategorySpending.size} categories")
                
                // Use all-time data for now (we can add month filtering later)
                val categorySpending = allTimeCategorySpending
                
                android.util.Log.d("CategoriesFragment", "📊 Category spending data received: ${categorySpending.size} categories")
                categorySpending.forEach { spending ->
                    android.util.Log.d("CategoriesFragment", "📊 Category: ${spending.categoryId}, Amount: ${spending.totalAmount}")
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
        android.util.Log.d("CategoriesFragment", "🔄 onResume - Refreshing category data...")
        loadCategoryData()
    }
    
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            // Fragment became visible - refresh data
            android.util.Log.d("CategoriesFragment", "🔄 Fragment became visible - Refreshing category data...")
            loadCategoryData()
        }
    }
    
    fun refreshData() {
        // Public method to refresh data from external sources
        android.util.Log.d("CategoriesFragment", "🔄 External refresh triggered - Reloading category data...")
        if (_binding != null) {
            loadCategoryData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
