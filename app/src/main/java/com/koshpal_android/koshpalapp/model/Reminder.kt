package com.koshpal_android.koshpalapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    // Type: GIVE (you need to pay) or RECEIVE (someone needs to pay you)
    val type: ReminderType,
    
    // Person details
    val personName: String,
    val contact: String? = null,
    
    // Amount details
    val amount: Double,
    val purpose: String,
    
    // Date and time
    val dueDate: Long, // Timestamp in milliseconds
    val dueTime: Long, // Time in milliseconds from midnight
    
    // Repeat settings
    val repeatType: RepeatType = RepeatType.NONE,
    
    // Priority
    val priority: ReminderPriority = ReminderPriority.MEDIUM,
    
    // Status
    val status: ReminderStatus = ReminderStatus.PENDING,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    
    // Notification ID for cancellation
    val notificationId: Int = id.hashCode()
)

enum class ReminderType {
    GIVE,     // You need to pay someone
    RECEIVE   // Someone needs to pay you
}

enum class RepeatType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY
}

enum class ReminderPriority {
    LOW,
    MEDIUM,
    HIGH
}

enum class ReminderStatus {
    PENDING,
    COMPLETED,
    CANCELLED,
    OVERDUE
}

// Extension functions for easy display
fun ReminderType.getDisplayName(): String = when (this) {
    ReminderType.GIVE -> "Pay"
    ReminderType.RECEIVE -> "Receive"
}

fun ReminderType.getColorResource(): Int = when (this) {
    ReminderType.GIVE -> com.koshpal_android.koshpalapp.R.color.expense
    ReminderType.RECEIVE -> com.koshpal_android.koshpalapp.R.color.income
}

fun RepeatType.getDisplayName(): String = when (this) {
    RepeatType.NONE -> "Does not repeat"
    RepeatType.DAILY -> "Daily"
    RepeatType.WEEKLY -> "Weekly"
    RepeatType.MONTHLY -> "Monthly"
}

fun ReminderPriority.getDisplayName(): String = when (this) {
    ReminderPriority.LOW -> "Low"
    ReminderPriority.MEDIUM -> "Medium"
    ReminderPriority.HIGH -> "High"
}

fun ReminderPriority.getColorResource(): Int = when (this) {
    ReminderPriority.LOW -> com.koshpal_android.koshpalapp.R.color.success
    ReminderPriority.MEDIUM -> com.koshpal_android.koshpalapp.R.color.warning
    ReminderPriority.HIGH -> com.koshpal_android.koshpalapp.R.color.error
}

fun ReminderStatus.getDisplayName(): String = when (this) {
    ReminderStatus.PENDING -> "Pending"
    ReminderStatus.COMPLETED -> "Completed"
    ReminderStatus.CANCELLED -> "Cancelled"
    ReminderStatus.OVERDUE -> "Overdue"
}
