package com.koshpal_android.koshpalapp.ui.categories.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.databinding.ItemCategorySpendingBinding
import com.koshpal_android.koshpalapp.model.CategorySpending
import com.koshpal_android.koshpalapp.model.TransactionCategory

class CategorySpendingAdapter(
    private val onSetBudgetClick: (CategorySpending) -> Unit
) : ListAdapter<CategorySpending, CategorySpendingAdapter.CategorySpendingViewHolder>(CategorySpendingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategorySpendingViewHolder {
        val binding = ItemCategorySpendingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategorySpendingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategorySpendingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategorySpendingViewHolder(
        private val binding: ItemCategorySpendingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(categorySpending: CategorySpending) {
            binding.apply {
                // Get category details from default categories
                val category = getCategoryById(categorySpending.categoryId)
                tvCategoryName.text = category?.name ?: "Unknown Category"
                tvAmount.text = "₹${String.format("%.0f", categorySpending.totalAmount)}"
                
                // For now, show "1 Spend" as placeholder - can be enhanced later
                tvTransactionCount.text = "1 Spend"

                // Set category icon and color based on category
                if (category != null) {
                    ivCategoryIcon.setImageResource(category.icon)
                    try {
                        val color = Color.parseColor(category.color)
                        cardIcon.setCardBackgroundColor(color)
                        ivCategoryIcon.setColorFilter(Color.WHITE)
                    } catch (e: Exception) {
                        // Fallback to default colors
                        cardIcon.setCardBackgroundColor(
                            binding.root.context.getColor(com.koshpal_android.koshpalapp.R.color.primary_light)
                        )
                        ivCategoryIcon.setColorFilter(
                            binding.root.context.getColor(com.koshpal_android.koshpalapp.R.color.primary)
                        )
                    }
                }

                tvSetBudget.setOnClickListener {
                    onSetBudgetClick(categorySpending)
                }
            }
        }

        private fun getCategoryById(categoryId: String): TransactionCategory? {
            return TransactionCategory.getDefaultCategories().find { it.id == categoryId }
        }
    }

    private class CategorySpendingDiffCallback : DiffUtil.ItemCallback<CategorySpending>() {
        override fun areItemsTheSame(oldItem: CategorySpending, newItem: CategorySpending): Boolean {
            return oldItem.categoryId == newItem.categoryId
        }

        override fun areContentsTheSame(oldItem: CategorySpending, newItem: CategorySpending): Boolean {
            return oldItem == newItem
        }
    }
}
