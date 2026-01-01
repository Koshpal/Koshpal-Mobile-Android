package com.koshpal_android.koshpalapp.ui.update

import android.app.Activity
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.utils.UpdateInfo
import com.koshpal_android.koshpalapp.utils.UpdateManager
import com.google.android.material.button.MaterialButton

class UpdateDialog : DialogFragment() {
    
    private var updateInfo: UpdateInfo? = null
    private var isForceUpdate: Boolean = false
    private lateinit var updateManager: UpdateManager
    private var isDownloading = false
    
    companion object {
        private const val ARG_UPDATE_INFO = "updateInfo"
        
        fun newInstance(updateInfo: UpdateInfo): UpdateDialog {
            return UpdateDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_UPDATE_INFO, updateInfo)
                }
            }
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        updateInfo = arguments?.getParcelable(ARG_UPDATE_INFO)
        isForceUpdate = updateInfo?.isForceUpdate ?: false
        
        // If no update info provided, dismiss
        if (updateInfo == null) {
            return super.onCreateDialog(savedInstanceState)
        }
        
        updateManager = UpdateManager(requireContext())
        
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_update_available, null)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(!isForceUpdate)
        dialog.setCanceledOnTouchOutside(!isForceUpdate)
        
        val btnCloseUpdate = dialogView.findViewById<ImageButton>(R.id.btnCloseUpdate)
        val btnUpdateLater = dialogView.findViewById<MaterialButton>(R.id.btnUpdateLater)
        val btnUpdateNow = dialogView.findViewById<MaterialButton>(R.id.btnUpdateNow)
        val tvVersionInfo = dialogView.findViewById<android.widget.TextView>(R.id.tvVersionInfo)
        val tvReleaseNotes = dialogView.findViewById<android.widget.TextView>(R.id.tvReleaseNotes)
        val progressUpdate = dialogView.findViewById<ProgressBar>(R.id.progressUpdate)
        
        updateInfo?.let { info ->
            val (currentVersionCode, currentVersionName) = updateManager.getCurrentVersion()
            tvVersionInfo.text = "New version ${info.versionName} is available\nCurrent version: $currentVersionName"
            
            if (info.releaseNotes != null && info.releaseNotes.isNotEmpty()) {
                tvReleaseNotes.text = "What's new:\n${info.releaseNotes}"
                tvReleaseNotes.visibility = View.VISIBLE
            }
            
            // Hide close button and "Later" button for force updates
            if (isForceUpdate) {
                btnCloseUpdate.visibility = View.GONE
                btnUpdateLater.visibility = View.GONE
            } else {
                btnCloseUpdate.visibility = View.VISIBLE
                btnUpdateLater.visibility = View.VISIBLE
            }
        }
        
        btnCloseUpdate.setOnClickListener {
            if (!isForceUpdate) {
                dialog.dismiss()
            }
        }
        
        btnUpdateLater.setOnClickListener {
            if (!isForceUpdate) {
                dialog.dismiss()
            }
        }
        
        btnUpdateNow.setOnClickListener {
            if (!isDownloading) {
                startDownload(dialogView, progressUpdate, btnUpdateNow)
            }
        }
        
        return dialog
    }
    
    private fun startDownload(
        dialogView: View,
        progressBar: ProgressBar,
        updateButton: MaterialButton
    ) {
        updateInfo?.let { info ->
            isDownloading = true
            progressBar.visibility = View.VISIBLE
            updateButton.text = "Downloading..."
            updateButton.isEnabled = false
            
            // Download is not a suspend function, so we can call it directly
            try {
                updateManager.downloadUpdate(info.downloadUrl,
                    onProgress = { progress ->
                        progressBar.progress = progress
                    },
                    onComplete = { apkUri ->
                        isDownloading = false
                        progressBar.visibility = View.GONE
                        
                        if (apkUri != null) {
                            updateButton.text = "Install"
                            updateButton.isEnabled = true
                            updateButton.setOnClickListener {
                                updateManager.installApk(apkUri, requireActivity())
                                dismiss()
                            }
                        } else {
                            // For now, show a message that download will be handled by system
                            Toast.makeText(
                                requireContext(),
                                "Download started. Check notifications for progress.",
                                Toast.LENGTH_LONG
                            ).show()
                            updateButton.text = "UPDATE NOW"
                            updateButton.isEnabled = true
                        }
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("UpdateDialog", "❌ Error downloading update: ${e.message}", e)
                isDownloading = false
                progressBar.visibility = View.GONE
                updateButton.text = "UPDATE NOW"
                updateButton.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    "Failed to download update. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

