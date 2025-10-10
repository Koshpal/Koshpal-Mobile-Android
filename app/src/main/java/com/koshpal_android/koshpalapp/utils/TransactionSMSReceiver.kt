package com.koshpal_android.koshpalapp.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import com.koshpal_android.koshpalapp.Application
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.engine.TransactionCategorizationEngine
import com.koshpal_android.koshpalapp.model.PaymentSms
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class TransactionSMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras
            if (bundle != null) {
                try {
                    val pdus = bundle.get("pdus") as? Array<*>
                    val format = bundle.getString("format")
                    
                    pdus?.forEach { pdu ->
                        val smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            SmsMessage.createFromPdu(pdu as ByteArray, format)
                        } else {
                            @Suppress("DEPRECATION")
                            SmsMessage.createFromPdu(pdu as ByteArray)
                        }
                        
                        val messageBody = smsMessage?.messageBody
                        val sender = smsMessage?.originatingAddress
                        
                        if (messageBody != null && sender != null) {
                            // Check if this looks like a transaction SMS
                            if (isTransactionSMS(messageBody, sender)) {
                                Log.d("TransactionSMS", "Detected transaction SMS from $sender: $messageBody")
                                
                                // Process SMS immediately in background
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        context?.let { ctx ->
                                            val database = KoshpalDatabase.getDatabase(ctx)
                                            val paymentSmsDao = database.paymentSmsDao()
                                            val transactionDao = database.transactionDao()
                                            
                                            val currentTime = System.currentTimeMillis()
                                            
                                            // Check if SMS already processed
                                            val existingSms = paymentSmsDao.getSMSByBodyAndSender(messageBody, sender)
                                            if (existingSms != null) {
                                                Log.d("TransactionSMS", "⏭️ SMS already exists, skipping")
                                                return@launch
                                            }
                                            
                                            // Save SMS to database
                                            val paymentSms = PaymentSms(
                                                id = UUID.randomUUID().toString(),
                                                sender = sender,
                                                smsBody = messageBody,
                                                timestamp = currentTime,
                                                isProcessed = false
                                            )
                                            paymentSmsDao.insertSms(paymentSms)
                                            Log.d("TransactionSMS", "✅ SMS saved to database")
                                            
                                            // Process SMS immediately to create transaction
                                            val engine = TransactionCategorizationEngine()
                                            val details = engine.extractTransactionDetails(messageBody)
                                            
                                            if (details.amount > 0 && details.merchant.isNotBlank()) {
                                                // Check for duplicates before creating transaction
                                                val existingTransaction = transactionDao.getTransactionsBySmsBody(messageBody)
                                                if (existingTransaction != null) {
                                                    Log.d("TransactionSMS", "⏭️ Transaction already exists for this SMS, skipping")
                                                    paymentSmsDao.markAsProcessed(paymentSms.id)
                                                    return@launch
                                                }
                                                
                                                // Get categories
                                                val categoryDao = database.categoryDao()
                                                val categories = try {
                                                    categoryDao.getAllActiveCategoriesList()
                                                } catch (e: Exception) {
                                                    emptyList()
                                                }
                                                
                                                // Determine category
                                                val categoryId = determineCategoryId(details, categories, messageBody)
                                                
                                                // Create transaction
                                                val transaction = Transaction(
                                                    id = UUID.randomUUID().toString(),
                                                    amount = details.amount,
                                                    type = details.type,
                                                    merchant = details.merchant,
                                                    categoryId = categoryId,
                                                    confidence = 85.0f,
                                                    date = currentTime,
                                                    description = details.description,
                                                    smsBody = messageBody
                                                )
                                                
                                                transactionDao.insertTransaction(transaction)
                                                paymentSmsDao.markAsProcessed(paymentSms.id)
                                                
                                                Log.d("TransactionSMS", "🎉 NEW TRANSACTION CREATED: ₹${details.amount} at ${details.merchant}")
                                            } else {
                                                Log.d("TransactionSMS", "⚠️ Could not extract valid transaction data")
                                                paymentSmsDao.markAsProcessed(paymentSms.id)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("TransactionSMS", "❌ Error processing SMS", e)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TransactionSMS", "Error receiving SMS", e)
                }
            }
        }
    }

    private fun isTransactionSMS(messageBody: String, sender: String): Boolean {
        // Use centralized bank constants (80+ banks supported)
        val transactionKeywords = BankConstants.TRANSACTION_KEYWORDS
        val bankSenders = BankConstants.BANK_SENDERS
        
        val messageBodyLower = messageBody.lowercase()
        val senderUpper = sender.uppercase()
        
        // Check if sender is from known bank/payment service
        val isFromBank = bankSenders.any { senderUpper.contains(it) }
        
        // Check if message contains transaction keywords
        val hasTransactionKeywords = transactionKeywords.any { 
            messageBodyLower.contains(it) 
        }
        
        // Check if message contains amount pattern
        // Format 1: ₹500, Rs.500, INR 500
        // Format 2: debited by 2000.0, credited by 5000.0 (SBI UPI format)
        val hasAmountPattern = messageBody.matches(
            Regex(".*(?:(?:₹|rs\\.?|inr)\\s*[0-9,]+(?:\\.[0-9]{1,2})?|(?:debited|credited)\\s+by\\s+[0-9,]+(?:\\.[0-9]{1,2})?).*", RegexOption.IGNORE_CASE)
        )
        
        return (isFromBank || hasTransactionKeywords) && hasAmountPattern
    }
    
    private fun determineCategoryId(
        details: com.koshpal_android.koshpalapp.engine.TransactionDetails,
        categories: List<com.koshpal_android.koshpalapp.model.TransactionCategory>,
        smsBody: String
    ): String {
        val merchant = details.merchant.lowercase()
        val description = details.description.lowercase()
        val combinedText = "$merchant $description $smsBody".lowercase()
        
        // Try to match with existing categories using their keywords
        for (category in categories) {
            for (keyword in category.keywords) {
                if (combinedText.contains(keyword.lowercase())) {
                    Log.d("TransactionSMS", "🏷️ Matched category '${category.name}' using keyword '$keyword'")
                    return category.id
                }
            }
        }
        
        // Fallback to simple mapping
        return when {
            combinedText.contains("amazon") || combinedText.contains("flipkart") -> "shopping"
            combinedText.contains("zomato") || combinedText.contains("swiggy") -> "food"
            combinedText.contains("uber") || combinedText.contains("ola") -> "transport"
            combinedText.contains("salary") || details.type == TransactionType.CREDIT -> "salary"
            combinedText.contains("grocery") || combinedText.contains("dmart") -> "grocery"
            combinedText.contains("recharge") || combinedText.contains("mobile") -> "bills"
            else -> "others"
        }
    }
}
