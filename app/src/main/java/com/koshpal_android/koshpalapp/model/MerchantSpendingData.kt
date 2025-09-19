package com.koshpal_android.koshpalapp.model

data class MerchantSpendingData(
    val merchantName: String,
    val totalAmount: Double,
    val transactionCount: Int
) {
    fun getFormattedAmount(): String {
        return "₹${String.format("%.2f", totalAmount)}"
    }
    
    fun getTransactionText(): String {
        return if (transactionCount == 1) {
            "1 transaction"
        } else {
            "$transactionCount transactions"
        }
    }
}
