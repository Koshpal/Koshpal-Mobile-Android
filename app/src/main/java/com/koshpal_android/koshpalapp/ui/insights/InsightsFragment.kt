package com.koshpal_android.koshpalapp.ui.insights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.koshpal_android.koshpalapp.databinding.FragmentInsightsBinding
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import javax.inject.Inject
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*
import com.koshpal_android.koshpalapp.R
import android.graphics.Color as AndroidColor
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.koshpal_android.koshpalapp.ui.insights.PremiumAnimationUtils
import com.koshpal_android.koshpalapp.ui.insights.TextHighlightUtils

@AndroidEntryPoint
class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var transactionRepository: TransactionRepository
    
    private val viewModel: InsightsViewModel by viewModels()

    private lateinit var recurringPaymentEnhancedAdapter: RecurringPaymentEnhancedAdapter
    private lateinit var topCreditMerchantAdapter: TopMerchantProgressAdapter
    private lateinit var topDebitMerchantAdapter: TopMerchantProgressAdapter
    
    // Month selector
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    
    // Performance optimization: Cache frequently used data
    private var cachedTransactions: List<com.koshpal_android.koshpalapp.model.Transaction>? = null
    private var lastDataLoadTime: Long = 0
    private val DATA_CACHE_DURATION = 5 * 60 * 1000L // 5 minutes
    
    // Toggle state for comparison view
    private var showPercentages = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        
        // Auto-refresh when transactions flow emits updates
        viewLifecycleOwner.lifecycleScope.launch {
            transactionRepository.getAllTransactions().collect {
                // Clear cache and reload insights when any transaction changes
                cachedTransactions = null
                lastDataLoadTime = 0
                loadInsightsData()
            }
        }
        
        loadInsightsData()
        viewModel.loadMonthComparisonData()
        viewModel.loadRecurringPayments() // Load real recurring payments data
    }

    private fun setupUI() {
        binding.apply {
            // Setup collapsing toolbar title
            collapsingToolbar.title = "Insights"
            
            // Setup enhanced recurring payments adapter
            recurringPaymentEnhancedAdapter = RecurringPaymentEnhancedAdapter()
            rvRecurringPayments.layoutManager = LinearLayoutManager(requireContext())
            rvRecurringPayments.adapter = recurringPaymentEnhancedAdapter
            
            // Setup Top Credit Merchants RecyclerView (Money IN)
            topCreditMerchantAdapter = TopMerchantProgressAdapter()
            rvTopCreditMerchants.layoutManager = LinearLayoutManager(requireContext())
            rvTopCreditMerchants.adapter = topCreditMerchantAdapter
            
            // Setup Top Debit Merchants RecyclerView (Money OUT)
            topDebitMerchantAdapter = TopMerchantProgressAdapter()
            rvTopDebitMerchants.layoutManager = LinearLayoutManager(requireContext())
            rvTopDebitMerchants.adapter = topDebitMerchantAdapter
            
            // Setup swipe to refresh
            swipeRefresh?.setOnRefreshListener {
                refreshAllData()
            }
            
            // Setup month comparison
            setupMonthComparison()
            
            // Setup toggle chips for view mode
            setupToggleChips()
        }
        
        // Observe ViewModel data
        observeViewModel()
        observeRecurringPayments()
    }

    private fun loadInsightsData() {
        lifecycleScope.launch {
            try {
                val context = requireContext().applicationContext
                val db = KoshpalDatabase.getDatabase(context)
                val transactionDao = db.transactionDao()

                // Use cached data if available and not expired
                val allTransactions = if (cachedTransactions != null && 
                    (System.currentTimeMillis() - lastDataLoadTime) < DATA_CACHE_DURATION) {
                    cachedTransactions!!
                } else {
                    withContext(Dispatchers.IO) {
                    transactionDao.getAllTransactionsOnce()
                    }.also {
                        cachedTransactions = it
                        lastDataLoadTime = System.currentTimeMillis()
                    }
                }

                // Load merchant hotspots data
                loadMerchantHotspotsData(allTransactions)
                
            } catch (e: Exception) {
                android.util.Log.e("InsightsFragment", "Failed to load insights: ${e.message}")
                Toast.makeText(requireContext(), "Failed to load insights data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Merchant Hotspots Data Loading (Split into Credit & Debit)
    private fun loadMerchantHotspotsData(allTransactions: List<com.koshpal_android.koshpalapp.model.Transaction>) {
        android.util.Log.d("InsightsFragment", "\n\n═══════════════════════════════════════════")
        android.util.Log.d("InsightsFragment", "🔍 MERCHANT ANALYSIS STARTED (Credit & Debit)")
        android.util.Log.d("InsightsFragment", "═══════════════════════════════════════════")
        
        android.util.Log.d("InsightsFragment", "📊 Total transactions in DB: ${allTransactions.size}")
        
        val currentMonth = getCurrentMonthRange()
        android.util.Log.d("InsightsFragment", "📅 Current month range: ${java.util.Date(currentMonth.first)} to ${java.util.Date(currentMonth.second)}")
        
        // Filter current month transactions
        val currentMonthTransactions = allTransactions.filter { 
            it.date in currentMonth.first..currentMonth.second 
        }
        
        // Split by CREDIT (incoming) and DEBIT (outgoing)
        val creditTransactions = currentMonthTransactions.filter { it.type == TransactionType.CREDIT }
        val debitTransactions = currentMonthTransactions.filter { it.type == TransactionType.DEBIT }
        
        android.util.Log.d("InsightsFragment", "💰 Current month CREDIT transactions: ${creditTransactions.size}")
        android.util.Log.d("InsightsFragment", "💸 Current month DEBIT transactions: ${debitTransactions.size}")
        
        // Process Credit Merchants (Money IN)
        val topCreditMerchants = if (creditTransactions.isNotEmpty()) {
            val grouped = creditTransactions.groupBy { normalizeMerchantName(it.merchant) }
            android.util.Log.d("InsightsFragment", "🏦 Unique CREDIT merchants: ${grouped.size}")
            
            val top = grouped
                .mapValues { it.value.sumOf { t -> t.amount } }
                .toList()
                .sortedByDescending { it.second }
                .take(5)
            
            android.util.Log.d("InsightsFragment", "\n💰 TOP CREDIT MERCHANTS (Money IN):")
            top.forEachIndexed { index, (merchant, amount) ->
                android.util.Log.d("InsightsFragment", "  ${index + 1}. $merchant = ₹$amount")
            }
            top
        } else {
            android.util.Log.w("InsightsFragment", "⚠️ No credit transactions in current month")
            emptyList()
        }
        
        // Process Debit Merchants (Money OUT)
        val topDebitMerchants = if (debitTransactions.isNotEmpty()) {
            val grouped = debitTransactions.groupBy { normalizeMerchantName(it.merchant) }
            android.util.Log.d("InsightsFragment", "🏪 Unique DEBIT merchants: ${grouped.size}")
            
            val top = grouped
                .mapValues { it.value.sumOf { t -> t.amount } }
                .toList()
                .sortedByDescending { it.second }
                .take(5)
            
            android.util.Log.d("InsightsFragment", "\n💸 TOP DEBIT MERCHANTS (Money OUT):")
            top.forEachIndexed { index, (merchant, amount) ->
                android.util.Log.d("InsightsFragment", "  ${index + 1}. $merchant = ₹$amount")
            }
            top
        } else {
            android.util.Log.w("InsightsFragment", "⚠️ No debit transactions in current month")
            emptyList()
        }
        
        android.util.Log.d("InsightsFragment", "\n🎨 Rendering merchant charts...\n")
        
        // Render both sections
        renderTopCreditMerchantsChart(topCreditMerchants)
        renderTopDebitMerchantsChart(topDebitMerchants)
    }

    // Helper Methods
    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, selectedYear)
        cal.set(Calendar.MONTH, selectedMonth)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val end = cal.timeInMillis
        
        return Pair(start, end)
    }

    // Merchant name normalization (still used for merchant hotspots)
    private fun normalizeMerchantName(merchant: String): String {
        return merchant.lowercase()
            .replace(Regex("(upi|imps|neft|ref|txn|id|pos|card|debit|credit)"), "")
            .replace(Regex("\\d+"), "")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    // Export functionality removed - Budget Usage section no longer exists

    private fun renderTopCreditMerchantsChart(topMerchants: List<Pair<String, Double>>) {
        android.util.Log.d("InsightsFragment", "💰 TOP CREDIT MERCHANTS: Rendering ${topMerchants.size} items")
        
        if (topMerchants.isEmpty()) {
            android.util.Log.w("InsightsFragment", "   No credit merchants to display")
            // Show empty view or hide section if needed
            return
        }
        
        val maxAmount = topMerchants.maxOfOrNull { it.second } ?: 1.0
        val total = topMerchants.sumOf { it.second }
        
        val progressItems = topMerchants.map { (merchant, amount) ->
            val percentageOfMax = (amount / maxAmount).toFloat()
            val sharePercentage = (amount / total * 100).toFloat()
            
            android.util.Log.d("InsightsFragment", "  💰 $merchant: ₹$amount = ${sharePercentage.toInt()}%")
            
            TopMerchantProgress(
                merchantName = merchant,
                amount = amount,
                percentageOfMax = percentageOfMax,
                sharePercentage = sharePercentage
            )
        }
        
        android.util.Log.d("InsightsFragment", "✅ Submitting ${progressItems.size} credit merchants")
        topCreditMerchantAdapter.submitList(progressItems)
    }
    
    private fun renderTopDebitMerchantsChart(topMerchants: List<Pair<String, Double>>) {
        android.util.Log.d("InsightsFragment", "💸 TOP DEBIT MERCHANTS: Rendering ${topMerchants.size} items")
        
        if (topMerchants.isEmpty()) {
            android.util.Log.w("InsightsFragment", "   No debit merchants to display")
            // Show empty view or hide section if needed
            return
        }
        
        val maxAmount = topMerchants.maxOfOrNull { it.second } ?: 1.0
        val total = topMerchants.sumOf { it.second }
        
        val progressItems = topMerchants.map { (merchant, amount) ->
            val percentageOfMax = (amount / maxAmount).toFloat()
            val sharePercentage = (amount / total * 100).toFloat()
            
            android.util.Log.d("InsightsFragment", "  💸 $merchant: ₹$amount = ${sharePercentage.toInt()}%")
            
            TopMerchantProgress(
                merchantName = merchant,
                amount = amount,
                percentageOfMax = percentageOfMax,
                sharePercentage = sharePercentage
            )
        }
        
        android.util.Log.d("InsightsFragment", "✅ Submitting ${progressItems.size} debit merchants")
        topDebitMerchantAdapter.submitList(progressItems)
    }

    // Old helper methods removed - now using RecyclerView adapter like working Budget progress bars!

    // Category distribution removed per design

    private fun mapCategoryIdToName(categoryId: String): String {
        return when (categoryId.lowercase()) {
            "food" -> "Food"
            "travel" -> "Travel"
            "rent" -> "Rent"
            "shopping" -> "Shopping"
            "bills" -> "Bills"
            "entertainment" -> "Entertainment"
            "transport" -> "Transport"
            "grocery" -> "Grocery"
            "education" -> "Education"
            "healthcare" -> "Healthcare"
            "others" -> "Others"
            else -> categoryId.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    private fun mapBudgetNameToCategoryId(name: String): String {
        val n = name.trim().lowercase()
        return when (n) {
            "food", "food & dining", "food and dining", "dining" -> "food"
            "grocery", "groceries" -> "grocery"
            "transport", "transportation", "travel", "commute" -> "transport"
            "bills", "bills & utilities", "bills and utilities", "utilities" -> "bills"
            "education", "school", "tuition" -> "education"
            "entertainment", "subscriptions", "movies" -> "entertainment"
            "health", "healthcare", "medical" -> "healthcare"
            "shopping", "ecommerce" -> "shopping"
            "salary", "salary & income", "income" -> "salary"
            "others", "other" -> "others"
            else -> n.replace(" ", "").replace("&", "")
        }
    }

    private fun calculateBudgetCategoryProgress(
        budgetCategories: List<com.koshpal_android.koshpalapp.model.BudgetCategory>,
        allTransactions: List<com.koshpal_android.koshpalapp.model.Transaction>,
        currentMonthRange: Pair<Long, Long>
    ): List<BudgetCategoryProgress> {
        val progressList = mutableListOf<BudgetCategoryProgress>()
        
        // Get current month transactions
        val currentMonthTransactions = allTransactions.filter { 
            it.type == com.koshpal_android.koshpalapp.model.TransactionType.DEBIT && 
            it.date in currentMonthRange.first..currentMonthRange.second 
        }
        
        for (budgetCategory in budgetCategories) {
            val categoryId = mapBudgetNameToCategoryId(budgetCategory.name)
            // Calculate spent amount for this category in current month
            val spentAmount = currentMonthTransactions
                .filter { it.categoryId.equals(categoryId, ignoreCase = true) }
                .sumOf { it.amount }
            
            val percentageUsed = if (budgetCategory.allocatedAmount > 0) {
                (spentAmount / budgetCategory.allocatedAmount).toFloat()
            } else 0f
            
            val remainingAmount = budgetCategory.allocatedAmount - spentAmount
            val isOverBudget = spentAmount > budgetCategory.allocatedAmount
            
            // Get category details from default categories
            val categoryDetails = getCategoryDetails(budgetCategory.name)
            
            progressList.add(
                BudgetCategoryProgress(
                    categoryName = budgetCategory.name,
                    categoryId = categoryId,
                    allocatedAmount = budgetCategory.allocatedAmount,
                    spentAmount = spentAmount,
                    percentageUsed = percentageUsed,
                    remainingAmount = remainingAmount,
                    isOverBudget = isOverBudget,
                    categoryColor = categoryDetails.first,
                    categoryIcon = categoryDetails.second
                )
            )
        }
        
        // Sort by percentage used (highest first)
        return progressList.sortedByDescending { it.percentageUsed }
    }
    
    private fun getCategoryDetails(categoryName: String): Pair<String, Int> {
        return when (categoryName.lowercase()) {
            "food & dining", "food" -> Pair("#FF9800", com.koshpal_android.koshpalapp.R.drawable.ic_food_dining)
            "grocery" -> Pair("#4CAF50", com.koshpal_android.koshpalapp.R.drawable.ic_grocery_cart)
            "transportation", "transport" -> Pair("#2196F3", com.koshpal_android.koshpalapp.R.drawable.ic_transport_car)
            "bills & utilities", "bills" -> Pair("#FFC107", com.koshpal_android.koshpalapp.R.drawable.ic_bills_receipt)
            "education" -> Pair("#9C27B0", com.koshpal_android.koshpalapp.R.drawable.ic_education_book)
            "entertainment" -> Pair("#E91E63", com.koshpal_android.koshpalapp.R.drawable.ic_entertainment_movie)
            "healthcare", "health" -> Pair("#00BCD4", com.koshpal_android.koshpalapp.R.drawable.ic_healthcare_medical)
            "shopping" -> Pair("#795548", com.koshpal_android.koshpalapp.R.drawable.ic_shopping_bag)
            "salary & income", "salary" -> Pair("#4CAF50", com.koshpal_android.koshpalapp.R.drawable.ic_salary_money)
            else -> Pair("#607D8B", com.koshpal_android.koshpalapp.R.drawable.ic_category_default)
        }
    }
    
    private fun createSampleBudgetProgressData(): List<BudgetCategoryProgress> {
        return listOf(
            BudgetCategoryProgress(
                categoryName = "Food & Dining",
                categoryId = "food",
                allocatedAmount = 5000.0,
                spentAmount = 4200.0,
                percentageUsed = 0.84f,
                remainingAmount = 800.0,
                isOverBudget = false,
                categoryColor = "#FF9800",
                categoryIcon = com.koshpal_android.koshpalapp.R.drawable.ic_food_dining
            ),
            BudgetCategoryProgress(
                categoryName = "Transportation",
                categoryId = "transport",
                allocatedAmount = 3000.0,
                spentAmount = 3200.0,
                percentageUsed = 1.07f,
                remainingAmount = -200.0,
                isOverBudget = true,
                categoryColor = "#2196F3",
                categoryIcon = com.koshpal_android.koshpalapp.R.drawable.ic_transport_car
            ),
            BudgetCategoryProgress(
                categoryName = "Entertainment",
                categoryId = "entertainment",
                allocatedAmount = 2000.0,
                spentAmount = 1500.0,
                percentageUsed = 0.75f,
                remainingAmount = 500.0,
                isOverBudget = false,
                categoryColor = "#E91E63",
                categoryIcon = com.koshpal_android.koshpalapp.R.drawable.ic_entertainment_movie
            ),
            BudgetCategoryProgress(
                categoryName = "Shopping",
                categoryId = "shopping",
                allocatedAmount = 4000.0,
                spentAmount = 1800.0,
                percentageUsed = 0.45f,
                remainingAmount = 2200.0,
                isOverBudget = false,
                categoryColor = "#795548",
                categoryIcon = com.koshpal_android.koshpalapp.R.drawable.ic_shopping_bag
            ),
            BudgetCategoryProgress(
                categoryName = "Bills & Utilities",
                categoryId = "bills",
                allocatedAmount = 2500.0,
                spentAmount = 2400.0,
                percentageUsed = 0.96f,
                remainingAmount = 100.0,
                isOverBudget = false,
                categoryColor = "#FFC107",
                categoryIcon = com.koshpal_android.koshpalapp.R.drawable.ic_bills_receipt
            )
        )
    }

    // Month selector and pull-to-refresh removed with Budget Usage section
    
    // ==================== Month Comparison Feature ====================
    
    private fun setupMonthComparison() {
        // Month comparison setup is now handled by setupToggleChips()
        // This method is kept for compatibility but the toggle functionality
        // has been moved to ChipGroup in the premium layout
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe loading state
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        val comparisonCard = binding.root.findViewById<View>(R.id.cardMonthComparison)
                        val shimmer = comparisonCard?.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerComparisonChart)
                        val chart = comparisonCard?.findViewById<View>(R.id.chartMonthComparison)
                        
                        if (shimmer != null && chart != null) {
                            if (isLoading) {
                                shimmer.visibility = View.VISIBLE
                                shimmer.startShimmer()
                                chart.visibility = View.GONE
                            } else {
                                shimmer.stopShimmer()
                                shimmer.visibility = View.GONE
                                chart.visibility = View.VISIBLE
                            }
                        }
                    }
                }
                
                // Observe comparison data
                launch {
                    viewModel.monthComparisonData.collect { data ->
                        if (data.isNotEmpty()) {
                            renderMonthComparisonChart(data, showPercentages)
                        }
                    }
                }
                
                // Observe insights
                launch {
                    viewModel.comparisonInsight.collect { insight ->
                        insight?.let {
                            renderInsights(it)
                        }
                    }
                }
            }
        }
    }
    
    private fun showComparisonShimmer(shimmer: com.facebook.shimmer.ShimmerFrameLayout, content: View) {
        shimmer.isVisible = true
        shimmer.startShimmer()
        content.isVisible = false
    }
    
    private fun hideComparisonShimmer(shimmer: com.facebook.shimmer.ShimmerFrameLayout, content: View) {
        shimmer.stopShimmer()
        shimmer.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                shimmer.isVisible = false
                shimmer.alpha = 1f
                
                content.alpha = 0f
                content.isVisible = true
                content.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
            .start()
    }
    
    private fun renderMonthComparisonChart(data: List<MonthComparisonData>, showPercentage: Boolean) {
        val comparisonCard = binding.root.findViewById<View>(R.id.cardMonthComparison)
        val chart = comparisonCard.findViewById<BarChart>(R.id.chartMonthComparison)
        
        // Prepare data
        val entries1 = mutableListOf<BarEntry>() // Previous month
        val entries2 = mutableListOf<BarEntry>() // Current month
        val labels = mutableListOf<String>()
        
        data.take(6).forEachIndexed { index, item ->
            if (showPercentage) {
                // Show percentage values
                entries1.add(BarEntry(index.toFloat(), item.previousMonthAmount.toFloat()))
                entries2.add(BarEntry(index.toFloat(), item.currentMonthAmount.toFloat()))
            } else {
                // Show absolute amounts
                entries1.add(BarEntry(index.toFloat(), item.previousMonthAmount.toFloat()))
                entries2.add(BarEntry(index.toFloat(), item.currentMonthAmount.toFloat()))
            }
            labels.add(item.categoryName.take(8))
        }
        
        val dataSet1 = BarDataSet(entries1, "Previous Month").apply {
            color = AndroidColor.parseColor("#B0BEC5")
            valueTextColor = AndroidColor.parseColor("#546E7A")
            valueTextSize = 10f
        }
        
        val dataSet2 = BarDataSet(entries2, "Current Month").apply {
            color = AndroidColor.parseColor("#5C6BC0")
            valueTextColor = AndroidColor.parseColor("#3949AB")
            valueTextSize = 10f
        }
        
        val barData = BarData(dataSet1, dataSet2)
        val groupSpace = 0.3f
        val barSpace = 0.05f
        val barWidth = 0.3f
        
        barData.barWidth = barWidth
        
        chart.apply {
            this.data = barData
            description.isEnabled = false
            
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setCenterAxisLabels(true)
                setDrawGridLines(false)
                textColor = AndroidColor.parseColor("#546E7A")
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                textColor = AndroidColor.parseColor("#546E7A")
                axisMinimum = 0f
            }
            
            axisRight.isEnabled = false
            
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                textColor = AndroidColor.parseColor("#546E7A")
            }
            
            groupBars(0f, groupSpace, barSpace)
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = data.size.toFloat()
            
            // Make chart interactive
            setTouchEnabled(true)
            setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                    e?.let {
                        val index = it.x.toInt()
                        if (index < data.size) {
                            showCategoryDrilldown(data[index])
                        }
                    }
                }
                
                override fun onNothingSelected() {}
            })
            
            animateY(800, Easing.EaseInOutQuad)
            invalidate()
        }
    }
    
    private fun renderInsights(insight: MonthComparisonInsight) {
        val comparisonCard = binding.root.findViewById<View>(R.id.cardMonthComparison) ?: return
        val layoutInsights = comparisonCard.findViewById<android.widget.LinearLayout>(R.id.layoutComparisonInsights)
        val layoutTopIncreases = comparisonCard.findViewById<android.widget.LinearLayout>(R.id.layoutTopIncreases)
        val layoutTopDecreases = comparisonCard.findViewById<android.widget.LinearLayout>(R.id.layoutTopDecreases)
        
        // Show insights section if we have data
        if (insight.topIncreases.isNotEmpty() || insight.topDecreases.isNotEmpty()) {
            layoutInsights?.visibility = View.VISIBLE
            
            // Clear existing views
            layoutTopIncreases?.removeAllViews()
            layoutTopDecreases?.removeAllViews()
            
            // Add top increases
            insight.topIncreases.take(3).forEach { change ->
                val itemView = layoutInflater.inflate(R.layout.item_top_change, layoutTopIncreases, false)
                setupTopChangeItem(itemView, change, true)
                layoutTopIncreases?.addView(itemView)
            }
            
            // Add top decreases
            insight.topDecreases.take(3).forEach { change ->
                val itemView = layoutInflater.inflate(R.layout.item_top_change, layoutTopDecreases, false)
                setupTopChangeItem(itemView, change, false)
                layoutTopDecreases?.addView(itemView)
            }
        } else {
            layoutInsights?.visibility = View.GONE
        }
    }
    
    private fun setupTopChangeItem(itemView: View, change: MonthComparisonData, isIncrease: Boolean) {
        val ivCategoryIcon = itemView.findViewById<android.widget.ImageView>(R.id.ivCategoryIcon)
        val tvCategoryName = itemView.findViewById<android.widget.TextView>(R.id.tvCategoryName)
        val tvAmountChange = itemView.findViewById<android.widget.TextView>(R.id.tvAmountChange)
        val tvPercentageChange = itemView.findViewById<android.widget.TextView>(R.id.tvPercentageChange)
        val cardBadge = itemView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardPercentageBadge)
        
        ivCategoryIcon.setImageResource(change.categoryIcon)
        tvCategoryName.text = change.categoryName
        
        val amountText = if (isIncrease) {
            "₹${String.format("%.0f", abs(change.absoluteChange))} more"
        } else {
            "₹${String.format("%.0f", abs(change.absoluteChange))} less"
        }
        tvAmountChange.text = amountText
        
        val arrow = if (isIncrease) "↑" else "↓"
        tvPercentageChange.text = "$arrow ${abs(change.percentageChange).toInt()}%"
        
        val bgColor = if (isIncrease) {
            AndroidColor.parseColor("#FFEBEE")
        } else {
            AndroidColor.parseColor("#E8F5E9")
        }
        
        val textColor = if (isIncrease) {
            AndroidColor.parseColor("#D32F2F")
        } else {
            AndroidColor.parseColor("#388E3C")
        }
        
        cardBadge.setCardBackgroundColor(bgColor)
        tvPercentageChange.setTextColor(textColor)
        
        itemView.setOnClickListener {
            showCategoryDrilldown(change)
        }
    }
    
    private fun showCategoryDrilldown(data: MonthComparisonData) {
        val dialog = CategoryDrilldownDialog.newInstance(
            data.categoryId,
            data.categoryName,
            data.categoryIcon
        )
        dialog.show(childFragmentManager, "CategoryDrilldown")
    }
    
    private fun refreshAllData() {
        cachedTransactions = null
        lastDataLoadTime = 0
        loadInsightsData()
        viewModel.loadMonthComparisonData()
        viewModel.loadRecurringPayments()
        binding.swipeRefresh?.isRefreshing = false
    }
    
    // ==================== Recurring Payments Real Data Loading ====================
    
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
    
    private fun renderRecurringInsights(insights: RecurringPaymentsInsight) {
        // Find views
        val insightsCard = binding.root.findViewById<View>(R.id.cardRecurringInsights) ?: return
        val tvSummary = insightsCard.findViewById<android.widget.TextView>(R.id.tvInsightSummary)
        val tvCount = insightsCard.findViewById<android.widget.TextView>(R.id.tvTotalCount)
        val tvSpend = insightsCard.findViewById<android.widget.TextView>(R.id.tvTotalSpend)
        val cardSavings = insightsCard.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSavingsSuggestion)
        val tvSavings = insightsCard.findViewById<android.widget.TextView>(R.id.tvSavingsSuggestion)
        
        // Highlight text with colors (amounts and percentages)
        TextHighlightUtils.highlightInsightText(tvSummary, insights.insightText)
        
        // Animate counters with roll-up effect
        PremiumAnimationUtils.animateNumberRollUp(
            tvCount,
            insights.totalRecurringCount,
            duration = 800L
        )
        
        PremiumAnimationUtils.animateCurrencyRollUp(
            tvSpend,
            insights.totalMonthlySpend,
            duration = 1000L
        )
        
        // Fade in card
        PremiumAnimationUtils.fadeInSlideUp(insightsCard, duration = 300L, startDelay = 100L)
        
        // Show/hide savings suggestion with pop animation
        if (insights.potentialSavings > 0 && insights.savingsSuggestion.isNotEmpty()) {
            cardSavings.visibility = View.VISIBLE
            tvSavings.text = insights.savingsSuggestion
            
            // Pop in with bounce
            PremiumAnimationUtils.popInBadge(cardSavings, duration = 400L)
        } else {
            cardSavings.visibility = View.GONE
        }
    }
    
    private fun setupToggleChips() {
        binding.root.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupViewMode)?.let { chipGroup ->
            chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
                val chipId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
                
                when (chipId) {
                    R.id.chipAbsolute -> {
                        showPercentages = false
                        renderMonthComparisonChart(viewModel.monthComparisonData.value, false)
                    }
                    R.id.chipPercentage -> {
                        showPercentages = true
                        renderMonthComparisonChart(viewModel.monthComparisonData.value, true)
                    }
                }
            }
        }
    }
    
    private fun showRecurringShimmer() {
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
        val content = binding.rvRecurringPayments
        
        // Use premium shimmer to content transition
        PremiumAnimationUtils.shimmerToContentTransition(
            shimmer,
            content,
            fadeOutDuration = 200L,
            fadeInDuration = 300L
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
