package com.koshpal_android.koshpalapp.sms.processor

import android.content.Context
import android.util.Log
import com.koshpal_android.koshpalapp.Application
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.model.PaymentSms
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.service.NewTransactionSyncService
import com.koshpal_android.koshpalapp.service.TransactionSyncServiceEntryPoint
import com.koshpal_android.koshpalapp.sms.rules.RuleEngine
import com.koshpal_android.koshpalapp.sms.rules.RuleMatchResult
import com.koshpal_android.koshpalapp.utils.BudgetMonitor
import com.koshpal_android.koshpalapp.utils.KoshpalNotificationManager
import com.koshpal_android.koshpalapp.utils.MerchantCategorizer
import dagger.hilt.android.EntryPointAccessors
import java.util.UUID

/**
 * Central SMS processing pipeline using rules.json.
 * Replaces ML-based classification.
 */
class SmsProcessingPipeline(private val context: Context) {

    private val database = KoshpalDatabase.getDatabase(context)
    private val paymentSmsDao = database.paymentSmsDao()
    private val transactionDao = database.transactionDao()

    companion object {
        private const val TAG = "SmsProcessingPipeline"
        private const val DUPLICATE_TIME_WINDOW_MS = 120_000L // 2 minutes
    }

    /**
     * Process a single SMS.
     *
     * @param isInitialScan true when processing historical SMS (no notifications)
     * @return PipelineResult indicating what happened.
     */
    suspend fun process(
        sender: String,
        body: String,
        timestamp: Long,
        isInitialScan: Boolean
    ): PipelineResult {
        // 1. Check if SMS already exists
        val existingSms = paymentSmsDao.getSMSByBodyAndSender(body, sender)
        if (existingSms != null) return PipelineResult.SkippedDuplicate

        // 2. Insert SMS into PaymentSms table
        val paymentSms = PaymentSms(
            id = UUID.randomUUID().toString(),
            sender = sender,
            smsBody = body,
            timestamp = timestamp,
            isProcessed = false
        )
        paymentSmsDao.insertSms(paymentSms)

        // 3. Run RuleEngine.match()
        val matchResult = RuleEngine.match(sender, body, timestamp)

        if (matchResult == null) {
            paymentSmsDao.markAsProcessed(paymentSms.id)
            return PipelineResult.NoMatch
        }

        if (!matchResult.shouldCreateTransaction) {
            paymentSmsDao.markAsProcessed(paymentSms.id)
            return PipelineResult.MatchedNoTransaction
        }

        if (isDuplicate(matchResult.amount, timestamp, body)) {
            paymentSmsDao.markAsProcessed(paymentSms.id)
            return PipelineResult.SkippedDuplicate
        }

        // 6. Determine transaction type (DEBIT/CREDIT)
        val transactionType = mapTransactionType(matchResult.transactionType)

        // 7. Determine category
        val categoryId = matchResult.transactionCategory?.let { ruleCategory ->
            mapRuleCategoryToAppCategory(ruleCategory)
        } ?: MerchantCategorizer.categorizeTransaction(
            matchResult.pos ?: "Unknown",
            body
        )

        // 8. Create and insert Transaction
        val merchant = matchResult.pos?.takeIf { it.isNotBlank() } ?: "Unknown Merchant"
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            amount = matchResult.amount,
            type = transactionType,
            merchant = merchant,
            categoryId = categoryId,
            confidence = 100f,
            date = matchResult.date,
            description = "Payment to $merchant",
            smsBody = body,
            bankName = matchResult.bankName,
            isManuallySet = false
        )

        transactionDao.insertTransaction(transaction)
        paymentSmsDao.markAsProcessed(paymentSms.id)

        // 9. Trigger sync (if not initial scan), and optionally notification/budget checks
        triggerPostTransactionHooks(transaction, isInitialScan)

