package com.koshpal_android.koshpalapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.ui.notification.NotificationActivity
import android.util.Log

class KoshpalNotificationManager private constructor(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "koshpal_transactions"
        private const val CHANNEL_NAME = "Transaction Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for new payment transactions"
        
        private const val NOTIFICATION_ID_BASE = 1000
        
        @Volatile
        private var INSTANCE: KoshpalNotificationManager? = null

        fun getInstance(context: Context): KoshpalNotificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: KoshpalNotificationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH  // Use HIGH like transaction notifications
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = Color.parseColor("#2196F3") // Koshpal blue
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTransactionNotification(transaction: Transaction) {
        try {
            // Validate transaction data
            if (transaction.merchant.isBlank() || transaction.amount <= 0) {
                Log.w("KoshpalNotification", "⚠️ Invalid transaction data, skipping notification")
                return
            }
            
            val notificationId = NOTIFICATION_ID_BASE + transaction.id.hashCode()
            
            // Create intent to open lightweight notification activity
            val intent = NotificationActivity.createIntent(context, transaction.id)

            val pendingIntent = PendingIntent.getActivity(
                context,
                transaction.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Create rich notification content
            val notification = createRichNotification(transaction, pendingIntent)
            
            // Show notification
            with(NotificationManagerCompat.from(context)) {
                if (areNotificationsEnabled()) {
                    notify(notificationId, notification)
                    Log.d("KoshpalNotification", "✅ Notification sent for transaction: ${transaction.merchant} - ₹${transaction.amount}")
                } else {
                    Log.w("KoshpalNotification", "⚠️ Notifications are disabled by user")
                }
            }
        } catch (e: Exception) {
            Log.e("KoshpalNotification", "❌ Failed to show notification", e)
        }
    }

    private fun createRichNotification(transaction: Transaction, pendingIntent: PendingIntent): android.app.Notification {
        val isDebit = transaction.type == TransactionType.DEBIT
        val amountText = "₹${String.format("%.2f", transaction.amount)}"
        val typeText = if (isDebit) "Debited" else "Credited"
        val typeEmoji = if (isDebit) "💸" else "💰"
        
        // Create title and content
        val title = "$typeEmoji Payment $typeText"
        val content = "$amountText at ${transaction.merchant}"
        
        // Create big text style for more details
        val categoryName = getCategoryDisplayName(transaction.categoryId)
        val categoryEmoji = getCategoryEmoji(transaction.categoryId)
        
        val bigText = buildString {
            append("$typeText: $amountText\n")
            append("Merchant: ${transaction.merchant}\n")
            append("$categoryEmoji Category: $categoryName\n")
            append("Bank: ${transaction.bankName}\n")
            append("Time: ${formatTime(transaction.timestamp)}")
            
            // Add SMS preview if available
            transaction.smsBody?.let { smsBody ->
                append("\n\nSMS Preview:\n${smsBody.take(100)}...")
            }
            
            // Add note about editing
            append("\n\n💡 Tap to edit category or add notes")
        }

        // Create custom notification layout
        val customView = createCustomNotificationView(transaction, title, content)
        
        // Create notification with rich content
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setLargeIcon(createLargeIcon())
            .setContentTitle(title)
            .setContentText(content)
            .setCustomBigContentView(customView)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(bigText)
                .setSummaryText("Tap to view details"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(Color.parseColor("#2196F3")) // Koshpal blue
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setWhen(transaction.timestamp)
            .build()
    }

    private fun createCustomNotificationView(transaction: Transaction, title: String, content: String): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_transaction)
        
        // Set basic information
        remoteViews.setTextViewText(R.id.tv_notification_title, title)
        remoteViews.setTextViewText(R.id.tv_amount, "₹${String.format("%.2f", transaction.amount)}")
        remoteViews.setTextViewText(R.id.tv_merchant, transaction.merchant)
        remoteViews.setTextViewText(R.id.tv_bank, transaction.bankName)
        remoteViews.setTextViewText(R.id.tv_time, formatTime(transaction.timestamp))
        
        // Set transaction type
        val isDebit = transaction.type == TransactionType.DEBIT
        val typeText = if (isDebit) "Debited" else "Credited"
        val typeColor = if (isDebit) "#D32F2F" else "#2E7D32"
        remoteViews.setTextViewText(R.id.tv_transaction_type, typeText)
        remoteViews.setTextColor(R.id.tv_transaction_type, Color.parseColor(typeColor))
        
        // Set category information
        val categoryName = getCategoryDisplayName(transaction.categoryId)
        val categoryEmoji = getCategoryEmoji(transaction.categoryId)
        remoteViews.setTextViewText(R.id.tv_category, categoryName)
        remoteViews.setTextViewText(R.id.tv_category_emoji, categoryEmoji)
        
        return remoteViews
    }

    private fun createCustomBudgetNotificationView(
        transaction: Transaction,
        title: String,
        message: String,
        percentage: Double
    ): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_budget)
        
        val isExceeded = percentage >= 100
        val color = if (isExceeded) "#F44336" else "#FF9800"
        val emoji = if (isExceeded) "🚨" else "⚠️"
        
        // Calculate budget amount from percentage and spent amount
        val spentAmount = transaction.amount
        val budgetAmount = if (percentage > 0) (spentAmount / percentage * 100) else 0.0
        
        // Set basic information
        remoteViews.setTextViewText(R.id.tv_notification_title, "$emoji $title")
        remoteViews.setTextViewText(R.id.tv_category_name, transaction.merchant.replace("Budget Alert: ", ""))
        remoteViews.setTextViewText(R.id.tv_percentage, "${String.format("%.0f", percentage)}%")
        remoteViews.setTextViewText(R.id.tv_spent_amount, "₹${String.format("%.0f", spentAmount)}")
        remoteViews.setTextViewText(R.id.tv_budget_amount, "₹${String.format("%.0f", budgetAmount)}")
        remoteViews.setTextViewText(R.id.tv_time, formatTime(transaction.timestamp))
        
        // Set progress bar
        remoteViews.setProgressBar(R.id.progress_budget, 100, percentage.toInt(), false)
        remoteViews.setInt(R.id.progress_budget, "setProgressTint", Color.parseColor(color))
        
        // Set status message
        val statusMessage = when {
            percentage >= 100 -> "Budget exceeded! Consider reviewing your spending"
            percentage >= 90 -> "You're very close to your budget limit"
            percentage >= 50 -> "You're approaching your budget limit"
            else -> "Keep track of your remaining budget"
        }
        remoteViews.setTextViewText(R.id.tv_status_message, statusMessage)
        
        return remoteViews
    }

    private fun createCustomBudgetNotificationViewSimple(
        transaction: Transaction,
        title: String,
        message: String,
        percentage: Double
    ): RemoteViews {
        // Use the working transaction layout but populate it with budget information
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_transaction)
        
        val isExceeded = percentage >= 100
        val color = if (isExceeded) "#F44336" else "#FF9800"
        val emoji = if (isExceeded) "🚨" else "⚠️"
        
        // Calculate budget amount from percentage and spent amount
        val spentAmount = transaction.amount
        val budgetAmount = if (percentage > 0) (spentAmount / percentage * 100) else 0.0
        
        // Set basic information using transaction layout fields
        remoteViews.setTextViewText(R.id.tv_notification_title, "$emoji $title")
        remoteViews.setTextViewText(R.id.tv_amount, "₹${String.format("%.0f", spentAmount)}")
        remoteViews.setTextViewText(R.id.tv_merchant, transaction.merchant.replace("Budget Alert: ", ""))
        remoteViews.setTextViewText(R.id.tv_bank, "Budget: ₹${String.format("%.0f", budgetAmount)}")
        remoteViews.setTextViewText(R.id.tv_time, formatTime(transaction.timestamp))
        
        // Set budget percentage as transaction type
        val percentageText = "${String.format("%.0f", percentage)}% Used"
        remoteViews.setTextViewText(R.id.tv_transaction_type, percentageText)
        remoteViews.setTextColor(R.id.tv_transaction_type, Color.parseColor(color))
        
        // Set category information (budget category)
        val categoryName = transaction.merchant.replace("Budget Alert: ", "")
        val categoryEmoji = "💰" // Budget emoji
        remoteViews.setTextViewText(R.id.tv_category, categoryName)
        remoteViews.setTextViewText(R.id.tv_category_emoji, categoryEmoji)
        
        return remoteViews
    }

    private fun createLargeIcon(): Bitmap? {
        return try {
            // Use app logo as large icon
            BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
        } catch (e: Exception) {
            Log.e("KoshpalNotification", "Failed to create large icon", e)
            null
        }
    }

    private fun getCategoryDisplayName(categoryId: String?): String {
        return when (categoryId) {
            "food" -> "Food & Dining"
            "transport" -> "Transportation"
            "shopping" -> "Shopping"
            "entertainment" -> "Entertainment"
            "bills" -> "Bills & Utilities"
            "healthcare" -> "Healthcare"
            "education" -> "Education"
            "travel" -> "Travel"
            "subscriptions" -> "Subscriptions"
            "others" -> "Others"
            else -> "Uncategorized"
        }
    }

    private fun getCategoryEmoji(categoryId: String?): String {
        return when (categoryId) {
            "food" -> "🍽️"
            "transport" -> "🚗"
            "shopping" -> "🛍️"
            "entertainment" -> "🎬"
            "bills" -> "💡"
            "healthcare" -> "🏥"
            "education" -> "📚"
            "travel" -> "✈️"
            "subscriptions" -> "📱"
            "others" -> "📦"
            else -> "❓"
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("h:mm a, MMM d", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    private fun areNotificationsEnabled(): Boolean {
        return try {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } catch (e: Exception) {
            Log.e("KoshpalNotification", "Failed to check notification permission", e)
            true // Default to true if we can't check
        }
    }

    fun cancelNotification(transactionId: String) {
        try {
            val notificationId = NOTIFICATION_ID_BASE + transactionId.hashCode()
            with(NotificationManagerCompat.from(context)) {
                cancel(notificationId)
                Log.d("KoshpalNotification", "✅ Cancelled notification for transaction: $transactionId")
            }
        } catch (e: Exception) {
            Log.e("KoshpalNotification", "❌ Failed to cancel notification", e)
        }
    }

    fun cancelAllNotifications() {
        try {
            with(NotificationManagerCompat.from(context)) {
                cancelAll()
                Log.d("KoshpalNotification", "✅ Cancelled all notifications")
            }
        } catch (e: Exception) {
            Log.e("KoshpalNotification", "❌ Failed to cancel all notifications", e)
        }
    }

    /**
     * Show budget notification with custom title and message
     */
    fun showBudgetNotification(
        transaction: Transaction, 
        title: String, 
        message: String, 
        percentage: Double
    ) {
        try {
            val notificationId = NOTIFICATION_ID_BASE + transaction.id.hashCode()
            
            // Create intent to open budget fragment
            val intent = Intent(context, com.koshpal_android.koshpalapp.ui.home.HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("open_budget_fragment", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                transaction.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Create budget notification using the same approach as transaction notifications
            val notification = createBudgetNotification(transaction, title, message, percentage, pendingIntent)
            
            // Show notification
            with(NotificationManagerCompat.from(context)) {
                if (areNotificationsEnabled()) {
                    notify(notificationId, notification)
                    Log.d("KoshpalNotification", "✅ Budget notification sent: $title")
                } else {
                    Log.w("KoshpalNotification", "⚠️ Notifications are disabled by user")
                }
            }
        } catch (e: Exception) {
            Log.e("KoshpalNotification", "❌ Failed to show budget notification", e)
        }
    }

    private fun createBudgetNotification(
        transaction: Transaction,
        title: String,
        message: String,
        percentage: Double,
        pendingIntent: PendingIntent
    ): android.app.Notification {
        val isExceeded = percentage >= 100
        val emoji = if (isExceeded) "🚨" else "⚠️"
        val color = if (isExceeded) "#F44336" else "#FF9800" // Red for exceeded, Orange for warning
        
        val bigText = buildString {
            append("$emoji $title\n\n")
            append("$message\n\n")
            append("Category: ${transaction.merchant}\n")
            append("Spent: ₹${String.format("%.0f", transaction.amount)}\n")
            append("Percentage: ${String.format("%.1f", percentage)}%\n\n")
            
            if (isExceeded) {
                append("💡 Consider reviewing your spending or adjusting your budget")
            } else {
                append("💡 Keep track of your remaining budget")
            }
        }

        // Create custom budget notification layout (using transaction layout for now to ensure it works)
        val customView = createCustomBudgetNotificationViewSimple(transaction, title, message, percentage)
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setLargeIcon(createLargeIcon())
            .setContentTitle("$emoji $title")
            .setContentText(message)
            .setCustomBigContentView(customView)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(bigText)
                .setSummaryText("Tap to view budget details"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // Use HIGH like transaction notifications
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)  // Use MESSAGE like transaction notifications
            .setAutoCancel(true)  // Allow auto-cancel like transaction notifications
            .setContentIntent(pendingIntent)
            .setColor(Color.parseColor(color))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setWhen(transaction.timestamp)
            .build()
    }


    /**
     * Test method to show a sample notification (for debugging)
     */
    fun showTestNotification() {
        try {
            val testTransaction = Transaction(
                id = "test_${System.currentTimeMillis()}",
                amount = 500.0,
                type = TransactionType.DEBIT,
                merchant = "Test Merchant",
                categoryId = "shopping",
                confidence = 85.0f,
                date = System.currentTimeMillis(),
                description = "Test transaction for notification",
                smsBody = "Test SMS body for notification testing",
                bankName = "Test Bank"
            )
            
            showTransactionNotification(testTransaction)
            Log.d("KoshpalNotification", "🧪 Test notification sent")
        } catch (e: Exception) {
            Log.e("KoshpalNotification", "❌ Failed to send test notification", e)
        }
    }
}
