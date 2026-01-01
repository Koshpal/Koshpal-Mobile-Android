package com.koshpal_android.koshpalapp

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.multidex.MultiDex
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.koshpal_android.koshpalapp.service.TransactionSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class Application : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Initialize multidex to ensure all classes are available
        MultiDex.install(this)
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Schedule periodic background sync for transactions
        try {
            TransactionSyncScheduler.schedulePeriodicSync(this)
            Log.d("Application", "✅ Background transaction sync scheduled")
        } catch (e: Exception) {
            Log.e("Application", "❌ Failed to schedule background sync: ${e.message}", e)
        }
        
        // Note: SmsClassifier is initialized on-demand when SMS is received
        // This avoids loading the ML model at app startup, improving launch time
        // The model will be loaded automatically when TransactionSMSReceiver processes SMS
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
}