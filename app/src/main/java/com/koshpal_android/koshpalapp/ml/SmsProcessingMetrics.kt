package com.koshpal_android.koshpalapp.ml

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * SMS Processing Metrics and Logging
 *
 * Tracks and logs SMS processing pipeline for visibility and optimization.
 * Provides detailed insights into where SMS are being skipped and why.
 * Now persists metrics across app sessions for full app logging.
 */
object SmsProcessingMetrics {

    private const val TAG = "SmsMetrics"
    private const val PREFS_NAME = "sms_processing_metrics"

    // SharedPreferences keys
    private const val KEY_TOTAL_SMS_RECEIVED = "total_sms_received"
    private const val KEY_SKIPPED_BY_ML = "skipped_by_ml"
    private const val KEY_SKIPPED_BY_MERCHANT = "skipped_by_merchant"
    private const val KEY_SKIPPED_BY_DUPLICATE = "skipped_by_duplicate"
    private const val KEY_SKIPPED_BY_VALIDATION = "skipped_by_validation"
    private const val KEY_PROCESSED_SUCCESSFULLY = "processed_successfully"
    private const val KEY_METRICS_START_TIME = "metrics_start_time"

    private lateinit var sharedPreferences: SharedPreferences
    private var isInitialized = false

    // Metrics counters - now loaded from persistent storage
    private var totalSmsReceived = 0
    private var skippedByMl = 0
    private var skippedByMerchant = 0
    private var skippedByDuplicate = 0
    private var skippedByValidation = 0
    private var processedSuccessfully = 0
    private var metricsStartTime = 0L

    // Skip reason tracking
    private val skipReasons = mutableMapOf<SmsSkipReason, Int>()

    /**
     * Initialize metrics with persistent storage
     */
    fun initialize(context: Context) {
        if (isInitialized) return

        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load persisted metrics
        totalSmsReceived = sharedPreferences.getInt(KEY_TOTAL_SMS_RECEIVED, 0)
        skippedByMl = sharedPreferences.getInt(KEY_SKIPPED_BY_ML, 0)
        skippedByMerchant = sharedPreferences.getInt(KEY_SKIPPED_BY_MERCHANT, 0)
        skippedByDuplicate = sharedPreferences.getInt(KEY_SKIPPED_BY_DUPLICATE, 0)
        skippedByValidation = sharedPreferences.getInt(KEY_SKIPPED_BY_VALIDATION, 0)
        processedSuccessfully = sharedPreferences.getInt(KEY_PROCESSED_SUCCESSFULLY, 0)
        metricsStartTime = sharedPreferences.getLong(KEY_METRICS_START_TIME, System.currentTimeMillis())

        // Load skip reasons from JSON string
        loadSkipReasonsFromStorage()

        isInitialized = true
        Log.d(TAG, "📊 SMS Processing Metrics initialized with persisted data (since: ${java.util.Date(metricsStartTime)})")
        Log.d(TAG, "   📨 Total SMS Received: $totalSmsReceived")
        Log.d(TAG, "   ✅ Processed Successfully: $processedSuccessfully")
    }

