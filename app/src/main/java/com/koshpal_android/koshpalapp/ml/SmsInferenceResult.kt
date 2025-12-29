package com.koshpal_android.koshpalapp.ml

/**
 * SMS Inference Result
 * 
 * Contains prediction results from the classifier.
 */
data class SmsInferenceResult(
    /**
     * Predicted label (debit_transaction, credit_transaction, otp, promo, other)
     */
    val label: String,
    
    /**
     * Confidence score (0.0 to 1.0)
     */
    val confidence: Float,
    
    /**
     * Probability distribution for all 5 classes
     * [debit_transaction, credit_transaction, otp, promo, other]
     */
    val probabilities: List<Float>,
    
    /**
     * Cleaned SMS text (for debugging)
     */
    val cleanedText: String
) {
    /**
     * Get probability for a specific label.
     */
    fun getProbability(label: String): Float {
        val index = LabelMapper.getIndex(label)
        return if (index >= 0 && index < probabilities.size) {
            probabilities[index]
        } else {
            0f
        }
    }
    
    /**
     * Check if prediction meets confidence threshold.
     */
    fun meetsThreshold(threshold: Float): Boolean {
        return confidence >= threshold
    }
}

