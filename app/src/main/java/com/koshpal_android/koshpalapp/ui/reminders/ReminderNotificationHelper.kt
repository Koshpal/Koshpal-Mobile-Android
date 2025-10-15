package com.koshpal_android.koshpalapp.ui.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.koshpal_android.koshpalapp.model.Reminder
import java.util.*

object ReminderNotificationHelper {

    fun scheduleReminder(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
            putExtra("person_name", reminder.personName)
            putExtra("amount", reminder.amount)
            putExtra("purpose", reminder.purpose)
            putExtra("type", reminder.type.name)
            putExtra("contact", reminder.contact)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate trigger time
        val triggerTime = reminder.dueDate + reminder.dueTime

        // Schedule exact alarm
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ - check if can schedule exact alarms
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    android.util.Log.d(
                        "ReminderNotificationHelper",
                        "✅ Exact alarm scheduled for ${Date(triggerTime)} (ID: ${reminder.notificationId})"
                    )
                } else {
                    // Fallback to inexact alarm
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    android.util.Log.w(
                        "ReminderNotificationHelper",
                        "⚠️ Using inexact alarm (exact alarms not permitted)"
                    )
                }
            } else {
                // Android 11 and below
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                android.util.Log.d(
                    "ReminderNotificationHelper",
                    "✅ Exact alarm scheduled for ${Date(triggerTime)} (ID: ${reminder.notificationId})"
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(
                "ReminderNotificationHelper",
                "❌ Failed to schedule alarm: ${e.message}",
                e
            )
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        android.util.Log.d(
            "ReminderNotificationHelper",
            "🗑️ Notification cancelled (ID: $notificationId)"
        )
    }

    fun rescheduleReminder(context: Context, reminder: Reminder, delayMinutes: Int) {
        // Cancel existing alarm
        cancelNotification(context, reminder.notificationId)

        // Calculate new trigger time
        val newTriggerTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
            putExtra("person_name", reminder.personName)
            putExtra("amount", reminder.amount)
            putExtra("purpose", reminder.purpose)
            putExtra("type", reminder.type.name)
            putExtra("contact", reminder.contact)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule snooze alarm
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                newTriggerTime,
                pendingIntent
            )
            android.util.Log.d(
                "ReminderNotificationHelper",
                "⏰ Reminder snoozed for $delayMinutes minutes"
            )
        } catch (e: Exception) {
            android.util.Log.e(
                "ReminderNotificationHelper",
                "❌ Failed to snooze reminder: ${e.message}",
                e
            )
        }
    }
}
