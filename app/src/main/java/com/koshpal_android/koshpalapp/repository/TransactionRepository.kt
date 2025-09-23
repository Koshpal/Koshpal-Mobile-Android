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
    
    suspend fun updateTransactionCategory(transactionId: String, categoryId: String) {
        transactionDao.updateTransactionCategory(transactionId, categoryId)
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
