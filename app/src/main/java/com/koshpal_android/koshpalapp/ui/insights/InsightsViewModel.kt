package com.koshpal_android.koshpalapp.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.data.local.dao.TransactionDao
import com.koshpal_android.koshpalapp.model.CategorySpending
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import com.koshpal_android.koshpalapp.R

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val transactionDao: TransactionDao
) : ViewModel() {

    private val _monthComparisonData = MutableStateFlow<List<MonthComparisonData>>(emptyList())
    val monthComparisonData: StateFlow<List<MonthComparisonData>> = _monthComparisonData.asStateFlow()

    private val _comparisonInsight = MutableStateFlow<MonthComparisonInsight?>(null)
    val comparisonInsight: StateFlow<MonthComparisonInsight?> = _comparisonInsight.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Recurring payments state
    private val _recurringPayments = MutableStateFlow<List<RecurringPaymentEnhanced>>(emptyList())
    val recurringPayments: StateFlow<List<RecurringPaymentEnhanced>> = _recurringPayments.asStateFlow()
    
    private val _recurringInsights = MutableStateFlow<RecurringPaymentsInsight?>(null)
    val recurringInsights: StateFlow<RecurringPaymentsInsight?> = _recurringInsights.asStateFlow()
    
    private val _isLoadingRecurring = MutableStateFlow(false)
    val isLoadingRecurring: StateFlow<Boolean> = _isLoadingRecurring.asStateFlow()

    fun loadMonthComparisonData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val comparisonData = withContext(Dispatchers.IO) {
                    fetchAndProcessMonthComparison()
                }
                _monthComparisonData.value = comparisonData
                _comparisonInsight.value = generateSmartInsights(comparisonData)
            } catch (e: Exception) {
                android.util.Log.e("InsightsViewModel", "Error loading comparison data: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchAndProcessMonthComparison(): List<MonthComparisonData> {
        val currentMonthRange = getCurrentMonthRange()
        val previousMonthRange = getPreviousMonthRange()

        // Fetch spending data from Room DB
        val currentMonthSpending = transactionDao.getMonthlySpendingByCategory(
            currentMonthRange.first,
            currentMonthRange.second
        )
        val previousMonthSpending = transactionDao.getMonthlySpendingByCategory(
            previousMonthRange.first,
            previousMonthRange.second
        )

        // Create a map for easy lookup
        val previousMap = previousMonthSpending.associateBy { it.categoryId }
        val currentMap = currentMonthSpending.associateBy { it.categoryId }

        // Get all unique categories
        val allCategories = (currentMonthSpending.map { it.categoryId } + 
                            previousMonthSpending.map { it.categoryId }).distinct()

        // Process comparison data
        return allCategories.mapNotNull { categoryId ->
            val currentAmount = currentMap[categoryId]?.totalAmount ?: 0.0
            val previousAmount = previousMap[categoryId]?.totalAmount ?: 0.0

            // Skip if both are zero
            if (currentAmount == 0.0 && previousAmount == 0.0) return@mapNotNull null

            val absoluteChange = currentAmount - previousAmount
            val percentageChange = if (previousAmount > 0) {
                ((absoluteChange / previousAmount) * 100).toFloat()
            } else if (currentAmount > 0) {
                100f // New spending category
            } else {
                0f
            }

            val categoryDetails = getCategoryDetails(categoryId)
            
            MonthComparisonData(
                categoryId = categoryId,
                categoryName = mapCategoryIdToName(categoryId),
                currentMonthAmount = currentAmount,
                previousMonthAmount = previousAmount,
                percentageChange = percentageChange,
                absoluteChange = absoluteChange,
                categoryColor = categoryDetails.first,
                categoryIcon = categoryDetails.second
            )
        }.sortedByDescending { it.currentMonthAmount }
    }

    private fun generateSmartInsights(comparisonData: List<MonthComparisonData>): MonthComparisonInsight {
        if (comparisonData.isEmpty()) {
            return MonthComparisonInsight(
                insightText = "No spending data available for comparison.",
                topIncreases = emptyList(),
                topDecreases = emptyList(),
                overallSavingChange = 0.0,
                totalCurrentMonthSpending = 0.0,
                totalPreviousMonthSpending = 0.0
            )
        }

        // Calculate totals
        val totalCurrent = comparisonData.sumOf { it.currentMonthAmount }
        val totalPrevious = comparisonData.sumOf { it.previousMonthAmount }
        val overallSavingChange = totalPrevious - totalCurrent

        // Get top increases and decreases
        val topIncreases = comparisonData
            .filter { it.isIncrease && it.hasSignificantChange }
            .sortedByDescending { it.percentageChange }
            .take(3)

        val topDecreases = comparisonData
            .filter { it.isDecrease && it.hasSignificantChange }
            .sortedBy { it.percentageChange }
            .take(3)

        // Generate insight text
        val insightText = buildInsightText(topIncreases, topDecreases, overallSavingChange)

        return MonthComparisonInsight(
            insightText = insightText,
            topIncreases = topIncreases,
            topDecreases = topDecreases,
            overallSavingChange = overallSavingChange,
            totalCurrentMonthSpending = totalCurrent,
            totalPreviousMonthSpending = totalPrevious
        )
    }

    private fun buildInsightText(
        topIncreases: List<MonthComparisonData>,
        topDecreases: List<MonthComparisonData>,
        overallSavingChange: Double
    ): String {
        val parts = mutableListOf<String>()

        // Add top increase
        if (topIncreases.isNotEmpty()) {
            val top = topIncreases.first()
            parts.add("${top.categoryName} spending ↑ ${kotlin.math.abs(top.percentageChange).toInt()}%")
        }

        // Add top decrease
        if (topDecreases.isNotEmpty()) {
            val top = topDecreases.first()
            parts.add("${top.categoryName} ↓ ${kotlin.math.abs(top.percentageChange).toInt()}%")
        }

        // Add overall saving change
        val savingText = if (overallSavingChange > 0) {
            "Overall saving ↑ ₹${String.format("%.0f", kotlin.math.abs(overallSavingChange))} compared to last month"
        } else if (overallSavingChange < 0) {
            "Overall spending ↑ ₹${String.format("%.0f", kotlin.math.abs(overallSavingChange))} compared to last month"
        } else {
            "Overall spending unchanged from last month"
        }
        parts.add(savingText)

        return parts.joinToString(", ") + "."
    }

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

    private fun getPreviousMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
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

    private fun getCategoryDetails(categoryId: String): Pair<String, Int> {
        return when (categoryId.lowercase()) {
            "food" -> Pair("#FF9800", R.drawable.ic_food_dining)
            "grocery" -> Pair("#4CAF50", R.drawable.ic_grocery_cart)
            "transport", "travel" -> Pair("#2196F3", R.drawable.ic_transport_car)
            "bills" -> Pair("#FFC107", R.drawable.ic_bills_receipt)
            "education" -> Pair("#9C27B0", R.drawable.ic_education_book)
            "entertainment" -> Pair("#E91E63", R.drawable.ic_entertainment_movie)
            "healthcare" -> Pair("#00BCD4", R.drawable.ic_healthcare_medical)
            "shopping" -> Pair("#795548", R.drawable.ic_shopping_bag)
            "rent" -> Pair("#607D8B", R.drawable.ic_category_default)
            else -> Pair("#607D8B", R.drawable.ic_category_default)
        }
    }
    
    // ==================== Recurring Payments Detection ====================
    
    fun loadRecurringPayments() {
        viewModelScope.launch {
            _isLoadingRecurring.value = true
            try {
                val recurringData = withContext(Dispatchers.IO) {
                    detectRecurringPayments()
                }
                _recurringPayments.value = recurringData
                _recurringInsights.value = generateRecurringInsights(recurringData)
            } catch (e: Exception) {
                android.util.Log.e("InsightsViewModel", "Error loading recurring payments: ${e.message}")
            } finally {
                _isLoadingRecurring.value = false
            }
        }
    }
    
    private suspend fun detectRecurringPayments(): List<RecurringPaymentEnhanced> {
        // Get last 3 months of transactions
        val currentMonthRange = getCurrentMonthRange()
        val previousMonthRange = getPreviousMonthRange()
        val twoMonthsAgoRange = getTwoMonthsAgoRange()
        
        // Fetch all debit transactions from last 3 months
        val allTransactions = transactionDao.getAllTransactionsOnce()
        val debitTransactions = allTransactions.filter { 
            it.type == com.koshpal_android.koshpalapp.model.TransactionType.DEBIT &&
            it.date >= twoMonthsAgoRange.first
        }
        
        // Group by normalized merchant name
        val groupedByMerchant = debitTransactions.groupBy { normalizeMerchantName(it.merchant) }
        
        val recurringPayments = mutableListOf<RecurringPaymentEnhanced>()
        
        groupedByMerchant.forEach { (merchant, transactions) ->
            // Check if merchant appears in at least 2 consecutive months
            val currentMonthTxns = transactions.filter { it.date in currentMonthRange.first..currentMonthRange.second }
            val previousMonthTxns = transactions.filter { it.date in previousMonthRange.first..previousMonthRange.second }
            val twoMonthsAgoTxns = transactions.filter { it.date in twoMonthsAgoRange.first..twoMonthsAgoRange.second }
            
            val monthsWithTransactions = listOf(
                currentMonthTxns.isNotEmpty(),
                previousMonthTxns.isNotEmpty(),
                twoMonthsAgoTxns.isNotEmpty()
            ).count { it }
            
            // Must appear in at least 2 months (and current + previous for comparison)
            if (monthsWithTransactions >= 2 && currentMonthTxns.isNotEmpty() && previousMonthTxns.isNotEmpty()) {
                val currentAmount = currentMonthTxns.sumOf { it.amount }
                val previousAmount = previousMonthTxns.sumOf { it.amount }
                
                // Calculate frequency
                val allDates = transactions.map { it.date }.sorted()
                val frequency = calculatePaymentFrequency(allDates)
                
                // Calculate confidence score
                val amountVariance = calculateAmountVariance(transactions.map { it.amount })
                val confidence = calculateSubscriptionConfidence(monthsWithTransactions, amountVariance, frequency)
                
                // Get category from first transaction
                val category = transactions.firstOrNull()?.categoryId ?: "others"
                
                // Prefer showing current month transactions in the expanded view;
                // if none, include previous month; otherwise fallback to the latest 3
                val recentForUi = (currentMonthTxns + previousMonthTxns)
                    .sortedByDescending { it.date }
                    .take(3)
                    .ifEmpty { transactions.sortedByDescending { it.date }.take(3) }

                recurringPayments.add(
                    RecurringPaymentEnhanced(
                        merchantName = merchant,
                        merchantInitials = RecurringPaymentEnhanced.getMerchantInitials(merchant),
                        category = category,
                        currentMonthAmount = currentAmount,
                        previousMonthAmount = previousAmount,
                        frequency = frequency,
                        consecutiveMonths = monthsWithTransactions,
                        subscriptionConfidence = confidence,
                        firstDetectedDate = allDates.firstOrNull() ?: System.currentTimeMillis(),
                        lastTransactionDate = allDates.lastOrNull() ?: System.currentTimeMillis(),
                        recentTransactions = recentForUi,
                        categoryTag = RecurringPaymentEnhanced.getCategoryTag(merchant)
                    )
                )
            }
        }
        
        // Sort by current month amount descending
        return recurringPayments.sortedByDescending { it.currentMonthAmount }
    }
    
    private fun normalizeMerchantName(merchant: String): String {
        return merchant.lowercase()
            .replace(Regex("(upi|imps|neft|ref|txn|id|pos|card|debit|credit|payment|subscription)"), "")
            .replace(Regex("\\d+"), "")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    
    private fun calculatePaymentFrequency(dates: List<Long>): String {
        if (dates.size < 2) return "Monthly"
        
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
    
    private fun calculateAmountVariance(amounts: List<Double>): Double {
        if (amounts.isEmpty()) return 0.0
        val average = amounts.average()
        val variance = amounts.map { (it - average) * (it - average) }.average()
        return kotlin.math.sqrt(variance) / average
    }
    
    private fun calculateSubscriptionConfidence(monthsCount: Int, amountVariance: Double, frequency: String): Float {
        var score = 0f
        
        // Months score (0-40 points)
        score += (monthsCount * 13.3f).coerceAtMost(40f)
        
        // Amount consistency score (0-30 points)
        score += (30f * (1f - amountVariance.toFloat())).coerceAtLeast(0f).coerceAtMost(30f)
        
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
    
    private fun generateRecurringInsights(payments: List<RecurringPaymentEnhanced>): RecurringPaymentsInsight {
        if (payments.isEmpty()) {
            return RecurringPaymentsInsight(
                totalRecurringCount = 0,
                totalMonthlySpend = 0.0,
                topRecurringPayments = emptyList(),
                potentialSavings = 0.0,
                savingsSuggestion = "",
                insightText = "No recurring payments detected yet."
            )
        }
        
        val totalSpend = payments.sumOf { it.currentMonthAmount }
        val topPayments = payments.take(3)
        
        // Calculate potential savings (subscriptions with low usage or duplicates)
        val potentialSavings = payments.filter { it.currentMonthAmount > 199 && it.categoryTag == "Streaming" }
            .sumOf { it.currentMonthAmount * 0.5 } // Assume 50% could be saved
        
        // Generate insight text
        val topMerchants = topPayments.joinToString(", ") { "${it.merchantName} ₹${it.currentMonthAmount.toInt()}" }
        val insightText = "We detected ${payments.size} recurring payments: $topMerchants."
        
        val savingsSuggestion = if (potentialSavings > 0) {
            "You could save ₹${potentialSavings.toInt()}/month by reviewing streaming subscriptions."
        } else {
            ""
        }
        
        return RecurringPaymentsInsight(
            totalRecurringCount = payments.size,
            totalMonthlySpend = totalSpend,
            topRecurringPayments = topPayments,
            potentialSavings = potentialSavings,
            savingsSuggestion = savingsSuggestion,
            insightText = insightText
        )
    }
    
    private fun getTwoMonthsAgoRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -2)
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
}
