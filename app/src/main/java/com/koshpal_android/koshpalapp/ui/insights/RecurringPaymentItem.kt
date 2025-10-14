package com.koshpal_android.koshpalapp.ui.insights

data class RecurringPaymentItem(
    val merchantName: String,
    val monthlyAvgAmount: Double,
    val frequency: String, // "Monthly", "Weekly", etc.
    val last3MonthsFrequency: Int,
    val subscriptionScore: Float, // 0-100 confidence score
    val firstSeen: Long,
    val lastSeen: Long,
    val timelineData: List<Double>, // Last 6 months spending
    val isEssential: Boolean = false,
    val isReimbursable: Boolean = false
)
