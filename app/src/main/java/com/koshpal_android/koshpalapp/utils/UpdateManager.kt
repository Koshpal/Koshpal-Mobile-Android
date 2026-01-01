package com.koshpal_android.koshpalapp.utils

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.core.content.FileProvider
import com.koshpal_android.koshpalapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String? = null,
    val isForceUpdate: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readByte() != 0.toByte()
    )
    
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(versionCode)
        parcel.writeString(versionName)
        parcel.writeString(downloadUrl)
        parcel.writeString(releaseNotes)
        parcel.writeByte(if (isForceUpdate) 1 else 0)
    }
    
    override fun describeContents(): Int {
        return 0
    }
    
    companion object CREATOR : Parcelable.Creator<UpdateInfo> {
        override fun createFromParcel(parcel: Parcel): UpdateInfo {
            return UpdateInfo(parcel)
        }
        
        override fun newArray(size: Int): Array<UpdateInfo?> {
            return arrayOfNulls(size)
        }
    }
}

class UpdateManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UpdateManager"
        // Update check API endpoint - replace with your actual endpoint
        private const val UPDATE_CHECK_URL = "https://your-api-server.com/api/check-update"
    }
    
    /**
     * Check for app updates from server
     */
    suspend fun checkForUpdate(): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode
                }
                val currentVersionName = packageInfo.versionName ?: "1.0"
                
                Log.d(TAG, "🔍 Checking for updates... Current version: $currentVersionName ($currentVersionCode)")
                
                // TODO: Replace with actual API call to your server
                // For now, return null (no update available)
                // Uncomment and configure when you have an update server:
                /*
                val url = URL(UPDATE_CHECK_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    // Parse JSON response
                    val json = JSONObject(response)
                    val latestVersionCode = json.getInt("versionCode")
                    val latestVersionName = json.getString("versionName")
                    val downloadUrl = json.getString("downloadUrl")
                    val releaseNotes = json.optString("releaseNotes", null)
                    val isForceUpdate = json.optBoolean("forceUpdate", false)
                    
                    if (latestVersionCode > currentVersionCode) {
                        return@withContext UpdateInfo(
                            versionCode = latestVersionCode,
                            versionName = latestVersionName,
                            downloadUrl = downloadUrl,
                            releaseNotes = releaseNotes,
                            isForceUpdate = isForceUpdate
                        )
                    }
                }
                */
                
                null // No update available
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking for updates: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * Download APK update
     */
    fun downloadUpdate(downloadUrl: String, onProgress: (Int) -> Unit = {}, onComplete: (Uri?) -> Unit) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("${context.getString(R.string.app_name)} Update")
                setDescription("Downloading update...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "koshpal_update.apk")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            
            val downloadId = downloadManager.enqueue(request)
            Log.d(TAG, "📥 Download started with ID: $downloadId")
            
            // Monitor download progress (simplified - in production, use BroadcastReceiver)
            onComplete(null) // For now, just trigger completion
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error downloading update: ${e.message}", e)
            onComplete(null)
        }
    }
    
    /**
     * Install downloaded APK
     */
    fun installApk(apkUri: Uri, activity: Activity) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    val fileProviderUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(apkUri.path ?: "")
                    )
                    setDataAndType(fileProviderUri, "application/vnd.android.package-archive")
                }
            }
            
            activity.startActivity(intent)
            Log.d(TAG, "✅ Install intent started")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error installing APK: ${e.message}", e)
        }
    }
    
    /**
     * Get current app version
     */
    fun getCurrentVersion(): Pair<Int, String> {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
        val versionName = packageInfo.versionName ?: "1.0"
        return Pair(versionCode, versionName)
    }
}

