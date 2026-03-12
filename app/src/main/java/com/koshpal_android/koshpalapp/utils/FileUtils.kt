package com.koshpal_android.koshpalapp.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object FileUtils {
    
    private const val ATTACHMENTS_DIR = "attachments"

    /**
     * Saves an image from a URI to the app's internal storage and returns the absolute path.
     * This is useful for persisting Photo Picker URIs that have temporary grants.
     */
    fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            
            if (inputStream == null) return null
            
            // Create attachments directory if it doesn't exist
            val directory = File(context.filesDir, ATTACHMENTS_DIR)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            // Generate a unique file name
            val fileName = "attachment_${UUID.randomUUID()}.jpg"
            val outputFile = File(directory, fileName)
            
            // Copy data to the new file
            FileOutputStream(outputFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            
            outputFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("FileUtils", "Failed to save image to internal storage: ${e.message}", e)
            null
        }
    }

    /**
     * Deletes a file from the internal storage.
     */
    fun deleteInternalFile(path: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            android.util.Log.e("FileUtils", "Failed to delete file: ${e.message}", e)
        }
    }
}
