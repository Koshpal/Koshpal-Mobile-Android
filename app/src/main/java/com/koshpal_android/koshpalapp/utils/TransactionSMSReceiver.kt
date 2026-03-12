package com.koshpal_android.koshpalapp.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import com.koshpal_android.koshpalapp.sms.processor.SmsProcessingPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                            Log.d("TransactionSMS", "📨 Received SMS from $sender - processing via rule engine")

                            val pendingResult = goAsync()
                            val smsTimestamp = smsMessage?.timestampMillis ?: System.currentTimeMillis()

                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    context?.let { ctx ->
                                        val pipeline = SmsProcessingPipeline(ctx)
                                        val result = pipeline.process(
                                            sender = sender,
                                            body = messageBody,
                                            timestamp = smsTimestamp,
                                            isInitialScan = false
                                        )

                                        Log.d("TransactionSMS", "📊 Pipeline result: $result")
                                    }
                                } catch (e: Exception) {
                                    Log.e("TransactionSMS", "❌ Error processing SMS", e)
                                } finally {
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
}
