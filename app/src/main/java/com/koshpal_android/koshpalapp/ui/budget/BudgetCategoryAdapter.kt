package com.koshpal_android.koshpalapp.ui.budget

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.databinding.ItemBudgetCategoryBinding

class BudgetCategoryAdapter(
    private val onCategoryClick: (BudgetCategory) -> Unit
) : ListAdapter<BudgetCategory, BudgetCategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemBudgetCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemBudgetCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: BudgetCategory) {
            binding.apply {
                tvCategoryName.text = category.name
                ivCategoryIcon.setImageResource(category.iconRes)
                
                // Show/hide selection indicator
                ivSelectionCheck.visibility = if (category.isSelected) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
                
                root.setOnClickListener {
                    onCategoryClick(category)
                }
            }
        }
    }

    private class CategoryDiffCallback : DiffUtil.ItemCallback<BudgetCategory>() {
        override fun areItemsTheSame(oldItem: BudgetCategory, newItem: BudgetCategory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BudgetCategory, newItem: BudgetCategory): Boolean {
            return oldItem == newItem
        }
    }
}
