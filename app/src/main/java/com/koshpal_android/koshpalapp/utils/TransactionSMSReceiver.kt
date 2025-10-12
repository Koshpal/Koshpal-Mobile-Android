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
import com.koshpal_android.koshpalapp.utils.MerchantCategorizer
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
                                Log.d("TransactionSMS", "🔔 Detected transaction SMS from $sender")
                                Log.d("TransactionSMS", "📱 App State: ${if (isAppInForeground(context)) "FOREGROUND" else "BACKGROUND/CLOSED"}")
                                
                                // FIXED: Use goAsync() to ensure processing completes even when app is closed
                                val pendingResult = goAsync()
                                
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
                                                
                                                // Auto-categorize using MerchantCategorizer
                                                val categoryId = MerchantCategorizer.categorizeTransaction(
                                                    details.merchant, 
                                                    messageBody
                                                )
                                                
                                                Log.d("TransactionSMS", "🤖 Auto-categorized '${details.merchant}' → $categoryId (${MerchantCategorizer.getCategoryDisplayName(categoryId)})")
                                                
                                                // Extract bank name from SMS
                                                val bankName = extractBankNameFromSMS(messageBody, paymentSms.sender)
                                                
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
                                                    smsBody = messageBody,
                                                    bankName = bankName
                                                )
                                                
                                                transactionDao.insertTransaction(transaction)
                                                paymentSmsDao.markAsProcessed(paymentSms.id)
                                                
                                                Log.d("TransactionSMS", "🎉 NEW TRANSACTION CREATED: ₹${details.amount} at ${details.merchant}")
                                                Log.d("TransactionSMS", "💾 Transaction saved to database successfully")
                                                
                                                // Send notification for new transaction
                                                try {
                                                    val notificationManager = KoshpalNotificationManager.getInstance(ctx)
                                                    notificationManager.showTransactionNotification(transaction)
                                                    Log.d("TransactionSMS", "🔔 Notification sent for new transaction")
                                                } catch (e: Exception) {
                                                    Log.e("TransactionSMS", "❌ Failed to send notification", e)
                                                }
                                                
                                                // Check budget status after new transaction
                                                try {
                                                    val budgetMonitor = BudgetMonitor.getInstance(ctx)
                                                    budgetMonitor.checkBudgetStatus(transaction)
                                                    Log.d("TransactionSMS", "💰 Budget status checked for new transaction")
                                                } catch (e: Exception) {
                                                    Log.e("TransactionSMS", "❌ Failed to check budget status", e)
                                                }
                                            } else {
                                                Log.d("TransactionSMS", "⚠️ Could not extract valid transaction data")
                                                paymentSmsDao.markAsProcessed(paymentSms.id)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("TransactionSMS", "❌ Error processing SMS", e)
                                    } finally {
                                        // FIXED: Always finish the async operation to prevent ANR
                                        try {
                                            pendingResult.finish()
                                            Log.d("TransactionSMS", "✅ Background processing completed")
                                        } catch (e: Exception) {
                                            Log.e("TransactionSMS", "Error finishing pending result", e)
                                        }
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
    
    /**
     * Check if app is in foreground
     * Used for logging and debugging purposes
     */
    private fun isAppInForeground(context: Context?): Boolean {
        if (context == null) return false
        
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            activityManager?.runningAppProcesses?.any { processInfo ->
                processInfo.processName == context.packageName && 
                processInfo.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    private fun extractBankNameFromSMS(smsBody: String, sender: String): String {
        val text = smsBody.uppercase()
        val senderUpper = sender.uppercase()
        
        // First try to identify from sender
        return when {
            senderUpper.contains("SBI") || senderUpper.contains("STATE BANK") -> "SBI"
            senderUpper.contains("HDFC") -> "HDFC Bank"
            senderUpper.contains("ICICI") -> "ICICI Bank"
            senderUpper.contains("AXIS") -> "Axis Bank"
            senderUpper.contains("KOTAK") -> "Kotak Mahindra"
            senderUpper.contains("IPPB") || senderUpper.contains("INDIA POST") -> "IPPB"
            senderUpper.contains("PAYTM") -> "Paytm"
            senderUpper.contains("PHONEPE") -> "PhonePe"
            senderUpper.contains("GPAY") || senderUpper.contains("GOOGLE PAY") -> "Google Pay"
            senderUpper.contains("BOB") || senderUpper.contains("BANK OF BARODA") -> "Bank of Baroda"
            senderUpper.contains("PNB") || senderUpper.contains("PUNJAB NATIONAL") -> "PNB"
            senderUpper.contains("CANARA") -> "Canara Bank"
            senderUpper.contains("UNION BANK") -> "Union Bank"
            senderUpper.contains("IDBI") -> "IDBI Bank"
            senderUpper.contains("YES BANK") -> "Yes Bank"
            
            // Then try to identify from SMS body content
            text.contains("SBI") || text.contains("STATE BANK") -> "SBI"
            text.contains("HDFC") -> "HDFC Bank"
            text.contains("ICICI") -> "ICICI Bank"
            text.contains("AXIS") -> "Axis Bank"
            text.contains("KOTAK") -> "Kotak Mahindra"
            text.contains("IPPB") || text.contains("INDIA POST") -> "IPPB"
            text.contains("PAYTM") -> "Paytm"
            text.contains("PHONEPE") -> "PhonePe"
            text.contains("GPAY") || text.contains("GOOGLE PAY") -> "Google Pay"
            text.contains("BOB") || text.contains("BANK OF BARODA") -> "Bank of Baroda"
            text.contains("PNB") || text.contains("PUNJAB NATIONAL") -> "PNB"
            text.contains("CANARA") -> "Canara Bank"
            text.contains("UNION BANK") -> "Union Bank"
            text.contains("IDBI") -> "IDBI Bank"
            text.contains("YES BANK") -> "Yes Bank"
            
            else -> "Other Banks"
        }
    }
    
}
