package com.koshpal_android.koshpalapp.ui.transactions.dialog.compose

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.data.local.KoshpalDatabase
import com.koshpal_android.koshpalapp.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Example showing how to use the Compose Transaction Details Dialog
 * This is just an example - you would integrate this into your actual Fragments
 */
class TransactionDetailsDialogUsageExample : Fragment(R.layout.fragment_transactions) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Example of how you would show the dialog when a transaction is clicked
        // This would typically be in your click handler for a transaction item
        showTransactionDetails("your_transaction_id")
    }
    
    private fun showTransactionDetails(transactionId: String) {
        // Fetch transaction from database
        lifecycleScope.launch {
            try {
                // Get transaction from database
                val transaction = withContext(Dispatchers.IO) {
                    val database = KoshpalDatabase.getDatabase(requireContext())
                    database.transactionDao().getTransactionById(transactionId)
                }
                
                if (transaction != null) {
                    // Show the Compose dialog using the helper
                    TransactionDetailsDialogHelper.showFromFragment(
                        fragment = this@TransactionDetailsDialogUsageExample,
                        transaction = transaction,
                        onSave = { updatedTransaction ->
                            // Handle the updated transaction
                            saveUpdatedTransaction(updatedTransaction)
                        }
                    )
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Transaction not found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error loading transaction: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun saveUpdatedTransaction(transaction: Transaction) {
        lifecycleScope.launch {
            try {
                // Save to database
                withContext(Dispatchers.IO) {
                    val database = KoshpalDatabase.getDatabase(requireContext())
                    database.transactionDao().updateTransaction(transaction)
                }
                
                Toast.makeText(
                    requireContext(),
                    "Transaction updated successfully",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Refresh your UI/data if needed
                // refreshTransactions()
                
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error updating transaction: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
