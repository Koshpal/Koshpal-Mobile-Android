package com.koshpal_android.koshpalapp.ui.transactions

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.ItemTransactionBinding
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit,
    private val onTransactionDelete: (Transaction, Int) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    private val dateFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
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
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.apply {
                tvMerchant.text = transaction.merchant
                tvTimestamp.text = dateFormatter.format(Date(transaction.timestamp))

                // Load and display category info
                loadCategoryInfo(transaction.categoryId)

                // Set amount color and prefix based on transaction type
                val amountColor = if (transaction.type == TransactionType.CREDIT) {
                    ContextCompat.getColor(root.context, R.color.success)
                } else {
                    ContextCompat.getColor(root.context, R.color.error)
                }
                tvAmount.setTextColor(amountColor)

                // Set amount with prefix
                tvAmount.text = if (transaction.type == TransactionType.CREDIT) {
                    "+₹${String.format("%.2f", transaction.amount)}"
                } else {
                    "-₹${String.format("%.2f", transaction.amount)}"
                }

                // Set transaction type text
                tvTransactionType.text = if (transaction.type == TransactionType.CREDIT) {
                    "Income"
                } else {
                    "Expense"
                }

                // Show/hide starred icon
                ivStarred.visibility = if (transaction.isStarred) {
                    View.VISIBLE
                } else {
                    View.GONE
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
                    binding.tvCategory.text = it.name
                    binding.ivCategoryIcon.setImageResource(it.icon)
                    
                    // Set category icon background color
                    try {
                        val color = Color.parseColor(it.color)
                        binding.cardCategoryIcon.setCardBackgroundColor(color)
                        binding.ivCategoryIcon.setColorFilter(Color.WHITE)
                    } catch (e: Exception) {
                        // Fallback to default colors
                        binding.cardCategoryIcon.setCardBackgroundColor(
                            ContextCompat.getColor(binding.root.context, R.color.primary_light)
                        )
                        binding.ivCategoryIcon.setColorFilter(
                            ContextCompat.getColor(binding.root.context, R.color.primary)
                        )
                    }
                } ?: run {
                    // Default category if not found
                    binding.tvCategory.text = "Uncategorized"
                    binding.ivCategoryIcon.setImageResource(R.drawable.ic_category_default)
                    binding.cardCategoryIcon.setCardBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.surface_gray)
                    )
                    binding.ivCategoryIcon.setColorFilter(
                        ContextCompat.getColor(binding.root.context, R.color.text_secondary)
                    )
                }
            } catch (e: Exception) {
                // Error handling
                binding.tvCategory.text = "Uncategorized"
                binding.ivCategoryIcon.setImageResource(R.drawable.ic_category_default)
            }
        }
    }

    fun deleteItem(position: Int) {
        val currentList = currentList.toMutableList()
        if (position >= 0 && position < currentList.size) {
            val deletedTransaction = currentList[position]
            currentList.removeAt(position)
            submitList(currentList)
            onTransactionDelete(deletedTransaction, position)
        }
    }

    fun restoreItem(transaction: Transaction, position: Int) {
        val currentList = currentList.toMutableList()
        currentList.add(position.coerceAtMost(currentList.size), transaction)
        submitList(currentList)
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
