package com.koshpal_android.koshpalapp.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.model.PaymentSms
import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.sms.processor.SmsProcessingPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class SMSManager(private val context: Context) {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    @Inject
    lateinit var syncService: com.koshpal_android.koshpalapp.service.TransactionSyncService

    suspend fun processAllSMS(): ProcessResult {
        return withContext(Dispatchers.IO) {
            val result = ProcessResult()

            try {
                if (!hasPermissions()) {
                    result.error = "SMS permissions not granted"
                    return@withContext result
                }

                val database = KoshpalDatabase.getDatabase(context)

                val smsMessages = readSMSFromDevice()
                result.smsFound = smsMessages.size

                // Ensure default categories exist
                val categoryDao = database.categoryDao()
                val existingCategories = categoryDao.getDefaultCategories()

                if (existingCategories.isEmpty()) {
                    val defaultCategories = TransactionCategory.getDefaultCategories()
                    categoryDao.insertCategories(defaultCategories)
                }

                // Use optimized batch pipeline for initial scan so that:
                // - All scanned SMS are stored in payment_sms table (marked processed)
                // - Transactions are created only for matching payment SMS
                val pipeline = SmsProcessingPipeline(context)
                val paymentSmsDao = database.paymentSmsDao()
                val transactionDao = database.transactionDao()

                // Existing processed SMS (body + sender) so we don't duplicate PaymentSms rows
                val existingProcessedPairs = paymentSmsDao
                    .getAllBodySenderPairs()
                    .map { it.smsBody to it.sender }
                    .toSet()

                // Existing transaction SMS bodies so we don't recreate transactions
                val existingTransactionSmsBodies = transactionDao
                    .getAllSmsBodies()
                    .toSet()

                val (transactionsToInsert, paymentSmsToInsert) =
                    pipeline.processBatchForInitialScan(
                        smsList = smsMessages,
                        existingSmsBodies = existingTransactionSmsBodies,
                        existingProcessed = existingProcessedPairs
                    )

                if (paymentSmsToInsert.isNotEmpty()) {
                    paymentSmsDao.insertSmsList(paymentSmsToInsert)
                }
                if (transactionsToInsert.isNotEmpty()) {
                    transactionDao.insertTransactions(transactionsToInsert)
                }

                result.smsProcessed = smsMessages.size
                result.transactionSmsFound = transactionsToInsert.size
                result.transactionsCreated = transactionsToInsert.size
                result.success = true

            } catch (e: Exception) {
                result.error = "Error: ${e.message}"
                Log.e("SMSManager", "SMS processing failed", e)
            }

            result
        }
    }

    private fun readSMSFromDevice(): List<PaymentSms> {
        val smsList = mutableListOf<PaymentSms>()

        try {
            val uri = Uri.parse("content://sms")
            val projection = arrayOf("_id", "address", "body", "date")

            val twoMonthsAgo = System.currentTimeMillis() - (2 * 30 * 24 * 60 * 60 * 1000L)
            val selection = "date >= ?"
            val selectionArgs = arrayOf(twoMonthsAgo.toString())

            val cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "date DESC"
            )

            cursor?.use { c ->
                val addressIndex = c.getColumnIndexOrThrow("address")
                val bodyIndex = c.getColumnIndexOrThrow("body")
                val dateIndex = c.getColumnIndexOrThrow("date")

                while (c.moveToNext()) {
                    val address = c.getString(addressIndex) ?: "Unknown"
                    val body = c.getString(bodyIndex) ?: ""
                    val timestamp = c.getLong(dateIndex)

                    val sms = PaymentSms(
                        id = UUID.randomUUID().toString(),
                        sender = address,
                        smsBody = body,
                        timestamp = timestamp,
                        isProcessed = false
                    )

                    smsList.add(sms)
                }
            }

        } catch (e: Exception) {
            Log.e("SMSManager", "Error reading SMS: ${e.message}", e)
        }
        return smsList
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECEIVE_SMS
                ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun createSampleData(): ProcessResult {
        return withContext(Dispatchers.IO) {
            val result = ProcessResult()

            try {
                Log.d("SMSManager", "🧪 Starting sample data creation...")
                val database = KoshpalDatabase.getDatabase(context)
                val transactionDao = database.transactionDao()
                val categoryDao = database.categoryDao()

                Log.d("SMSManager", "📂 Ensuring default categories exist...")
                val existingCategories = categoryDao.getDefaultCategories()
                if (existingCategories.isEmpty()) {
                    val defaultCategories = TransactionCategory.getDefaultCategories()
                    categoryDao.insertCategories(defaultCategories)
                    Log.d("SMSManager", "✅ Inserted ${defaultCategories.size} default categories")
                }

                val sampleTransactions = createSampleTransactions()
                Log.d("SMSManager", "📝 Created ${sampleTransactions.size} sample transactions")

                sampleTransactions.forEach { transaction ->
                    try {
                        val existing = transactionDao.getTransactionsBySmsBody(transaction.smsBody ?: "")
                        if (existing == null) {
                            transactionDao.insertTransaction(transaction)
                            result.transactionsCreated++
                            Log.d("SMSManager", "✅ Inserted transaction: ${transaction.merchant} - ₹${transaction.amount}")
                        } else {
                            Log.d("SMSManager", "⏭️ Transaction already exists: ${transaction.merchant}")
                        }
                    } catch (e: Exception) {
                        Log.e("SMSManager", "❌ Error inserting transaction ${transaction.merchant}: ${e.message}")
                        throw e
                    }
                }

                result.success = true
                result.smsFound = sampleTransactions.size
                result.transactionSmsFound = sampleTransactions.size
                result.smsProcessed = sampleTransactions.size

                Log.d("SMSManager", "🎉 Sample data creation completed! Created ${result.transactionsCreated} transactions")

            } catch (e: Exception) {
                Log.e("SMSManager", "❌ Error creating sample data: ${e.message}", e)
                result.error = "Error creating sample data: ${e.message}"
                result.success = false
            }

            result
        }
    }

    private fun createSampleTransactions(): List<com.koshpal_android.koshpalapp.model.Transaction> {
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
        val monthStart = calendar.timeInMillis

        return listOf(
            com.koshpal_android.koshpalapp.model.Transaction(
                id = UUID.randomUUID().toString(),
                amount = 500.0,
                type = com.koshpal_android.koshpalapp.model.TransactionType.DEBIT,
                merchant = "Amazon India",
                categoryId = "shopping",
                confidence = 100f,
                date = monthStart + (1 * 24 * 60 * 60 * 1000),
                description = "Online shopping",
                smsBody = "Your A/c debited by Rs.500.00 at AMAZON INDIA"
            ),
            com.koshpal_android.koshpalapp.model.Transaction(
                id = UUID.randomUUID().toString(),
                amount = 1200.0,
                type = com.koshpal_android.koshpalapp.model.TransactionType.DEBIT,
                merchant = "Zomato",
                categoryId = "food",
                confidence = 100f,
                date = monthStart + (5 * 24 * 60 * 60 * 1000),
                description = "Food delivery",
                smsBody = "Rs.1200 debited for UPI/ZOMATO"
            ),
            com.koshpal_android.koshpalapp.model.Transaction(
                id = UUID.randomUUID().toString(),
                amount = 25000.0,
                type = com.koshpal_android.koshpalapp.model.TransactionType.CREDIT,
                merchant = "Salary Credit",
                categoryId = "salary",
                confidence = 100f,
                date = monthStart + (10 * 24 * 60 * 60 * 1000),
                description = "Monthly salary",
                smsBody = "Your account credited with Rs.25000.00 Salary credit"
            ),
            com.koshpal_android.koshpalapp.model.Transaction(
                id = UUID.randomUUID().toString(),
                amount = 350.0,
                type = com.koshpal_android.koshpalapp.model.TransactionType.DEBIT,
                merchant = "Uber",
                categoryId = "transport",
                confidence = 100f,
                date = monthStart + (12 * 24 * 60 * 60 * 1000),
                description = "Cab ride",
                smsBody = "INR 350.00 debited for UBER TRIP"
            ),
            com.koshpal_android.koshpalapp.model.Transaction(
                id = UUID.randomUUID().toString(),
                amount = 800.0,
                type = com.koshpal_android.koshpalapp.model.TransactionType.DEBIT,
                merchant = "DMart",
                categoryId = "grocery",
                confidence = 100f,
                date = monthStart + (15 * 24 * 60 * 60 * 1000),
                description = "Grocery shopping",
                smsBody = "Rs.800 spent at DMART GROCERY"
            ),
            com.koshpal_android.koshpalapp.model.Transaction(
                id = UUID.randomUUID().toString(),
                amount = 2500.0,
                type = com.koshpal_android.koshpalapp.model.TransactionType.DEBIT,
                merchant = "Flipkart",
                categoryId = "shopping",
                confidence = 100f,
                date = monthStart + (18 * 24 * 60 * 60 * 1000),
                description = "Online shopping",
                smsBody = "₹2500 spent at FLIPKART"
            ),
            com.koshpal_android.koshpalapp.model.Transaction(
                id = UUID.randomUUID().toString(),
                amount = 150.0,
                type = com.koshpal_android.koshpalapp.model.TransactionType.DEBIT,
                merchant = "Swiggy",
                categoryId = "food",
                confidence = 100f,
                date = currentTime - 604800000,
                description = "Food delivery",
                smsBody = "You paid ₹150 to SWIGGY via UPI"
            ),
            com.koshpal_android.koshpalapp.model.Transaction(
                id = UUID.randomUUID().toString(),
                amount = 45000.0,
                type = com.koshpal_android.koshpalapp.model.TransactionType.CREDIT,
                merchant = "Salary Credit",
                categoryId = "salary",
                confidence = 100f,
                date = currentTime - 2592000000,
                description = "Monthly salary",
                smsBody = "Your salary Rs.45000 credited to account"
            )
        )
    }
}

data class ProcessResult(
    var success: Boolean = false,
    var smsFound: Int = 0,
    var transactionSmsFound: Int = 0,
    var smsProcessed: Int = 0,
    var transactionsCreated: Int = 0,
    var error: String? = null
)
