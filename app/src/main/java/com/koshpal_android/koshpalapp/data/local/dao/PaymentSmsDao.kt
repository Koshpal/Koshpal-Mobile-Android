package com.koshpal_android.koshpalapp.data.local.dao

import androidx.room.*
import com.koshpal_android.koshpalapp.model.PaymentSms
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentSmsDao {
    
    @Query("SELECT * FROM payment_sms ORDER BY timestamp DESC")
    fun getAllSms(): Flow<List<PaymentSms>>
    
    @Query("SELECT * FROM payment_sms WHERE isProcessed = 0 ORDER BY timestamp DESC")
    suspend fun getUnprocessedSms(): List<PaymentSms>
    
    @Query("SELECT * FROM payment_sms WHERE id = :id")
    suspend fun getSmsById(id: String): PaymentSms?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSms(sms: PaymentSms)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsList(smsList: List<PaymentSms>)
    
    @Update
    suspend fun updateSms(sms: PaymentSms)
    
    @Query("UPDATE payment_sms SET isProcessed = 1 WHERE id = :id")
    suspend fun markAsProcessed(id: String)
    
    @Delete
    suspend fun deleteSms(sms: PaymentSms)
    
    @Query("DELETE FROM payment_sms WHERE timestamp < :timestamp")
    suspend fun deleteOldSms(timestamp: Long)
    
    @Query("SELECT * FROM payment_sms WHERE body = :body AND address = :address LIMIT 1")
    suspend fun getSMSByBodyAndSender(body: String, address: String): PaymentSms?
}
