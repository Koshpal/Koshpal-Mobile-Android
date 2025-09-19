package com.koshpal_android.koshpalapp.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.koshpal_android.koshpalapp.model.*
import com.koshpal_android.koshpalapp.data.local.dao.*

@Database(
    entities = [
        Transaction::class,
        TransactionCategory::class,
        Budget::class,
        SavingsGoal::class,
        FinancialInsight::class,
        PaymentSms::class,
        User::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KoshpalDatabase : RoomDatabase() {
    
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun financialInsightDao(): FinancialInsightDao
    abstract fun paymentSmsDao(): PaymentSmsDao
    abstract fun userDao(): UserDao
    
    companion object {
        @Volatile
        private var INSTANCE: KoshpalDatabase? = null
        
        fun getDatabase(context: Context): KoshpalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KoshpalDatabase::class.java,
                    "koshpal_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Initialize default categories
                        // This will be handled in the repository
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
