package com.koshpal_android.koshpalapp.service

import com.koshpal_android.koshpalapp.repository.TransactionRepository
import com.koshpal_android.koshpalapp.data.local.dao.PaymentSmsDao
import com.koshpal_android.koshpalapp.engine.TransactionCategorizationEngine
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionProcessingService @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val paymentSmsDao: PaymentSmsDao,
    private val categorizationEngine: TransactionCategorizationEngine
) {
    
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
            } catch (e: Exception) {
                // Handle error
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
