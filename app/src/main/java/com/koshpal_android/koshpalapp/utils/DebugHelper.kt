package com.koshpal_android.koshpalapp.utils

import android.content.Context
import android.util.Log
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.engine.TransactionCategorizationEngine
import com.koshpal_android.koshpalapp.model.PaymentSms
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

object DebugHelper {
    
    suspend fun debugFullFlow(context: Context): String {
        return withContext(Dispatchers.IO) {
            val log = StringBuilder()
            
            try {
                log.append("=== KOSHPAL DEBUG REPORT ===\n\n")
                
                val database = KoshpalDatabase.getDatabase(context)
                val paymentSmsDao = database.paymentSmsDao()
                val transactionDao = database.transactionDao()
                val categoryDao = database.categoryDao()
                
                // 1. Check database connection
                log.append("1. DATABASE CONNECTION: ✅ Connected\n\n")
                
                // 2. Check categories
                val categories = categoryDao.getAllActiveCategories()
                categories.collect { cats ->
                    log.append("2. CATEGORIES: ${cats.size} categories found\n")
                    cats.forEach { cat ->
                        log.append("   - ${cat.name} (${cat.id})\n")
                    }
                    log.append("\n")
                }
                
                // 3. Create and test SMS processing
                log.append("3. CREATING TEST SMS...\n")
                val testSMS = createTestSMS()
                
                testSMS.forEach { sms ->
                    log.append("   Inserting: ${sms.body.take(50)}...\n")
                    paymentSmsDao.insertSms(sms)
                }
                
                // 4. Check SMS count
                val allSms = paymentSmsDao.getUnprocessedSms()
                log.append("   SMS Count: ${allSms.size}\n\n")
                
                // 5. Test transaction extraction
                log.append("4. TESTING TRANSACTION EXTRACTION...\n")
                val engine = TransactionCategorizationEngine()
                
                allSms.forEach { sms ->
                    val details = engine.extractTransactionDetails(sms.body)
                    log.append("   SMS: ${sms.body.take(30)}...\n")
                    log.append("   Amount: ₹${details.amount}\n")
                    log.append("   Merchant: ${details.merchant}\n")
                    log.append("   Type: ${details.type}\n")
                    log.append("   ---\n")
                }
                
                // 6. Create transactions directly
                log.append("\n5. CREATING TRANSACTIONS DIRECTLY...\n")
                val transactions = createDirectTransactions()
                
                transactions.forEach { transaction ->
                    transactionDao.insertTransaction(transaction)
                    log.append("   Created: ₹${transaction.amount} at ${transaction.merchant}\n")
                }
                
                // 7. Check final counts
                val finalTransactions = transactionDao.getRecentTransactions(20)
                log.append("\n6. FINAL RESULTS:\n")
                log.append("   Total Transactions: ${finalTransactions.size}\n")
                
                finalTransactions.forEach { txn ->
                    log.append("   - ₹${txn.amount} | ${txn.merchant} | ${txn.type}\n")
                }
                
                log.append("\n=== DEBUG COMPLETE ===")
                
            } catch (e: Exception) {
                log.append("ERROR: ${e.message}\n")
                log.append("Stack: ${e.stackTrace.joinToString("\n")}")
            }
            
            val result = log.toString()
            Log.d("DebugHelper", result)
            result
        }
    }
    
    private fun createTestSMS(): List<PaymentSms> {
        val currentTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        return listOf(
            PaymentSms(
                id = UUID.randomUUID().toString(),
                sender = "SBIINB",
                smsBody = "Your A/c XX1234 debited by Rs.500.00 on 15-Dec-23 at AMAZON INDIA. Avl Bal: Rs.10000.00",
                timestamp = currentTime - 86400000,
                isProcessed = false
            ),
            PaymentSms(
                id = UUID.randomUUID().toString(),
                sender = "HDFCBK",
                smsBody = "Rs.1200 debited from A/c XX5678 for UPI/ZOMATO/123456789 on 15-Dec-23. Bal: Rs.8800",
                timestamp = currentTime - 172800000,
                isProcessed = false
            ),
            PaymentSms(
                id = UUID.randomUUID().toString(),
                sender = "ICICIB",
                smsBody = "Your account credited with Rs.25000.00 on 15-Dec-23. Salary credit. Available balance Rs.35000.00",
                timestamp = currentTime - 259200000,
                isProcessed = false
            )
        )
    }
    
    private fun createDirectTransactions(): List<Transaction> {
        val currentTime = System.currentTimeMillis()
        
        return listOf(
            Transaction(
                id = UUID.randomUUID().toString(),
                amount = 500.0,
                type = TransactionType.DEBIT,
                merchant = "Amazon India",
                categoryId = "shopping",
                confidence = 0.95f,
                date = currentTime - 86400000,
                description = "Online shopping",
                smsBody = "Amazon purchase"
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                amount = 1200.0,
                type = TransactionType.DEBIT,
                merchant = "Zomato",
                categoryId = "food",
                confidence = 0.90f,
                date = currentTime - 172800000,
                description = "Food delivery",
                smsBody = "Rs.1200 debited from A/c XX5678 for UPI/ZOMATO/123456789 on 15-Dec-23. Bal: Rs.8800"
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                amount = 25000.0,
                type = TransactionType.CREDIT,
                merchant = "Salary Credit",
                categoryId = "salary",
                confidence = 0.98f,
                date = currentTime - 259200000,
                description = "Monthly salary",
                smsBody = "Salary credit"
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                amount = 350.0,
                type = TransactionType.DEBIT,
                merchant = "Uber",
                categoryId = "transport",
                confidence = 0.85f,
                date = currentTime - 345600000,
                description = "Cab ride",
                smsBody = "Uber trip"
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                amount = 800.0,
                type = TransactionType.DEBIT,
                merchant = "DMart",
                categoryId = "grocery",
                confidence = 0.88f,
                date = currentTime - 432000000,
                description = "Grocery shopping",
                smsBody = "DMart purchase"
            )
        )
    }
}
