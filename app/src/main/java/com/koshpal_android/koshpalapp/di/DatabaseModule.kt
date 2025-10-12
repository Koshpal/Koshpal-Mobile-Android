package com.koshpal_android.koshpalapp.di

import android.content.Context
import androidx.room.Room
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.data.local.dao.*
import com.koshpal_android.koshpalapp.engine.TransactionCategorizationEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideKoshpalDatabase(@ApplicationContext context: Context): KoshpalDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            KoshpalDatabase::class.java,
            "koshpal_database_v7"
        ).fallbackToDestructiveMigration()
         .fallbackToDestructiveMigrationOnDowngrade()
         .build()
    }
    
    @Provides
    fun provideTransactionDao(database: KoshpalDatabase): TransactionDao {
        return database.transactionDao()
    }
    
    @Provides
    fun provideCategoryDao(database: KoshpalDatabase): CategoryDao {
        return database.categoryDao()
    }
    
    
    @Provides
    fun providePaymentSmsDao(database: KoshpalDatabase): PaymentSmsDao {
        return database.paymentSmsDao()
    }
    
    @Provides
    fun provideUserDao(database: KoshpalDatabase): UserDao {
        return database.userDao()
    }
    
    @Provides
    fun provideBudgetNewDao(database: KoshpalDatabase): BudgetNewDao {
        return database.budgetNewDao()
    }
    
    @Provides
    fun provideBudgetCategoryNewDao(database: KoshpalDatabase): BudgetCategoryNewDao {
        return database.budgetCategoryNewDao()
    }
    
    @Provides
    fun provideCashFlowTransactionDao(database: KoshpalDatabase): CashFlowTransactionDao {
        return database.cashFlowTransactionDao()
    }
    
    @Provides
    @Singleton
    fun provideTransactionCategorizationEngine(): TransactionCategorizationEngine {
        return TransactionCategorizationEngine()
    }
}
