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
import com.koshpal_android.koshpalapp.service.NewTransactionSyncService
import com.koshpal_android.koshpalapp.service.TransactionSyncServiceEntryPoint
import com.koshpal_android.koshpalapp.utils.MerchantCategorizer
import com.koshpal_android.koshpalapp.ml.SmsClassifier
import com.koshpal_android.koshpalapp.ml.SmsProcessingMetrics
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class TransactionSMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("TransactionSMS", "📨 SMS Broadcast received - Action: ${intent?.action}")
        
        if (intent?.action == "android.provider.Telephony.SMS_RECEIVED") {
            Log.d("TransactionSMS", "✅ SMS_RECEIVED action confirmed")
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
                            // Send ALL SMS to ML model (no rule-based filtering)
                            Log.d("TransactionSMS", "📨 Received SMS from $sender - sending to ML model")
                            Log.d("TransactionSMS", "📱 App State: ${if (isAppInForeground(context)) "FOREGROUND" else "BACKGROUND/CLOSED"}")
                            
                            // FIXED: Use goAsync() to ensure processing completes even when app is closed
                            val pendingResult = goAsync()
                            
                            // Process SMS immediately in background
                            CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        // Record SMS received for metrics
                                        SmsProcessingMetrics.recordSmsReceived()

                                        context?.let { ctx ->
                                            val database = KoshpalDatabase.getDatabase(ctx)
                                            val paymentSmsDao = database.paymentSmsDao()
                                            val transactionDao = database.transactionDao()
                                            
                                            val currentTime = System.currentTimeMillis()
                                            
                                            // Check if SMS already processed
                                            val existingSms = paymentSmsDao.getSMSByBodyAndSender(messageBody, sender)
                                            if (existingSms != null) {
                                                Log.d("TransactionSMS", "⏭️ SMS already exists, skipping")
                                                SmsProcessingMetrics.logSkippedSms(
                                                    reason = SmsProcessingMetrics.SmsSkipReason.SMS_ALREADY_EXISTS,
                                                    smsBody = messageBody,
                                                    additionalContext = "Existing SMS ID: ${existingSms.id}"
                                                )
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
                                            
                                            // ============================================
                                            // INTEGRATED ML MODULE: SMS Classifier Inference
                                            // Uses TensorFlow Lite INT8 model to classify SMS
                                            // Determines if SMS is a transaction (debit/credit) or other type
                                            // Falls back to regex-based detection if ML fails
                                            // ============================================
                                            val classifier = SmsClassifier(ctx)
                                            val mlResult = try {
                                                classifier.classify(messageBody)
                                            } catch (e: Exception) {
                                                Log.e("TransactionSMS", "⚠️ ML inference failed, using fallback: ${e.message}", e)
                                                null
                                            }
                                            
                                            // CONFIDENCE-BASED ML DECISION - REPLACES BINARY GATING

                                            // Declare variables at higher scope for transaction creation
                                            var mlTransactionType: TransactionType? = null
                                            var transactionConfidence = 0.0f

                                            if (mlResult != null) {
                                                val (shouldCreate, confidence, transactionType) = classifier.shouldCreateTransaction(mlResult)
                                                transactionConfidence = confidence

                                                Log.d("TransactionSMS", "🤖 ML Confidence Decision: label=${mlResult.label}, max_confidence=${mlResult.confidence}, transaction_confidence=$transactionConfidence, should_create=$shouldCreate, type=$transactionType")

                                                if (!shouldCreate) {
                                                    Log.d("TransactionSMS", "⏭️ ML confidence too low ($transactionConfidence < 0.20) - skipping")
                                                    SmsProcessingMetrics.logSkippedSms(
                                                        reason = SmsProcessingMetrics.SmsSkipReason.ML_LOW_CONFIDENCE,
                                                        smsBody = messageBody,
                                                        mlResult = mlResult,
                                                        additionalContext = "Transaction confidence: $transactionConfidence (threshold: 0.20), label: ${mlResult.label}"
                                                    )
                                                    paymentSmsDao.markAsProcessed(paymentSms.id)
                                                    return@launch
                                                }

                                                // LOG BORDERLINE CONFIDENCE FOR RETRAINING DATA
                                                if (SmsClassifier(ctx).isBorderlineConfidence(mlResult)) {
                                                    Log.i("TransactionSMS", "📊 BORDERLINE CONFIDENCE: SMS='${messageBody.take(100)}...', confidence=$transactionConfidence, debit=${mlResult.probabilities.getOrElse(0){0.0f}}, credit=${mlResult.probabilities.getOrElse(1){0.0f}}, decision=CREATE_${transactionType}")
                                                }

                                                // ML approved - proceed with transaction creation
                                                Log.d("TransactionSMS", "✅ ML approved transaction creation (confidence: $transactionConfidence, type: $transactionType)")
                                                SmsProcessingMetrics.recordSuccessfulProcessing()

                                                // Store the ML-determined transaction type
                                                mlTransactionType = transactionType

                                            } else {
                                                // ML inference failed - cannot determine transaction confidence
                                                Log.d("TransactionSMS", "⚠️ ML inference failed - skipping SMS (no confidence available)")
                                                SmsProcessingMetrics.logSkippedSms(
                                                    reason = SmsProcessingMetrics.SmsSkipReason.ML_INFERENCE_FAILED,
                                                    smsBody = messageBody,
                                                    additionalContext = "ML inference completely failed - no confidence available"
                                                )
                                                paymentSmsDao.markAsProcessed(paymentSms.id)
                                                return@launch
                                            }

                                            // At this point, ML has approved transaction creation
                                            if (mlTransactionType == null) {
                                                Log.e("TransactionSMS", "❌ Critical error: ML approved transaction but type is null")
                                                return@launch
                                            }
                                            
                                            // Process SMS immediately to create transaction
                                            val engine = TransactionCategorizationEngine()
                                            val details = engine.extractTransactionDetails(messageBody)
                                            
                                            // STRICT VALIDATION: Require both amount and merchant (after ML approval)
                                            if (details.amount > 0 && details.merchant.isNotBlank()) {
                                                Log.d("TransactionSMS", "✅ Processing SMS - Amount: ${details.amount}, Merchant: '${details.merchant}'")
                                                // ML-SAFE DUPLICATE DETECTION
                                                // Check for transactions with similar content and timing
                                                val normalizedSmsHash = messageBody.trim().lowercase().hashCode().toString()
                                                val timeWindow = 120000L // 2 minutes window

                                                // Use amount as rough filter + content hash check
                                                val existingByContentAndTime = transactionDao.getTransactionByAmountAndTime(
                                                    details.amount,
                                                    currentTime - timeWindow,
                                                    currentTime + timeWindow
                                                )

                                                if (existingByContentAndTime != null) {
                                                    val existingSmsHash = existingByContentAndTime.smsBody?.trim()?.lowercase()?.hashCode().toString()
                                                    if (existingSmsHash == normalizedSmsHash) {
                                                        Log.d("TransactionSMS", "⏭️ Duplicate transaction detected (similar content + timing), skipping")
                                                        SmsProcessingMetrics.logSkippedSms(
                                                            reason = SmsProcessingMetrics.SmsSkipReason.TRANSACTION_ALREADY_EXISTS,
                                                            smsBody = messageBody,
                                                            mlResult = mlResult,
                                                            additionalContext = "Existing transaction ID: ${existingByContentAndTime.id}, SMS hash match within ${timeWindow/1000}s window"
                                                        )
                                                        paymentSmsDao.markAsProcessed(paymentSms.id)
                                                        return@launch
                                                    }
                                                }
                                                
                                                // Auto-categorize using MerchantCategorizer
                                                val categoryId = MerchantCategorizer.categorizeTransaction(
                                                    details.merchant, 
                                                    messageBody
                                                )
                                                
                                                Log.d("TransactionSMS", "🤖 Auto-categorized '${details.merchant}' → $categoryId (${MerchantCategorizer.getCategoryDisplayName(categoryId)})")
                                                
                                                // Extract bank name from SMS
                                                val bankName = extractBankNameFromSMS(messageBody, paymentSms.sender)
                                                
                                                // Use ML-determined transaction type and confidence
                                                val finalType = mlTransactionType
                                                val finalConfidence = transactionConfidence * 100f // Convert 0.0-1.0 to 0-100
                                                
                                                // Create transaction
                                                val transaction = Transaction(
                                                    id = UUID.randomUUID().toString(),
                                                    amount = details.amount,
                                                    type = finalType,
                                                    merchant = details.merchant,
                                                    categoryId = categoryId,
                                                    confidence = finalConfidence,
                                                    date = currentTime,
                                                    description = details.description,
                                                    smsBody = messageBody,
                                                    bankName = bankName
                                                )
                                                
                                                transactionDao.insertTransaction(transaction)
                                                paymentSmsDao.markAsProcessed(paymentSms.id)
                                                
                                                Log.d("TransactionSMS", "🎉 NEW TRANSACTION CREATED: ₹${details.amount} at ${details.merchant}")
                                                Log.d("TransactionSMS", "💾 Transaction saved to database successfully")

                                                // Record successful processing
                                                SmsProcessingMetrics.recordSuccessfulProcessing()

                                                // Auto-sync to MongoDB
                                                try {
                                                    val syncService = getSyncService(context)
                                                    if (syncService != null) {
                                                        syncService.autoSyncNewTransaction(transaction)
                                                        Log.d("TransactionSMS", "🔄 Auto-sync triggered for new SMS transaction")
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("TransactionSMS", "❌ Auto-sync failed: ${e.message}")
                                                }
                                                
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
                                                // MISSING AMOUNT OR MERCHANT - SKIP (after ML approval)
                                                val skipReason = when {
                                                    details.amount <= 0 && details.merchant.isBlank() ->
                                                        SmsProcessingMetrics.SmsSkipReason.MISSING_AMOUNT
                                                    details.amount <= 0 ->
                                                        SmsProcessingMetrics.SmsSkipReason.MISSING_AMOUNT
                                                    details.merchant.isBlank() ->
                                                        SmsProcessingMetrics.SmsSkipReason.MISSING_MERCHANT
                                                    else ->
                                                        SmsProcessingMetrics.SmsSkipReason.INVALID_AMOUNT
                                                }

                                                Log.d("TransactionSMS", "⚠️ Missing amount (${details.amount}) or merchant ('${details.merchant}') - skipping")
                                                SmsProcessingMetrics.logSkippedSms(
                                                    reason = skipReason,
                                                    smsBody = messageBody,
                                                    mlResult = mlResult,
                                                    detectedAmount = details.amount,
                                                    detectedMerchant = details.merchant,
                                                    additionalContext = "ML approved but extraction failed: amount=${details.amount}, merchant='${details.merchant}'"
                                                )
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
    
    private fun getSyncService(context: Context?): NewTransactionSyncService? {
        return try {
            val application = context?.applicationContext as? Application
            application?.let { app ->
                val entryPoint = EntryPointAccessors.fromApplication(app, TransactionSyncServiceEntryPoint::class.java)
                entryPoint.newTransactionSyncService()
            }
        } catch (e: Exception) {
            Log.e("TransactionSMS", "Failed to get sync service: ${e.message}")
            null
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
