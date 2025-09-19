package com.koshpal_android.koshpalapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.koshpal_android.koshpalapp.service.TransactionProcessingService
import com.koshpal_android.koshpalapp.utils.SMSReader
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class Application : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var transactionProcessingService: TransactionProcessingService
    
    @Inject
    lateinit var smsReader: SMSReader
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // WorkManager is automatically initialized because we implement Configuration.Provider
        // No need to manually initialize it
        
        // Initialize app services
        initializeAppServices()
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    private fun initializeAppServices() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Initialize default categories
                transactionProcessingService.initializeDefaultCategories()
                
                // Read and process existing SMS messages
                smsReader.readAndProcessExistingSMS()
                
                // Process any unprocessed SMS
                transactionProcessingService.processUnprocessedSms()
                
                // Schedule periodic tasks
                transactionProcessingService.schedulePeriodicTasks()
            } catch (e: Exception) {
                // Handle initialization errors
            }
        }
    }
}