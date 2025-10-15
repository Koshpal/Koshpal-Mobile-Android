package com.koshpal_android.koshpalapp.ui.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.model.ReminderStatus
import com.koshpal_android.koshpalapp.model.ReminderType
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("ReminderBroadcastReceiver", "🔔 Reminder alarm received")

        val reminderId = intent.getStringExtra("reminder_id") ?: return
        val personName = intent.getStringExtra("person_name") ?: "Someone"
        val amount = intent.getDoubleExtra("amount", 0.0)
        val purpose = intent.getStringExtra("purpose") ?: "Payment"
        val typeStr = intent.getStringExtra("type") ?: "GIVE"
        val contact = intent.getStringExtra("contact")

        val type = try {
            ReminderType.valueOf(typeStr)
        } catch (e: Exception) {
            ReminderType.GIVE
        }

        // Create notification
        createNotificationChannel(context)
        showReminderNotification(context, reminderId, personName, amount, purpose, type, contact)

        // Update reminder status to OVERDUE if needed
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = KoshpalDatabase.getDatabase(context)
                val reminderDao = database.reminderDao()
                val reminder = reminderDao.getReminderById(reminderId)

                if (reminder != null && reminder.status == ReminderStatus.PENDING) {
                    // Don't automatically mark as overdue - let user handle it
                    android.util.Log.d("ReminderBroadcastReceiver", "📋 Reminder notification shown for: $personName")
                }
            } catch (e: Exception) {
                android.util.Log.e("ReminderBroadcastReceiver", "Error updating reminder: ${e.message}", e)
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Payment Reminders"
            val descriptionText = "Notifications for payment reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showReminderNotification(
        context: Context,
        reminderId: String,
        personName: String,
        amount: Double,
        purpose: String,
        type: ReminderType,
        contact: String?
    ) {
        val notificationId = reminderId.hashCode()

        // Create intent to open app
        val openIntent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_reminders", true)
            putExtra("reminder_id", reminderId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create "Mark as Paid" action
        val markPaidIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_MARK_PAID
            putExtra("reminder_id", reminderId)
            putExtra("notification_id", notificationId)
        }
        val markPaidPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            markPaidIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create "Snooze" action
        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SNOOZE
            putExtra("reminder_id", reminderId)
            putExtra("notification_id", notificationId)
            putExtra("person_name", personName)
            putExtra("amount", amount)
            putExtra("purpose", purpose)
            putExtra("type", type.name)
            putExtra("contact", contact)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification content
        val actionText = if (type == ReminderType.GIVE) "Pay" else "Collect"
        val title = "💰 Reminder: $actionText ₹${String.format("%.0f", amount)}"
        val message = "$actionText ₹${String.format("%.0f", amount)} to/from $personName"
        val bigText = "$message\n📝 Purpose: $purpose${contact?.let { "\n📞 Contact: $it" } ?: ""}"

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                R.drawable.ic_check_circle,
                "Mark Paid",
                markPaidPendingIntent
            )
            .addAction(
                R.drawable.ic_calendar,
                "Snooze 1hr",
                snoozePendingIntent
            )
            .build()

        // Show notification
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, notification)
            android.util.Log.d("ReminderBroadcastReceiver", "✅ Notification shown: $title")
        } catch (e: SecurityException) {
            android.util.Log.e("ReminderBroadcastReceiver", "❌ Notification permission denied", e)
        }
    }

    companion object {
        const val CHANNEL_ID = "payment_reminders_channel"
    }
}

// Separate receiver for notification actions
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("reminder_id") ?: return
        val notificationId = intent.getIntExtra("notification_id", 0)

        when (intent.action) {
            ACTION_MARK_PAID -> {
                android.util.Log.d("ReminderActionReceiver", "✅ Mark as Paid clicked for: $reminderId")

                // Mark reminder as completed in database
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val database = KoshpalDatabase.getDatabase(context)
                        val reminderDao = database.reminderDao()
                        reminderDao.markReminderCompleted(reminderId)
                        android.util.Log.d("ReminderActionReceiver", "✅ Reminder marked as completed")
                    } catch (e: Exception) {
                        android.util.Log.e("ReminderActionReceiver", "Error marking reminder complete: ${e.message}", e)
                    }
                }

                // Cancel notification
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.cancel(notificationId)

                // Show toast (requires foreground app)
                // Instead, we'll just log it
                android.util.Log.d("ReminderActionReceiver", "🎉 Reminder completed!")
            }

            ACTION_SNOOZE -> {
                android.util.Log.d("ReminderActionReceiver", "⏰ Snooze clicked for: $reminderId")

                // Get reminder data from intent
                val personName = intent.getStringExtra("person_name") ?: "Someone"
                val amount = intent.getDoubleExtra("amount", 0.0)
                val purpose = intent.getStringExtra("purpose") ?: "Payment"
                val typeStr = intent.getStringExtra("type") ?: "GIVE"
                val contact = intent.getStringExtra("contact")

                // Reschedule reminder for 1 hour later
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val database = KoshpalDatabase.getDatabase(context)
                        val reminderDao = database.reminderDao()
                        val reminder = reminderDao.getReminderById(reminderId)

                        if (reminder != null) {
                            ReminderNotificationHelper.rescheduleReminder(context, reminder, 60)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ReminderActionReceiver", "Error snoozing reminder: ${e.message}", e)
                    }
                }

                // Cancel current notification
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.cancel(notificationId)

                android.util.Log.d("ReminderActionReceiver", "⏰ Reminder snoozed for 1 hour")
            }
        }
    }

    companion object {
        const val ACTION_MARK_PAID = "com.koshpal_android.koshpalapp.ACTION_MARK_PAID"
        const val ACTION_SNOOZE = "com.koshpal_android.koshpalapp.ACTION_SNOOZE"
    }
}