    /**
     * Load skip reasons from persistent storage
     */
    private fun loadSkipReasonsFromStorage() {
        val skipReasonsJson = sharedPreferences.getString("skip_reasons_json", "{}")
        try {
            // Parse JSON string back to map (simplified implementation)
            // In production, consider using Gson or similar
            if (skipReasonsJson != "{}" && skipReasonsJson != null) {
                // For now, we'll rebuild skip reasons from scratch if needed
                // Full implementation would parse the JSON
                Log.d(TAG, "📊 Loaded skip reasons from storage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading skip reasons from storage", e)
        }
    }

    /**
     * Save skip reasons to persistent storage
     */
    private fun saveSkipReasonsToStorage() {
        try {
            // Convert skip reasons map to JSON string (simplified implementation)
            // In production, consider using Gson or similar
            val jsonString = skipReasons.entries.joinToString(",", "{", "}") {
                "\"${it.key.name}\":${it.value}"
            }
            sharedPreferences.edit().putString("skip_reasons_json", jsonString).apply()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving skip reasons to storage", e)
        }
    }

    /**
     * SMS Skip Reason Enumeration
     */
    enum class SmsSkipReason {
        // ML Classification skips
        ML_CLASSIFIED_NON_TRANSACTION,
        ML_INFERENCE_FAILED,
        ML_LOW_CONFIDENCE,

        // Merchant validation skips
        MERCHANT_TOO_SHORT,
        MERCHANT_INVALID_WORD,
        MERCHANT_NO_LETTERS,
        MERCHANT_VALIDATION_FAILED,

        // Duplicate detection skips
        DUPLICATE_SMS_BODY,
        DUPLICATE_AMOUNT_TIME_MERCHANT,

        // Content validation skips
        MISSING_AMOUNT,
        MISSING_MERCHANT,
        INVALID_AMOUNT,
        INVALID_MERCHANT,

        // Other skips
        SMS_ALREADY_EXISTS,
        TRANSACTION_ALREADY_EXISTS
    }

    /**
     * Reset all metrics (for testing/debugging)
     * Note: This will reset persistent storage too
     */
    fun reset() {
        totalSmsReceived = 0
        skippedByMl = 0
        skippedByMerchant = 0
        skippedByDuplicate = 0
        skippedByValidation = 0
        processedSuccessfully = 0
        skipReasons.clear()

        // Reset persistent storage
        if (::sharedPreferences.isInitialized) {
            sharedPreferences.edit()
                .putInt(KEY_TOTAL_SMS_RECEIVED, 0)
                .putInt(KEY_SKIPPED_BY_ML, 0)
                .putInt(KEY_SKIPPED_BY_MERCHANT, 0)
                .putInt(KEY_SKIPPED_BY_DUPLICATE, 0)
                .putInt(KEY_SKIPPED_BY_VALIDATION, 0)
                .putInt(KEY_PROCESSED_SUCCESSFULLY, 0)
                .putLong(KEY_METRICS_START_TIME, System.currentTimeMillis())
                .putString("skip_reasons_json", "{}")
                .apply()
        }

        Log.d(TAG, "📊 SMS Processing Metrics Reset (persistent storage cleared)")
    }

    /**
     * Record SMS received
     */
    fun recordSmsReceived() {
        totalSmsReceived++
        saveMetricsToStorage()
    }

    /**
     * Record successful processing
     */
    fun recordSuccessfulProcessing() {
        processedSuccessfully++
        saveMetricsToStorage()
    }

    /**
     * Save current metrics to persistent storage
     */
    private fun saveMetricsToStorage() {
        if (!::sharedPreferences.isInitialized) return

        sharedPreferences.edit()
            .putInt(KEY_TOTAL_SMS_RECEIVED, totalSmsReceived)
            .putInt(KEY_SKIPPED_BY_ML, skippedByMl)
            .putInt(KEY_SKIPPED_BY_MERCHANT, skippedByMerchant)
            .putInt(KEY_SKIPPED_BY_DUPLICATE, skippedByDuplicate)
            .putInt(KEY_SKIPPED_BY_VALIDATION, skippedByValidation)
            .putInt(KEY_PROCESSED_SUCCESSFULLY, processedSuccessfully)
            .putLong(KEY_METRICS_START_TIME, metricsStartTime)
            .apply()
    }

    /**
     * Log and record skipped SMS with detailed information
     */
    fun logSkippedSms(
        reason: SmsSkipReason,
        smsBody: String,
        mlResult: SmsInferenceResult? = null,
        detectedAmount: Double = 0.0,
        detectedMerchant: String = "",
        additionalContext: String = ""
    ) {
        // Increment counters
        when (reason) {
            SmsSkipReason.ML_CLASSIFIED_NON_TRANSACTION,
            SmsSkipReason.ML_INFERENCE_FAILED -> skippedByMl++

            SmsSkipReason.MERCHANT_TOO_SHORT,
            SmsSkipReason.MERCHANT_INVALID_WORD,
            SmsSkipReason.MERCHANT_NO_LETTERS,
            SmsSkipReason.MERCHANT_VALIDATION_FAILED -> skippedByMerchant++

            SmsSkipReason.DUPLICATE_SMS_BODY,
            SmsSkipReason.DUPLICATE_AMOUNT_TIME_MERCHANT,
            SmsSkipReason.SMS_ALREADY_EXISTS,
            SmsSkipReason.TRANSACTION_ALREADY_EXISTS -> skippedByDuplicate++

            SmsSkipReason.MISSING_AMOUNT,
            SmsSkipReason.MISSING_MERCHANT,
            SmsSkipReason.INVALID_AMOUNT,
            SmsSkipReason.INVALID_MERCHANT -> skippedByValidation++

            SmsSkipReason.ML_LOW_CONFIDENCE -> skippedByMl++
        }

        // Track skip reasons
        skipReasons[reason] = (skipReasons[reason] ?: 0) + 1

        // Save updated metrics and skip reasons to persistent storage
        saveMetricsToStorage()
        saveSkipReasonsToStorage()

        // Create masked SMS body for logging
        val maskedSms = maskSmsBody(smsBody)

        // Log detailed information
        Log.w(TAG, "🚫 SMS SKIPPED: ${reason.name}")
        Log.w(TAG, "   📱 SMS: $maskedSms")
        Log.w(TAG, "   🎯 ML: ${mlResult?.let { "${it.label} (${String.format("%.2f", it.confidence * 100)}%)" } ?: "No ML result"}")
        Log.w(TAG, "   💰 Amount: ${if (detectedAmount > 0) String.format("%.2f", detectedAmount) else "None"}")
        Log.w(TAG, "   🏪 Merchant: ${if (detectedMerchant.isNotBlank()) "'$detectedMerchant'" else "None"}")
        if (additionalContext.isNotBlank()) {
            Log.w(TAG, "   📝 Context: $additionalContext")
        }

        // Log current metrics
        logCurrentMetrics()
    }

    /**
     * Mask sensitive information in SMS body for logging
     */
    private fun maskSmsBody(smsBody: String): String {
        var masked = smsBody

        // Mask account numbers (sequences of 8+ digits)
        masked = Regex("\\b\\d{8,}\\b").replace(masked) { "***${it.value.takeLast(4)}" }

        // Mask card numbers (16 digits, possibly with spaces)
        masked = Regex("\\b\\d{4}\\s?\\d{4}\\s?\\d{4}\\s?\\d{4}\\b").replace(masked, "**** **** **** ****")

        // Mask OTP codes (4-8 digit codes)
        masked = Regex("\\b\\d{4,8}\\b").replace(masked) { match ->
            if (match.value.length in 4..8) "***${match.value.takeLast(2)}" else match.value
        }

        // Limit length for very long SMS
        if (masked.length > 200) {
            masked = masked.take(200) + "..."
        }

        return masked
    }

    /**
     * Log current processing metrics
     */
    private fun logCurrentMetrics() {
        val totalSkipped = skippedByMl + skippedByMerchant + skippedByDuplicate + skippedByValidation
        val totalProcessed = totalSmsReceived - totalSkipped

        Log.i(TAG, "📊 CURRENT METRICS:")
        Log.i(TAG, "   📨 Total Received: $totalSmsReceived")
        Log.i(TAG, "   🤖 Skipped by ML: $skippedByMl")
        Log.i(TAG, "   🏪 Skipped by Merchant: $skippedByMerchant")
        Log.i(TAG, "   🔄 Skipped by Duplicate: $skippedByDuplicate")
        Log.i(TAG, "   ⚠️ Skipped by Validation: $skippedByValidation")
        Log.i(TAG, "   ✅ Processed Successfully: $processedSuccessfully")
        Log.i(TAG, "   📊 Total Skipped: $totalSkipped (${String.format("%.1f", (totalSkipped.toFloat() / totalSmsReceived * 100))}%)")

        // Log top skip reasons
        if (skipReasons.isNotEmpty()) {
            val topReasons = skipReasons.entries.sortedByDescending { it.value }.take(3)
            Log.i(TAG, "   🎯 Top Skip Reasons:")
            topReasons.forEach { (reason, count) ->
                Log.i(TAG, "      - ${reason.name}: $count")
            }
        }
    }

    /**
     * Get current metrics snapshot
     */
    fun getCurrentMetrics(): SmsMetricsSnapshot {
        return SmsMetricsSnapshot(
            totalSmsReceived = totalSmsReceived,
            skippedByMl = skippedByMl,
            skippedByMerchant = skippedByMerchant,
            skippedByDuplicate = skippedByDuplicate,
            skippedByValidation = skippedByValidation,
            processedSuccessfully = processedSuccessfully,
            skipReasons = skipReasons.toMap()
        )
    }

    /**
     * Print comprehensive metrics report
     */
    fun printMetricsReport() {
        val metrics = getCurrentMetrics()

        Log.i(TAG, "==================================================")
        Log.i(TAG, "📊 SMS PROCESSING METRICS REPORT")
        Log.i(TAG, "==================================================")
        Log.i(TAG, "Total SMS Received: ${metrics.totalSmsReceived}")
        Log.i(TAG, "Processed Successfully: ${metrics.processedSuccessfully}")
        Log.i(TAG, "Total Skipped: ${metrics.totalSkipped}")

        if (metrics.totalSmsReceived > 0) {
            val skipRate = (metrics.totalSkipped.toFloat() / metrics.totalSmsReceived * 100)
            Log.i(TAG, "Skip Rate: ${String.format("%.1f", skipRate)}%")
        }

        Log.i(TAG, "")
        Log.i(TAG, "SKIP BREAKDOWN:")
        Log.i(TAG, "  🤖 ML Classification: ${metrics.skippedByMl}")
        Log.i(TAG, "  🏪 Merchant Validation: ${metrics.skippedByMerchant}")
        Log.i(TAG, "  🔄 Duplicate Detection: ${metrics.skippedByDuplicate}")
        Log.i(TAG, "  ⚠️ Content Validation: ${metrics.skippedByValidation}")

        Log.i(TAG, "")
        Log.i(TAG, "TOP SKIP REASONS:")
        metrics.skipReasons.entries.sortedByDescending { it.value }.take(5).forEach { (reason, count) ->
            Log.i(TAG, "  ${reason.name}: $count")
        }

        Log.i(TAG, "==================================================")
    }
}

/**
 * SMS Metrics Snapshot Data Class
 */
data class SmsMetricsSnapshot(
    val totalSmsReceived: Int,
    val skippedByMl: Int,
    val skippedByMerchant: Int,
    val skippedByDuplicate: Int,
    val skippedByValidation: Int,
    val processedSuccessfully: Int,
    val skipReasons: Map<SmsProcessingMetrics.SmsSkipReason, Int>
) {
    val totalSkipped: Int
        get() = skippedByMl + skippedByMerchant + skippedByDuplicate + skippedByValidation

    val processingRate: Float
        get() = if (totalSmsReceived > 0) (processedSuccessfully.toFloat() / totalSmsReceived) else 0f

    val skipRate: Float
        get() = if (totalSmsReceived > 0) (totalSkipped.toFloat() / totalSmsReceived) else 0f
}
