package com.koshpal_android.koshpalapp.ui.profile

import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.ActivityProfileBinding
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import com.koshpal_android.koshpalapp.ui.sync.SyncActivity
import com.koshpal_android.koshpalapp.ui.splash.SplashActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityProfileBinding
    private val profileViewModel: ProfileViewModel by viewModels()
    
    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var syncRepository: com.koshpal_android.koshpalapp.repository.SyncRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ProfileActivity", "🚀 ProfileActivity onCreate started")
        
        // Set status bar to black
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.black)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = ContextCompat.getColor(this, android.R.color.black)
        }
        
        // Make status bar icons light (white) for dark background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = window.decorView.systemUiVisibility
            flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
            window.decorView.systemUiVisibility = flags
        }
        
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        observeViewModel()
        loadUserInfo()

        // Initialize sync status
        updateSyncStatus()
    }
    
    private fun setupUI() {
        Log.d("ProfileActivity", "🔧 Setting up UI components")
        
        binding.apply {
            ivBack.setOnClickListener {
                Log.d("ProfileActivity", "⬅️ Back button clicked")
                finish()
            }
            
            btnLogout.setOnClickListener {
                Log.d("ProfileActivity", "🚪 Logout button clicked")
                logout()
            }
            
            // Terms and Conditions
            btnTerms.setOnClickListener {
                Log.d("ProfileActivity", "📄 Terms and Conditions clicked")
                openTermsAndConditions()
            }
            
            // Website
            btnWebsite.setOnClickListener {
                Log.d("ProfileActivity", "🌐 Website clicked")
                openWebsite()
            }
            
            // Export Statement
            btnExportStatement.setOnClickListener {
                Log.d("ProfileActivity", "📄 Export Statement clicked")
                showExportStatementDialog()
            }
            
            // Report Undetected SMS
            btnReportSms.setOnClickListener {
                Log.d("ProfileActivity", "📧 Report Undetected SMS clicked")
                reportUndetectedSMS()
            }

            // Sync All Transactions
            binding.btnSyncTransactions.setOnClickListener {
                Log.d("ProfileActivity", "🔄 Sync All Transactions clicked")
                performSync()
            }
        }
        
        Log.d("ProfileActivity", "✅ UI setup completed")
    }

    private fun performSync() {
        Log.d("ProfileActivity", "🔄 Starting manual transaction sync")

        // Check if already syncing
        val currentStatus = profileViewModel.syncStatus.value
        if (currentStatus == ProfileViewModel.SyncStatus.SYNCING) {
            Log.d("ProfileActivity", "⚠️ Sync already in progress, ignoring")
            return
        }

        // Check if user is logged in
        if (!userPreferences.isLoggedIn()) {
            Log.w("ProfileActivity", "🚪 User not logged in, cannot sync")
            Toast.makeText(this, "Please login to sync transactions", Toast.LENGTH_SHORT).show()
            return
        }

        // Start sync
        profileViewModel.performInitialSync()
        updateSyncUI(isSyncing = true)
    }

    private fun updateSyncStatus() {
        lifecycleScope.launch {
            try {
                val unsyncedCount = syncRepository.getUnsyncedTransactionCount()
                val lastSyncTime = profileViewModel.lastSyncTime.value ?: 0L

                val statusText = when {
                    unsyncedCount > 0 -> "$unsyncedCount pending transactions"
                    lastSyncTime > 0 -> "Last sync: ${formatLastSyncTime(lastSyncTime)}"
                    else -> "All transactions synced"
                }

                binding.tvSyncStatus.text = statusText

                // Update status icon
                if (unsyncedCount == 0 && lastSyncTime > 0) {
                    binding.ivSyncStatus.visibility = View.VISIBLE
                    binding.ivSyncStatus.setColorFilter(ContextCompat.getColor(this@ProfileActivity, R.color.success))
                } else {
                    binding.ivSyncStatus.visibility = View.GONE
                }

            } catch (e: Exception) {
                Log.e("ProfileActivity", "❌ Error updating sync status: ${e.message}")
                binding.tvSyncStatus.text = "Unable to check sync status"
            }
        }
    }

    private fun updateSyncUI(isSyncing: Boolean) {
        if (isSyncing) {
            binding.progressSync.visibility = View.VISIBLE
            binding.btnSyncTransactions.isEnabled = false
            binding.btnSyncTransactions.alpha = 0.6f
        } else {
            binding.progressSync.visibility = View.GONE
            binding.btnSyncTransactions.isEnabled = true
            binding.btnSyncTransactions.alpha = 1.0f
        }
    }

    private fun formatLastSyncTime(timestamp: Long): String {
        return try {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            when {
                diff < 60_000 -> "Just now" // Less than 1 minute
                diff < 3_600_000 -> "${diff / 60_000}m ago" // Less than 1 hour
                diff < 86_400_000 -> "${diff / 3_600_000}h ago" // Less than 1 day
                else -> "${diff / 86_400_000}d ago" // Days ago
            }
        } catch (e: Exception) {
            "Recently"
        }
    }
    
    private fun openTermsAndConditions() {
        try {
            // Open Terms and Conditions URL in browser
            val url = "https://koshpal.com/terms" // Update with actual URL
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("ProfileActivity", "❌ Error opening Terms and Conditions: ${e.message}", e)
            Toast.makeText(this, "Unable to open Terms and Conditions", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openWebsite() {
        try {
            // Open website URL in browser
            val url = "https://koshpal.com" // Update with actual URL
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("ProfileActivity", "❌ Error opening website: ${e.message}", e)
            Toast.makeText(this, "Unable to open website", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun reportUndetectedSMS() {
        try {
            Log.d("ProfileActivity", "📧 Opening email to report undetected SMS")
            
            // Check if Gmail is installed
            val pm = packageManager
            val gmailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                setPackage("com.google.android.gm")
            }
            val isGmailInstalled = pm.queryIntentActivities(gmailIntent, 0).isNotEmpty()
            
            if (isGmailInstalled) {
                // Open Gmail directly
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:koshpal@koshpal.com")
                    setPackage("com.google.android.gm")
                    putExtra(Intent.EXTRA_SUBJECT, "Report undetected sms")
                }
                startActivity(emailIntent)
                Toast.makeText(this, "Opening Gmail...", Toast.LENGTH_SHORT).show()
            } else {
                // Fallback: Open email chooser with any available email app
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:koshpal@koshpal.com")
                    putExtra(Intent.EXTRA_SUBJECT, "Report undetected sms")
                }
                startActivity(Intent.createChooser(emailIntent, "Send Email via"))
            }
        } catch (e: Exception) {
            Log.e("ProfileActivity", "❌ Error opening email: ${e.message}", e)
            Toast.makeText(this, "Unable to open email client", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun observeViewModel() {
        Log.d("ProfileActivity", "👀 Setting up ViewModel observers")
        
        profileViewModel.apply {
            // Observe sync status
            syncStatus.observe(this@ProfileActivity) { status ->
                Log.d("ProfileActivity", "🔄 Sync status: $status")
                val inProgress = status == ProfileViewModel.SyncStatus.SYNCING

                // Update sync UI
                updateSyncUI(inProgress)

                binding.apply {
                    progressBar.visibility = if (inProgress) View.VISIBLE else View.GONE
                }

                // Show toast on completion
                when (status) {
                    ProfileViewModel.SyncStatus.SUCCESS -> {
                        Toast.makeText(this@ProfileActivity, "Sync completed successfully!", Toast.LENGTH_SHORT).show()
                        updateSyncStatus() // Refresh status after successful sync
                    }
                    ProfileViewModel.SyncStatus.ERROR -> {
                        val error = lastSyncError.value ?: "Unknown error"
                        Toast.makeText(this@ProfileActivity, "Sync failed: $error", Toast.LENGTH_LONG).show()
                        updateSyncStatus() // Refresh status after failed sync
                    }
                    else -> {} // IDLE or SYNCING
                }
            }
            
            // Observe total synced count
            totalSyncedCount.observe(this@ProfileActivity) { count ->
                Log.d("ProfileActivity", "📊 Total synced count: $count")
                updateSyncStatus()
            }
        }
        
        Log.d("ProfileActivity", "✅ ViewModel observers setup completed")
    }
    
    private fun loadUserInfo() {
        Log.d("ProfileActivity", "👤 Loading user information")
        
        try {
            // Use static employee ID (no login required)
            val staticEmployeeId = "68ee28ce2f3fd392ea436576"
            val email = userPreferences.getEmail()?.ifEmpty { "koshpal.user@app.com" } ?: "koshpal.user@app.com"
            val isLoggedIn = userPreferences.isLoggedIn()
            val isSyncCompleted = userPreferences.isInitialSyncCompleted()
            
            Log.d("ProfileActivity", "📊 User info - Email: $email, EmployeeId: $staticEmployeeId, LoggedIn: $isLoggedIn, SyncCompleted: $isSyncCompleted")
            
            binding.apply {
                tvPhone.text = "+919307879687"
                tvEmail.text = email
                tvVersion.text = "Version 8.8.8v888r59f163d126rdt3"
            }
        } catch (e: Exception) {
            Log.e("ProfileActivity", "❌ Error loading user info: ${e.message}", e)
            Toast.makeText(this, "Error loading user info: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    
    private suspend fun getLast6MonthsTransactions(): List<Transaction> {
        val allTransactions = transactionRepository.getAllTransactionsOnce()
        
        // Calculate date 6 months ago
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -6)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val sixMonthsAgo = calendar.timeInMillis
        
        // Filter transactions from last 6 months
        val filteredTransactions = allTransactions.filter { it.date >= sixMonthsAgo }
        
        Log.d("ProfileActivity", "📅 Filtered ${filteredTransactions.size} transactions from last 6 months out of ${allTransactions.size} total")
        return filteredTransactions
    }
    
    private fun logout() {
        Log.d("ProfileActivity", "🚪 Logging out user")
        try {
            // Clear user preferences
            userPreferences.setLoggedIn(false)
            userPreferences.setInitialSyncCompleted(false)
            userPreferences.saveUserId("")
            userPreferences.saveEmail("")
            userPreferences.saveUserToken("")
            
            Log.d("ProfileActivity", "✅ User logged out successfully")
            
            // Navigate to splash screen
            val intent = Intent(this, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("ProfileActivity", "❌ Error during logout: ${e.message}", e)
            Toast.makeText(this, "Error during logout: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showExportStatementDialog() {
        val dialog = MaterialAlertDialogBuilder(this, R.style.Theme_KoshpalApp_DarkDialog)
            .setTitle("Export Statement")
            .setMessage("Choose how you want to export your transaction statement")
            .setPositiveButton("DOWNLOAD") { _, _ ->
                lifecycleScope.launch {
                    generateAndDownloadPDF()
                }
            }
            .setNegativeButton("SHARE") { _, _ ->
                lifecycleScope.launch {
                    generateAndSharePDF()
                }
            }
            .setNeutralButton("CANCEL", null)
            .create()
        
        dialog.setOnShowListener {
            // Style dialog window background
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(
                    ContextCompat.getColor(this, R.color.background_dark)
                )
            )
            
            // Style buttons to match dark theme with blue color
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.primary))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.primary))
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(ContextCompat.getColor(this, R.color.primary))
            
            // Style title and message text
            val titleView = dialog.findViewById<android.widget.TextView>(android.R.id.title)
            titleView?.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            
            val messageView = dialog.findViewById<android.widget.TextView>(android.R.id.message)
            messageView?.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        }
        
        dialog.show()
    }
    
    private suspend fun generateAndDownloadPDF() {
        try {
            binding.progressBar.visibility = View.VISIBLE
            
            // Get last 6 months of transactions
            val transactions = getLast6MonthsTransactions()
            Log.d("ProfileActivity", "📊 Generating PDF for ${transactions.size} transactions (last 6 months)")
            
            if (transactions.isEmpty()) {
                Toast.makeText(this, "No transactions to export", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                return
            }
            
            // Generate PDF
            val pdfFile = generatePDF(transactions)
            
            if (pdfFile != null && pdfFile.exists()) {
                // Try to save to public Downloads folder first
                val downloadsDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // For Android 10+, use app-specific directory
                    getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                } else {
                    // For older versions, try public Downloads
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                
                if (downloadsDir != null && !downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                
                val fileName = "Koshpal_Statement_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
                val destinationFile = File(downloadsDir ?: cacheDir, fileName)
                
                try {
                    pdfFile.copyTo(destinationFile, overwrite = true)
                    pdfFile.delete()
                    
                    // Notify media scanner for older Android versions
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                        mediaScanIntent.data = Uri.fromFile(destinationFile)
                        sendBroadcast(mediaScanIntent)
                    }
                    
                    Toast.makeText(this, "Statement downloaded successfully", Toast.LENGTH_SHORT).show()
                    Log.d("ProfileActivity", "✅ PDF saved to: ${destinationFile.absolutePath}")
                } catch (e: Exception) {
                    Log.e("ProfileActivity", "❌ Error saving PDF: ${e.message}", e)
                    Toast.makeText(this, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ProfileActivity", "❌ Error generating PDF: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            binding.progressBar.visibility = View.GONE
        }
    }
    
    private suspend fun generateAndSharePDF() {
        try {
            binding.progressBar.visibility = View.VISIBLE
            
            // Get last 6 months of transactions
            val transactions = getLast6MonthsTransactions()
            Log.d("ProfileActivity", "📊 Generating PDF for sharing ${transactions.size} transactions (last 6 months)")
            
            if (transactions.isEmpty()) {
                Toast.makeText(this, "No transactions to export", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                return
            }
            
            // Generate PDF
            val pdfFile = generatePDF(transactions)
            
            if (pdfFile != null && pdfFile.exists()) {
                try {
                    // Share the PDF
                    val uri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        pdfFile
                    )
                    
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "Koshpal Transaction Statement")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    val chooser = Intent.createChooser(shareIntent, "Share Statement")
                    chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(chooser)
                    Log.d("ProfileActivity", "✅ PDF shared successfully")
                } catch (e: Exception) {
                    Log.e("ProfileActivity", "❌ Error sharing PDF: ${e.message}", e)
                    Toast.makeText(this, "Error sharing file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ProfileActivity", "❌ Error sharing PDF: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            binding.progressBar.visibility = View.GONE
        }
    }
    
    private fun generatePDF(transactions: List<Transaction>): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            
            val paint = android.graphics.Paint()
            paint.color = android.graphics.Color.BLACK
            paint.textSize = 12f
            
            val titlePaint = android.graphics.Paint()
            titlePaint.color = android.graphics.Color.BLACK
            titlePaint.textSize = 18f
            titlePaint.isFakeBoldText = true
            
            val headerPaint = android.graphics.Paint()
            headerPaint.color = android.graphics.Color.BLACK
            headerPaint.textSize = 14f
            headerPaint.isFakeBoldText = true
            
            var y = 50f
            val margin = 50f
            val lineHeight = 20f
            var currentCanvas = canvas
            var currentPage = page
            
            // Title
            currentCanvas.drawText("Koshpal Transaction Statement", margin, y, titlePaint)
            y += lineHeight * 2
            
            // Date range
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val sortedTransactions = transactions.sortedBy { it.date }
            val startDate = if (sortedTransactions.isNotEmpty()) dateFormat.format(Date(sortedTransactions.first().date)) else "N/A"
            val endDate = if (sortedTransactions.isNotEmpty()) dateFormat.format(Date(sortedTransactions.last().date)) else "N/A"
            currentCanvas.drawText("Period: $startDate to $endDate", margin, y, paint)
            y += lineHeight * 2
            
            // Summary
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            val totalIncome = transactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
            val totalExpense = transactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
            val balance = totalIncome - totalExpense
            
            currentCanvas.drawText("Total Income: ${currencyFormat.format(totalIncome)}", margin, y, headerPaint)
            y += lineHeight
            currentCanvas.drawText("Total Expense: ${currencyFormat.format(totalExpense)}", margin, y, headerPaint)
            y += lineHeight
            currentCanvas.drawText("Balance: ${currencyFormat.format(balance)}", margin, y, headerPaint)
            y += lineHeight * 2
            
            // Table header
            currentCanvas.drawLine(margin, y, 545f, y, paint)
            y += lineHeight
            currentCanvas.drawText("Date", margin, y, headerPaint)
            currentCanvas.drawText("Description", margin + 100f, y, headerPaint)
            currentCanvas.drawText("Amount", margin + 400f, y, headerPaint)
            y += lineHeight
            currentCanvas.drawLine(margin, y, 545f, y, paint)
            y += lineHeight
            
            // Transactions
            val transactionDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            sortedTransactions.forEach { transaction ->
                if (y > 800) {
                    // Start new page if needed
                    pdfDocument.finishPage(currentPage)
                    val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                    val newPage = pdfDocument.startPage(newPageInfo)
                    val newCanvas = newPage.canvas
                    y = 50f
                    
                    // Draw header on new page
                    newCanvas.drawText("Koshpal Transaction Statement (continued)", margin, y, titlePaint)
                    y += lineHeight * 2
                    newCanvas.drawLine(margin, y, 545f, y, paint)
                    y += lineHeight
                    newCanvas.drawText("Date", margin, y, headerPaint)
                    newCanvas.drawText("Description", margin + 100f, y, headerPaint)
                    newCanvas.drawText("Amount", margin + 400f, y, headerPaint)
                    y += lineHeight
                    newCanvas.drawLine(margin, y, 545f, y, paint)
                    y += lineHeight
                    
                    currentCanvas = newCanvas
                    currentPage = newPage
                }
                
                val dateStr = transactionDateFormat.format(Date(transaction.date))
                val description = transaction.merchant.ifEmpty { transaction.description }
                val amount = if (transaction.type == TransactionType.CREDIT) {
                    "+${currencyFormat.format(transaction.amount)}"
                } else {
                    "-${currencyFormat.format(transaction.amount)}"
                }
                
                currentCanvas.drawText(dateStr, margin, y, paint)
                val descWidth = paint.measureText(description)
                if (descWidth > 280f) {
                    // Truncate long descriptions
                    val truncated = description.take(30) + "..."
                    currentCanvas.drawText(truncated, margin + 100f, y, paint)
                } else {
                    currentCanvas.drawText(description, margin + 100f, y, paint)
                }
                currentCanvas.drawText(amount, margin + 400f, y, paint)
                y += lineHeight
            }
            
            // Finish the last page
            pdfDocument.finishPage(currentPage)
            
            // Save to temporary file
            val tempFile = File(cacheDir, "statement_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(tempFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()
            
            if (tempFile.exists() && tempFile.length() > 0) {
                Log.d("ProfileActivity", "✅ PDF generated: ${tempFile.absolutePath}, size: ${tempFile.length()} bytes")
                tempFile
            } else {
                Log.e("ProfileActivity", "❌ PDF file is empty or doesn't exist")
                null
            }
        } catch (e: Exception) {
            Log.e("ProfileActivity", "❌ Error generating PDF: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
}
