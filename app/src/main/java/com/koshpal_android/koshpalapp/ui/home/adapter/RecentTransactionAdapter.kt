package com.koshpal_android.koshpalapp.ui.home.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.ItemRecentTransactionBinding
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.model.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class RecentTransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit
) : ListAdapter<Transaction, RecentTransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val dateFormatter = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemRecentTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemRecentTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.apply {
                tvMerchantName.text = transaction.merchant ?: "Unknown Merchant"
                tvTransactionDate.text = dateFormatter.format(Date(transaction.timestamp))
                
                // Format amount based on transaction type
                val formattedAmount = currencyFormatter.format(transaction.amount)
                tvAmount.text = if (transaction.type == TransactionType.DEBIT) {
                    "-$formattedAmount"
                } else {
                    "+$formattedAmount"
                }
                
                // Set amount color based on type
                val amountColor = if (transaction.type == TransactionType.DEBIT) {
                    ContextCompat.getColor(root.context, R.color.error)
                } else {
                    ContextCompat.getColor(root.context, R.color.success)
                }
                tvAmount.setTextColor(amountColor)
                
                // Load and display category info using TransactionCategory system
                loadCategoryInfo(transaction.categoryId)
                
                // Set payment method badge icon (Cash or Online)
                setPaymentMethodBadge(transaction)
                
                // Add visual indicator only for truly uncategorized transactions (empty categoryId)
                if (transaction.categoryId.isNullOrEmpty()) {
                    tvMerchantName.text = "${transaction.merchant ?: "Unknown Merchant"} • Tap to categorize"
                } else {
                    // All transactions with any categoryId (including "others") are considered categorized
                    tvMerchantName.text = transaction.merchant ?: "Unknown Merchant"
                }
                
                root.setOnClickListener {
                    onTransactionClick(transaction)
                }
            }
        }
        
        private fun loadCategoryInfo(categoryId: String?) {
            try {
                val categories = TransactionCategory.getDefaultCategories()
                val category = categories.find { it.id == categoryId }
                
                category?.let {
                    // Set category icon
                    binding.ivTransactionIcon.setImageResource(it.icon)
                    
                    // Set category icon background color
                    try {
                        val color = Color.parseColor(it.color)
                        binding.cardCategoryIcon.setCardBackgroundColor(color)
                        binding.ivTransactionIcon.setColorFilter(Color.WHITE)
                    } catch (e: Exception) {
                        // Fallback to default colors
                        binding.cardCategoryIcon.setCardBackgroundColor(
                            ContextCompat.getColor(binding.root.context, R.color.primary_light)
                        )
                        binding.ivTransactionIcon.setColorFilter(
                            ContextCompat.getColor(binding.root.context, R.color.primary)
                        )
                    }
                } ?: run {
                    // Default category if not found
                    binding.ivTransactionIcon.setImageResource(R.drawable.ic_category_default)
                    binding.cardCategoryIcon.setCardBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.surface_gray)
                    )
                    binding.ivTransactionIcon.setColorFilter(
                        ContextCompat.getColor(binding.root.context, R.color.text_secondary)
                    )
                }
            } catch (e: Exception) {
                // Error handling
                binding.ivTransactionIcon.setImageResource(R.drawable.ic_category_default)
                binding.cardCategoryIcon.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.surface_gray)
                )
            }
        }
        
        private fun setPaymentMethodBadge(transaction: Transaction) {
            try {
                // Determine if transaction is cash or online:
                // - If smsId is NOT null → It's from SMS (scanned/online) → Show online icon
                // - If smsId is null → It's manually added (cash) → Show cash icon
                val isCash = transaction.smsId == null
                
                // Set the payment method icon
                val paymentMethodIcon = if (isCash) {
                    R.drawable.ic_cash  // Cash icon for manually added transactions
                } else {
                    R.drawable.ic_online  // Online/Card icon for SMS scanned transactions
                }
                
                binding.ivPaymentMethodIcon.setImageResource(paymentMethodIcon)
                
                // Optional: Set tint color based on payment method
                val tintColor = if (isCash) {
                    ContextCompat.getColor(binding.root.context, R.color.success)  // Green for cash
                } else {
                    ContextCompat.getColor(binding.root.context, R.color.primary)  // Blue for online
                }
                binding.ivPaymentMethodIcon.setColorFilter(tintColor)
                
            } catch (e: Exception) {
                // Error handling - hide badge on error
                binding.cardPaymentMethodBadge.visibility = android.view.View.GONE
            }
        }
        
    }
}

private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
    override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem == newItem
    }
}
