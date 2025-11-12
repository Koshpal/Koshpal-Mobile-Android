package com.koshpal_android.koshpalapp.ui.insights.compose

import com.koshpal_android.koshpalapp.ui.insights.MonthComparisonData
import com.koshpal_android.koshpalapp.ui.insights.MonthComparisonInsight
import com.koshpal_android.koshpalapp.ui.insights.RecurringPaymentEnhanced
import com.koshpal_android.koshpalapp.ui.insights.RecurringPaymentsInsight
import com.koshpal_android.koshpalapp.ui.insights.TopMerchantProgress

/**
 * Main data class for InsightsScreen - all data passed as parameters for stateless design
 */
data class InsightsScreenData(
    val recurringPayments: List<RecurringPaymentEnhanced>,
    val recurringInsights: RecurringPaymentsInsight?,
    val spendingTrends: SpendingTrendsData,
    val topMerchants: TopMerchantsData,
    val isLoading: Boolean = false
)

/**
 * Data for Spending Trends section
 */
data class SpendingTrendsData(
    val comparisonData: List<MonthComparisonData>,
    val insights: MonthComparisonInsight?,
    val showPercentages: Boolean = false
)

/**
 * Data for Top Merchants / Money Flow section
 */
data class TopMerchantsData(
    val moneyReceivedFrom: List<TopMerchantProgress>,
    val moneySpentOn: List<TopMerchantProgress>
)

