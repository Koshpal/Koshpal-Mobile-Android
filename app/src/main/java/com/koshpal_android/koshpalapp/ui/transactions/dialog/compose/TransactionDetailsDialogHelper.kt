package com.koshpal_android.koshpalapp.ui.transactions.dialog.compose

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.Window
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.ui.theme.KoshpalTheme

/**
 * Helper class to show the Compose Transaction Details Dialog from traditional Android code
 */
object TransactionDetailsDialogHelper {
    
    /**
     * Show the transaction details dialog from a Fragment
     */
    fun showFromFragment(
        fragment: Fragment,
        transaction: Transaction,
        onSave: (Transaction) -> Unit
    ): Dialog {
        return showDialog(
            context = fragment.requireContext(),
            lifecycleOwner = fragment.viewLifecycleOwner,
            transaction = transaction,
            onSave = onSave
        )
    }
    
    /**
     * Show the transaction details dialog from an Activity
     */
    fun showFromActivity(
        activity: FragmentActivity,
        transaction: Transaction,
        onSave: (Transaction) -> Unit
    ): Dialog {
        return showDialog(
            context = activity,
            lifecycleOwner = activity,
            transaction = transaction,
            onSave = onSave
        )
    }
    
    /**
     * Core implementation to create and show the dialog
     */
    private fun showDialog(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        transaction: Transaction,
        onSave: (Transaction) -> Unit
    ): Dialog {
        try {
            val dialog = Dialog(context)
            
            // Configure dialog window
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            
            // Set the Compose content
            try {
                val composeView = ComposeView(context).apply {
                    setContent {
                        KoshpalTheme {
                            TransactionDetailsDialog(
                                transaction = transaction,
                                onDismiss = { 
                                    try {
                                        dialog.dismiss() 
                                    } catch (e: Exception) {
                                        android.util.Log.e("TransactionDetailsDialog", "❌ Error dismissing dialog: ${e.message}", e)
                                    }
                                },
                                onSave = { updatedTransaction ->
                                    try {
                                        onSave(updatedTransaction)
                                        dialog.dismiss()
                                    } catch (e: Exception) {
                                        android.util.Log.e("TransactionDetailsDialog", "❌ Error saving transaction: ${e.message}", e)
                                        try { dialog.dismiss() } catch (e2: Exception) { }
                                    }
                                }
                            )
                        }
                    }
                }
                
                // Set the content view and show the dialog
                dialog.setContentView(composeView)
                dialog.show()
                
                return dialog
            } catch (e: Exception) {
                android.util.Log.e("TransactionDetailsDialog", "❌ Error creating Compose content: ${e.message}", e)
                return dialog // Return dialog even though it's not shown
            }
        } catch (e: Exception) {
            android.util.Log.e("TransactionDetailsDialog", "❌ Error creating dialog: ${e.message}", e)
            // Create a dummy dialog to return
            return Dialog(context).apply { setCancelable(true) }
        }
    }
}
