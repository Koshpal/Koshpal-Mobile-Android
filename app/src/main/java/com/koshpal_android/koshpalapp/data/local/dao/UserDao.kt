package com.koshpal_android.koshpalapp.data.local.dao

import androidx.room.*
import com.koshpal_android.koshpalapp.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    
    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUserById(uid: String): User?
    
    @Query("SELECT * FROM users WHERE phoneNumber = :phoneNumber")
    suspend fun getUserByPhoneNumber(phoneNumber: String): User?
    
    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): User?
    
    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUserFlow(): Flow<User?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
    
    @Update
    suspend fun updateUser(user: User)
    
    @Delete
    suspend fun deleteUser(user: User)
    
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
    
    @Query("UPDATE users SET apiToken = :token, updatedAt = :updatedAt WHERE uid = :uid")
    suspend fun updateApiToken(uid: String, token: String?, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE users SET isVerified = :isVerified, updatedAt = :updatedAt WHERE uid = :uid")
    suspend fun updateVerificationStatus(uid: String, isVerified: Boolean, updatedAt: Long = System.currentTimeMillis())
}
