package com.koshpal_android.koshpalapp.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        BudgetCategory::class,
        CashFlowTransaction::class,
        Reminder::class
    ],
    version = 10, // Updated for additional sync tracking columns (serverTransactionId, syncedAt)
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
    abstract fun cashFlowTransactionDao(): CashFlowTransactionDao
    abstract fun reminderDao(): ReminderDao
    
    companion object {
        @Volatile
         private var INSTANCE: KoshpalDatabase? = null
        
        /**
         * Migration from version 8 to 9
         * Adds sync tracking columns to transactions table
         */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add isSynced column (default false - will be synced on next sync cycle)
                database.execSQL("ALTER TABLE transactions ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")

                // Add lastSyncAttempt column (default null)
                database.execSQL("ALTER TABLE transactions ADD COLUMN lastSyncAttempt INTEGER")
            }
        }

        /**
         * Migration from version 9 to 10
         * Adds additional sync tracking columns to transactions table
         */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add serverTransactionId column (default null)
                database.execSQL("ALTER TABLE transactions ADD COLUMN serverTransactionId TEXT")

                // Add syncedAt column (default null)
                database.execSQL("ALTER TABLE transactions ADD COLUMN syncedAt INTEGER")
            }
        }
        
        fun getDatabase(context: Context): KoshpalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KoshpalDatabase::class.java,
                    "koshpal_database_v8" // Keep the same name to allow migration!
                )
                .addMigrations(MIGRATION_8_9, MIGRATION_9_10) // Preserve existing data with migration
                .fallbackToDestructiveMigration() // Only if migration fails
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
