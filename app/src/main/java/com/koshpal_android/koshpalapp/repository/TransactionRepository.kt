package com.koshpal_android.koshpalapp.repository

import com.koshpal_android.koshpalapp.data.local.dao.TransactionDao
import com.koshpal_android.koshpalapp.data.local.dao.CategoryDao
import com.koshpal_android.koshpalapp.model.CategorySpending
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.engine.TransactionCategorizationEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val categorizationEngine: TransactionCategorizationEngine
) {
    
    
    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions()
    }
    
    suspend fun getAllTransactionsOnce(): List<Transaction> {
        return transactionDao.getAllTransactionsOnce()
    }
    
    suspend fun getTransactionById(id: String): Transaction? {
        return transactionDao.getTransactionById(id)
    }
    
    fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(categoryId)
    }
    
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByType(type)
    }
    
    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startTime, endTime)
    }
    
    suspend fun getTransactionsByDateRangeAndType(
        startTime: Long, 
        endTime: Long, 
        type: TransactionType
    ): List<Transaction> {
        return transactionDao.getTransactionsByDateRangeAndType(startTime, endTime, type)
    }
    
    suspend fun getCategoryWiseSpending(startTime: Long, endTime: Long): List<CategorySpending> {
        return transactionDao.getCategoryWiseSpending(startTime, endTime)
    }
    
    suspend fun getAllTimeCategorySpending(): List<CategorySpending> {
        return transactionDao.getAllTimeCategorySpending()
    }
    
    suspend fun getCurrentMonthCategorySpending(startOfMonth: Long, endOfMonth: Long): List<CategorySpending> {
        return transactionDao.getCurrentMonthCategorySpending(startOfMonth, endOfMonth)
    }
    
    suspend fun getSimpleCategorySpending(): List<CategorySpending> {
        return transactionDao.getSimpleCategorySpending()
    }
    
    suspend fun getAllCategorizedTransactions(): List<Transaction> {
        return transactionDao.getAllCategorizedTransactions()
    }
    
    suspend fun getTotalAmountByTypeAndDateRange(
        type: TransactionType, 
        startTime: Long, 
        endTime: Long
    ): Double {
        return transactionDao.getTotalAmountByTypeAndDateRange(type, startTime, endTime) ?: 0.0
    }
    
    fun searchTransactions(query: String): Flow<List<Transaction>> {
        return transactionDao.searchTransactions(query)
    }
    
    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }
    
    suspend fun insertTransactions(transactions: List<Transaction>) {
        transactionDao.insertTransactions(transactions)
    }
    
    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }
    
    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }
    
    suspend fun deleteTestTransactions(): Int {
        return transactionDao.deleteTestTransactions()
    }
    
    suspend fun resetAllTransactionsToOthers(): Int {
        return transactionDao.resetAllTransactionsToOthers()
    }
    
    // Debug method to verify categorized transactions are saved
    suspend fun debugCategorizedTransactions() {
        try {
            val allTransactions = transactionDao.getAllTransactionsOnce()
            android.util.Log.d("TransactionRepository", "🔍 DEBUG: Total transactions in DB: ${allTransactions.size}")
            
            val categorizedTransactions = allTransactions.filter { it.categoryId != "others" }
            android.util.Log.d("TransactionRepository", "🔍 DEBUG: Categorized transactions: ${categorizedTransactions.size}")
            
            categorizedTransactions.forEach { txn ->
                android.util.Log.d("TransactionRepository", "   📋 ${txn.id}: '${txn.categoryId}' - ${txn.merchant} - ₹${txn.amount}")
            }
            
            val othersTransactions = allTransactions.filter { it.categoryId == "others" }
            android.util.Log.d("TransactionRepository", "🔍 DEBUG: 'Others' transactions: ${othersTransactions.size}")
            
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepository", "❌ Debug failed: ${e.message}")
        }
    }
    
    suspend fun ensureDefaultCategoriesExist() {
        try {
            val existingCategories = categoryDao.getAllCategoriesOnce()
            android.util.Log.d("TransactionRepository", "🔍 Existing categories in DB: ${existingCategories.size}")
            
            if (existingCategories.isEmpty()) {
                android.util.Log.d("TransactionRepository", "📝 Inserting default categories...")
                val defaultCategories = TransactionCategory.getDefaultCategories()
                categoryDao.insertCategories(defaultCategories)
                android.util.Log.d("TransactionRepository", "✅ Inserted ${defaultCategories.size} default categories")
            } else {
                android.util.Log.d("TransactionRepository", "✅ Categories already exist in database")
            }
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepository", "❌ Failed to ensure categories exist: ${e.message}")
        }
    }
    
    suspend fun updateTransactionCategory(transactionId: String, categoryId: String): Int {
        android.util.Log.d("TransactionRepository", "🔄 Starting updateTransactionCategory: $transactionId -> $categoryId")
        
        // Ensure categories exist in database
        ensureDefaultCategoriesExist()
        
        // Get the current transaction to see its state
        val currentTransaction = transactionDao.getTransactionById(transactionId)
        if (currentTransaction == null) {
            android.util.Log.e("TransactionRepository", "❌ Transaction $transactionId does not exist!")
            return 0
        }
        
        android.util.Log.d("TransactionRepository", "📋 Current transaction: ID=$transactionId, CurrentCategory='${currentTransaction.categoryId}', Merchant='${currentTransaction.merchant}', Amount=${currentTransaction.amount}")
        
        // Try the update
        try {
            val currentTime = System.currentTimeMillis()
            val result = transactionDao.updateTransactionCategory(transactionId, categoryId, currentTime)
            android.util.Log.d("TransactionRepository", "✅ Update result: $result rows affected")
            
            if (result > 0) {
                // Verify the update worked
                val updatedTransaction = transactionDao.getTransactionById(transactionId)
                android.util.Log.d("TransactionRepository", "🔍 After update: CategoryId='${updatedTransaction?.categoryId}', UpdatedAt=${updatedTransaction?.updatedAt}")
                
                if (updatedTransaction?.categoryId == categoryId) {
                    android.util.Log.d("TransactionRepository", "✅ VERIFICATION SUCCESS: Category update confirmed")
                } else {
                    android.util.Log.e("TransactionRepository", "❌ VERIFICATION FAILED: Expected '$categoryId', got '${updatedTransaction?.categoryId}'")
                }
            }
            
            return result
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepository", "❌ Exception during update: ${e.message}")
            android.util.Log.e("TransactionRepository", "❌ Exception cause: ${e.cause}")
            e.printStackTrace()
            return 0
        }
    }
    
    suspend fun getUncategorizedTransactions(): List<Transaction> {
        return transactionDao.getLowConfidenceTransactions()
    }
    
    suspend fun getMonthlyIncome(): Double {
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
        
        return transactionDao.getTotalIncomeForPeriod(startOfMonth, endOfMonth) ?: 0.0
    }
    
    suspend fun getMonthlyExpenses(): Double {
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
        
        return transactionDao.getTotalExpensesForPeriod(startOfMonth, endOfMonth) ?: 0.0
    }
    
    suspend fun getRecentTransactions(limit: Int): List<Transaction> {
        return transactionDao.getRecentTransactions(limit)
    }
    
    suspend fun categorizeAndInsertTransaction(
        smsBody: String,
        merchant: String,
        amount: Double,
        type: TransactionType,
        timestamp: Long
    ): Transaction {
        val categories = categoryDao.getAllActiveCategories().first()
        val (category, confidence) = categorizationEngine.categorizeTransaction(
            smsBody, merchant, amount, type, categories
        )
        
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            type = type,
            merchant = merchant,
            categoryId = category.id,
            confidence = confidence,
            date = timestamp,
            description = categorizationEngine.extractTransactionDetails(smsBody).description,
            smsBody = smsBody
        )
        
        insertTransaction(transaction)
        return transaction
    }
    
    suspend fun recategorizeTransaction(transactionId: String, newCategoryId: String) {
        val transaction = getTransactionById(transactionId)
        if (transaction != null) {
            val updatedTransaction = transaction.copy(
                categoryId = newCategoryId,
                isManuallySet = true,
                updatedAt = System.currentTimeMillis()
            )
            updateTransaction(updatedTransaction)
            
            // Learn from user correction
            val category = categoryDao.getCategoryById(newCategoryId)
            if (category != null) {
                categorizationEngine.updateCategoryRules(transaction, category)
            }
        }
    }
    
    suspend fun getLastNMonthsIncomeExpenses(n: Int = 4): List<Pair<String, Pair<Double, Double>>> {
        val calendar = Calendar.getInstance()
        val results = mutableListOf<Pair<String, Pair<Double, Double>>>()
        // Start from current month going back n-1 months
        for (i in (n - 1) downTo 0) {
            val monthCal = calendar.clone() as Calendar
            monthCal.add(Calendar.MONTH, -i)
            monthCal.set(Calendar.DAY_OF_MONTH, 1)
            monthCal.set(Calendar.HOUR_OF_DAY, 0)
            monthCal.set(Calendar.MINUTE, 0)
            monthCal.set(Calendar.SECOND, 0)
            monthCal.set(Calendar.MILLISECOND, 0)
            val start = monthCal.timeInMillis
            val label = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault()).format(monthCal.time)
            monthCal.add(Calendar.MONTH, 1)
            monthCal.add(Calendar.MILLISECOND, -1)
            val end = monthCal.timeInMillis
            val income = transactionDao.getTotalIncomeForPeriod(start, end) ?: 0.0
            val expenses = transactionDao.getTotalExpensesForPeriod(start, end) ?: 0.0
            results.add(label to (income to expenses))
        }
        return results
    }

    
    suspend fun getMonthlyIncome(month: Int, year: Int): Double {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        val startTime = calendar.timeInMillis
        
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endTime = calendar.timeInMillis
        
        return getTotalAmountByTypeAndDateRange(TransactionType.CREDIT, startTime, endTime)
    }
    
    suspend fun getMonthlyExpense(month: Int, year: Int): Double {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        val startTime = calendar.timeInMillis
        
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endTime = calendar.timeInMillis
        
        return getTotalAmountByTypeAndDateRange(TransactionType.DEBIT, startTime, endTime)
    }
    
    suspend fun getTransactionCount(): Int {
        return transactionDao.getTransactionCount()
    }
}