        return PipelineResult.TransactionCreated
    }

    /**
     * Fast batch processing for initial scan. No per-SMS DB writes, no sync, no notifications.
     * Returns (transactions to insert, PaymentSms to insert for all processed).
     */
    fun processBatchForInitialScan(
        smsList: List<PaymentSms>,
        existingSmsBodies: Set<String>,
        existingProcessed: Set<Pair<String, String>>
    ): Pair<List<Transaction>, List<PaymentSms>> {
        val processedSet = existingProcessed.toMutableSet()
        val txSmsBodies = existingSmsBodies.toMutableSet()
        val transactionsToInsert = mutableListOf<Transaction>()
        val paymentSmsToInsert = mutableListOf<PaymentSms>()

        for (sms in smsList) {
            val key = Pair(sms.smsBody, sms.sender)
            if (key in processedSet) continue
            processedSet.add(key)

            val matchResult = RuleEngine.match(sms.sender, sms.smsBody, sms.timestamp)
            if (matchResult == null) {
                paymentSmsToInsert.add(sms.copy(isProcessed = true))
                continue
            }
            if (!matchResult.shouldCreateTransaction) {
                paymentSmsToInsert.add(sms.copy(isProcessed = true))
                continue
            }
            if (sms.smsBody in txSmsBodies) {
                paymentSmsToInsert.add(sms.copy(isProcessed = true))
                continue
            }
            txSmsBodies.add(sms.smsBody)

            val transaction = createTransactionFromMatch(matchResult, sms.smsBody)
            transactionsToInsert.add(transaction)
            paymentSmsToInsert.add(sms.copy(isProcessed = true))
        }
        return Pair(transactionsToInsert, paymentSmsToInsert)
    }

    private fun createTransactionFromMatch(matchResult: RuleMatchResult, body: String): Transaction {
        val transactionType = mapTransactionType(matchResult.transactionType)
        val categoryId = matchResult.transactionCategory?.let { mapRuleCategoryToAppCategory(it) }
            ?: MerchantCategorizer.categorizeTransaction(matchResult.pos ?: "Unknown", body)
        val merchant = matchResult.pos?.takeIf { it.isNotBlank() } ?: "Unknown Merchant"
        return Transaction(
            id = UUID.randomUUID().toString(),
            amount = matchResult.amount,
            type = transactionType,
            merchant = merchant,
            categoryId = categoryId,
            confidence = 100f,
            date = matchResult.date,
            description = "Payment to $merchant",
            smsBody = body,
            bankName = matchResult.bankName,
            isManuallySet = false
        )
    }

    private suspend fun isDuplicate(amount: Double, timestamp: Long, smsBody: String): Boolean {
        val existingBySmsBody = transactionDao.getTransactionBySmsBody(smsBody)
        if (existingBySmsBody != null) return true

        val timeWindow = DUPLICATE_TIME_WINDOW_MS
        val existingByAmountTime = transactionDao.getTransactionByAmountAndTime(
            amount,
            timestamp - timeWindow,
            timestamp + timeWindow
        ) ?: return false

        val existingHash = existingByAmountTime.smsBody?.trim()?.lowercase()?.hashCode().toString()
        val currentHash = smsBody.trim().lowercase().hashCode().toString()
        return existingHash == currentHash
    }

    private fun mapTransactionType(ruleType: String?): TransactionType {
        return when (ruleType?.lowercase()) {
            "credit" -> TransactionType.CREDIT
            "debit_card", "debit_atm", "upi", "net_banking", "cheque" -> TransactionType.DEBIT
            "credit_card" -> TransactionType.DEBIT // credit card spend is debit
            "balance" -> TransactionType.DEBIT // shouldn't reach here for transaction creation
            else -> TransactionType.DEBIT
        }
    }

    private fun mapRuleCategoryToAppCategory(ruleCategory: String): String {
        return when (ruleCategory.lowercase()) {
            "walnut_refund", "walnut_bill_payment" -> "others"
            else -> MerchantCategorizer.categorizeTransaction(ruleCategory, null)
        }
    }

    private fun triggerPostTransactionHooks(
        transaction: Transaction,
        isInitialScan: Boolean
    ) {
        // Skip sync during initial scan - saves network calls, user can sync later
        if (!isInitialScan) {
            try {
                getSyncService()?.autoSyncNewTransaction(transaction)
            } catch (e: Exception) {
                Log.e(TAG, "Auto-sync failed: ${e.message}")
            }
        }

        // Only show notifications and budget alerts for NEW incoming SMS,
        // not for historical initial scan.
        if (!isInitialScan) {
            try {
                KoshpalNotificationManager.getInstance(context).showTransactionNotification(transaction)
            } catch (e: Exception) {
                Log.e(TAG, "Notification failed", e)
            }
            try {
                BudgetMonitor.getInstance(context).checkBudgetStatus(transaction)
            } catch (e: Exception) {
                Log.e(TAG, "Budget check failed", e)
            }
        }
    }

    private fun getSyncService(): NewTransactionSyncService? {
        return try {
            val app = context.applicationContext as? Application
            app?.let {
                EntryPointAccessors.fromApplication(it, TransactionSyncServiceEntryPoint::class.java)
                    .newTransactionSyncService()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get sync service: ${e.message}")
            null
        }
    }
}

sealed class PipelineResult {
    object TransactionCreated : PipelineResult()
    object SkippedDuplicate : PipelineResult()
    object NoMatch : PipelineResult()
    object MatchedNoTransaction : PipelineResult()
}
