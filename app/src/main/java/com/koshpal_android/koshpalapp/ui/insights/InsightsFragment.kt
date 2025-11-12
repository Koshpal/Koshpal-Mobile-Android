package com.koshpal_android.koshpalapp.ui.insights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.ui.insights.compose.InsightsScreen
import com.koshpal_android.koshpalapp.ui.insights.compose.InsightsScreenData
import com.koshpal_android.koshpalapp.ui.insights.compose.SpendingTrendsData
import com.koshpal_android.koshpalapp.ui.insights.compose.TopMerchantsData
import com.koshpal_android.koshpalapp.ui.theme.KoshpalTheme
import java.util.*
import kotlin.math.*

@AndroidEntryPoint
class InsightsFragment : Fragment() {

    @Inject
    lateinit var transactionRepository: TransactionRepository
    
    private val viewModel: InsightsViewModel by viewModels()
    
    // Performance optimization: Cache frequently used data
    private var cachedTransactions: List<com.koshpal_android.koshpalapp.model.Transaction>? = null
    private var lastDataLoadTime: Long = 0
    private val DATA_CACHE_DURATION = 5 * 60 * 1000L // 5 minutes

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                KoshpalTheme {
                    InsightsScreenContent()
                }
            }
        }
    }

    @Composable
    private fun InsightsScreenContent() {
        // Observe ViewModel state
        val recurringPayments by viewModel.recurringPayments.collectAsState()
        val recurringInsights by viewModel.recurringInsights.collectAsState()
        val monthComparisonData by viewModel.monthComparisonData.collectAsState()
        val comparisonInsight by viewModel.comparisonInsight.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val isLoadingRecurring by viewModel.isLoadingRecurring.collectAsState()
        
        // Load merchant data
        var topMerchantsData by remember { mutableStateOf(TopMerchantsData(emptyList(), emptyList())) }
        
        LaunchedEffect(Unit) {
            loadInsightsData()
            viewModel.loadMonthComparisonData()
            viewModel.loadRecurringPayments()
        }
        
        // Load merchant hotspots data
        LaunchedEffect(Unit) {
            val context = requireContext().applicationContext
            val db = KoshpalDatabase.getDatabase(context)
            val transactionDao = db.transactionDao()
            
            val allTransactions = withContext(Dispatchers.IO) {
                transactionDao.getAllTransactionsOnce()
            }
            
            val merchantsData = loadMerchantHotspotsData(allTransactions)
            topMerchantsData = merchantsData
        }
        
        // Prepare screen data
        val screenData = InsightsScreenData(
            recurringPayments = recurringPayments,
            recurringInsights = recurringInsights,
            spendingTrends = SpendingTrendsData(
                comparisonData = monthComparisonData,
                insights = comparisonInsight,
                showPercentages = false
            ),
            topMerchants = topMerchantsData,
            isLoading = isLoading || isLoadingRecurring
        )
        
        val context = requireContext()
        val onProfileClick: () -> Unit = remember {
            {
                val intent = android.content.Intent(context, com.koshpal_android.koshpalapp.ui.profile.ProfileActivity::class.java)
                startActivity(intent)
            }
        }
        
        InsightsScreen(
            data = screenData,
            onBackClick = {
                (activity as? com.koshpal_android.koshpalapp.ui.home.HomeActivity)?.onBackPressed()
            },
            onRecurringPaymentClick = { payment ->
                // Handle recurring payment click
            },
            onCategoryClick = { categoryId ->
                // Handle category click
            },
            onProfileClick = onProfileClick
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Auto-refresh when transactions flow emits updates
        viewLifecycleOwner.lifecycleScope.launch {
            transactionRepository.getAllTransactions().collect {
                // Clear cache and reload insights when any transaction changes
                cachedTransactions = null
                lastDataLoadTime = 0
                loadInsightsData()
            }
        }
    }


    private fun loadInsightsData() {
        viewLifecycleOwner.lifecycleScope.launch {
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
                
            } catch (e: Exception) {
                android.util.Log.e("InsightsFragment", "Failed to load insights: ${e.message}")
            }
        }
    }

    // Merchant Hotspots Data Loading (Split into Credit & Debit) - Returns data for Compose
    private suspend fun loadMerchantHotspotsData(allTransactions: List<com.koshpal_android.koshpalapp.model.Transaction>): TopMerchantsData {
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
        
        android.util.Log.d("InsightsFragment", "\n🎨 Processing merchant data...\n")
        
        // Convert to TopMerchantProgress format
        val maxCreditAmount = topCreditMerchants.maxOfOrNull { it.second } ?: 1.0
        val totalCredit = topCreditMerchants.sumOf { it.second }
        val creditProgressItems = topCreditMerchants.map { (merchant, amount) ->
            TopMerchantProgress(
                merchantName = merchant,
                amount = amount,
                percentageOfMax = (amount / maxCreditAmount).toFloat(),
                sharePercentage = if (totalCredit > 0) (amount / totalCredit * 100).toFloat() else 0f
            )
        }
        
        val maxDebitAmount = topDebitMerchants.maxOfOrNull { it.second } ?: 1.0
        val totalDebit = topDebitMerchants.sumOf { it.second }
        val debitProgressItems = topDebitMerchants.map { (merchant, amount) ->
            TopMerchantProgress(
                merchantName = merchant,
                amount = amount,
                percentageOfMax = (amount / maxDebitAmount).toFloat(),
                sharePercentage = if (totalDebit > 0) (amount / totalDebit * 100).toFloat() else 0f
            )
        }
        
        return TopMerchantsData(
            moneyReceivedFrom = creditProgressItems,
            moneySpentOn = debitProgressItems
        )
    }

    // Helper Methods
    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
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
}
