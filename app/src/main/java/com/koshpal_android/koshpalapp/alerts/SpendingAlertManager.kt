package com.koshpal_android.koshpalapp.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.repository.BudgetRepository
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import com.koshpal_android.koshpalapp.model.Budget
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpendingAlertManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    
    companion object {
        private const val CHANNEL_ID = "spending_alerts"
        private const val CHANNEL_NAME = "Spending Alerts"
        private const val BUDGET_CHECK_WORK = "budget_check_work"
        private const val DAILY_SUMMARY_WORK = "daily_summary_work"
        private const val WEEKLY_SUMMARY_WORK = "weekly_summary_work"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for budget alerts and spending summaries"
                enableVibration(true)
                setShowBadge(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    suspend fun checkBudgetThresholds(transaction: Transaction) {
        withContext(Dispatchers.IO) {
            try {
                val calendar = Calendar.getInstance()
                val month = calendar.get(Calendar.MONTH) + 1
                val year = calendar.get(Calendar.YEAR)
                
                val budget = budgetRepository.getBudgetByCategoryAndMonth(
                    transaction.categoryId, month, year
                )
                
                budget?.let { b ->
                    val progressPercentage = b.progressPercentage
                    
                    when {
                        progressPercentage >= 100f && b.status.name != "EXCEEDED" -> {
                            sendBudgetAlert(b, AlertType.BUDGET_EXCEEDED)
                        }
                        progressPercentage >= 80f && progressPercentage < 100f -> {
                            sendBudgetAlert(b, AlertType.BUDGET_80_PERCENT)
                        }
                        progressPercentage >= 50f && progressPercentage < 80f -> {
                            sendBudgetAlert(b, AlertType.BUDGET_50_PERCENT)
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    private suspend fun sendBudgetAlert(budget: Budget, alertType: AlertType) {
        val categoryName = getCategoryName(budget.categoryId)
        
        val (title, message) = when (alertType) {
            AlertType.BUDGET_50_PERCENT -> {
                "Budget Alert: 50% Used" to 
                "You've used 50% of your $categoryName budget (${budget.getFormattedSpent()} of ${budget.getFormattedLimit()})"
            }
            AlertType.BUDGET_80_PERCENT -> {
                "Budget Warning: 80% Used" to 
                "You've used 80% of your $categoryName budget. Only ${budget.getFormattedRemaining()} remaining!"
            }
            AlertType.BUDGET_EXCEEDED -> {
                "Budget Exceeded!" to 
                "You've exceeded your $categoryName budget by ₹${String.format("%.2f", budget.spent - budget.monthlyLimit)}"
            }
            else -> return
        }
        
        showNotification(title, message, alertType)
    }
    
    fun scheduleDailySummary() {
        val dailySummaryRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(calculateTimeUntilNextRun(20, 0), TimeUnit.MILLISECONDS) // 8 PM daily
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_SUMMARY_WORK,
            ExistingPeriodicWorkPolicy.REPLACE,
            dailySummaryRequest
        )
    }
    
    fun scheduleWeeklySummary() {
        val weeklySummaryRequest = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(calculateTimeUntilNextSunday(19, 0), TimeUnit.MILLISECONDS) // 7 PM every Sunday
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WEEKLY_SUMMARY_WORK,
            ExistingPeriodicWorkPolicy.REPLACE,
            weeklySummaryRequest
        )
    }
    
    suspend fun sendDailySummary() {
        withContext(Dispatchers.IO) {
            try {
                val today = Calendar.getInstance()
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                
                val todayTransactions = transactionRepository.getTransactionsByDateRange(
                    startOfDay.timeInMillis, endOfDay.timeInMillis
                )
                
                todayTransactions.collect { transactions ->
                    val totalSpent = transactions.filter { it.isExpense() }.sumOf { it.amount }
                    val totalEarned = transactions.filter { it.isIncome() }.sumOf { it.amount }
                    val transactionCount = transactions.size
                    
                    if (transactionCount > 0) {
                        val title = "Daily Summary"
                        val message = "Today: ${transactionCount} transactions, " +
                                "Spent ₹${String.format("%.0f", totalSpent)}" +
                                if (totalEarned > 0) ", Earned ₹${String.format("%.0f", totalEarned)}" else ""
                        
                        showNotification(title, message, AlertType.DAILY_SUMMARY)
                    }
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    suspend fun sendWeeklySummary() {
        withContext(Dispatchers.IO) {
            try {
                val endOfWeek = Calendar.getInstance()
                val startOfWeek = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_WEEK, -7)
                }
                
                val weekTransactions = transactionRepository.getTransactionsByDateRange(
                    startOfWeek.timeInMillis, endOfWeek.timeInMillis
                )
                
                weekTransactions.collect { transactions ->
                    val totalSpent = transactions.filter { it.isExpense() }.sumOf { it.amount }
                    val totalEarned = transactions.filter { it.isIncome() }.sumOf { it.amount }
                    val transactionCount = transactions.size
                    
                    val title = "Weekly Summary"
                    val message = "This week: ${transactionCount} transactions, " +
                            "Spent ₹${String.format("%.0f", totalSpent)}, " +
                            "Earned ₹${String.format("%.0f", totalEarned)}"
                    
                    showNotification(title, message, AlertType.WEEKLY_SUMMARY)
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    private fun showNotification(title: String, message: String, alertType: AlertType) {
        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val icon = when (alertType) {
            AlertType.BUDGET_50_PERCENT -> R.drawable.ic_warning
            AlertType.BUDGET_80_PERCENT -> R.drawable.ic_warning
            AlertType.BUDGET_EXCEEDED -> R.drawable.ic_error
            AlertType.DAILY_SUMMARY -> R.drawable.ic_summary
            AlertType.WEEKLY_SUMMARY -> R.drawable.ic_summary
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(alertType.ordinal, notification)
    }
    
    private suspend fun getCategoryName(categoryId: String): String {
        // This should fetch from CategoryDao, simplified for now
        return when (categoryId) {
            "food" -> "Food & Dining"
            "grocery" -> "Grocery"
            "transport" -> "Transportation"
            "bills" -> "Bills & Utilities"
            "education" -> "Education"
            "entertainment" -> "Entertainment"
            "healthcare" -> "Healthcare"
            "shopping" -> "Shopping"
            else -> "Other"
        }
    }
    
    private fun calculateTimeUntilNextRun(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val nextRun = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        
        return nextRun.timeInMillis - now.timeInMillis
    }
    
    private fun calculateTimeUntilNextSunday(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val nextSunday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            if (before(now)) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }
        
        return nextSunday.timeInMillis - now.timeInMillis
    }
    
    // Additional methods for AlertsViewModel
    fun enableAlerts() {
        // Enable all alert-related work managers
        scheduleBudgetChecks()
    }
    
    fun disableAlerts() {
        // Cancel all alert-related work managers
        WorkManager.getInstance(context).cancelUniqueWork(BUDGET_CHECK_WORK)
    }
    
    fun updateThresholdSettings() {
        // Restart budget check work with updated settings
        scheduleBudgetChecks()
    }
    
    fun cancelDailySummary() {
        WorkManager.getInstance(context).cancelUniqueWork(DAILY_SUMMARY_WORK)
    }
    
    fun cancelWeeklySummary() {
        WorkManager.getInstance(context).cancelUniqueWork(WEEKLY_SUMMARY_WORK)
    }
    
    fun sendTestNotification() {
        val title = "Test Notification"
        val message = "This is a test notification from Koshpal. Your alerts are working correctly!"
        showNotification(title, message, AlertType.DAILY_SUMMARY)
    }
    
    private fun scheduleBudgetChecks() {
        val budgetCheckRequest = PeriodicWorkRequestBuilder<BudgetCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BUDGET_CHECK_WORK,
            ExistingPeriodicWorkPolicy.REPLACE,
            budgetCheckRequest
        )
    }
}

enum class AlertType {
    BUDGET_50_PERCENT,
    BUDGET_80_PERCENT,
    BUDGET_EXCEEDED,
    DAILY_SUMMARY,
    WEEKLY_SUMMARY
}
