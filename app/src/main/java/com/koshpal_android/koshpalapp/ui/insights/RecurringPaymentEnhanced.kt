package com.koshpal_android.koshpalapp.ui.insights

import com.koshpal_android.koshpalapp.model.Transaction

/**
 * Enhanced data model for recurring payments with month-over-month comparison
 */
data class RecurringPaymentEnhanced(
    val merchantName: String,
    val merchantInitials: String, // For avatar
    val category: String, // For badges like "Streaming", "Bills", "Subscription"
    val currentMonthAmount: Double,
    val previousMonthAmount: Double,
    val frequency: String, // "Monthly", "Weekly", etc.
    val consecutiveMonths: Int, // How many consecutive months detected
    val subscriptionConfidence: Float, // 0-1 confidence score
    val firstDetectedDate: Long,
    val lastTransactionDate: Long,
    val recentTransactions: List<Transaction>, // Last 3 transactions for expandable view
    val isEssential: Boolean = false,
    val isReimbursable: Boolean = false,
    val categoryTag: String = getCategoryTag(merchantName)
) {
    val amountChange: Double
        get() = currentMonthAmount - previousMonthAmount
    
    val percentageChange: Float
        get() = if (previousMonthAmount > 0) {
            ((amountChange / previousMonthAmount) * 100).toFloat()
        } else 0f
    
    val hasIncreased: Boolean
        get() = amountChange > 0
    
    val hasDecreased: Boolean
        get() = amountChange < 0
    
    val isStable: Boolean
        get() = kotlin.math.abs(amountChange) < 10.0 // Less than ₹10 change
    
    val statusText: String
        get() = when {
            isStable -> "Stable"
            hasIncreased -> "Increased"
            hasDecreased -> "Decreased"
            else -> "Recurring"
        }
    
    val monthlyAverage: Double
        get() = (currentMonthAmount + previousMonthAmount) / 2.0
    
    companion object {
        fun getCategoryTag(merchantName: String): String {
            return when {
                merchantName.contains("netflix", ignoreCase = true) -> "Streaming"
                merchantName.contains("spotify", ignoreCase = true) -> "Music"
                merchantName.contains("amazon", ignoreCase = true) -> "Shopping"
                merchantName.contains("prime", ignoreCase = true) -> "Streaming"
                merchantName.contains("hotstar", ignoreCase = true) -> "Streaming"
                merchantName.contains("zee5", ignoreCase = true) -> "Streaming"
                merchantName.contains("vodafone", ignoreCase = true) -> "Telecom"
                merchantName.contains("jio", ignoreCase = true) -> "Telecom"
                merchantName.contains("airtel", ignoreCase = true) -> "Telecom"
                merchantName.contains("electricity", ignoreCase = true) -> "Bills"
                merchantName.contains("gas", ignoreCase = true) -> "Bills"
                merchantName.contains("water", ignoreCase = true) -> "Bills"
                merchantName.contains("gym", ignoreCase = true) -> "Fitness"
                merchantName.contains("swiggy", ignoreCase = true) -> "Food"
                merchantName.contains("zomato", ignoreCase = true) -> "Food"
                else -> "Subscription"
            }
        }
        
        fun getMerchantInitials(merchantName: String): String {
            val words = merchantName.split(" ", "-", "_")
            return if (words.size >= 2) {
                "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
            } else {
                merchantName.take(2).uppercase()
            }
        }
    }
}

/**
 * Smart insights for recurring payments section
 */
data class RecurringPaymentsInsight(
    val totalRecurringCount: Int,
    val totalMonthlySpend: Double,
    val topRecurringPayments: List<RecurringPaymentEnhanced>,
    val potentialSavings: Double, // Estimated savings from cancelling low-usage subscriptions
    val savingsSuggestion: String, // Auto-generated text
    val insightText: String // Summary text
)
