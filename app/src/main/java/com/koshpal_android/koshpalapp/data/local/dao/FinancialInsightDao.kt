package com.koshpal_android.koshpalapp.data.local.dao

import androidx.room.*
import com.koshpal_android.koshpalapp.model.FinancialInsight
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialInsightDao {
    
    @Query("SELECT * FROM financial_insights ORDER BY year DESC, month DESC")
    fun getAllInsights(): Flow<List<FinancialInsight>>
    
    @Query("SELECT * FROM financial_insights WHERE month = :month AND year = :year")
    suspend fun getInsightByMonth(month: Int, year: Int): FinancialInsight?
    
    @Query("SELECT * FROM financial_insights ORDER BY year DESC, month DESC LIMIT :limit")
    suspend fun getRecentInsights(limit: Int = 6): List<FinancialInsight>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: FinancialInsight)
    
    @Update
    suspend fun updateInsight(insight: FinancialInsight)
    
    @Delete
    suspend fun deleteInsight(insight: FinancialInsight)
    
    @Query("DELETE FROM financial_insights WHERE month = :month AND year = :year")
    suspend fun deleteInsightByMonth(month: Int, year: Int)
}
