package com.koshpal_android.koshpalapp.ui.insights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*
import com.koshpal_android.koshpalapp.R

@AndroidEntryPoint
class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var transactionRepository: TransactionRepository

    private lateinit var recurringPaymentAdapter: RecurringPaymentAdapter
    private lateinit var budgetCategoryProgressAdapterModern: BudgetCategoryProgressAdapterModern
    private lateinit var topCreditMerchantAdapter: TopMerchantProgressAdapter
    private lateinit var topDebitMerchantAdapter: TopMerchantProgressAdapter
    
    // Month selector
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    
    // Performance optimization: Cache frequently used data
    private var cachedTransactions: List<com.koshpal_android.koshpalapp.model.Transaction>? = null
    private var lastDataLoadTime: Long = 0
    private val DATA_CACHE_DURATION = 5 * 60 * 1000L // 5 minutes

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
    }

    private fun setupUI() {
        binding.apply {
            // Setup charts
            setupMerchantHotspotsChart()
            // setupCategoryDistributionChart() // Removed as per design

            // Setup recurring payments adapter
            recurringPaymentAdapter = RecurringPaymentAdapter(
                onMarkEssential = { item -> markRecurringAsEssential(item) },
                onCancelSuggestion = { item -> showCancelSuggestion(item) },
                onMarkReimbursable = { item -> markRecurringAsReimbursable(item) }
            )
            rvRecurringPayments.layoutManager = LinearLayoutManager(requireContext())
            rvRecurringPayments.adapter = recurringPaymentAdapter

            // Setup Budget Category Progress RecyclerView (modern)
            budgetCategoryProgressAdapterModern = BudgetCategoryProgressAdapterModern()
            rvBudgetCategories.layoutManager = LinearLayoutManager(requireContext())
            rvBudgetCategories.adapter = budgetCategoryProgressAdapterModern
            
            // Setup Top Credit Merchants RecyclerView (Money IN)
            topCreditMerchantAdapter = TopMerchantProgressAdapter()
            rvTopCreditMerchants.layoutManager = LinearLayoutManager(requireContext())
            rvTopCreditMerchants.adapter = topCreditMerchantAdapter
            android.util.Log.d("InsightsFragment", "✅ Top Credit Merchants RecyclerView setup complete")
            
            // Setup Top Debit Merchants RecyclerView (Money OUT)
            topDebitMerchantAdapter = TopMerchantProgressAdapter()
            rvTopDebitMerchants.layoutManager = LinearLayoutManager(requireContext())
            rvTopDebitMerchants.adapter = topDebitMerchantAdapter
            android.util.Log.d("InsightsFragment", "✅ Top Debit Merchants RecyclerView setup complete")
            
            // Add pull-to-refresh functionality
            setupPullToRefresh()

            // Setup month selector click listener
            tvMonthSelector.setOnClickListener {
                showMonthPickerDialog()
            }
            
            // Update month selector text
            updateMonthSelectorText()

            // Adjust button removed per design
        }
    }

    private fun loadInsightsData() {
        lifecycleScope.launch {
            try {
                val context = requireContext().applicationContext
                val db = KoshpalDatabase.getDatabase(context)
                val transactionDao = db.transactionDao()
                val budgetDao = db.budgetNewDao()
                val budgetCategoryDao = db.budgetCategoryNewDao()

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

                // Load all insights data in parallel for better performance
                val budgetJob = async { loadBudgetUsageData(allTransactions, budgetDao, budgetCategoryDao) }
                // Spending Trend removed
                val recurringJob = async { loadRecurringPaymentsData(allTransactions) }
                val merchantJob = async { loadMerchantHotspotsData(allTransactions) }

                // Wait for all jobs to complete
                budgetJob.await()
                recurringJob.await()
                merchantJob.await()
                
            } catch (e: Exception) {
                android.util.Log.e("InsightsFragment", "Failed to load insights: ${e.message}")
                Toast.makeText(requireContext(), "Failed to load insights data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 1. Budget Usage Data Loading
    private suspend fun loadBudgetUsageData(
        allTransactions: List<com.koshpal_android.koshpalapp.model.Transaction>,
        budgetDao: com.koshpal_android.koshpalapp.data.local.dao.BudgetNewDao,
        budgetCategoryDao: com.koshpal_android.koshpalapp.data.local.dao.BudgetCategoryNewDao
    ) {
        val currentMonth = getCurrentMonthRange()
        val currentMonthExpenses = allTransactions.filter { 
            it.type == TransactionType.DEBIT && it.date in currentMonth.first..currentMonth.second 
        }.sumOf { it.amount }

        val budget = withContext(Dispatchers.IO) { budgetDao.getSingleBudget() }
        if (budget != null) {
            val categories = withContext(Dispatchers.IO) { budgetCategoryDao.getCategoriesForBudget(budget.id) }
            
            // Update KPI values
            binding.tvTotalBudget.text = "₹${String.format("%.0f", budget.totalBudget)}"
            binding.tvSpentThisMonth.text = "₹${String.format("%.0f", currentMonthExpenses)}"
            
            val percentUsed = (currentMonthExpenses / budget.totalBudget * 100).coerceAtMost(100.0)
            binding.tvPercentUsed.text = "${String.format("%.0f", percentUsed)}%"
            
            // Set warning color based on usage
            val percentColor = when {
                percentUsed >= 100 -> requireContext().getColor(com.koshpal_android.koshpalapp.R.color.error)
                percentUsed >= 80 -> requireContext().getColor(com.koshpal_android.koshpalapp.R.color.warning)
                else -> requireContext().getColor(com.koshpal_android.koshpalapp.R.color.success)
            }
            binding.tvPercentUsed.setTextColor(percentColor)
            
            // Warning badge removed per design
            
            // Calculate and display budget category progress using repository (same as Categories screen)
            val repoSpending = withContext(Dispatchers.IO) {
                transactionRepository.getCurrentMonthCategorySpending(currentMonth.first, currentMonth.second)
            }
            val spendingById = repoSpending.associateBy { it.categoryId }

            val budgetCategoryProgressList = categories.map { bc ->
                val categoryId = mapBudgetNameToCategoryId(bc.name)
                val spentAmount = spendingById[categoryId]?.totalAmount ?: 0.0
                val percentageUsed = if (bc.allocatedAmount > 0) (spentAmount / bc.allocatedAmount).toFloat() else 0f
                val remainingAmount = bc.allocatedAmount - spentAmount
                val isOverBudget = spentAmount > bc.allocatedAmount
                val details = getCategoryDetails(bc.name)

                BudgetCategoryProgress(
                    categoryName = bc.name,
                    categoryId = categoryId,
                    allocatedAmount = bc.allocatedAmount,
                    spentAmount = spentAmount,
                    percentageUsed = percentageUsed,
                    remainingAmount = remainingAmount,
                    isOverBudget = isOverBudget,
                    categoryColor = details.first,
                    categoryIcon = details.second
                )
            }.sortedByDescending { it.percentageUsed }

            android.util.Log.d("InsightsFragment", "Budget categories found: ${categories.size}")
            android.util.Log.d("InsightsFragment", "Budget progress items: ${budgetCategoryProgressList.size}")

            budgetCategoryProgressAdapterModern.submitList(budgetCategoryProgressList)
            
            // Show a message if no budget categories are set
            if (budgetCategoryProgressList.isEmpty()) {
                Toast.makeText(requireContext(), "No budget categories set. Go to Budget settings to set category limits.", Toast.LENGTH_LONG).show()
                
                // For testing purposes, show sample data if no budget categories are set
                val sampleProgressList = createSampleBudgetProgressData()
                budgetCategoryProgressAdapterModern.submitList(sampleProgressList)
            }
            
            // Old donut/sparkline charts removed in favor of per-category progress bars
            // renderBudgetUsageChart(categories, currentMonthExpenses)
            // renderBudgetTrendSparkline(allTransactions)
        } else {
            // No budget set
            binding.tvTotalBudget.text = "₹0"
            binding.tvSpentThisMonth.text = "₹${String.format("%.0f", currentMonthExpenses)}"
            binding.tvPercentUsed.text = "0%"
            // Warning badge removed per design
        }
    }

    // 3. Recurring Payments Data Loading
    private fun loadRecurringPaymentsData(allTransactions: List<com.koshpal_android.koshpalapp.model.Transaction>) {
        val recurringPayments = detectRecurringPayments(allTransactions)
        
        binding.tvRecurringCount.text = "${recurringPayments.size} found"
        recurringPaymentAdapter.submitList(recurringPayments)
    }

    // 4. Merchant Hotspots Data Loading (Split into Credit & Debit)
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

    private fun detectRecurringPayments(transactions: List<com.koshpal_android.koshpalapp.model.Transaction>): List<RecurringPaymentItem> {
        val result = mutableListOf<RecurringPaymentItem>()

        // Consider last 3 full months: M0 (current), M-1, M-2
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val monthStarts = mutableListOf<Long>()
        val monthEnds = mutableListOf<Long>()
        for (i in 0..2) {
            val c = cal.clone() as Calendar
            c.add(Calendar.MONTH, -i)
            val start = c.timeInMillis
            c.add(Calendar.MONTH, 1)
            c.add(Calendar.MILLISECOND, -1)
            val end = c.timeInMillis
            monthStarts.add(start)
            monthEnds.add(end)
        }

        val last3monthsTx = transactions.filter { it.type == TransactionType.DEBIT && it.date >= monthStarts.last() && it.date <= monthEnds.first() }
        val groups = last3monthsTx.groupBy { normalizeMerchantName(it.merchant) }

        groups.forEach { (merchant, txs) ->
            // Check presence in each month
            var monthsWithTxn = 0
            for (i in 0..2) {
                val hasTxn = txs.any { it.date in monthStarts[i]..monthEnds[i] }
                if (hasTxn) monthsWithTxn++
            }
            if (monthsWithTxn == 3) {
                // Monthly average over last 3 months (sum per month / 3)
                var sum3 = 0.0
                for (i in 0..2) {
                    val monthSum = txs.filter { it.date in monthStarts[i]..monthEnds[i] }.sumOf { it.amount }
                    sum3 += monthSum
                }
                val monthlyAvg = sum3 / 3.0
                val dates = txs.map { it.date }.sorted()
                val timelineData = getTimelineData(transactions, merchant, 6)

                result.add(
                    RecurringPaymentItem(
                        merchantName = merchant,
                        monthlyAvgAmount = monthlyAvg,
                        frequency = "Monthly (3/3)",
                        last3MonthsFrequency = 3,
                        subscriptionScore = 0.9f,
                        firstSeen = dates.firstOrNull() ?: monthStarts.last(),
                        lastSeen = dates.lastOrNull() ?: monthEnds.first(),
                        timelineData = timelineData
                    )
                )
            }
        }

        return result.sortedByDescending { it.monthlyAvgAmount }
    }

    private fun normalizeMerchantName(merchant: String): String {
        return merchant.lowercase()
            .replace(Regex("(upi|imps|neft|ref|txn|id|pos|card|debit|credit)"), "")
            .replace(Regex("\\d+"), "")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun calculateFrequency(dates: List<Long>): String {
        if (dates.size < 2) return "Unknown"
        
        val intervals = dates.zipWithNext().map { (first, second) -> second - first }
        val avgInterval = intervals.average()
        val days = avgInterval / (24 * 60 * 60 * 1000)
        
        return when {
            days <= 7 -> "Weekly"
            days <= 14 -> "Bi-weekly"
            days <= 35 -> "Monthly"
            days <= 70 -> "Bi-monthly"
            else -> "Quarterly"
        }
    }

    private fun calculateSubscriptionScore(transactionCount: Int, amountVariance: Double, frequency: String): Float {
        var score = 0f
        
        // Transaction count score (0-40 points)
        score += (transactionCount * 10f).coerceAtMost(40f)
        
        // Amount consistency score (0-30 points)
        score += (30f * (1f - amountVariance.toFloat())).coerceAtLeast(0f)
        
        // Frequency score (0-30 points)
        score += when (frequency) {
            "Monthly" -> 30f
            "Weekly" -> 25f
            "Bi-weekly" -> 20f
            "Bi-monthly" -> 15f
            else -> 10f
        }
        
        return (score / 100f).coerceIn(0f, 1f)
    }

    private fun getTimelineData(transactions: List<com.koshpal_android.koshpalapp.model.Transaction>, merchant: String, months: Int): List<Double> {
        val timelineData = mutableListOf<Double>()
        val cal = Calendar.getInstance()
        
        for (i in months - 1 downTo 0) {
            cal.time = Date()
            cal.add(Calendar.MONTH, -i)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val monthStart = cal.timeInMillis
            
            cal.add(Calendar.MONTH, 1)
            cal.add(Calendar.MILLISECOND, -1)
            val monthEnd = cal.timeInMillis
            
            val monthAmount = transactions.filter { 
                it.type == TransactionType.DEBIT && 
                it.date in monthStart..monthEnd && 
                normalizeMerchantName(it.merchant) == merchant
            }.sumOf { it.amount }
            
            timelineData.add(monthAmount)
        }
        
        return timelineData
    }

    // Action handlers for recurring payments
    private fun markRecurringAsEssential(item: RecurringPaymentItem) {
        Toast.makeText(requireContext(), "Marked ${item.merchantName} as essential", Toast.LENGTH_SHORT).show()
        // TODO: Update database
    }

    private fun showCancelSuggestion(item: RecurringPaymentItem) {
        Toast.makeText(requireContext(), "Cancel suggestion for ${item.merchantName}", Toast.LENGTH_SHORT).show()
        // TODO: Show cancel dialog
    }

    private fun markRecurringAsReimbursable(item: RecurringPaymentItem) {
        Toast.makeText(requireContext(), "Marked ${item.merchantName} as reimbursable", Toast.LENGTH_SHORT).show()
        // TODO: Update database
    }

    private fun exportInsightsCsv() {
        try {
            val content = StringBuilder()
                .appendLine("Insights Export - ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}")
                .appendLine("")
                .appendLine("Budget Usage")
                .appendLine("Total Budget,${binding.tvTotalBudget.text}")
                .appendLine("Spent This Month,${binding.tvSpentThisMonth.text}")
                .appendLine("Percent Used,${binding.tvPercentUsed.text}")
                .appendLine("")
                // Spending Trend removed from export
                .toString()
            
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Koshpal Insights Export")
                putExtra(android.content.Intent.EXTRA_TEXT, content)
            }
            startActivity(android.content.Intent.createChooser(intent, "Share insights"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Export failed", Toast.LENGTH_SHORT).show()
        }
    }

    // Chart Setup Methods
    // private fun setupBudgetUsageChart() {
    //     // Deprecated: donut chart removed from layout
    // }

    // setupSpendingTrendChart removed (trend card deleted)

    private fun setupMerchantHotspotsChart() {
        // No chart to setup; replaced with progress rows
    }

    // Category distribution removed per design

    // Chart Rendering Methods
    // private fun renderBudgetUsageChart(categories: List<com.koshpal_android.koshpalapp.model.BudgetCategory>, currentMonthExpenses: Double) {
    //     // Deprecated: donut chart removed from layout
    // }

    // private fun renderBudgetTrendSparkline(allTransactions: List<com.koshpal_android.koshpalapp.model.Transaction>) {
    //     // Deprecated: sparkline removed from layout
    // }

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

    private fun setupPullToRefresh() {
        // Add swipe-to-refresh functionality
        binding.root.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                val startY = event.y
                binding.root.setOnTouchListener { _, moveEvent ->
                    if (moveEvent.action == android.view.MotionEvent.ACTION_UP) {
                        val endY = moveEvent.y
                        if (startY - endY > 100) { // Swipe up to refresh
                            refreshData()
                        }
                    }
                    false
                }
            }
            false
        }
    }
    
    private fun refreshData() {
        // Clear cache and reload data
        cachedTransactions = null
        lastDataLoadTime = 0
        loadInsightsData()
        Toast.makeText(requireContext(), "Refreshing insights...", Toast.LENGTH_SHORT).show()
    }
    
    private fun showMonthPickerDialog() {
        val calendar = Calendar.getInstance()
        calendar.set(selectedYear, selectedMonth, 1)
        
        val builder = android.app.AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_month_picker, null)
        
        val monthPicker = dialogView.findViewById<android.widget.NumberPicker>(R.id.monthPicker)
        val yearPicker = dialogView.findViewById<android.widget.NumberPicker>(R.id.yearPicker)
        
        // Setup month picker
        val months = arrayOf("January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")
        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.displayedValues = months
        monthPicker.value = selectedMonth
        monthPicker.wrapSelectorWheel = false
        
        // Setup year picker (last 5 years to current year)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        yearPicker.minValue = currentYear - 5
        yearPicker.maxValue = currentYear
        yearPicker.value = selectedYear
        yearPicker.wrapSelectorWheel = false
        
        builder.setView(dialogView)
            .setTitle("Select Month")
            .setPositiveButton("OK") { _, _ ->
                selectedMonth = monthPicker.value
                selectedYear = yearPicker.value
                
                // Update UI and reload data
                updateMonthSelectorText()
                refreshData()
            }
            .setNegativeButton("Cancel", null)
        
        builder.create().show()
    }
    
    private fun updateMonthSelectorText() {
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        
        val currentCal = Calendar.getInstance()
        val isCurrentMonth = selectedMonth == currentCal.get(Calendar.MONTH) && 
                             selectedYear == currentCal.get(Calendar.YEAR)
        
        binding.tvMonthSelector.text = if (isCurrentMonth) {
            "This Month"
        } else {
            "${months[selectedMonth]} $selectedYear"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
