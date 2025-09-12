package com.koshpal_android.koshpalapp.repository

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.koshpal_android.koshpalapp.model.PaymentSms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SmsRepository(private val context: Context) {

    // Updated to match latest banking SMS transaction patterns
    private val transactionKeywords = listOf(
        "debited", "credited", "debit", "credit"
    )
    
    private val amountPatterns = listOf(
        "rs.", "rs ", "inr", "₹", "rupees"
    )
    
    private val bankingTerms = listOf(
        "account", "a/c", "ac", "available balance", "avbl bal", "bal", "transaction", "txn"
    )

    suspend fun getPaymentSms(): List<PaymentSms> = withContext(Dispatchers.IO) {
        val smsList = mutableListOf<PaymentSms>()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        try {
            val cursor: Cursor? = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE
                ),
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT 100"
            )

            cursor?.use { c ->
                val idIndex = c.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressIndex = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = c.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (c.moveToNext()) {
                    val id = c.getString(idIndex)
                    val address = c.getString(addressIndex) ?: "Unknown"
                    val body = c.getString(bodyIndex) ?: ""
                    val timestamp = c.getLong(dateIndex)
                    val formattedDate = dateFormat.format(Date(timestamp))

                    // Filter messages containing payment-related keywords
                    if (isPaymentRelated(body)) {
                        smsList.add(
                            PaymentSms(
                                id = id,
                                address = address,
                                body = body,
                                timestamp = timestamp,
                                date = formattedDate
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        smsList
    }

    private fun isPaymentRelated(messageBody: String): Boolean {
        val lowerCaseBody = messageBody.lowercase()
        
        // Must contain at least one transaction keyword (debited/credited)
        val hasTransactionKeyword = transactionKeywords.any { keyword ->
            lowerCaseBody.contains(keyword)
        }
        
        // Must contain at least one amount pattern (Rs./₹)
        val hasAmountPattern = amountPatterns.any { pattern ->
            lowerCaseBody.contains(pattern)
        }
        
        // Must contain at least one banking term
        val hasBankingTerm = bankingTerms.any { term ->
            lowerCaseBody.contains(term)
        }
        
        // Additional patterns for modern banking SMS formats
        val hasModernPatterns = lowerCaseBody.contains("spent") || 
                               lowerCaseBody.contains("received") ||
                               lowerCaseBody.contains("transferred") ||
                               lowerCaseBody.contains("withdrawn") ||
                               lowerCaseBody.contains("deposited") ||
                               lowerCaseBody.contains("paid to") ||
                               lowerCaseBody.contains("payment of") ||
                               lowerCaseBody.contains("upi") ||
                               lowerCaseBody.contains("imps") ||
                               lowerCaseBody.contains("neft") ||
                               lowerCaseBody.contains("rtgs")
        
        // Message must have transaction keyword AND (amount pattern OR modern patterns) AND banking term
        return hasTransactionKeyword && (hasAmountPattern || hasModernPatterns) && hasBankingTerm
    }
}
