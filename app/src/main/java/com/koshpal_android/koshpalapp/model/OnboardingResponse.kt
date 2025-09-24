package com.koshpal_android.koshpalapp.model

import com.google.gson.annotations.SerializedName

data class OnboardingResponse(
    @SerializedName("email")
    val email: String,
    @SerializedName("ageGroup")
    val ageGroup: String,
    @SerializedName("monthlySalary")
    val monthlySalary: String,
    @SerializedName("financialSituation")
    val financialSituation: String,
    @SerializedName("expenseTracking")
    val expenseTracking: String,
    @SerializedName("moneyWorryLevel")
    val moneyWorryLevel: Int, // 1-5 rating
    @SerializedName("highestExpenseCategory")
    val highestExpenseCategory: String,
    @SerializedName("primaryFinancialGoal")
    val primaryFinancialGoal: String,
    @SerializedName("goalTimeframe")
    val goalTimeframe: String,
    @SerializedName("financialStressArea")
    val financialStressArea: String,
    @SerializedName("completedAt")
    val completedAt: String = System.currentTimeMillis().toString()
)

// Individual question response for tracking
data class QuestionResponse(
    val questionId: String,
    val answer: String,
    val sectionNumber: Int
)

// Onboarding sections enum
enum class OnboardingSection(val title: String, val description: String) {
    WELCOME("Welcome", "Welcome to Koshpal! We'll ask you 9 quick questions to personalize your financial wellness journey. Your data stays private and is never shared with your employer."),
    QUICK_PROFILE("Quick Profile", "Tell us a bit about yourself (30 seconds)"),
    FINANCIAL_HEALTH("Financial Health", "Let's understand your money habits (45 seconds)"),
    GOALS_PRIORITIES("Goals & Priorities", "What matters most to you? (45 seconds)")
}
