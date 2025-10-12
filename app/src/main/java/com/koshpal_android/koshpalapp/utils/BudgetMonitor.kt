package com.koshpal_android.koshpalapp.utils

import android.content.Context
import android.util.Log
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.model.Budget
import com.koshpal_android.koshpalapp.model.BudgetCategory
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class BudgetMonitor private constructor(private val context: Context) {

    companion object {
        private const val TAG = "BudgetMonitor"
        
        // Notification thresholds (as per original requirement)
        private const val THRESHOLD_40_PERCENT = 40  // First warning: 40-50%
        private const val THRESHOLD_90_PERCENT = 90  // Second warning: 90-99%
        private const val THRESHOLD_100_PERCENT = 100 // Budget exceeded: 100%+
        
        @Volatile
        private var INSTANCE: BudgetMonitor? = null

        fun getInstance(context: Context): BudgetMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BudgetMonitor(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Check budget status for all categories and send notifications if needed
     * This should be called whenever a new transaction is added or updated
     */
    fun checkBudgetStatus(transaction: Transaction? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🔍 ===== BUDGET STATUS CHECK STARTED =====")
                Log.d(TAG, "🔍 Triggered by transaction: ${transaction?.merchant ?: "Manual/Other"}")
                
                val database = KoshpalDatabase.getDatabase(context)
                val budgetDao = database.budgetNewDao()
                val budgetCategoryDao = database.budgetCategoryNewDao()
                val transactionDao = database.transactionDao()
                
                // Get current budget
                val budget = budgetDao.getSingleBudget()
                Log.d(TAG, "🔍 DEBUG: Budget query result: $budget")
                if (budget == null) {
                    Log.d(TAG, "📊 No budget set, skipping budget monitoring")
                    Log.d(TAG, "🔍 DEBUG: This means either no budget was saved or there's a database issue")
                    return@launch
                }
                
                Log.d(TAG, "💰 Budget found: Total ₹${budget.totalBudget}, Savings ₹${budget.savings}, ID: ${budget.id}")
                
                // Get all budget categories
                val budgetCategories = budgetCategoryDao.getCategoriesForBudget(budget.id)
                Log.d(TAG, "🔍 DEBUG: Budget categories query result: ${budgetCategories.size} categories")
                if (budgetCategories.isEmpty()) {
                    Log.d(TAG, "📂 No budget categories found for budget ID: ${budget.id}")
                    Log.d(TAG, "🔍 DEBUG: This means budget categories were not saved properly")
                    return@launch
                }
                
                Log.d(TAG, "📂 Found ${budgetCategories.size} budget categories:")
                budgetCategories.forEach { category ->
                    Log.d(TAG, "   - ${category.name}: Budget ₹${category.allocatedAmount}, Spent ₹${category.spentAmount}, ID: ${category.id}")
                }
                
                // DEBUG: Compare with Categories Fragment approach
                debugCompareSpendingCalculations(transactionDao, budgetCategories)
                
                // Calculate current month spending for each category
                val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                
                Log.d(TAG, "📅 Checking spending for month: ${currentMonth + 1}/$currentYear")
                
                for (budgetCategory in budgetCategories) {
                    val categorySpending = calculateCategorySpending(
                        transactionDao, 
                        budgetCategory.name, 
                        currentMonth, 
                        currentYear
                    )
                    
                    val percentage = if (budgetCategory.allocatedAmount > 0) {
                        (categorySpending / budgetCategory.allocatedAmount) * 100
                    } else {
                        0.0
                    }
                    
                    Log.d(TAG, "📊 ${budgetCategory.name}: ₹$categorySpending / ₹${budgetCategory.allocatedAmount} (${String.format("%.1f", percentage)}%)")
                    
                    // Check if we need to send notification
                    Log.d(TAG, "🔔 Checking notification thresholds for ${budgetCategory.name}...")
                    checkAndSendNotification(budgetCategory, categorySpending, percentage)
                    
                    // Update spent amount in database
                    if (budgetCategory.spentAmount != categorySpending) {
                        val updatedCategory = budgetCategory.copy(spentAmount = categorySpending)
                        budgetCategoryDao.update(updatedCategory)
                        Log.d(TAG, "💾 Updated spent amount for ${budgetCategory.name}: ₹$categorySpending")
                    }
                }
                
                Log.d(TAG, "🔍 ===== BUDGET STATUS CHECK COMPLETED =====")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking budget status", e)
                e.printStackTrace()
            }
        }
    }
    
    /**
     * DEBUG: Compare spending calculations between Budget Monitor and Categories Fragment
     */
    private suspend fun debugCompareSpendingCalculations(
        transactionDao: com.koshpal_android.koshpalapp.data.local.dao.TransactionDao,
        budgetCategories: List<BudgetCategory>
    ) {
        try {
            Log.d(TAG, "🔍 ===== DEBUG: COMPARING SPENDING CALCULATIONS =====")
            
            // Get current month date range (same as Categories Fragment)
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis
            
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val endOfMonth = calendar.timeInMillis
            
            Log.d(TAG, "📅 Date range: ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(startOfMonth))} to ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(endOfMonth))}")
            
            // Method 1: Categories Fragment approach
            val categorySpendingFromFragment = transactionDao.getCurrentMonthCategorySpending(startOfMonth, endOfMonth)
            Log.d(TAG, "📊 Categories Fragment approach:")
            categorySpendingFromFragment.forEach { spending ->
                Log.d(TAG, "   - ${spending.categoryId}: ₹${spending.totalAmount}")
            }
            
            // Method 2: Budget Monitor approach
            Log.d(TAG, "📊 Budget Monitor approach:")
            for (budgetCategory in budgetCategories) {
                val categoryId = convertBudgetCategoryNameToTransactionCategoryId(budgetCategory.name)
                val transactions = transactionDao.getTransactionsByCategoryAndDateRange(categoryId, startOfMonth, endOfMonth)
                val totalSpending = transactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
                Log.d(TAG, "   - ${budgetCategory.name} (ID: $categoryId): ₹$totalSpending (${transactions.size} transactions)")
            }
            
            Log.d(TAG, "🔍 ===== END DEBUG COMPARISON =====")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Debug comparison failed", e)
        }
    }
    
    /**
     * Calculate spending for a specific category in current month
     */
    private suspend fun calculateCategorySpending(
        transactionDao: com.koshpal_android.koshpalapp.data.local.dao.TransactionDao,
        categoryName: String,
        month: Int,
        year: Int
    ): Double {
        // Get all transactions for this category in current month
        val startOfMonth = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val endOfMonth = Calendar.getInstance().apply {
            set(year, month, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        
        // CRITICAL FIX: Convert budget category name to transaction category ID
        val categoryId = convertBudgetCategoryNameToTransactionCategoryId(categoryName)
        Log.d(TAG, "🔄 Converting budget category '$categoryName' to transaction category ID '$categoryId'")
        
        val transactions = transactionDao.getTransactionsByCategoryAndDateRange(
            categoryId, 
            startOfMonth, 
            endOfMonth
        )
        
        val totalSpending = transactions
            .filter { it.type == TransactionType.DEBIT } // Only count expenses
            .sumOf { it.amount }
            
        Log.d(TAG, "💰 Found ${transactions.size} transactions for '$categoryName' (ID: '$categoryId') = ₹$totalSpending")
        
        return totalSpending
    }
    
    /**
     * Convert budget category name to transaction category ID
     * This is crucial for proper matching between budget and transaction categories
     */
    private fun convertBudgetCategoryNameToTransactionCategoryId(budgetCategoryName: String): String {
        return when (budgetCategoryName.lowercase()) {
            // Food & Dining
            "food" -> "food"
            "food & dining" -> "food"
            "dining" -> "food"
            
            // Grocery
            "grocery" -> "grocery"
            
            // Transportation
            "transport" -> "transport"
            "transportation" -> "transport"
            "travel" -> "transport"  // Map travel to transport
            
            // Entertainment
            "entertainment" -> "entertainment"
            
            // Bills & Utilities
            "bills" -> "bills"
            "bills & utilities" -> "bills"
            "utilities" -> "bills"
            
            // Education
            "education" -> "education"
            
            // Healthcare
            "healthcare" -> "healthcare"
            "health" -> "healthcare"
            "medical" -> "healthcare"
            
            // Shopping
            "shopping" -> "shopping"
            
            // Salary & Income (usually not budgeted, but just in case)
            "salary" -> "salary"
            "salary & income" -> "salary"
            "income" -> "salary"
            
            // Others
            "others" -> "others"
            "other" -> "others"
            
            // Additional common budget categories
            "rent" -> "others"  // Map rent to others since no specific rent category
            "emi" -> "others"   // Map EMI to others since no specific EMI category
            
            else -> {
                // Fallback: try to match by converting to lowercase
                Log.w(TAG, "⚠️ Unknown budget category: '$budgetCategoryName', using lowercase as fallback")
                budgetCategoryName.lowercase()
            }
        }
    }
    
    /**
     * Check if notification should be sent and send it
     */
    private fun checkAndSendNotification(
        budgetCategory: BudgetCategory,
        spentAmount: Double,
        percentage: Double
    ) {
        Log.d(TAG, "🔔 Checking notification for ${budgetCategory.name}: ${String.format("%.1f", percentage)}%")
        Log.d(TAG, "🔔 Thresholds: 40%=${THRESHOLD_40_PERCENT}, 90%=${THRESHOLD_90_PERCENT}, 100%=${THRESHOLD_100_PERCENT}")
        
        when {
            percentage >= THRESHOLD_100_PERCENT -> {
                Log.d(TAG, "🚨 Budget exceeded! Checking if notification already sent...")
                // Budget exceeded (100%+)
                if (!hasNotificationBeenSent(budgetCategory.id, THRESHOLD_100_PERCENT)) {
                    Log.d(TAG, "🚨 Sending budget exceeded notification for ${budgetCategory.name}")
                    sendBudgetExceededNotification(budgetCategory, spentAmount, percentage)
                    markNotificationSent(budgetCategory.id, THRESHOLD_100_PERCENT)
                } else {
                    Log.d(TAG, "🚨 Budget exceeded notification already sent for ${budgetCategory.name}")
                }
            }
            percentage >= THRESHOLD_90_PERCENT -> {
                Log.d(TAG, "⚠️ 90% threshold reached! Checking if notification already sent...")
                // 90-99% threshold
                if (!hasNotificationBeenSent(budgetCategory.id, THRESHOLD_90_PERCENT)) {
                    Log.d(TAG, "⚠️ Sending 90% warning notification for ${budgetCategory.name}")
                    sendBudgetWarningNotification(budgetCategory, spentAmount, percentage, 90)
                    markNotificationSent(budgetCategory.id, THRESHOLD_90_PERCENT)
                } else {
                    Log.d(TAG, "⚠️ 90% warning notification already sent for ${budgetCategory.name}")
                }
            }
            percentage >= THRESHOLD_40_PERCENT -> {
                Log.d(TAG, "⚠️ 40% threshold reached! Checking if notification already sent...")
                // 40-50% threshold (first warning)
                if (!hasNotificationBeenSent(budgetCategory.id, THRESHOLD_40_PERCENT)) {
                    Log.d(TAG, "⚠️ Sending 40% warning notification for ${budgetCategory.name}")
                    sendBudgetWarningNotification(budgetCategory, spentAmount, percentage, 40)
                    markNotificationSent(budgetCategory.id, THRESHOLD_40_PERCENT)
                } else {
                    Log.d(TAG, "⚠️ 40% warning notification already sent for ${budgetCategory.name}")
                }
            }
            else -> {
                Log.d(TAG, "✅ ${budgetCategory.name} is within budget limits (${String.format("%.1f", percentage)}%)")
            }
        }
    }
    
    /**
     * Send budget exceeded notification
     */
    private fun sendBudgetExceededNotification(
        budgetCategory: BudgetCategory,
        spentAmount: Double,
        percentage: Double
    ) {
        try {
            val notificationManager = KoshpalNotificationManager.getInstance(context)
            
            val title = "🚨 Budget Exceeded!"
            val message = "${budgetCategory.name} budget exceeded by ₹${String.format("%.0f", spentAmount - budgetCategory.allocatedAmount)}"
            
            Log.d(TAG, "🚨 Sending budget exceeded notification: $title - $message")
            Log.d(TAG, "🚨 Notification details: Category=${budgetCategory.name}, Spent=₹$spentAmount, Budget=₹${budgetCategory.allocatedAmount}, Percentage=${String.format("%.1f", percentage)}%")
            
            // Create a mock transaction for notification
            val mockTransaction = Transaction(
                id = "budget_${budgetCategory.id}_exceeded",
                amount = spentAmount,
                type = TransactionType.DEBIT,
                merchant = "Budget Alert: ${budgetCategory.name}",
                categoryId = budgetCategory.name.lowercase(),
                description = "Budget exceeded by ${String.format("%.1f", percentage)}%",
                date = System.currentTimeMillis()
            )
            
            notificationManager.showBudgetNotification(mockTransaction, title, message, percentage)
            Log.d(TAG, "✅ Budget exceeded notification sent successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send budget exceeded notification", e)
        }
    }
    
    /**
     * Send budget warning notification
     */
    private fun sendBudgetWarningNotification(
        budgetCategory: BudgetCategory,
        spentAmount: Double,
        percentage: Double,
        threshold: Int
    ) {
        val notificationManager = KoshpalNotificationManager.getInstance(context)
        
        val title = "⚠️ Budget Alert"
        val message = "${budgetCategory.name} budget reached ${String.format("%.1f", percentage)}% (₹${String.format("%.0f", spentAmount)}/${String.format("%.0f", budgetCategory.allocatedAmount)})"
        
        Log.d(TAG, "⚠️ Sending budget warning notification: $title - $message")
        
        val mockTransaction = Transaction(
            id = "budget_${budgetCategory.id}_${threshold}",
            amount = spentAmount,
            type = TransactionType.DEBIT,
            merchant = "Budget Alert: ${budgetCategory.name}",
            categoryId = budgetCategory.name.lowercase(),
            description = "Budget reached ${threshold}%",
            date = System.currentTimeMillis()
        )
        
        notificationManager.showBudgetNotification(mockTransaction, title, message, percentage)
    }
    
    /**
     * Check if notification has already been sent for this threshold
     * This prevents spam notifications
     */
    private fun hasNotificationBeenSent(categoryId: Int, threshold: Int): Boolean {
        val prefs = context.getSharedPreferences("budget_notifications", Context.MODE_PRIVATE)
        val key = "notification_sent_${categoryId}_${threshold}"
        return prefs.getBoolean(key, false)
    }
    
    /**
     * Mark notification as sent
     */
    private fun markNotificationSent(categoryId: Int, threshold: Int) {
        val prefs = context.getSharedPreferences("budget_notifications", Context.MODE_PRIVATE)
        val key = "notification_sent_${categoryId}_${threshold}"
        prefs.edit().putBoolean(key, true).apply()
    }
    
    /**
     * Reset notification flags (call this when budget is updated)
     */
    fun resetNotificationFlags() {
        val prefs = context.getSharedPreferences("budget_notifications", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Log.d(TAG, "🔄 Reset all budget notification flags")
    }
    
    /**
     * Force send a test budget notification to check if notifications are working
     */
    fun sendTestBudgetNotification() {
        try {
            Log.d(TAG, "🧪 Sending test budget notification...")
            
            val notificationManager = KoshpalNotificationManager.getInstance(context)
            
            val testTransaction = Transaction(
                id = "test_budget_notification",
                amount = 2500.0,
                type = TransactionType.DEBIT,
                merchant = "Budget Alert: Food",
                categoryId = "food",
                description = "Test budget notification",
                date = System.currentTimeMillis()
            )
            
            notificationManager.showBudgetNotification(testTransaction, "🧪 Test Budget Alert", "This is a test budget notification", 75.0)
            
            Log.d(TAG, "✅ Test budget notification sent")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send test budget notification", e)
        }
    }

    /**
     * Comprehensive test for all thresholds and categories
     * This method tests all percentage thresholds (40%, 90%, 100%) for all categories
     */
    fun testAllThresholdsAndCategories() {
        try {
            Log.d(TAG, "🧪 ===== COMPREHENSIVE THRESHOLD & CATEGORY TEST =====")
            
            val notificationManager = KoshpalNotificationManager.getInstance(context)
            
            // Test all categories with all thresholds
            val testCategories = listOf(
                "Food & Dining", "Grocery", "Transportation", "Entertainment", 
                "Bills & Utilities", "Education", "Healthcare", "Shopping", "Others"
            )
            
            val testThresholds = listOf(40, 90, 100)
            
            testCategories.forEach { categoryName ->
                testThresholds.forEach { threshold ->
                    val testTransaction = Transaction(
                        id = "test_${categoryName.lowercase()}_${threshold}",
                        amount = when (threshold) {
                            40 -> 400.0
                            90 -> 900.0
                            100 -> 1000.0
                            else -> 500.0
                        },
                        type = TransactionType.DEBIT,
                        merchant = "Budget Alert: $categoryName",
                        categoryId = convertBudgetCategoryNameToTransactionCategoryId(categoryName),
                        description = "Test notification for $categoryName at $threshold%",
                        date = System.currentTimeMillis()
                    )
                    
                    val title = when (threshold) {
                        40 -> "⚠️ Budget Warning: $categoryName"
                        90 -> "🚨 Budget Alert: $categoryName"
                        100 -> "💥 Budget Exceeded: $categoryName"
                        else -> "📊 Budget Update: $categoryName"
                    }
                    
                    val message = when (threshold) {
                        40 -> "You've spent 40% of your $categoryName budget"
                        90 -> "You've spent 90% of your $categoryName budget"
                        100 -> "You've exceeded your $categoryName budget!"
                        else -> "Budget update for $categoryName"
                    }
                    
                    Log.d(TAG, "🧪 Testing: $categoryName at $threshold% -> ${testTransaction.categoryId}")
                    notificationManager.showBudgetNotification(testTransaction, title, message, threshold.toDouble())
                    
                    // Small delay between notifications
                    Thread.sleep(100)
                }
            }
            
            Log.d(TAG, "✅ ===== COMPREHENSIVE TEST COMPLETED =====")
            Log.d(TAG, "📊 Tested ${testCategories.size} categories × ${testThresholds.size} thresholds = ${testCategories.size * testThresholds.size} notifications")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Comprehensive test failed", e)
        }
    }
    
    /**
     * Check if any budget is set and return budget info for debugging
     */
    suspend fun getBudgetInfo(): String {
        return try {
            val database = KoshpalDatabase.getDatabase(context)
            val budgetDao = database.budgetNewDao()
            val budgetCategoryDao = database.budgetCategoryNewDao()
            
            val budget = budgetDao.getSingleBudget()
            if (budget == null) {
                "No budget set"
            } else {
                val budgetCategories = budgetCategoryDao.getCategoriesForBudget(budget.id)
                val categoriesInfo = budgetCategories.joinToString("\n") { 
                    "- ${it.name}: ₹${it.allocatedAmount} (spent: ₹${it.spentAmount})" 
                }
                "Budget: ₹${budget.totalBudget}\nCategories:\n$categoriesInfo"
            }
        } catch (e: Exception) {
            "Error getting budget info: ${e.message}"
        }
    }
}
