package com.koshpal_android.koshpalapp.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver to handle when users swipe away budget notifications
 * This allows us to track notification dismissal and potentially resend if needed
 */
class NotificationDeleteReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "NotificationDeleteReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val notificationId = intent.getIntExtra("notification_id", -1)
            val notificationType = intent.getStringExtra("notification_type") ?: "unknown"
            
            Log.d(TAG, "🗑️ Notification dismissed: ID=$notificationId, Type=$notificationType")
            
            // Here you could add logic to:
            // 1. Track which notifications were dismissed
            // 2. Resend notifications if needed
            // 3. Update notification preferences
            // 4. Log analytics
            
            when (notificationType) {
                "budget" -> {
                    Log.d(TAG, "💰 Budget notification dismissed by user")
                    // Could implement logic to not resend the same budget alert
                    // until the next threshold is reached
                }
                "transaction" -> {
                    Log.d(TAG, "💳 Transaction notification dismissed by user")
                }
                else -> {
                    Log.d(TAG, "📱 Unknown notification type dismissed")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling notification deletion", e)
        }
    }
}
