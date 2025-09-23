package com.koshpal_android.koshpalapp.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import com.koshpal_android.koshpalapp.Application
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.model.PaymentSms
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                                
                                // Save SMS to database for processing
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        context?.let { ctx ->
                                            val database = KoshpalDatabase.getDatabase(ctx)
                                            val paymentSmsDao = database.paymentSmsDao()
                                            
                                            val currentTime = System.currentTimeMillis()
                                            val paymentSms = PaymentSms(
                                                id = java.util.UUID.randomUUID().toString(),
                                                sender = sender,
                                                smsBody = messageBody,
                                                timestamp = currentTime,
                                                isProcessed = false
                                            )
                                            
                                            // Save to database
                                            paymentSmsDao.insertSms(paymentSms)
                                            
                                            Log.d("TransactionSMS", "SMS saved successfully, will be processed by background service")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("TransactionSMS", "Error processing SMS", e)
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
        
        // Check if message contains amount pattern (₹ or Rs. followed by numbers)
        val hasAmountPattern = messageBody.matches(
            Regex(".*(?:₹|rs\\.?|inr)\\s*[0-9,]+(?:\\.[0-9]{2})?.*", RegexOption.IGNORE_CASE)
        )
        
        return (isFromBank || hasTransactionKeywords) && hasAmountPattern
    }
}
