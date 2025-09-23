package com.koshpal_android.koshpalapp.utils

import android.content.Context
import android.util.Log
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.utils.SMSManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class DebugDataManager(private val context: Context) {
    
    private val TAG = "DebugDataManager"
    
    suspend fun performCompleteDataCheck(): DataCheckResult {
        return withContext(Dispatchers.IO) {
            val result = DataCheckResult()
            
            try {
                Log.d(TAG, "🔍 ===== STARTING COMPLETE DATA CHECK =====")
                
                val database = KoshpalDatabase.getDatabase(context)
                
                // Step 1: Check database connection
                Log.d(TAG, "📊 Step 1: Checking database connection...")
                result.databaseConnected = true
                Log.d(TAG, "✅ Database connection successful")
                
                // Step 2: Check categories
                Log.d(TAG, "📂 Step 2: Checking categories...")
                val categories = database.categoryDao().getAllCategoriesOnce()
                result.categoriesCount = categories.size
                Log.d(TAG, "📂 Found ${categories.size} categories:")
                categories.forEach { category ->
                    Log.d(TAG, "   - ${category.id}: ${category.name} (active: ${category.isActive})")
                }
                
                // Step 3: Check transactions
                Log.d(TAG, "💳 Step 3: Checking transactions...")
                val transactions = database.transactionDao().getAllTransactionsOnce()
                result.transactionsCount = transactions.size
                Log.d(TAG, "💳 Found ${transactions.size} transactions:")
                
                var totalIncome = 0.0
                var totalExpenses = 0.0
                
                transactions.forEachIndexed { index, transaction ->
                    Log.d(TAG, "   ${index + 1}. ${transaction.merchant} - ₹${transaction.amount} (${transaction.type}) [${transaction.categoryId}]")
                    if (transaction.type == TransactionType.CREDIT) {
                        totalIncome += transaction.amount
                    } else {
                        totalExpenses += transaction.amount
                    }
                }
                
                result.totalIncome = totalIncome
                result.totalExpenses = totalExpenses
                result.balance = totalIncome - totalExpenses
                
                Log.d(TAG, "💰 FINANCIAL SUMMARY:")
                Log.d(TAG, "   Income: ₹${totalIncome}")
                Log.d(TAG, "   Expenses: ₹${totalExpenses}")
                Log.d(TAG, "   Balance: ₹${result.balance}")
                
                // Step 4: Check SMS data
                Log.d(TAG, "📱 Step 4: Checking SMS data...")
                val smsCount = database.paymentSmsDao().getUnprocessedSms().size
                result.smsCount = smsCount
                Log.d(TAG, "📱 Found ${smsCount} unprocessed SMS")
                
                // Step 5: Check foreign key integrity
                Log.d(TAG, "🔗 Step 5: Checking foreign key integrity...")
                val orphanedTransactions = mutableListOf<Transaction>()
                transactions.forEach { transaction ->
                    val categoryExists = categories.any { it.id == transaction.categoryId }
                    if (!categoryExists) {
                        orphanedTransactions.add(transaction)
                        Log.e(TAG, "❌ Orphaned transaction: ${transaction.id} references non-existent category: ${transaction.categoryId}")
                    }
                }
                result.orphanedTransactions = orphanedTransactions.size
                
                result.success = true
                Log.d(TAG, "✅ ===== DATA CHECK COMPLETED SUCCESSFULLY =====")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Data check failed: ${e.message}", e)
                result.error = e.message
                result.success = false
            }
            
            result
        }
    }
    
    suspend fun parseRealSMSAndCreateData(): CreateDataResult {
        return withContext(Dispatchers.IO) {
            val result = CreateDataResult()
            
            try {
                Log.d(TAG, "📱 ===== PARSING REAL SMS DATA =====")
                
                val database = KoshpalDatabase.getDatabase(context)
                
                // Step 1: Clear existing demo data
                Log.d(TAG, "🧽 Step 1: Clearing demo data...")
                database.transactionDao().deleteAllTransactions()
                Log.d(TAG, "✅ Cleared existing demo transactions")
                
                // Step 2: Ensure categories exist
                Log.d(TAG, "📂 Step 2: Ensuring categories exist...")
                val existingCategories = database.categoryDao().getAllCategoriesOnce()
                if (existingCategories.isEmpty()) {
                    val defaultCategories = TransactionCategory.getDefaultCategories()
                    database.categoryDao().insertCategories(defaultCategories)
                    Log.d(TAG, "✅ Inserted ${defaultCategories.size} default categories")
                } else {
                    Log.d(TAG, "✅ Found ${existingCategories.size} existing categories")
                }
                
                // Step 3: Parse real SMS messages
                Log.d(TAG, "📱 Step 3: Parsing real SMS messages...")
                val smsManager = SMSManager(context)
                val smsResult = smsManager.processAllSMS()
                
                if (smsResult.success && smsResult.transactionsCreated > 0) {
                    result.transactionsCreated = smsResult.transactionsCreated
                    result.finalTransactionCount = smsResult.transactionsCreated
                    result.success = true
                    
                    Log.d(TAG, "✅ Real SMS parsing successful!")
                    Log.d(TAG, "   SMS Found: ${smsResult.smsFound}")
                    Log.d(TAG, "   Transaction SMS: ${smsResult.transactionSmsFound}")
                    Log.d(TAG, "   Transactions Created: ${smsResult.transactionsCreated}")
                } else {
                    // Fallback to sample data if no real SMS found
                    Log.d(TAG, "📝 No real SMS found, creating sample data as fallback...")
                    return@withContext createGuaranteedSampleData()
                }
                
                // Step 4: Verify data was inserted
                Log.d(TAG, "🔍 Step 4: Verifying inserted data...")
                val verifyTransactions = database.transactionDao().getAllTransactionsOnce()
                result.finalTransactionCount = verifyTransactions.size
                
                Log.d(TAG, "📊 REAL SMS PARSING RESULTS:")
                Log.d(TAG, "   Transactions created: ${result.transactionsCreated}")
                Log.d(TAG, "   Final transaction count: ${result.finalTransactionCount}")
                
                if (result.finalTransactionCount > 0) {
                    Log.d(TAG, "🎉 ===== REAL SMS PARSING SUCCESSFUL =====")
                } else {
                    Log.e(TAG, "❌ ===== REAL SMS PARSING FAILED =====")
                    result.success = false
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Real SMS parsing failed: ${e.message}", e)
                result.error = e.message
                result.success = false
            }
            
            result
        }
    }
    
    suspend fun createGuaranteedSampleData(): CreateDataResult {
        return withContext(Dispatchers.IO) {
            val result = CreateDataResult()
            
            try {
                Log.d(TAG, "🚀 ===== CREATING GUARANTEED SAMPLE DATA =====")
                
                val database = KoshpalDatabase.getDatabase(context)
                
                // Step 1: Clear existing data to avoid conflicts
                Log.d(TAG, "🧹 Step 1: Clearing existing data...")
                database.transactionDao().deleteAllTransactions()
                Log.d(TAG, "✅ Cleared existing transactions")
                
                // Step 2: Ensure categories exist
                Log.d(TAG, "📂 Step 2: Ensuring categories exist...")
                val existingCategories = database.categoryDao().getAllCategoriesOnce()
                if (existingCategories.isEmpty()) {
                    val defaultCategories = TransactionCategory.getDefaultCategories()
                    database.categoryDao().insertCategories(defaultCategories)
                    Log.d(TAG, "✅ Inserted ${defaultCategories.size} default categories")
                } else {
                    Log.d(TAG, "✅ Found ${existingCategories.size} existing categories")
                }
                
                // Step 3: Create guaranteed transactions with existing category IDs
                Log.d(TAG, "💳 Step 3: Creating guaranteed transactions...")
                val currentTime = System.currentTimeMillis()
                
                // Ensure transactions are in current month
                val calendar = java.util.Calendar.getInstance()
                calendar.timeInMillis = currentTime
                val currentMonth = calendar.get(java.util.Calendar.MONTH)
                val currentYear = calendar.get(java.util.Calendar.YEAR)
                
                // Set to first day of current month
                calendar.set(currentYear, currentMonth, 1, 12, 0, 0)
                val currentMonthStart = calendar.timeInMillis
                
                Log.d(TAG, "📅 Creating transactions for current month: ${currentMonth + 1}/$currentYear")
                
                val guaranteedTransactions = listOf(
                    Transaction(
                        id = "sample_1_${UUID.randomUUID()}",
                        amount = 25000.0,
                        type = TransactionType.CREDIT,
                        merchant = "Salary Credit",
                        categoryId = "salary",
                        confidence = 1.0f,
                        date = currentMonthStart + (1 * 24 * 60 * 60 * 1000), // 1st day of current month
                        description = "Monthly salary credit",
                        smsBody = "Your account credited with Rs.25000.00 Salary"
                    ),
                    Transaction(
                        id = "sample_2_${UUID.randomUUID()}",
                        amount = 1200.0,
                        type = TransactionType.DEBIT,
                        merchant = "Zomato",
                        categoryId = "food",
                        confidence = 0.95f,
                        date = currentMonthStart + (5 * 24 * 60 * 60 * 1000), // 5th day of current month
                        description = "Food delivery order",
                        smsBody = "Rs.1200 debited for UPI/ZOMATO"
                    ),
                    Transaction(
                        id = "sample_3_${UUID.randomUUID()}",
                        amount = 500.0,
                        type = TransactionType.DEBIT,
                        merchant = "Amazon India",
                        categoryId = "shopping",
                        confidence = 0.90f,
                        date = currentMonthStart + (10 * 24 * 60 * 60 * 1000), // 10th day of current month
                        description = "Online shopping",
                        smsBody = "Your A/c debited by Rs.500.00 at AMAZON INDIA"
                    ),
                    Transaction(
                        id = "sample_4_${UUID.randomUUID()}",
                        amount = 350.0,
                        type = TransactionType.DEBIT,
                        merchant = "Uber",
                        categoryId = "transport",
                        confidence = 0.88f,
                        date = currentMonthStart + (15 * 24 * 60 * 60 * 1000), // 15th day of current month
                        description = "Cab ride",
                        smsBody = "INR 350.00 debited for UBER TRIP"
                    ),
                    Transaction(
                        id = "sample_5_${UUID.randomUUID()}",
                        amount = 800.0,
                        type = TransactionType.DEBIT,
                        merchant = "DMart",
                        categoryId = "grocery",
                        confidence = 0.92f,
                        date = currentMonthStart + (20 * 24 * 60 * 60 * 1000), // 20th day of current month
                        description = "Grocery shopping",
                        smsBody = "Rs.800 spent at DMART GROCERY"
                    )
                )
                
                // Insert transactions one by one with error handling
                guaranteedTransactions.forEach { transaction ->
                    try {
                        val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(transaction.date))
                        Log.d(TAG, "📅 Creating transaction: ${transaction.merchant} on $dateStr")
                        database.transactionDao().insertTransaction(transaction)
                        result.transactionsCreated++
                        Log.d(TAG, "✅ Inserted: ${transaction.merchant} - ₹${transaction.amount}")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to insert transaction ${transaction.merchant}: ${e.message}")
                        result.errors.add("Failed to insert ${transaction.merchant}: ${e.message}")
                    }
                }
                
                // Step 4: Verify data was inserted
                Log.d(TAG, "🔍 Step 4: Verifying inserted data...")
                val verifyTransactions = database.transactionDao().getAllTransactionsOnce()
                result.finalTransactionCount = verifyTransactions.size
                
                Log.d(TAG, "📊 VERIFICATION RESULTS:")
                Log.d(TAG, "   Transactions created: ${result.transactionsCreated}")
                Log.d(TAG, "   Final transaction count: ${result.finalTransactionCount}")
                Log.d(TAG, "   Errors: ${result.errors.size}")
                
                result.success = result.transactionsCreated > 0
                
                if (result.success) {
                    Log.d(TAG, "🎉 ===== SAMPLE DATA CREATION SUCCESSFUL =====")
                } else {
                    Log.e(TAG, "❌ ===== SAMPLE DATA CREATION FAILED =====")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Sample data creation failed: ${e.message}", e)
                result.error = e.message
                result.success = false
            }
            
            result
        }
    }
    
    suspend fun debugHomeScreenData(): HomeScreenDebugResult {
        return withContext(Dispatchers.IO) {
            val result = HomeScreenDebugResult()
            
            try {
                Log.d(TAG, "🏠 ===== DEBUGGING HOME SCREEN DATA =====")
                
                val database = KoshpalDatabase.getDatabase(context)
                
                // Get all data that home screen needs
                val transactions = database.transactionDao().getAllTransactionsOnce()
                result.transactionCount = transactions.size
                
                var totalIncome = 0.0
                var totalExpenses = 0.0
                
                transactions.forEach { transaction ->
                    if (transaction.type == TransactionType.CREDIT) {
                        totalIncome += transaction.amount
                    } else {
                        totalExpenses += transaction.amount
                    }
                }
                
                result.totalIncome = totalIncome
                result.totalExpenses = totalExpenses
                result.balance = totalIncome - totalExpenses
                result.hasTransactions = transactions.isNotEmpty()
                
                Log.d(TAG, "🏠 HOME SCREEN DATA:")
                Log.d(TAG, "   Has transactions: ${result.hasTransactions}")
                Log.d(TAG, "   Transaction count: ${result.transactionCount}")
                Log.d(TAG, "   Total income: ₹${result.totalIncome}")
                Log.d(TAG, "   Total expenses: ₹${result.totalExpenses}")
                Log.d(TAG, "   Balance: ₹${result.balance}")
                
                result.success = true
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Home screen debug failed: ${e.message}", e)
                result.error = e.message
                result.success = false
            }
            
            result
        }
    }
}

data class DataCheckResult(
    var success: Boolean = false,
    var databaseConnected: Boolean = false,
    var categoriesCount: Int = 0,
    var transactionsCount: Int = 0,
    var smsCount: Int = 0,
    var orphanedTransactions: Int = 0,
    var totalIncome: Double = 0.0,
    var totalExpenses: Double = 0.0,
    var balance: Double = 0.0,
    var error: String? = null
)

data class CreateDataResult(
    var success: Boolean = false,
    var transactionsCreated: Int = 0,
    var finalTransactionCount: Int = 0,
    var errors: MutableList<String> = mutableListOf(),
    var error: String? = null
)

data class HomeScreenDebugResult(
    var success: Boolean = false,
    var hasTransactions: Boolean = false,
    var transactionCount: Int = 0,
    var totalIncome: Double = 0.0,
    var totalExpenses: Double = 0.0,
    var balance: Double = 0.0,
    var error: String? = null
)
