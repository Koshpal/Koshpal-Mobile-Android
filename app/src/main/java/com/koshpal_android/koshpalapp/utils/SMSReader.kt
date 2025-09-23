package com.koshpal_android.koshpalapp.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.koshpal_android.koshpalapp.data.local.dao.PaymentSmsDao
import com.koshpal_android.koshpalapp.model.PaymentSms
import com.koshpal_android.koshpalapp.service.TransactionProcessingService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SMSReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val paymentSmsDao: PaymentSmsDao,
    private val transactionProcessingService: TransactionProcessingService
) {

    suspend fun readAndProcessExistingSMS() {
        withContext(Dispatchers.IO) {
            if (!hasReadSMSPermission()) {
                Log.w("SMSReader", "No SMS read permission granted")
                return@withContext
            }

            try {
                val smsUri = Uri.parse("content://sms/inbox")
                val projection = arrayOf("_id", "address", "body", "date")
                
                // Read SMS from last 30 days
                val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
                val selection = "date >= ?"
                val selectionArgs = arrayOf(thirtyDaysAgo.toString())
                
                val cursor = context.contentResolver.query(
                    smsUri, projection, selection, selectionArgs, "date DESC"
                )

                cursor?.use {
                    var processedCount = 0
                    val maxSmsToProcess = 500 // Limit to avoid overwhelming the system
                    
                    while (it.moveToNext() && processedCount < maxSmsToProcess) {
                        val address = it.getString(it.getColumnIndexOrThrow("address"))
                        val body = it.getString(it.getColumnIndexOrThrow("body"))
                        val date = it.getLong(it.getColumnIndexOrThrow("date"))

                        if (isTransactionSMS(body, address)) {
                            // Check if we already have this SMS
                            val existingSms = paymentSmsDao.getSMSByBodyAndSender(body, address)
                            
                            if (existingSms == null) {
                                val paymentSms = PaymentSms(
                                    id = UUID.randomUUID().toString(),
                                    sender = address,
                                    smsBody = body,
                                    timestamp = date,
                                    isProcessed = false
                                )
                                
                                paymentSmsDao.insertSms(paymentSms)
                                processedCount++
                            }
                        }
                    }
                    
                    Log.d("SMSReader", "Processed $processedCount SMS messages")
                    
                    // Process all unprocessed SMS
                    if (processedCount > 0) {
                        transactionProcessingService.processUnprocessedSms()
                    }
                }
            } catch (e: Exception) {
                Log.e("SMSReader", "Error reading SMS", e)
            }
        }
    }

    private fun hasReadSMSPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isTransactionSMS(messageBody: String, sender: String): Boolean {
        val transactionKeywords = listOf(
            "debited", "credited", "withdrawn", "deposited", 
            "paid", "received", "transaction", "transfer",
            "upi", "imps", "neft", "rtgs", "atm",
            "rs.", "rs ", "inr", "₹", "rupees"
        )
        
        val bankSenders = listOf(
            "SBIINB", "HDFCBK", "ICICIB", "AXISBK", "KOTAKB",
            "PNBSMS", "BOBSMS", "CANBKS", "UNISBI", "IOBNET",
            "PAYTM", "GPAY", "PHONEPE", "AMAZONP", "BHARTP"
        )
        
        val messageBodyLower = messageBody.lowercase()
        val senderUpper = sender.uppercase()
        
        // Check if sender is from known bank/payment service
        val isFromBank = bankSenders.any { senderUpper.contains(it) }
        
        // Check if message contains transaction keywords
        val hasTransactionKeywords = transactionKeywords.any { 
            messageBodyLower.contains(it) 
        }
        
        // Check if message contains amount pattern
        val hasAmountPattern = messageBody.matches(
            Regex(".*(?:₹|rs\\.?|inr)\\s*[0-9,]+(?:\\.[0-9]{2})?.*", RegexOption.IGNORE_CASE)
        )
        
        return (isFromBank || hasTransactionKeywords) && hasAmountPattern
    }
}
