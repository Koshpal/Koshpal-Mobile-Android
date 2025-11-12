package com.koshpal_android.koshpalapp.ui.insights.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koshpal_android.koshpalapp.ui.insights.TopMerchantProgress
import com.koshpal_android.koshpalapp.ui.theme.AppColors

/**
 * Top Merchants / Money Flow Section
 */
@Composable
fun TopMerchantsSection(
    data: TopMerchantsData,
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = AppColors.AccentBlue,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Top Merchants",
                    color = AppColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Where your money flows",
                color = AppColors.TextSecondary,
                fontSize = 14.sp
            )

            // Money Received From
            if (data.moneyReceivedFrom.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Money Received From",
                        color = AppColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    data.moneyReceivedFrom.forEach { merchant ->
                        MerchantProgressItem(
                            merchant = merchant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Money Spent On
            if (data.moneySpentOn.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Money Spent On",
                        color = AppColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    data.moneySpentOn.forEach { merchant ->
                        MerchantProgressItem(
                            merchant = merchant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Merchant Progress Item
 */
@Composable
fun MerchantProgressItem(
    merchant: TopMerchantProgress,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = merchant.merchantName,
                color = AppColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${merchant.sharePercentage.toInt()}%",
                color = AppColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(AppColors.DarkCard.copy(alpha = 0.5f))
        ) {
            // Progress fill with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth(merchant.percentageOfMax)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                AppColors.AccentBlue,
                                AppColors.AccentBlue.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
        }
    }
}

