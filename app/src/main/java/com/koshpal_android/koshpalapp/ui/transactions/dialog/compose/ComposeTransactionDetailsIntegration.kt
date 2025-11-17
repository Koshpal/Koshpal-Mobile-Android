package com.koshpal_android.koshpalapp.ui.transactions.dialog.compose

import androidx.fragment.app.Fragment
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.ui.transactions.dialog.TransactionDetailsDialog

/**
 * Integration utility that provides a drop-in replacement for the XML-based TransactionDetailsDialog
 * with the Compose implementation.
 */
object ComposeTransactionDetailsIntegration {
    
    /**
     * Creates a Compose-based transaction details dialog that mimics the API of the XML dialog
     */
    object TransactionDetailsDialog {
        /**
         * Factory method to create a new instance of the dialog, matching the XML dialog's API
         * @param transaction The transaction to display and potentially edit
         * @param onTransactionUpdated Callback when the transaction is updated
         */
        @JvmStatic
        fun newInstance(
            transaction: Transaction,
            onTransactionUpdated: (Transaction) -> Unit
        ): DialogWrapper {
            return DialogWrapper(transaction, onTransactionUpdated)
        }
        
        /**
         * Wrapper class that mimics the Fragment-like API of the original dialog
         */
        class DialogWrapper(
            private val transaction: Transaction,
            private val onTransactionUpdated: (Transaction) -> Unit
        ) {
            /**
             * Shows the dialog, mimicking the show() method of DialogFragment
             */
            fun show(fragmentManager: androidx.fragment.app.FragmentManager, tag: String) {
                try {
                    // Instead of using the Dialog wrapper approach which is causing issues,
                    // Let's revert to the original XML-based dialog for now to prevent crashes
                    val originalDialog = TransactionDetailsDialog.newInstance(
                        transaction, onTransactionUpdated
                    )
                    originalDialog.show(fragmentManager, tag)
                    
                    // Log that we're using the original dialog as a fallback
                    android.util.Log.d("TransactionDetailsDialog", "Using original XML dialog as fallback to prevent crashes")
                } catch (e: Exception) {
                    // Log the error
                    android.util.Log.e("TransactionDetailsDialog", "❌ Error showing dialog: ${e.message}", e)
                    // Show a toast to inform the user
                    try {
                        val context = fragmentManager.fragments.firstOrNull()?.requireContext()
                        if (context != null) {
                            android.widget.Toast.makeText(
                                context,
                                "Error showing transaction details",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e2: Exception) {
                        // Ignore any errors from the toast
                    }
                }
            }
        }
    }
}
