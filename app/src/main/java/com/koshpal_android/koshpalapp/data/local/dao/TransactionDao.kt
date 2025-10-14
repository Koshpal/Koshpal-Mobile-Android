package com.koshpal_android.koshpalapp.data.local.dao

import androidx.room.*
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.model.CategorySpending
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsOnce(): List<Transaction>
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): Transaction?
    
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getTransactionsByCategoryAndDateRange(categoryId: String, startDate: Long, endDate: Long): List<Transaction>
    
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE merchant LIKE '%' || :merchant || '%' ORDER BY date DESC")
    fun getTransactionsByMerchant(merchant: String): Flow<List<Transaction>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE bankName = :bankName 
        OR (bankName IS NULL AND (
            smsBody LIKE '%' || :bankName || '%' 
            OR merchant LIKE '%' || :bankName || '%'
            OR smsBody LIKE '%' || :bankNamePattern1 || '%'
            OR smsBody LIKE '%' || :bankNamePattern2 || '%'
        ))
        ORDER BY date DESC
    """)
    suspend fun getTransactionsByBank(bankName: String, bankNamePattern1: String, bankNamePattern2: String): List<Transaction>
    
    @Query("SELECT * FROM transactions WHERE smsId = :smsId")
    suspend fun getTransactionBySmsId(smsId: String): Transaction?
    
    @Query("SELECT * FROM transactions WHERE description LIKE '%' || :query || '%' OR merchant LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchTransactions(query: String): Flow<List<Transaction>>
    
    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int
    
    @Query("SELECT COUNT(*) FROM transactions WHERE type = :type")
    suspend fun getTransactionCountByType(type: TransactionType): Int
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type")
    suspend fun getTotalAmountByType(type: TransactionType): Double?
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalAmountByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long): Double?
    
    @Query("SELECT SUM(amount) FROM transactions WHERE categoryId = :categoryId AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalAmountByCategoryAndDateRange(categoryId: String, startDate: Long, endDate: Long): Double?
    
    @Query("SELECT * FROM transactions WHERE description LIKE '%' || :smsBody || '%' OR merchant LIKE '%' || :smsBody || '%' LIMIT 1")
    suspend fun getTransactionsBySmsBody(smsBody: String): Transaction?
    
    @Query("SELECT * FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getTransactionsByDateRangeAndType(startDate: Long, endDate: Long, type: TransactionType): List<Transaction>
    
    @Query("""
        SELECT categoryId, SUM(amount) as totalAmount 
        FROM transactions 
        WHERE type = 'DEBIT' 
        AND categoryId IS NOT NULL 
        AND date >= :startDate 
        AND date <= :endDate
        GROUP BY categoryId 
        HAVING SUM(amount) > 0
        ORDER BY totalAmount DESC
    """)
    suspend fun getCategoryWiseSpending(startDate: Long, endDate: Long): List<CategorySpending>
    
    // Get category spending for all time
    @Query("""
        SELECT categoryId, SUM(amount) as totalAmount 
        FROM transactions 
        WHERE type = 'DEBIT' 
        AND categoryId IS NOT NULL 
        AND categoryId != ''
        GROUP BY categoryId 
        HAVING SUM(amount) > 0
        ORDER BY totalAmount DESC
    """)
    suspend fun getAllTimeCategorySpending(): List<CategorySpending>
    
    // Get category spending for current month only
    @Query("""
        SELECT categoryId, SUM(amount) as totalAmount 
        FROM transactions 
        WHERE type = 'DEBIT' 
        AND categoryId IS NOT NULL 
        AND categoryId != ''
        AND date >= :startOfMonth 
        AND date <= :endOfMonth
        GROUP BY categoryId 
        HAVING SUM(amount) > 0
        ORDER BY totalAmount DESC
    """)
    suspend fun getCurrentMonthCategorySpending(startOfMonth: Long, endOfMonth: Long): List<CategorySpending>
    
    // Simple category spending query (all time, all categories)
    @Query("""
        SELECT categoryId, SUM(amount) as totalAmount 
        FROM transactions 
        WHERE type = 'DEBIT' 
        AND categoryId IS NOT NULL 
        AND categoryId != ''
        GROUP BY categoryId 
        HAVING SUM(amount) > 0
        ORDER BY totalAmount DESC
    """)
    suspend fun getSimpleCategorySpending(): List<CategorySpending>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE categoryId IS NOT NULL 
        AND categoryId != '' 
        AND categoryId != 'uncategorized'
        ORDER BY date DESC
    """)
    suspend fun getAllCategorizedTransactions(): List<Transaction>
    
    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE categoryId = :categoryId 
        AND type = 'DEBIT'
        AND date >= :startDate 
        AND date <= :endDate
    """)
    suspend fun getTransactionCountByCategory(categoryId: String, startDate: Long, endDate: Long): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)
    
    @Update
    suspend fun updateTransaction(transaction: Transaction)
    
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
    
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)
    
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
    
    // Delete transactions that might be test/dummy data
    @Query("DELETE FROM transactions WHERE merchant LIKE '%test%' OR merchant LIKE '%dummy%' OR merchant LIKE '%sample%' OR description LIKE '%test%'")
    suspend fun deleteTestTransactions(): Int
    
    // Reset all transactions to "others" category
    @Query("UPDATE transactions SET categoryId = 'others' WHERE categoryId IS NOT NULL AND categoryId != 'others'")
    suspend fun resetAllTransactionsToOthers(): Int
    
    // Additional missing methods
    
    @Query("UPDATE transactions SET categoryId = :categoryId, updatedAt = :updatedAt WHERE id = :transactionId")
    suspend fun updateTransactionCategory(transactionId: String, categoryId: String, updatedAt: Long = System.currentTimeMillis()): Int
    
    // Check if transaction exists
    @Query("SELECT COUNT(*) FROM transactions WHERE id = :transactionId")
    suspend fun transactionExists(transactionId: String): Int
    
    // Get transaction by ID with detailed info
    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionByIdDetailed(transactionId: String): Transaction?
    
    @Query("SELECT * FROM transactions WHERE confidence < 0.8 ORDER BY date DESC")
    suspend fun getLowConfidenceTransactions(): List<Transaction>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'CREDIT' AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalIncomeForPeriod(startDate: Long, endDate: Long): Double?
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT' AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpensesForPeriod(startDate: Long, endDate: Long): Double?
    
    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentTransactions(limit: Int = 10): List<Transaction>
    
    // Duplicate prevention: Check by amount and time window
    @Query("SELECT * FROM transactions WHERE amount = :amount AND date BETWEEN :startTime AND :endTime LIMIT 1")
    suspend fun getTransactionByAmountAndTime(amount: Double, startTime: Long, endTime: Long): Transaction?
}
