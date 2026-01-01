package com.koshpal_android.koshpalapp.repository

import com.koshpal_android.koshpalapp.data.local.dao.TransactionDao
import com.koshpal_android.koshpalapp.data.local.dao.CategoryDao
import com.koshpal_android.koshpalapp.data.local.dao.BudgetNewDao
import com.koshpal_android.koshpalapp.data.local.dao.BudgetCategoryNewDao
import com.koshpal_android.koshpalapp.data.local.dao.CashFlowTransactionDao
import com.koshpal_android.koshpalapp.model.CategorySpending
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.model.Budget
import com.koshpal_android.koshpalapp.model.BudgetCategory
import com.koshpal_android.koshpalapp.model.CashFlowTransaction
import com.koshpal_android.koshpalapp.engine.TransactionCategorizationEngine
import com.koshpal_android.koshpalapp.utils.MerchantCategorizer
import com.koshpal_android.koshpalapp.utils.BudgetMonitor
import com.koshpal_android.koshpalapp.service.TransactionSyncService
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetNewDao,
    private val budgetCategoryDao: BudgetCategoryNewDao,
    private val cashFlowTransactionDao: CashFlowTransactionDao,
    private val categorizationEngine: TransactionCategorizationEngine,
    private val syncService: TransactionSyncService,
    @ApplicationContext private val context: Context
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
    
    suspend fun getTransactionsByCategory(categoryId: String, month: Int, year: Int): List<Transaction> {
        // Calculate start and end timestamps for the given month
        val calendar = java.util.Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis
        
        // Set to last day of month
        calendar.set(year, month, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH), 23, 59, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis
        
        return transactionDao.getTransactionsByCategoryAndDateRange(categoryId, startDate, endDate)
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

    // Category helpers for UI (custom categories support)
    suspend fun getAllActiveCategoriesList(): List<TransactionCategory> {
        return categoryDao.getAllActiveCategoriesList()
    }

    suspend fun insertCustomCategory(
        name: String,
        colorHex: String = "#6B7280", // Neutral gray
        iconRes: Int = com.koshpal_android.koshpalapp.R.drawable.ic_budget_empty,
        keywords: List<String> = emptyList()
    ): TransactionCategory {
        val trimmedName = name.trim()

        // Check if a category with same name already exists (case-insensitive)
        val existing = categoryDao.getAllCategoriesOnce().firstOrNull { it.name.equals(trimmedName, ignoreCase = true) }
        if (existing != null) {
            // If it exists but inactive, activate it
            if (!existing.isActive) {
                categoryDao.activateCategory(existing.id)
                return existing.copy(isActive = true)
            }
            // Already exists and active - return it (treat as success, not failure)
            return existing
        }

        // Create brand new custom category with a robust unique id
        val uniqueId = java.util.UUID.randomUUID().toString()
        val category = TransactionCategory(
            id = uniqueId,
            name = trimmedName,
            icon = iconRes,
            color = colorHex,
            keywords = keywords,
            isDefault = false,
            isActive = true
        )
        categoryDao.insertCategory(category)
        return category
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
        
        // Check budget status after transaction update
        try {
            val budgetMonitor = BudgetMonitor.getInstance(context)
            budgetMonitor.checkBudgetStatus(transaction)
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepository", "❌ Failed to check budget status after update", e)
        }
        
        // Auto-sync transaction update to MongoDB
        try {
            syncService.autoSyncTransactionUpdate(transaction)
            android.util.Log.d("TransactionRepository", "🔄 Auto-sync triggered for transaction update")
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepository", "❌ Auto-sync failed for transaction update: ${e.message}")
        }
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
                    
                    // CRITICAL FIX: Trigger budget monitoring after category update
                    try {
                        val budgetMonitor = BudgetMonitor.getInstance(context)
                        budgetMonitor.checkBudgetStatus(updatedTransaction)
                        android.util.Log.d("TransactionRepository", "💰 Budget monitoring triggered after category update")
                    } catch (e: Exception) {
                        android.util.Log.e("TransactionRepository", "❌ Failed to trigger budget monitoring after category update", e)
                    }
                    
                    // Auto-sync category update to MongoDB
                    try {
                        syncService.autoSyncTransactionUpdate(updatedTransaction)
                        android.util.Log.d("TransactionRepository", "🔄 Auto-sync triggered for category update")
                    } catch (e: Exception) {
                        android.util.Log.e("TransactionRepository", "❌ Auto-sync failed for category update: ${e.message}")
                    }
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
        // Use MerchantCategorizer for automatic categorization based on keywords
        val autoCategoryId = MerchantCategorizer.categorizeTransaction(merchant, smsBody)
        
        android.util.Log.d("TransactionRepository", "🤖 Auto-categorized '$merchant' → $autoCategoryId")
        
        // Extract bank name from SMS
        val bankName = extractBankName(Transaction(
            id = "",
            merchant = merchant,
            smsBody = smsBody,
            amount = amount,
            type = type,
            date = timestamp
        ))
        
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            type = type,
            merchant = merchant,
            categoryId = autoCategoryId,
            confidence = 0.9f, // High confidence for keyword-based categorization
            date = timestamp,
            description = categorizationEngine.extractTransactionDetails(smsBody).description,
            smsBody = smsBody,
            bankName = bankName,
            isManuallySet = false
        )
        
        insertTransaction(transaction)
        
        android.util.Log.d("TransactionRepository", "✅ Transaction saved: $merchant → ${MerchantCategorizer.getCategoryDisplayName(autoCategoryId)} (Bank: $bankName)")
        
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

    suspend fun getTransactionsByBank(bankName: String): List<Transaction> {
        // Generate alternative patterns for bank name matching
        val pattern1 = when (bankName) {
            "SBI" -> "STATE BANK"
            "HDFC Bank" -> "HDFC"
            "ICICI Bank" -> "ICICI"
            "Axis Bank" -> "AXIS"
            "Kotak Mahindra" -> "KOTAK"
            "IPPB" -> "INDIA POST"
            "Paytm" -> "PAYTM"
            "PhonePe" -> "PHONEPE"
            "Google Pay" -> "GPAY"
            "Bank of Baroda" -> "BOB"
            "PNB" -> "PUNJAB NATIONAL"
            "Canara Bank" -> "CANARA"
            "Union Bank" -> "UNION BANK"
            "IDBI Bank" -> "IDBI"
            "Yes Bank" -> "YES BANK"
            else -> bankName
        }
        
        val pattern2 = when (bankName) {
            "SBI" -> "SBI"
            "HDFC Bank" -> "HDFC"
            "ICICI Bank" -> "ICICI"
            "Axis Bank" -> "AXIS"
            "Kotak Mahindra" -> "KOTAK"
            "IPPB" -> "IPPB"
            "Paytm" -> "PAYTM"
            "PhonePe" -> "PHONEPE"
            "Google Pay" -> "GOOGLE PAY"
            "Bank of Baroda" -> "BANK OF BARODA"
            "PNB" -> "PNB"
            "Canara Bank" -> "CANARA"
            "Union Bank" -> "UNION BANK"
            "IDBI Bank" -> "IDBI"
            "Yes Bank" -> "YES BANK"
            else -> bankName
        }
        
        val transactions = transactionDao.getTransactionsByBank(bankName, pattern1, pattern2)
        
        // If no transactions found, try to get all transactions and filter manually
        if (transactions.isEmpty()) {
            val allTransactions = transactionDao.getAllTransactionsOnce()
            
            // Try manual filtering using the same logic as extractBankName
            val manuallyFiltered = allTransactions.filter { transaction ->
                val extractedBank = extractBankName(transaction)
                extractedBank.equals(bankName, ignoreCase = true)
            }
            
            return manuallyFiltered
        }
        
        return transactions
    }
    
    suspend fun getBankWiseSpending(): List<com.koshpal_android.koshpalapp.model.BankSpending> {
        // Get current month date range
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
        
        android.util.Log.d("TransactionRepository", "📊 Getting bank spending for current month: ${java.util.Date(startOfMonth)} to ${java.util.Date(endOfMonth)}")
        
        // Get ONLY current month transactions
        val allTransactions = transactionDao.getAllTransactionsOnce()
        val currentMonthTransactions = allTransactions.filter { 
            it.date >= startOfMonth && it.date <= endOfMonth 
        }
        
        android.util.Log.d("TransactionRepository", "📊 Total transactions: ${allTransactions.size}, Current month: ${currentMonthTransactions.size}")
        
        // Extract bank name from SMS or merchant field - ONLY DEBIT transactions
        val bankTransactions = currentMonthTransactions
            .filter { it.type == TransactionType.DEBIT }
            .groupBy { extractBankName(it) }
            .map { (bankName, transactions) ->
                val totalSpending = transactions.sumOf { it.amount }
                
                // Get most recent transaction for account number, balance, and timestamp
                val mostRecentTransaction = transactions.maxByOrNull { it.date }
                val accountNumber = mostRecentTransaction?.smsBody?.let { extractAccountNumber(it) }
                val balance = mostRecentTransaction?.smsBody?.let { extractBalance(it) }
                val lastUpdated = mostRecentTransaction?.date
                
                android.util.Log.d("TransactionRepository", "💳 $bankName: ₹$totalSpending (${transactions.size} transactions), Account: $accountNumber, Balance: $balance")
                
                com.koshpal_android.koshpalapp.model.BankSpending(
                    bankName = bankName,
                    totalSpending = totalSpending,
                    transactionCount = transactions.size,
                    isCash = bankName == "Cash",
                    accountNumber = accountNumber,
                    balance = balance,
                    lastUpdated = lastUpdated
                )
            }
            .filter { it.totalSpending > 0 } // Only show banks with actual spending
            .sortedByDescending { it.totalSpending }
        
        android.util.Log.d("TransactionRepository", "📊 Found ${bankTransactions.size} banks with spending in current month")
        return bankTransactions
    }

    private fun extractBankName(transaction: Transaction): String {
        // If bank name is already set, use it
        if (!transaction.bankName.isNullOrBlank()) {
            return transaction.bankName
        }

        // Extract from SMS body or merchant
        val text = (transaction.smsBody ?: transaction.merchant).uppercase()
        
        return when {
            text.contains("SBI") || text.contains("STATE BANK") -> "SBI"
            text.contains("HDFC") -> "HDFC Bank"
            text.contains("ICICI") -> "ICICI Bank"
            text.contains("AXIS") -> "Axis Bank"
            text.contains("KOTAK") -> "Kotak Mahindra"
            text.contains("IPPB") || text.contains("INDIA POST") -> "IPPB"
            text.contains("PAYTM") -> "Paytm"
            text.contains("PHONEPE") -> "PhonePe"
            text.contains("GPAY") || text.contains("GOOGLE PAY") -> "Google Pay"
            text.contains("BOB") || text.contains("BANK OF BARODA") -> "Bank of Baroda"
            text.contains("PNB") || text.contains("PUNJAB NATIONAL") -> "PNB"
            text.contains("CANARA") -> "Canara Bank"
            text.contains("UNION BANK") -> "Union Bank"
            text.contains("IDBI") -> "IDBI Bank"
            text.contains("YES BANK") -> "Yes Bank"
            else -> "Other Banks"
        }
    }
    
    /**
     * Extract account number (last 4 digits) from SMS body
     * Patterns: "A/c X3695", "a/c X3695", "A/C XX3695", "account X3695"
     */
    private fun extractAccountNumber(smsBody: String): String? {
        if (smsBody.isBlank()) return null
        
        // Pattern: A/c, a/c, A/C followed by optional X/XX and 4 digits
        val patterns = listOf(
            Regex("(?:A/c|a/c|A/C|account|ac)\\s*X*([0-9]{4})", RegexOption.IGNORE_CASE),
            Regex("(?:A/c|a/c|A/C|account|ac)\\s*([0-9]{4})", RegexOption.IGNORE_CASE),
            Regex("X([0-9]{4})", RegexOption.IGNORE_CASE) // Direct pattern like "X3695"
        )
        
        for (pattern in patterns) {
            val match = pattern.find(smsBody)
            if (match != null) {
                val last4 = match.groupValues.lastOrNull { it.length == 4 && it.all { char -> char.isDigit() } }
                if (last4 != null) {
                    return last4
                }
            }
        }
        
        return null
    }
    
    /**
     * Extract balance from SMS body
     * Patterns: "Avl Bal: Rs.11246", "Bal: Rs.11246", "Available balance Rs.11246"
     */
    private fun extractBalance(smsBody: String): Double? {
        if (smsBody.isBlank()) return null
        
        // Pattern: Balance keywords followed by amount
        val patterns = listOf(
            Regex("(?:Avl\\s*Bal|Available\\s*balance|Bal|Balance)[:.]?\\s*(?:Rs\\.?|₹|INR)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE),
            Regex("(?:Avl\\s*Bal|Available\\s*balance|Bal|Balance)[:.]?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(smsBody)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                try {
                    return amountStr.toDouble()
                } catch (e: NumberFormatException) {
                    // Continue to next pattern
                }
            }
        }
        
        return null
    }

    /**
     * Auto-categorize existing transactions based on merchant keywords
     * Uses the same updateTransactionCategory method as manual categorization
     */
    suspend fun autoCategorizeExistingTransactions(): Int {
        android.util.Log.d("TransactionRepository", "🤖 ===== STARTING AUTO-CATEGORIZATION =====")
        
        try {
            // Get all transactions
            val allTransactions = transactionDao.getAllTransactionsOnce()
            android.util.Log.d("TransactionRepository", "📊 Found ${allTransactions.size} total transactions")
            
            if (allTransactions.isEmpty()) {
                android.util.Log.w("TransactionRepository", "⚠️ No transactions found to categorize!")
                return 0
            }
            
            // Debug: Check how many already have categories
            val alreadyCategorized = allTransactions.count { 
                !it.categoryId.isNullOrEmpty() && it.categoryId != "" && it.categoryId != "uncategorized" 
            }
            android.util.Log.d("TransactionRepository", "📊 Already categorized: $alreadyCategorized")
            android.util.Log.d("TransactionRepository", "📊 Need categorization: ${allTransactions.size - alreadyCategorized}")
            
            var categorizedCount = 0
            var skippedCount = 0
            var unchangedCount = 0
            var alreadyHasCategoryCount = 0
            
            // Process each transaction
            for (transaction in allTransactions) {
                val catInfo = "Current: '${transaction.categoryId}' (len: ${transaction.categoryId.length}, null: ${transaction.categoryId == null}, empty: ${transaction.categoryId.isEmpty()})"
                android.util.Log.d("TransactionRepository", "📝 Processing: ${transaction.merchant} - $catInfo, Manual: ${transaction.isManuallySet}")
                
                // Skip if already manually categorized
                if (transaction.isManuallySet) {
                    android.util.Log.d("TransactionRepository", "⏭️ Skipping ${transaction.merchant} - manually set")
                    skippedCount++
                    continue
                }
                
                // Use MerchantCategorizer to determine category
                val suggestedCategory = MerchantCategorizer.categorizeTransaction(
                    transaction.merchant,
                    transaction.smsBody
                )
                
                android.util.Log.d("TransactionRepository", "💡 Suggested category for '${transaction.merchant}': $suggestedCategory")
                
                // Only update if category changed
                if (suggestedCategory != transaction.categoryId) {
                    android.util.Log.d("TransactionRepository", "🔄 UPDATING ${transaction.merchant}: ${transaction.categoryId} → $suggestedCategory")
                    
                    // Use the same method as manual categorization
                    val result = updateTransactionCategory(transaction.id, suggestedCategory)
                    
                    if (result > 0) {
                        categorizedCount++
                        android.util.Log.d("TransactionRepository", "✅ Successfully categorized: ${transaction.merchant} → $suggestedCategory")
                    } else {
                        android.util.Log.e("TransactionRepository", "❌ Failed to update ${transaction.merchant}")
                    }
                } else {
                    unchangedCount++
                    android.util.Log.d("TransactionRepository", "➡️ ${transaction.merchant} already in correct category: $suggestedCategory")
                }
            }
            
            android.util.Log.d("TransactionRepository", "🎉 ===== AUTO-CATEGORIZATION COMPLETE =====")
            android.util.Log.d("TransactionRepository", "📊 Total: ${allTransactions.size}")
            android.util.Log.d("TransactionRepository", "📊 Updated: $categorizedCount")
            android.util.Log.d("TransactionRepository", "📊 Skipped (manual): $skippedCount")
            android.util.Log.d("TransactionRepository", "📊 Unchanged (already correct): $unchangedCount")
            android.util.Log.d("TransactionRepository", "📊 Already had category: $alreadyHasCategoryCount")
            
            // Final verification: Check how many are now categorized
            val finalCheck = transactionDao.getAllTransactionsOnce()
            val finalCategorized = finalCheck.count { 
                !it.categoryId.isNullOrEmpty() && it.categoryId != "" && it.categoryId != "uncategorized" 
            }
            android.util.Log.d("TransactionRepository", "✅ Final: $finalCategorized out of ${finalCheck.size} transactions are categorized")
            
            return categorizedCount
            
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepository", "❌ Auto-categorization failed: ${e.message}", e)
            e.printStackTrace()
            return 0
        }
    }

    // Budget methods
    suspend fun getSingleBudget(): Budget? {
        return budgetDao.getSingleBudget()
    }

    suspend fun insertBudget(budget: Budget): Long {
        return budgetDao.insertBudget(budget)
    }

    suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(budget)
    }

    suspend fun clearBudgets() {
        budgetDao.clearBudgets()
    }

    suspend fun getCategoriesForBudget(budgetId: Int): List<BudgetCategory> {
        return budgetCategoryDao.getCategoriesForBudget(budgetId)
    }
    
    suspend fun getBudgetCategoriesForBudget(budgetId: Int): List<BudgetCategory> {
        return budgetCategoryDao.getCategoriesForBudget(budgetId)
    }

    suspend fun insertAllBudgetCategories(categories: List<BudgetCategory>) {
        budgetCategoryDao.insertAll(categories)
    }

    suspend fun updateBudgetCategory(category: BudgetCategory) {
        budgetCategoryDao.update(category)
    }

    suspend fun clearBudgetCategoriesForBudget(budgetId: Int) {
        budgetCategoryDao.clearForBudget(budgetId)
    }
    
    suspend fun getTransactionCountByCategory(categoryId: String, startDate: Long, endDate: Long): Int {
        return transactionDao.getTransactionCountByCategory(categoryId, startDate, endDate)
    }

    // Cash Flow Transaction Methods
    suspend fun addToCashFlow(transactionId: String) {
        val cashFlowTransaction = CashFlowTransaction(
            id = UUID.randomUUID().toString(),
            transactionId = transactionId
        )
        cashFlowTransactionDao.insertCashFlowTransaction(cashFlowTransaction)
    }

    suspend fun removeFromCashFlow(transactionId: String) {
        cashFlowTransactionDao.deleteCashFlowTransactionByTransactionId(transactionId)
    }

    suspend fun isCashFlowTransaction(transactionId: String): Boolean {
        return cashFlowTransactionDao.isCashFlowTransaction(transactionId)
    }

    suspend fun getCashFlowTransactions(): List<Transaction> {
        return cashFlowTransactionDao.getCashFlowTransactionsWithDetails()
    }
    
    suspend fun getCashFlowTransactionIds(): Set<String> {
        val cashFlowTransactions = cashFlowTransactionDao.getAllCashFlowTransactions()
        return cashFlowTransactions.map { it.transactionId }.toSet()
    }
    
    /**
     * Manually trigger budget monitoring - useful for testing or manual checks
     */
    suspend fun triggerBudgetMonitoring() {
        try {
            val budgetMonitor = BudgetMonitor.getInstance(context)
            budgetMonitor.checkBudgetStatus()
            android.util.Log.d("TransactionRepository", "💰 Manual budget monitoring triggered")
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepository", "❌ Failed to trigger manual budget monitoring", e)
        }
    }
}
