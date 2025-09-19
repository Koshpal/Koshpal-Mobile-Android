package com.koshpal_android.koshpalapp.model

data class CategorySpendingData(
    val categoryId: String,
    val categoryName: String,
    val amount: Double,
    val color: String,
    val icon: Int,
    val transactionCount: Int
) {
    fun getFormattedAmount(): String {
        return "₹${String.format("%.2f", amount)}"
    }
    
    fun getPercentage(totalAmount: Double): Float {
        return if (totalAmount > 0) (amount / totalAmount * 100).toFloat() else 0f
    }
    
    fun getFormattedPercentage(totalAmount: Double): String {
        return "${String.format("%.1f", getPercentage(totalAmount))}%"
    }
}
