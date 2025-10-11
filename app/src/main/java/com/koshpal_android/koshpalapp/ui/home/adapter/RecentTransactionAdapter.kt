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
                
                // Set category icon and background color
                val categoryInfo = getCategoryInfo(transaction.categoryId)
                ivTransactionIcon.setImageResource(categoryInfo.icon)
                cardCategoryIcon.setCardBackgroundColor(ContextCompat.getColor(root.context, categoryInfo.backgroundColor))
                
                // Add visual indicator only for truly uncategorized transactions (empty categoryId)
                if (transaction.categoryId.isEmpty()) {
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
        
        private fun getCategoryInfo(categoryId: String?): CategoryInfo {
            return when (categoryId) {
                "food" -> CategoryInfo(R.drawable.ic_food_category, R.color.success)
                "grocery" -> CategoryInfo(R.drawable.ic_food_category, R.color.primary)
                "transport" -> CategoryInfo(R.drawable.ic_transport_category, R.color.warning)
                "bills" -> CategoryInfo(R.drawable.ic_category_default, R.color.error)
                "education" -> CategoryInfo(R.drawable.ic_info, R.color.secondary)
                "entertainment" -> CategoryInfo(R.drawable.ic_category_default, R.color.secondary_light)
                "healthcare" -> CategoryInfo(R.drawable.ic_category_default, R.color.success_light)
                "shopping" -> CategoryInfo(R.drawable.ic_shopping_category, R.color.primary_light)
                "salary" -> CategoryInfo(R.drawable.ic_trending_up, R.color.success)
                "home" -> CategoryInfo(R.drawable.ic_home_category, R.color.success)
                "trips" -> CategoryInfo(R.drawable.ic_trips_category, R.color.secondary)
                "others" -> CategoryInfo(R.drawable.ic_more_vert, R.color.text_secondary)
                else -> CategoryInfo(R.drawable.ic_more_vert, R.color.text_secondary)
            }
        }
        
    }
    
    private data class CategoryInfo(
        val icon: Int,
        val backgroundColor: Int
    )
}

private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
    override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem == newItem
    }
}
