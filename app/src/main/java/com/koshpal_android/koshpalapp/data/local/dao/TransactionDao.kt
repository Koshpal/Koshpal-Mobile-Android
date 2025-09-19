package com.koshpal_android.koshpalapp.data.local.dao

import androidx.room.*
import androidx.paging.PagingSource
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsPaged(): PagingSource<Int, Transaction>
    
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): Transaction?
    
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY timestamp DESC")
    fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE timestamp BETWEEN :startTime AND :endTime 
        ORDER BY timestamp DESC
    """)
    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE timestamp BETWEEN :startTime AND :endTime 
        AND type = :type
        ORDER BY timestamp DESC
    """)
    suspend fun getTransactionsByDateRangeAndType(
        startTime: Long, 
        endTime: Long, 
        type: TransactionType
    ): List<Transaction>
    
    @Query("""
        SELECT categoryId, SUM(amount) as totalAmount 
        FROM transactions 
        WHERE type = 'DEBIT' AND timestamp BETWEEN :startTime AND :endTime
        GROUP BY categoryId
        ORDER BY totalAmount DESC
    """)
    suspend fun getCategoryWiseSpending(startTime: Long, endTime: Long): List<CategorySpending>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalAmountByTypeAndDateRange(type: TransactionType, startTime: Long, endTime: Long): Double?
    
    @Query("SELECT * FROM transactions WHERE merchant LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%'")
    fun searchTransactions(searchQuery: String): Flow<List<Transaction>>
    
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
    
    @Query("UPDATE transactions SET categoryId = :categoryId, isManuallySet = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTransactionCategory(id: String, categoryId: String, updatedAt: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int
    
    @Query("SELECT * FROM transactions WHERE confidence < 70 ORDER BY timestamp DESC")
    suspend fun getLowConfidenceTransactions(): List<Transaction>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'CREDIT' AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalIncomeForPeriod(startTime: Long, endTime: Long): Double?
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT' AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalExpensesForPeriod(startTime: Long, endTime: Long): Double?
    
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentTransactions(limit: Int): List<Transaction>
    
    @Query("SELECT * FROM transactions WHERE smsBody = :smsBody")
    suspend fun getTransactionsBySmsBody(smsBody: String): List<Transaction>
}

data class CategorySpending(
    val categoryId: String,
    val totalAmount: Double
)
