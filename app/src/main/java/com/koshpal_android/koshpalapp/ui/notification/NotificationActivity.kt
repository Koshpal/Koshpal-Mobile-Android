package com.koshpal_android.koshpalapp.ui.notification

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.ui.transactions.dialog.TransactionDetailsDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class NotificationActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_TRANSACTION_ID = "transaction_id"
        
        fun createIntent(context: Context, transactionId: String): Intent {
            return Intent(context, NotificationActivity::class.java).apply {
                putExtra(EXTRA_TRANSACTION_ID, transactionId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val transactionId = intent.getStringExtra(EXTRA_TRANSACTION_ID)
        if (transactionId == null) {
            Log.e("NotificationActivity", "❌ No transaction ID provided")
            finish()
            return
        }
        
        Log.d("NotificationActivity", "🔔 Opening notification activity for transaction: $transactionId")
        
        // Set up timeout to close activity if something goes wrong
        lifecycleScope.launch {
            delay(10000) // 10 seconds timeout
            if (!isFinishing) {
                Log.w("NotificationActivity", "⏰ Timeout reached, closing activity")
                finish()
            }
        }
        
        // Load transaction and show dialog
        loadTransactionAndShowDialog(transactionId)
    }
    
    private fun loadTransactionAndShowDialog(transactionId: String) {
        lifecycleScope.launch {
            try {
                val transaction = withContext(Dispatchers.IO) {
                    val database = KoshpalDatabase.getDatabase(this@NotificationActivity)
                    val transactionDao = database.transactionDao()
                    transactionDao.getTransactionById(transactionId)
                }
                
                if (transaction != null) {
                    Log.d("NotificationActivity", "✅ Found transaction: ${transaction.merchant} - ₹${transaction.amount}")
                    
                    // Show transaction dialog immediately
                    val dialog = TransactionDetailsDialog.newInstance(transaction) { updatedTransaction ->
                        Log.d("NotificationActivity", "📝 Transaction updated from notification dialog")
                        // Close this activity after transaction is updated
                        finish()
                    }
                    
                    // Set up dialog dismissal listener
                    dialog.dialog?.setOnDismissListener {
                        Log.d("NotificationActivity", "🔚 Dialog dismissed, closing activity")
                        finish()
                    }
                    
                    dialog.show(supportFragmentManager, "TransactionDetailsDialog")
                    
                } else {
                    Log.w("NotificationActivity", "⚠️ Transaction not found with ID: $transactionId")
                    showErrorAndFinish("Transaction not found")
                }
                
            } catch (e: Exception) {
                Log.e("NotificationActivity", "❌ Error loading transaction", e)
                showErrorAndFinish("Error loading transaction")
            }
        }
    }
    
    private fun showErrorAndFinish(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
        finish()
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
