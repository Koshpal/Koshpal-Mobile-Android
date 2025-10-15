package com.koshpal_android.koshpalapp.repository

import com.koshpal_android.koshpalapp.data.local.ReminderDao
import com.koshpal_android.koshpalapp.model.Reminder
import com.koshpal_android.koshpalapp.model.ReminderStatus
import com.koshpal_android.koshpalapp.model.ReminderType
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao
) {
    
    // Get all reminders
    fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAllReminders()
    
    suspend fun getAllRemindersOnce(): List<Reminder> = reminderDao.getAllRemindersOnce()
    
    // Get pending reminders
    fun getPendingReminders(): Flow<List<Reminder>> = reminderDao.getPendingReminders()
    
    suspend fun getPendingRemindersOnce(): List<Reminder> = reminderDao.getPendingRemindersOnce()
    
    // Get overdue reminders
    fun getOverdueReminders(): Flow<List<Reminder>> = reminderDao.getOverdueReminders()
    
    // Get completed reminders
    fun getCompletedReminders(): Flow<List<Reminder>> = reminderDao.getCompletedReminders()
    
    // Get reminders by type
    fun getRemindersByType(type: ReminderType): Flow<List<Reminder>> = 
        reminderDao.getRemindersByType(type)
    
    // Get upcoming reminders (next 7 days)
    fun getUpcomingReminders(days: Int = 7): Flow<List<Reminder>> {
        val endDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, days)
        }.timeInMillis
        return reminderDao.getUpcomingReminders(endDate)
    }
    
    suspend fun getUpcomingRemindersOnce(days: Int = 7): List<Reminder> {
        val endDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, days)
        }.timeInMillis
        return reminderDao.getUpcomingRemindersOnce(endDate)
    }
    
    // Get next reminder
    fun getNextReminder(): Flow<Reminder?> = reminderDao.getNextReminder()
    
    suspend fun getNextReminderOnce(): Reminder? = reminderDao.getNextReminderOnce()
    
    // Get reminder by ID
    suspend fun getReminderById(reminderId: String): Reminder? = 
        reminderDao.getReminderById(reminderId)
    
    fun getReminderByIdFlow(reminderId: String): Flow<Reminder?> = 
        reminderDao.getReminderByIdFlow(reminderId)
    
    // Insert reminder
    suspend fun insertReminder(reminder: Reminder): Long = 
        reminderDao.insertReminder(reminder)
    
    // Update reminder
    suspend fun updateReminder(reminder: Reminder) = 
        reminderDao.updateReminder(reminder)
    
    // Delete reminder
    suspend fun deleteReminder(reminder: Reminder) = 
        reminderDao.deleteReminder(reminder)
    
    suspend fun deleteReminderById(reminderId: String) = 
        reminderDao.deleteReminderById(reminderId)
    
    // Update reminder status
    suspend fun updateReminderStatus(reminderId: String, status: ReminderStatus) = 
        reminderDao.updateReminderStatus(reminderId, status)
    
    // Mark as completed
    suspend fun markReminderCompleted(reminderId: String) = 
        reminderDao.markReminderCompleted(reminderId)
    
    // Update overdue reminders
    suspend fun updateOverdueReminders() {
        val currentDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        reminderDao.updateOverdueReminders(currentDate)
    }
    
    // Get counts
    suspend fun getReminderCountByStatus(status: ReminderStatus): Int = 
        reminderDao.getReminderCountByStatus(status)
    
    suspend fun getPendingReminderCount(): Int = 
        getReminderCountByStatus(ReminderStatus.PENDING)
    
    suspend fun getOverdueReminderCount(): Int = 
        getReminderCountByStatus(ReminderStatus.OVERDUE)
    
    // Get amounts
    suspend fun getTotalAmountToGive(): Double = 
        reminderDao.getTotalAmountToGive() ?: 0.0
    
    suspend fun getTotalAmountToReceive(): Double = 
        reminderDao.getTotalAmountToReceive() ?: 0.0
    
    // Search reminders
    fun searchReminders(query: String): Flow<List<Reminder>> = 
        reminderDao.searchReminders(query)
    
    // Delete completed/cancelled reminders
    suspend fun deleteAllCompletedReminders() = 
        reminderDao.deleteAllCompletedReminders()
    
    suspend fun deleteAllCancelledReminders() = 
        reminderDao.deleteAllCancelledReminders()
    
    // Helper: Check if reminder is overdue
    fun isReminderOverdue(reminder: Reminder): Boolean {
        val now = System.currentTimeMillis()
        val reminderDateTime = reminder.dueDate + reminder.dueTime
        return reminderDateTime < now && reminder.status == ReminderStatus.PENDING
    }
    
    // Helper: Get formatted due date/time
    fun getReminderDueDateTime(reminder: Reminder): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = reminder.dueDate + reminder.dueTime
        }
    }
}
