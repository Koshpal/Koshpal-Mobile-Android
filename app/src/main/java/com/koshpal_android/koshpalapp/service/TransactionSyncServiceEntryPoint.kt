package com.koshpal_android.koshpalapp.service

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TransactionSyncServiceEntryPoint {
    fun transactionSyncService(): TransactionSyncService
}
