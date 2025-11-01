package com.koshpal_android.koshpalapp.service

import android.content.Context
import android.util.Log
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import com.koshpal_android.koshpalapp.data.local.dao.PaymentSmsDao
import com.koshpal_android.koshpalapp.engine.TransactionCategorizationEngine
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionProcessingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val paymentSmsDao: PaymentSmsDao,
    private val categorizationEngine: TransactionCategorizationEngine
) {
    
    companion object {
        private const val TAG = "TransactionProcessingService"
    }
    
    suspend fun processUnprocessedSms() {
        withContext(Dispatchers.IO) {
            try {
                val unprocessedSms = paymentSmsDao.getUnprocessedSms()
                
                for (sms in unprocessedSms) {
                    try {
                        val transactionDetails = categorizationEngine.extractTransactionDetails(sms.body)
                        
                        if (transactionDetails.amount > 0) {
                            val transaction = transactionRepository.categorizeAndInsertTransaction(
                                smsBody = sms.body,
                                merchant = transactionDetails.merchant,
                                amount = transactionDetails.amount,
                                type = transactionDetails.type,
                                timestamp = sms.timestamp
                            )
                            
                            // Transaction processed successfully
                            Log.d(TAG, "✅ Transaction processed: ${transaction.id}")
                            
                            // Schedule background sync for this transaction
                            TransactionSyncScheduler.scheduleSingleTransactionSync(context, transaction.id)
                            Log.d(TAG, "📤 Scheduled background sync for transaction: ${transaction.id}")
                            
                            // Mark SMS as processed
                            paymentSmsDao.markAsProcessed(sms.id)
                        }
                    } catch (e: Exception) {
                        // Log error but continue processing other SMS
                        continue
                    }
                }
            } catch (e: Exception) {
                // Handle general error
            }
        }
    }
    
    suspend fun recategorizeTransaction(transactionId: String, newCategoryId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Update transaction category
                transactionRepository.recategorizeTransaction(transactionId, newCategoryId)
                
                Log.d(TAG, "✅ Transaction recategorized: $transactionId -> $newCategoryId")
                
                // Schedule background sync for the updated transaction
                TransactionSyncScheduler.scheduleSingleTransactionSync(context, transactionId)
                Log.d(TAG, "📤 Scheduled background sync for updated transaction: $transactionId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to recategorize transaction: ${e.message}", e)
            }
        }
    }
    
    suspend fun initializeDefaultCategories() {
        withContext(Dispatchers.IO) {
            try {
                // This would initialize default categories in the database
                // Implementation depends on your CategoryDao
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
