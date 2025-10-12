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
        PaymentSms::class,
        User::class,
        Budget::class,
        BudgetCategory::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KoshpalDatabase : RoomDatabase() {
    
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun paymentSmsDao(): PaymentSmsDao
    abstract fun userDao(): UserDao
    abstract fun budgetNewDao(): BudgetNewDao
    abstract fun budgetCategoryNewDao(): BudgetCategoryNewDao
    
    companion object {
        @Volatile
        private var INSTANCE: KoshpalDatabase? = null
        
        fun getDatabase(context: Context): KoshpalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KoshpalDatabase::class.java,
                    "koshpal_database_v6"
                )
                .fallbackToDestructiveMigration() // Allow database recreation when schema changes
                .fallbackToDestructiveMigrationOnDowngrade() // Handle downgrades too
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
