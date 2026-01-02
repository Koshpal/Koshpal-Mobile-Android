package com.koshpal_android.koshpalapp.ml

import android.util.Log

/**
 * SMS Processing Metrics and Logging
 *
 * Tracks and logs SMS processing pipeline for visibility and optimization.
 * Provides detailed insights into where SMS are being skipped and why.
 */
object SmsProcessingMetrics {

    private const val TAG = "SmsMetrics"

    // Metrics counters
    private var totalSmsReceived = 0
    private var skippedByMl = 0
    private var skippedByMerchant = 0
    private var skippedByDuplicate = 0
    private var skippedByValidation = 0
    private var processedSuccessfully = 0

    // Skip reason tracking
    private val skipReasons = mutableMapOf<SmsSkipReason, Int>()

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
     */
    fun reset() {
        totalSmsReceived = 0
        skippedByMl = 0
        skippedByMerchant = 0
        skippedByDuplicate = 0
        skippedByValidation = 0
        processedSuccessfully = 0
        skipReasons.clear()
        Log.d(TAG, "📊 SMS Processing Metrics Reset")
    }

    /**
     * Record SMS received
     */
    fun recordSmsReceived() {
        totalSmsReceived++
    }

    /**
     * Record successful processing
     */
    fun recordSuccessfulProcessing() {
        processedSuccessfully++
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
