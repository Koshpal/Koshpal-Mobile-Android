package com.koshpal_android.koshpalapp.ui.budget.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.koshpal_android.koshpalapp.R

/**
 * Revolutionary tiered budgeting system
 * Essentials (50%) - Wants (30%) - Goals (20%)
 */
enum class BudgetTier(
    val displayName: String,
    val description: String,
    val recommendedPercentage: Float,
    @ColorRes val primaryColor: Int,
    @ColorRes val secondaryColor: Int,
    @DrawableRes val iconRes: Int,
    val priority: Int
) {
    ESSENTIALS(
        displayName = "Essentials",
        description = "Non-negotiable costs that keep your life running",
        recommendedPercentage = 0.50f,
        primaryColor = R.color.tier_essentials_primary,
        secondaryColor = R.color.tier_essentials_secondary,
        iconRes = R.drawable.ic_essentials_shield,
        priority = 1
    ),
    
    WANTS(
        displayName = "Wants", 
        description = "Flexible spending for enjoyment and lifestyle",
        recommendedPercentage = 0.30f,
        primaryColor = R.color.tier_wants_primary,
        secondaryColor = R.color.tier_wants_secondary,
        iconRes = R.drawable.ic_wants_heart,
        priority = 2
    ),
    
    GOALS(
        displayName = "Goals",
        description = "Building your future through savings and investments", 
        recommendedPercentage = 0.20f,
        primaryColor = R.color.tier_goals_primary,
        secondaryColor = R.color.tier_goals_secondary,
        iconRes = R.drawable.ic_goals_rocket,
        priority = 3
    );
    
    companion object {
        fun getByPriority() = values().sortedBy { it.priority }
        fun getTotalPercentage() = values().sumOf { it.recommendedPercentage.toDouble() }.toFloat()
    }
}

/**
 * Enhanced budget category with tier integration
 */
data class BudgetCategory(
    val id: String,
    val name: String,
    val tier: BudgetTier,
    val allocatedAmount: Double,
    val spentAmount: Double,
    val isRecurring: Boolean = false,
    val isAutoDetected: Boolean = false,
    @DrawableRes val customIconRes: Int? = null,
    val color: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val remainingAmount: Double get() = allocatedAmount - spentAmount
    val spentPercentage: Float get() = if (allocatedAmount > 0) (spentAmount / allocatedAmount).toFloat() else 0f
    val isOverBudget: Boolean get() = spentAmount > allocatedAmount
    val urgencyLevel: UrgencyLevel get() = when {
        spentPercentage >= 1.0f -> UrgencyLevel.CRITICAL
        spentPercentage >= 0.8f -> UrgencyLevel.HIGH
        spentPercentage >= 0.6f -> UrgencyLevel.MEDIUM
        else -> UrgencyLevel.LOW
    }
}

enum class UrgencyLevel(val color: Int, val animationIntensity: Float) {
    LOW(R.color.urgency_low, 0.0f),
    MEDIUM(R.color.urgency_medium, 0.3f),
    HIGH(R.color.urgency_high, 0.6f),
    CRITICAL(R.color.urgency_critical, 1.0f)
}

/**
 * What-If scenario data model
 */
data class WhatIfScenario(
    val id: String,
    val name: String,
    val modifications: Map<String, Double>, // categoryId to new amount
    val projectedOutcome: ScenarioOutcome,
    val createdAt: Long = System.currentTimeMillis()
)

data class ScenarioOutcome(
    val newTierBreakdown: Map<BudgetTier, Double>,
    val projectedSavings: Double,
    val riskLevel: RiskLevel,
    val timeToGoal: Int?, // days
    val insights: List<String>
)

enum class RiskLevel {
    CONSERVATIVE, BALANCED, AGGRESSIVE
}
