package com.koshpal_android.koshpalapp.data.local.dao

import androidx.room.*
import com.koshpal_android.koshpalapp.model.CashFlowTransaction
import com.koshpal_android.koshpalapp.model.Transaction

@Dao
interface CashFlowTransactionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashFlowTransaction(cashFlowTransaction: CashFlowTransaction)
    
    @Delete
    suspend fun deleteCashFlowTransaction(cashFlowTransaction: CashFlowTransaction)
    
    @Query("DELETE FROM cash_flow_transactions WHERE transaction_id = :transactionId")
    suspend fun deleteCashFlowTransactionByTransactionId(transactionId: String)
    
    @Query("SELECT * FROM cash_flow_transactions")
    suspend fun getAllCashFlowTransactions(): List<CashFlowTransaction>
    
    @Query("SELECT EXISTS(SELECT 1 FROM cash_flow_transactions WHERE transaction_id = :transactionId)")
    suspend fun isCashFlowTransaction(transactionId: String): Boolean
    
    @Query("""
        SELECT t.* FROM transactions t
        INNER JOIN cash_flow_transactions cf ON t.id = cf.transaction_id
        ORDER BY t.date DESC
    """)
    suspend fun getCashFlowTransactionsWithDetails(): List<Transaction>
}
