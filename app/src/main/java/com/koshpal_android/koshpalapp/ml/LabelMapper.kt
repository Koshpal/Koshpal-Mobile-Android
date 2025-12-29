package com.koshpal_android.koshpalapp.ml

/**
 * Label Mapper
 * 
 * Maps model output indices to label strings.
 * Order must match training exactly:
 * [0] debit_transaction
 * [1] credit_transaction
 * [2] otp
 * [3] promo
 * [4] other
 */
object LabelMapper {
    
    private val LABELS = arrayOf(
        "debit_transaction",
        "credit_transaction",
        "otp",
        "promo",
        "other"
    )
    
    /**
     * Get label for given index.
     * 
     * @param index Model output index (0-4)
     * @return Label string
     * @throws IllegalArgumentException if index is invalid
     */
    fun getLabel(index: Int): String {
        require(index in 0..4) {
            "Invalid label index: $index (must be 0-4)"
        }
        return LABELS[index]
    }
    
    /**
     * Get all labels in order.
     */
    fun getAllLabels(): List<String> {
        return LABELS.toList()
    }
    
    /**
     * Get index for given label.
     * 
     * @param label Label string
     * @return Index (0-4) or -1 if not found
     */
    fun getIndex(label: String): Int {
        return LABELS.indexOf(label)
    }
}

