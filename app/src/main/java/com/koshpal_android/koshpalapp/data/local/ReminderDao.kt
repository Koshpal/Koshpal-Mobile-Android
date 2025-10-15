package com.koshpal_android.koshpalapp.data.local

import androidx.room.*
import com.koshpal_android.koshpalapp.model.Reminder
import com.koshpal_android.koshpalapp.model.ReminderStatus
import com.koshpal_android.koshpalapp.model.ReminderType
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long
    
    @Update
    suspend fun updateReminder(reminder: Reminder)
    
    @Delete
    suspend fun deleteReminder(reminder: Reminder)
    
    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: String)
    
    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    suspend fun getReminderById(reminderId: String): Reminder?
    
    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    fun getReminderByIdFlow(reminderId: String): Flow<Reminder?>
    
    // Get all reminders
    @Query("SELECT * FROM reminders ORDER BY dueDate ASC, dueTime ASC")
    fun getAllReminders(): Flow<List<Reminder>>
    
    @Query("SELECT * FROM reminders ORDER BY dueDate ASC, dueTime ASC")
    suspend fun getAllRemindersOnce(): List<Reminder>
    
    // Get pending reminders
    @Query("SELECT * FROM reminders WHERE status = 'PENDING' ORDER BY dueDate ASC, dueTime ASC")
    fun getPendingReminders(): Flow<List<Reminder>>
    
    @Query("SELECT * FROM reminders WHERE status = 'PENDING' ORDER BY dueDate ASC, dueTime ASC")
    suspend fun getPendingRemindersOnce(): List<Reminder>
    
    // Get overdue reminders
    @Query("SELECT * FROM reminders WHERE status = 'OVERDUE' ORDER BY dueDate ASC, dueTime ASC")
    fun getOverdueReminders(): Flow<List<Reminder>>
    
    // Get completed reminders
    @Query("SELECT * FROM reminders WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedReminders(): Flow<List<Reminder>>
    
    // Get reminders by type
    @Query("SELECT * FROM reminders WHERE type = :type AND status = 'PENDING' ORDER BY dueDate ASC, dueTime ASC")
    fun getRemindersByType(type: ReminderType): Flow<List<Reminder>>
    
    // Get upcoming reminders (next 7 days)
    @Query("SELECT * FROM reminders WHERE status = 'PENDING' AND dueDate <= :endDate ORDER BY dueDate ASC, dueTime ASC")
    fun getUpcomingReminders(endDate: Long): Flow<List<Reminder>>
    
    @Query("SELECT * FROM reminders WHERE status = 'PENDING' AND dueDate <= :endDate ORDER BY dueDate ASC, dueTime ASC")
    suspend fun getUpcomingRemindersOnce(endDate: Long): List<Reminder>
    
    // Get next reminder (closest one)
    @Query("SELECT * FROM reminders WHERE status = 'PENDING' ORDER BY dueDate ASC, dueTime ASC LIMIT 1")
    fun getNextReminder(): Flow<Reminder?>
    
    @Query("SELECT * FROM reminders WHERE status = 'PENDING' ORDER BY dueDate ASC, dueTime ASC LIMIT 1")
    suspend fun getNextReminderOnce(): Reminder?
    
    // Update reminder status
    @Query("UPDATE reminders SET status = :status, updatedAt = :updatedAt WHERE id = :reminderId")
    suspend fun updateReminderStatus(reminderId: String, status: ReminderStatus, updatedAt: Long = System.currentTimeMillis())
    
    // Mark as completed
    @Query("UPDATE reminders SET status = 'COMPLETED', completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :reminderId")
    suspend fun markReminderCompleted(reminderId: String, completedAt: Long = System.currentTimeMillis(), updatedAt: Long = System.currentTimeMillis())
    
    // Update overdue reminders
    @Query("UPDATE reminders SET status = 'OVERDUE', updatedAt = :currentTime WHERE status = 'PENDING' AND dueDate < :currentDate")
    suspend fun updateOverdueReminders(currentDate: Long, currentTime: Long = System.currentTimeMillis())
    
    // Get reminder count by status
    @Query("SELECT COUNT(*) FROM reminders WHERE status = :status")
    suspend fun getReminderCountByStatus(status: ReminderStatus): Int
    
    // Get total amount to give
    @Query("SELECT SUM(amount) FROM reminders WHERE type = 'GIVE' AND status = 'PENDING'")
    suspend fun getTotalAmountToGive(): Double?
    
    // Get total amount to receive
    @Query("SELECT SUM(amount) FROM reminders WHERE type = 'RECEIVE' AND status = 'PENDING'")
    suspend fun getTotalAmountToReceive(): Double?
    
    // Search reminders
    @Query("SELECT * FROM reminders WHERE personName LIKE '%' || :query || '%' OR purpose LIKE '%' || :query || '%' ORDER BY dueDate ASC")
    fun searchReminders(query: String): Flow<List<Reminder>>
    
    // Delete all completed reminders
    @Query("DELETE FROM reminders WHERE status = 'COMPLETED'")
    suspend fun deleteAllCompletedReminders()
    
    // Delete all cancelled reminders
    @Query("DELETE FROM reminders WHERE status = 'CANCELLED'")
    suspend fun deleteAllCancelledReminders()
}
