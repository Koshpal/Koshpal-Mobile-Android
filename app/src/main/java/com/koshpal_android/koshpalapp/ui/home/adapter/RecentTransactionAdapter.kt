package com.koshpal_android.koshpalapp.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.ItemRecentTransactionBinding
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class RecentTransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit
) : ListAdapter<Transaction, RecentTransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

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
                
                // Set category icon (you can enhance this with proper category mapping)
                ivTransactionIcon.setImageResource(getCategoryIcon(transaction.categoryId))
                
                // Add visual indicator only for truly uncategorized transactions (empty categoryId)
                if (transaction.categoryId.isEmpty()) {
                    tvMerchantName.text = "${transaction.merchant ?: "Unknown Merchant"} • Tap to categorize"
                    root.setBackgroundColor(ContextCompat.getColor(root.context, R.color.warning_light))
                } else {
                    // All transactions with any categoryId (including "others") are considered categorized
                    tvMerchantName.text = transaction.merchant ?: "Unknown Merchant"
                    root.setBackgroundColor(ContextCompat.getColor(root.context, android.R.color.transparent))
                }
                
                root.setOnClickListener {
                    onTransactionClick(transaction)
                }
            }
        }
        
        private fun getCategoryIcon(categoryId: String?): Int {
            return when (categoryId) {
                "food" -> R.drawable.ic_menu_eat
                "grocery" -> R.drawable.ic_menu_gallery
                "transport" -> R.drawable.ic_menu_directions
                "bills" -> R.drawable.ic_category_default
                "education" -> R.drawable.ic_info
                "entertainment" -> R.drawable.ic_category_default
                "healthcare" -> R.drawable.ic_category_default
                "shopping" -> R.drawable.ic_add
                "salary" -> R.drawable.ic_trending_up
                "others" -> R.drawable.ic_more_vert
                else -> R.drawable.ic_more_vert
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
