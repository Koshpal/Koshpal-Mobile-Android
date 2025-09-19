package com.koshpal_android.koshpalapp.ui.budget.model

/**
 * Revolutionary budget UI state supporting all advanced features
 */
data class AdvancedBudgetUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    
    // Hero Section Data
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val monthProgress: Float = 0.0f, // 0.0 to 1.0
    val budgetHealthScore: Float = 1.0f, // 1.0 = perfect, 0.0 = critical
    
    // Tiered Budget System
    val tierBreakdown: Map<BudgetTier, TierData> = emptyMap(),
    val categories: List<BudgetCategory> = emptyList(),
    
    // Financial Universe Visualization
    val universeData: FinancialUniverseData = FinancialUniverseData(),
    
    // What-If Scenario Planner
    val activeScenario: WhatIfScenario? = null,
    val scenarioHistory: List<WhatIfScenario> = emptyList(),
    val isScenarioMode: Boolean = false,
    
    // Smart Insights
    val insights: List<SmartInsight> = emptyList(),
    val predictiveAlerts: List<PredictiveAlert> = emptyList(),
    
    // Subscription Management
    val detectedSubscriptions: List<DetectedSubscription> = emptyList(),
    val upcomingPayments: List<UpcomingPayment> = emptyList(),
    
    // Animation States
    val animationStates: AnimationStates = AnimationStates(),
    
    // UI Interaction States
    val selectedCategory: BudgetCategory? = null,
    val isExpenseEntryVisible: Boolean = false,
    val isDrillDownVisible: Boolean = false,
    val drillDownData: CategoryDrillDown? = null
)

data class TierData(
    val tier: BudgetTier,
    val allocatedAmount: Double,
    val spentAmount: Double,
    val categories: List<BudgetCategory>,
    val healthScore: Float, // 0.0 to 1.0
    val trend: TrendDirection
) {
    val remainingAmount: Double get() = allocatedAmount - spentAmount
    val spentPercentage: Float get() = if (allocatedAmount > 0) (spentAmount / allocatedAmount).toFloat() else 0f
}

enum class TrendDirection {
    IMPROVING, STABLE, DECLINING
}

/**
 * Financial Universe Visualization Data
 */
data class FinancialUniverseData(
    val centerPoint: Pair<Float, Float> = Pair(0.5f, 0.5f),
    val planets: List<BudgetPlanet> = emptyList(),
    val orbitAnimationSpeed: Float = 1.0f,
    val selectedPlanet: BudgetPlanet? = null
)

data class BudgetPlanet(
    val category: BudgetCategory,
    val size: Float, // Relative size based on budget allocation
    val orbitRadius: Float,
    val orbitSpeed: Float,
    val currentAngle: Float,
    val pulseIntensity: Float, // For urgency animations
    val isExpanded: Boolean = false
)

/**
 * Smart Insights System
 */
data class SmartInsight(
    val id: String,
    val type: InsightType,
    val title: String,
    val description: String,
    val actionable: Boolean,
    val actionText: String? = null,
    val priority: InsightPriority,
    val createdAt: Long = System.currentTimeMillis()
)

enum class InsightType {
    SPENDING_PATTERN, SAVINGS_OPPORTUNITY, BUDGET_OPTIMIZATION, GOAL_PROGRESS, SUBSCRIPTION_ALERT
}

enum class InsightPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Predictive Alert System
 */
data class PredictiveAlert(
    val id: String,
    val category: BudgetCategory,
    val alertType: AlertType,
    val message: String,
    val daysUntilLimit: Int,
    val suggestedAction: String,
    val confidence: Float // 0.0 to 1.0
)

enum class AlertType {
    BUDGET_EXCEEDED, APPROACHING_LIMIT, UNUSUAL_SPENDING, GOAL_AT_RISK
}

/**
 * Subscription Management
 */
data class DetectedSubscription(
    val id: String,
    val name: String,
    val amount: Double,
    val frequency: SubscriptionFrequency,
    val nextPaymentDate: Long,
    val category: String,
    val confidence: Float,
    val isActive: Boolean = true
)

enum class SubscriptionFrequency {
    WEEKLY, MONTHLY, QUARTERLY, YEARLY
}

data class UpcomingPayment(
    val subscription: DetectedSubscription,
    val dueDate: Long,
    val amount: Double,
    val isOverdue: Boolean = false
)

/**
 * Animation States
 */
data class AnimationStates(
    val isHeroAnimating: Boolean = false,
    val liquidProgressAnimation: Float = 0.0f,
    val planetOrbitAnimations: Map<String, Float> = emptyMap(),
    val categoryTileAnimations: Map<String, TileAnimationState> = emptyMap(),
    val celebrationAnimation: CelebrationAnimation? = null
)

data class TileAnimationState(
    val wobbleIntensity: Float = 0.0f,
    val pulseIntensity: Float = 0.0f,
    val scaleAnimation: Float = 1.0f
)

data class CelebrationAnimation(
    val type: CelebrationType,
    val duration: Long,
    val startTime: Long
)

enum class CelebrationType {
    GOAL_COMPLETED, BUDGET_SAVED, MILESTONE_REACHED
}

/**
 * Category Drill-Down Data
 */
data class CategoryDrillDown(
    val category: BudgetCategory,
    val monthlyHistory: List<MonthlyData>,
    val recentTransactions: List<TransactionSummary>,
    val trends: List<TrendPoint>,
    val insights: List<String>
)

data class MonthlyData(
    val month: String,
    val year: Int,
    val budgeted: Double,
    val spent: Double
)

data class TransactionSummary(
    val id: String,
    val description: String,
    val amount: Double,
    val date: Long,
    val merchant: String?
)

data class TrendPoint(
    val date: Long,
    val value: Double
)
