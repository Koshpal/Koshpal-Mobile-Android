package com.koshpal_android.koshpalapp.service

import com.koshpal_android.koshpalapp.repository.TransactionRepository
import com.koshpal_android.koshpalapp.repository.BudgetRepository
import com.koshpal_android.koshpalapp.data.local.dao.PaymentSmsDao
import com.koshpal_android.koshpalapp.engine.TransactionCategorizationEngine
import com.koshpal_android.koshpalapp.alerts.SpendingAlertManager
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
    private val budgetRepository: BudgetRepository,
    private val paymentSmsDao: PaymentSmsDao,
    private val categorizationEngine: TransactionCategorizationEngine,
    private val spendingAlertManager: SpendingAlertManager
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
                            
                            // Update budget spending if it's an expense
                            if (transaction.type == TransactionType.DEBIT) {
                                updateBudgetSpending(transaction)
                            }
                            
                            // Check for spending alerts
                            spendingAlertManager.checkBudgetThresholds(transaction)
                            
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
    
    private suspend fun updateBudgetSpending(transaction: Transaction) {
        try {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = transaction.timestamp
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            
            val budget = budgetRepository.getBudgetByCategoryAndMonth(
                transaction.categoryId, month, year
            )
            
            budget?.let {
                budgetRepository.updateBudgetSpending(it.id, transaction.amount)
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }
    
    suspend fun recategorizeTransaction(transactionId: String, newCategoryId: String) {
        withContext(Dispatchers.IO) {
            try {
                val oldTransaction = transactionRepository.getTransactionById(transactionId)
                
                // Update transaction category
                transactionRepository.recategorizeTransaction(transactionId, newCategoryId)
                
                // Update budget spending
                oldTransaction?.let { old ->
                    if (old.type == TransactionType.DEBIT) {
                        // Remove from old budget
                        updateBudgetForCategoryChange(old, newCategoryId, old.amount)
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    private suspend fun updateBudgetForCategoryChange(
        oldTransaction: Transaction,
        newCategoryId: String,
        amount: Double
    ) {
        try {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = oldTransaction.timestamp
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            
            // Remove from old budget
            val oldBudget = budgetRepository.getBudgetByCategoryAndMonth(
                oldTransaction.categoryId, month, year
            )
            oldBudget?.let {
                budgetRepository.updateBudgetSpending(it.id, -amount)
            }
            
            // Add to new budget
            val newBudget = budgetRepository.getBudgetByCategoryAndMonth(
                newCategoryId, month, year
            )
            newBudget?.let {
                budgetRepository.updateBudgetSpending(it.id, amount)
            }
        } catch (e: Exception) {
            // Handle error silently
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
    
    suspend fun schedulePeriodicTasks() {
        withContext(Dispatchers.IO) {
            try {
                // Schedule daily and weekly summaries
                spendingAlertManager.scheduleDailySummary()
                spendingAlertManager.scheduleWeeklySummary()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
