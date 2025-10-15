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
import com.koshpal_android.koshpalapp.databinding.ItemTransactionShimmerBinding
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit,
    private val onTransactionDelete: (Transaction, Int) -> Unit
) : ListAdapter<TransactionListItem, RecyclerView.ViewHolder>(TransactionListItemDiffCallback()) {

    private val dateFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    companion object {
        private const val VIEW_TYPE_DATA = 0
        private const val VIEW_TYPE_LOADING = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TransactionListItem.Data -> VIEW_TYPE_DATA
            is TransactionListItem.Loading -> VIEW_TYPE_LOADING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DATA -> {
                val binding = ItemTransactionBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                TransactionViewHolder(binding)
            }
            VIEW_TYPE_LOADING -> {
                val binding = ItemTransactionShimmerBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ShimmerViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TransactionListItem.Data -> {
                (holder as TransactionViewHolder).bind(item.transaction)
            }
            is TransactionListItem.Loading -> {
                // Shimmer auto-animates, no binding needed
            }
        }
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            // Add fade-in animation when replacing shimmer with real data
            binding.root.alpha = 0f
            binding.root.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
            
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

    /**
     * ShimmerViewHolder for loading placeholders
     * Shimmer animation starts automatically via layout configuration
     */
    class ShimmerViewHolder(
        binding: ItemTransactionShimmerBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        // No binding needed - shimmer auto-animates
    }

    fun deleteItem(position: Int) {
        val currentList = currentList.toMutableList()
        if (position >= 0 && position < currentList.size) {
            val item = currentList[position]
            if (item is TransactionListItem.Data) {
                currentList.removeAt(position)
                submitList(currentList)
                onTransactionDelete(item.transaction, position)
            }
        }
    }

    fun restoreItem(transaction: Transaction, position: Int) {
        val currentList = currentList.toMutableList()
        currentList.add(position.coerceAtMost(currentList.size), TransactionListItem.Data(transaction))
        submitList(currentList)
    }

    private class TransactionListItemDiffCallback : DiffUtil.ItemCallback<TransactionListItem>() {
        override fun areItemsTheSame(oldItem: TransactionListItem, newItem: TransactionListItem): Boolean {
            return when {
                oldItem is TransactionListItem.Data && newItem is TransactionListItem.Data -> {
                    oldItem.transaction.id == newItem.transaction.id
                }
                oldItem is TransactionListItem.Loading && newItem is TransactionListItem.Loading -> {
                    oldItem.id == newItem.id
                }
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: TransactionListItem, newItem: TransactionListItem): Boolean {
            return when {
                oldItem is TransactionListItem.Data && newItem is TransactionListItem.Data -> {
                    oldItem.transaction == newItem.transaction
                }
                oldItem is TransactionListItem.Loading && newItem is TransactionListItem.Loading -> {
                    true // All loading items are the same
                }
                else -> false
            }
        }
    }
}
