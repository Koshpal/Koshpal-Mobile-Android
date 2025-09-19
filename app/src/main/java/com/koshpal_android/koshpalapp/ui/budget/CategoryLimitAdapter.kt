package com.koshpal_android.koshpalapp.ui.budget

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.databinding.ItemCategoryLimitBinding

class CategoryLimitAdapter(
    private val onRemoveClick: (CategoryLimit) -> Unit
) : ListAdapter<CategoryLimit, CategoryLimitAdapter.CategoryLimitViewHolder>(CategoryLimitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryLimitViewHolder {
        val binding = ItemCategoryLimitBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryLimitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryLimitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryLimitViewHolder(
        private val binding: ItemCategoryLimitBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(categoryLimit: CategoryLimit) {
            binding.apply {
                tvCategoryName.text = categoryLimit.name
                ivCategoryIcon.setImageResource(categoryLimit.iconRes)
                
                if (categoryLimit.budgetLimit > 0) {
                    tvBudgetInfo.text = "Budget: ₹${String.format("%.0f", categoryLimit.budgetLimit)}"
                    tvSpentInfo.text = "You are ₹${String.format("%.0f", categoryLimit.budgetLimit - categoryLimit.spent)} under your limit"
                    tvLimitAmount.text = "₹${String.format("%.0f", categoryLimit.spent)}"
                    
                    val progressPercentage = if (categoryLimit.budgetLimit > 0) {
                        (categoryLimit.spent / categoryLimit.budgetLimit * 100).toInt()
                    } else 0
                    tvProgressPercentage.text = "$progressPercentage%"
                } else {
                    tvBudgetInfo.text = "No limit set"
                    tvSpentInfo.text = "Tap to set budget limit"
                    tvLimitAmount.text = "₹0"
                    tvProgressPercentage.text = "0%"
                }
                
                btnRemoveCategory.setOnClickListener {
                    onRemoveClick(categoryLimit)
                }
            }
        }
    }

    private class CategoryLimitDiffCallback : DiffUtil.ItemCallback<CategoryLimit>() {
        override fun areItemsTheSame(oldItem: CategoryLimit, newItem: CategoryLimit): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CategoryLimit, newItem: CategoryLimit): Boolean {
            return oldItem == newItem
        }
    }
}
