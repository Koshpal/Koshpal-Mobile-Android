package com.koshpal_android.koshpalapp.ui.insights.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.ui.insights.RecurringPaymentEnhanced
import com.koshpal_android.koshpalapp.ui.theme.AppColors
import com.koshpal_android.koshpalapp.ui.insights.compose.SpendingTrendsSection
import com.koshpal_android.koshpalapp.ui.insights.compose.TopMerchantsSection

/**
 * Main Insights Screen Composable - Stateless design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    data: InsightsScreenData,
    onBackClick: () -> Unit,
    onRecurringPaymentClick: (RecurringPaymentEnhanced) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.backgroundstrucure2),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Common App Bar
        com.koshpal_android.koshpalapp.ui.common.CommonAppBar(
            onProfileClick = onProfileClick,
            modifier = Modifier.fillMaxWidth()
        )

        // Screen Title
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Insights",
                color = AppColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Analyze your spending patterns",
                color = AppColors.TextSecondary,
                fontSize = 14.sp
            )
        }

        // Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Recurring Payments
            item {
                RecurringPaymentsSection(
                    payments = data.recurringPayments,
                    insights = data.recurringInsights,
                    onPaymentClick = onRecurringPaymentClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Section 2: Spending Trends
            item {
                SpendingTrendsSection(
                    data = data.spendingTrends,
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Section 3: Top Merchants / Money Flow
            item {
                TopMerchantsSection(
                    data = data.topMerchants,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Bottom padding for navigation bar
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
        }
    }
}

/**
 * Recurring Payments Section
 */
@Composable
fun RecurringPaymentsSection(
    payments: List<RecurringPaymentEnhanced>,
    insights: com.koshpal_android.koshpalapp.ui.insights.RecurringPaymentsInsight?,
    onPaymentClick: (RecurringPaymentEnhanced) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.DarkCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = AppColors.AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Recurring Payments",
                        color = AppColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (payments.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${payments.size} found",
                            color = Color(0xFF4CAF50),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Text(
                text = "Smart detection with trends.",
                color = AppColors.TextSecondary,
                fontSize = 14.sp
            )

            // Smart Insights Card
            insights?.let { insight ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AppColors.DarkCard.copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        AppColors.AccentBlue.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = AppColors.AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Smart Insights",
                                color = AppColors.AccentBlue,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = insight.insightText,
                            color = AppColors.TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        // Summary Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Subscriptions Count
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = AppColors.DarkCard
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${insight.totalRecurringCount}",
                                        color = AppColors.AccentBlue,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Subscriptions",
                                        color = AppColors.TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Monthly Total
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = AppColors.DarkCard
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "₹${insight.totalMonthlySpend.toInt()}",
                                        color = AppColors.AccentBlue,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Monthly Total",
                                        color = AppColors.TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recurring Payment Items
            if (payments.isEmpty()) {
                Text(
                    text = "No recurring payments detected yet.",
                    color = AppColors.TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                payments.forEach { payment ->
                    RecurringPaymentItem(
                        payment = payment,
                        onClick = { onPaymentClick(payment) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        }
    }
}

/**
 * Individual Recurring Payment Item (Expandable)
 */
@Composable
fun RecurringPaymentItem(
    payment: RecurringPaymentEnhanced,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300)
    )

    Card(
        modifier = modifier.clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.DarkCard.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main Content Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Avatar and Name
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AppColors.AccentBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = payment.merchantInitials,
                            color = AppColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Name and Details
                    Column {
                        Text(
                            text = payment.merchantName,
                            color = AppColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${payment.frequency} • ${payment.consecutiveMonths} months",
                            color = AppColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Right: Amount and Tag
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "₹${payment.currentMonthAmount.toInt()}",
                            color = AppColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AppColors.AccentBlue.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = payment.categoryTag,
                                color = AppColors.AccentBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = AppColors.TextSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotationAngle)
                    )
                }
            }

            // Expanded Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Divider(color = AppColors.TextSecondary.copy(alpha = 0.2f))

                    // Change Indicator
                    val changePercentage = kotlin.math.abs(payment.percentageChange).toInt()
                    val changeText = if (payment.hasDecreased) {
                        "↓ $changePercentage%"
                    } else if (payment.hasIncreased) {
                        "↑ $changePercentage%"
                    } else {
                        "→ 0%"
                    }
                    val changeColor = when {
                        payment.hasDecreased -> Color(0xFF4CAF50)
                        payment.hasIncreased -> Color(0xFFF44336)
                        else -> AppColors.TextSecondary
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Previous Month",
                                color = AppColors.TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "₹${payment.previousMonthAmount.toInt()}",
                                color = AppColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Current Month",
                                color = AppColors.TextSecondary,
                                fontSize = 12.sp
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "₹${payment.currentMonthAmount.toInt()}",
                                    color = AppColors.AccentBlue,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = changeColor.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = changeText,
                                        color = changeColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

