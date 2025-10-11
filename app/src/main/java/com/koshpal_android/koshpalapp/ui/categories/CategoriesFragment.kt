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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.github.mikephil.charting.utils.MPPointF
import com.koshpal_android.koshpalapp.ui.trends.TrendsFragment

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
        setupTabLayout()
        setupMonthPicker()
        updateMonthDisplay()
        loadCategoryData()
    }

    private fun setupRecyclerView() {
        categorySpendingAdapter = CategorySpendingAdapter { categorySpending ->
            // Handle set budget click
            val category = TransactionCategory.getDefaultCategories()
                .find { it.id == categorySpending.categoryId }
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
        binding.tabLayout.addOnTabSelectedListener(object :
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
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
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Merchants view coming soon!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
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
            android.util.Log.d(
                "CategoriesFragment",
                "🔄 Manual refresh triggered by Set Budget button"
            )
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
                val selectedMonthCategorySpending = transactionRepository.getCurrentMonthCategorySpending(startOfMonth, endOfMonth)
                android.util.Log.d("CategoriesFragment", "📊 Selected month category spending: ${selectedMonthCategorySpending.size} categories")
                
                // Log the amounts for debugging
                selectedMonthCategorySpending.forEach { spending ->
                    val categoryName = TransactionCategory.getDefaultCategories().find { it.id == spending.categoryId }?.name ?: "Unknown"
                    android.util.Log.d("CategoriesFragment", "   💰 ${spending.categoryId} ('$categoryName') -> ₹${spending.totalAmount}")
                }
                
                val categorySpending = selectedMonthCategorySpending

                // Show ALL categories including "others" - this gives users a complete view
                android.util.Log.d("CategoriesFragment", "📊 Total categories: ${categorySpending.size}")
                
                if (categorySpending.isNotEmpty()) {
                    android.util.Log.d("CategoriesFragment", "✅ Showing all category data including 'others'")
                    updatePieChart(categorySpending)
                    updateCategoryList(categorySpending)
                    updateTotalSpending(categorySpending)
                    showDataViews()
                } else {
                    android.util.Log.d("CategoriesFragment", "📊 No transactions found")
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
            val category =
                TransactionCategory.getDefaultCategories().find { it.id == spending.categoryId }
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

    private fun setupTabLayout() {
        // Setup tab layout for Categories, Transactions, Merchants
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        // Transactions tab - navigate to transactions
                        (activity as? HomeActivity)?.showTransactionsFragment()
                    }
                    1 -> {
                        // Categories tab - already here, do nothing
                        android.util.Log.d("CategoriesFragment", "📊 Categories tab selected - already showing categories")
                    }
                    2 -> {
                        // Trends tab - show trends fragment
                        showTrendsFragment()
                    }
                }
            }
            
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
        
        // Set Categories tab as selected by default
        binding.tabLayout.getTabAt(1)?.select()
    }

    private fun setupMonthPicker() {
        binding.tvMonth.setOnClickListener {
            showMonthPickerDialog()
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

    private fun showTrendsFragment() {
        android.util.Log.d("CategoriesFragment", "📈 Navigating to Trends fragment")
        
        // Replace current fragment with TrendsFragment
        val trendsFragment = TrendsFragment()
        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, trendsFragment)
            .addToBackStack("trends")
            .commit()
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


