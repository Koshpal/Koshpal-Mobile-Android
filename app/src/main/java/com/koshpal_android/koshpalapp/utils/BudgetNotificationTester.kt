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

/**
 * Test utility for budget notifications
 * This class helps test the budget notification system with sample data
 */
class BudgetNotificationTester private constructor(private val context: Context) {

    companion object {
        private const val TAG = "BudgetNotificationTester"
        
        @Volatile
        private var INSTANCE: BudgetNotificationTester? = null

        fun getInstance(context: Context): BudgetNotificationTester {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BudgetNotificationTester(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Create test budget and transactions to demonstrate notifications
     */
    fun createTestBudgetScenario() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🧪 Creating test budget scenario...")
                
                val database = KoshpalDatabase.getDatabase(context)
                val budgetDao = database.budgetNewDao()
                val budgetCategoryDao = database.budgetCategoryNewDao()
                val transactionDao = database.transactionDao()
                
                // Clear existing budget
                budgetDao.clearBudgets()
                
                // Create test budget
                val testBudget = Budget(
                    totalBudget = 10000.0,
                    savings = 1000.0
                )
                val budgetId = budgetDao.insertBudget(testBudget).toInt()
                
                Log.d(TAG, "💰 Created test budget: ₹${testBudget.totalBudget}")
                
                // Create test budget categories
                val testCategories = listOf(
                    BudgetCategory(
                        budgetId = budgetId,
                        name = "Grocery",
                        allocatedAmount = 3000.0,
                        spentAmount = 0.0
                    ),
                    BudgetCategory(
                        budgetId = budgetId,
                        name = "Entertainment",
                        allocatedAmount = 2000.0,
                        spentAmount = 0.0
                    ),
                    BudgetCategory(
                        budgetId = budgetId,
                        name = "Transport",
                        allocatedAmount = 1500.0,
                        spentAmount = 0.0
                    ),
                    BudgetCategory(
                        budgetId = budgetId,
                        name = "Food",
                        allocatedAmount = 2500.0,
                        spentAmount = 0.0
                    )
                )
                
                budgetCategoryDao.insertAll(testCategories)
                Log.d(TAG, "📂 Created ${testCategories.size} test budget categories")
                
                // Create test transactions for current month
                val currentTime = System.currentTimeMillis()
                val testTransactions = listOf(
                    // Grocery transactions (will trigger 50% notification)
                    Transaction(
                        id = "test_grocery_1",
                        amount = 800.0,
                        type = TransactionType.DEBIT,
                        merchant = "Big Bazaar",
                        categoryId = "grocery",
                        date = currentTime - (5 * 24 * 60 * 60 * 1000L), // 5 days ago
                        description = "Grocery shopping"
                    ),
                    Transaction(
                        id = "test_grocery_2",
                        amount = 700.0,
                        type = TransactionType.DEBIT,
                        merchant = "Reliance Fresh",
                        categoryId = "grocery",
                        date = currentTime - (3 * 24 * 60 * 60 * 1000L), // 3 days ago
                        description = "Vegetables and fruits"
                    ),
                    
                    // Entertainment transactions (will trigger 90% notification)
                    Transaction(
                        id = "test_entertainment_1",
                        amount = 500.0,
                        type = TransactionType.DEBIT,
                        merchant = "Netflix",
                        categoryId = "entertainment",
                        date = currentTime - (10 * 24 * 60 * 60 * 1000L), // 10 days ago
                        description = "Monthly subscription"
                    ),
                    Transaction(
                        id = "test_entertainment_2",
                        amount = 800.0,
                        type = TransactionType.DEBIT,
                        merchant = "Movie Theater",
                        categoryId = "entertainment",
                        date = currentTime - (7 * 24 * 60 * 60 * 1000L), // 7 days ago
                        description = "Movie tickets"
                    ),
                    Transaction(
                        id = "test_entertainment_3",
                        amount = 400.0,
                        type = TransactionType.DEBIT,
                        merchant = "Spotify",
                        categoryId = "entertainment",
                        date = currentTime - (2 * 24 * 60 * 60 * 1000L), // 2 days ago
                        description = "Music subscription"
                    ),
                    
                    // Transport transactions (will trigger 100% notification)
                    Transaction(
                        id = "test_transport_1",
                        amount = 200.0,
                        type = TransactionType.DEBIT,
                        merchant = "Uber",
                        categoryId = "transport",
                        date = currentTime - (8 * 24 * 60 * 60 * 1000L), // 8 days ago
                        description = "Ride to office"
                    ),
                    Transaction(
                        id = "test_transport_2",
                        amount = 300.0,
                        type = TransactionType.DEBIT,
                        merchant = "Ola",
                        categoryId = "transport",
                        date = currentTime - (6 * 24 * 60 * 60 * 1000L), // 6 days ago
                        description = "Ride to airport"
                    ),
                    Transaction(
                        id = "test_transport_3",
                        amount = 500.0,
                        type = TransactionType.DEBIT,
                        merchant = "Metro",
                        categoryId = "transport",
                        date = currentTime - (4 * 24 * 60 * 60 * 1000L), // 4 days ago
                        description = "Monthly metro pass"
                    ),
                    Transaction(
                        id = "test_transport_4",
                        amount = 600.0,
                        type = TransactionType.DEBIT,
                        merchant = "Petrol Pump",
                        categoryId = "transport",
                        date = currentTime - (1 * 24 * 60 * 60 * 1000L), // 1 day ago
                        description = "Fuel for car"
                    )
                )
                
                // Insert test transactions
                transactionDao.insertTransactions(testTransactions)
                Log.d(TAG, "💳 Created ${testTransactions.size} test transactions")
                
                // Trigger budget monitoring
                val budgetMonitor = BudgetMonitor.getInstance(context)
                budgetMonitor.checkBudgetStatus()
                
                Log.d(TAG, "✅ Test budget scenario created successfully!")
                Log.d(TAG, "📊 Expected notifications:")
                Log.d(TAG, "   - Grocery: 50% notification (₹1,500 / ₹3,000)")
                Log.d(TAG, "   - Entertainment: 90% notification (₹1,700 / ₹2,000)")
                Log.d(TAG, "   - Transport: 100% notification (₹1,600 / ₹1,500)")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to create test budget scenario", e)
            }
        }
    }
    
    /**
     * Clear all test data
     */
    fun clearTestData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🧹 Clearing test data...")
                
                val database = KoshpalDatabase.getDatabase(context)
                val budgetDao = database.budgetNewDao()
                val transactionDao = database.transactionDao()
                
                // Clear budget
                budgetDao.clearBudgets()
                
                // Clear test transactions
                val testTransactionIds = listOf(
                    "test_grocery_1", "test_grocery_2",
                    "test_entertainment_1", "test_entertainment_2", "test_entertainment_3",
                    "test_transport_1", "test_transport_2", "test_transport_3", "test_transport_4"
                )
                
                testTransactionIds.forEach { id ->
                    transactionDao.deleteTransactionById(id)
                }
                
                Log.d(TAG, "✅ Test data cleared successfully!")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to clear test data", e)
            }
        }
    }
}
