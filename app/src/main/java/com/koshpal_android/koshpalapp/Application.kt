package com.koshpal_android.koshpalapp

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.multidex.MultiDex
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.koshpal_android.koshpalapp.ml.MobileBERTInference
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
        
        // Pre-initialize MobileBERT model (will load when first accessed)
        // This ensures model is ready when SMS arrives
        try {
            Log.d("Application", "🤖 Pre-initializing MobileBERT model...")
            val mlInference = MobileBERTInference.getInstance(this)
            // Model loads asynchronously in init block, so we just trigger initialization
            Log.d("Application", "✅ MobileBERT instance created (loading in background)")
        } catch (e: Exception) {
            Log.e("Application", "❌ Failed to create MobileBERT instance: ${e.message}", e)
        }
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
}