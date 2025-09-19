package com.koshpal_android.koshpalapp.data.local.dao

import androidx.room.*
import com.koshpal_android.koshpalapp.model.TransactionCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM transaction_categories WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveCategories(): Flow<List<TransactionCategory>>
    
    @Query("SELECT * FROM transaction_categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<TransactionCategory>>
    
    @Query("SELECT * FROM transaction_categories WHERE id = :id")
    suspend fun getCategoryById(id: String): TransactionCategory?
    
    @Query("SELECT * FROM transaction_categories WHERE isDefault = 1")
    suspend fun getDefaultCategories(): List<TransactionCategory>
    
    @Query("SELECT * FROM transaction_categories WHERE isDefault = 0")
    suspend fun getCustomCategories(): List<TransactionCategory>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: TransactionCategory)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<TransactionCategory>)
    
    @Update
    suspend fun updateCategory(category: TransactionCategory)
    
    @Delete
    suspend fun deleteCategory(category: TransactionCategory)
    
    @Query("UPDATE transaction_categories SET isActive = 0 WHERE id = :id")
    suspend fun deactivateCategory(id: String)
    
    @Query("UPDATE transaction_categories SET isActive = 1 WHERE id = :id")
    suspend fun activateCategory(id: String)
    
    @Query("SELECT COUNT(*) FROM transaction_categories WHERE isDefault = 0")
    suspend fun getCustomCategoryCount(): Int
}
