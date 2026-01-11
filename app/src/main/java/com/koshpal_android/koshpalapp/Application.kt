package com.koshpal_android.koshpalapp

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.multidex.MultiDex
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.ml.SmsClassifier
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

        // 🔄 FRESH INSTALL DETECTION: Clear database if this is a fresh install
        handleFreshInstallDatabaseCleanup()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Schedule periodic background sync for transactions
        try {
            TransactionSyncScheduler.schedulePeriodicSync(this)
            Log.d("Application", "✅ Background transaction sync scheduled")
        } catch (e: Exception) {
            Log.e("Application", "❌ Failed to schedule background sync: ${e.message}", e)
        }

        // ============================================
        // INTEGRATED ML MODULE: Pre-initialize SMS Classifier model
        // This ensures the TensorFlow Lite model is loaded and ready when SMS arrives
        // Improves responsiveness by avoiding model loading delay during SMS processing
        // ============================================
        try {
            Log.d("Application", "🤖 Pre-initializing SMS Classifier model...")
            val classifier = SmsClassifier(this)
            Log.d("Application", "✅ SMS Classifier instance created and initialized")
        } catch (e: Exception) {
            Log.e("Application", "❌ Failed to create SMS Classifier instance: ${e.message}", e)
        }
    }

    /**
     * Handle fresh install database cleanup
     * Clears SMS data and transactions when app is freshly installed
     */
    private fun handleFreshInstallDatabaseCleanup() {
        try {
            val userPreferences = UserPreferences(this)
            val currentVersionCode = getCurrentVersionCode()
            val storedVersionCode = userPreferences.getStoredVersionCode()

            // If stored version is 0 (first install) or different (update), clear database
            if (storedVersionCode == 0L || storedVersionCode != currentVersionCode) {
                Log.d("Application", "🔄 Fresh install detected - clearing database")

                // Clear the Room database
                val database = KoshpalDatabase.getDatabase(this)
                database.clearAllTables()
                Log.d("Application", "✅ Database cleared successfully")

                // Update stored version code
                userPreferences.setStoredVersionCode(currentVersionCode)
            } else {
                Log.d("Application", "✅ App already installed, keeping existing data")
            }
        } catch (e: Exception) {
            Log.e("Application", "❌ Error during fresh install cleanup: ${e.message}", e)
        }
    }

    private fun getCurrentVersionCode(): Long {
        return try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            Log.e("Application", "❌ Error getting version code: ${e.message}", e)
            1L // Default fallback
        }
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
}