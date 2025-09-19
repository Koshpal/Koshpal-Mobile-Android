package com.koshpal_android.koshpalapp.ui.dashboard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.databinding.ItemCategoryBreakdownBinding
import com.koshpal_android.koshpalapp.model.CategorySpendingData

class CategoryBreakdownAdapter : ListAdapter<CategorySpendingData, CategoryBreakdownAdapter.CategoryViewHolder>(
    CategoryDiffCallback()
) {
    
    private var totalAmount: Double = 0.0
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBreakdownBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    override fun submitList(list: List<CategorySpendingData>?) {
        totalAmount = list?.sumOf { it.amount } ?: 0.0
        super.submitList(list)
    }
    
    inner class CategoryViewHolder(
        private val binding: ItemCategoryBreakdownBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(category: CategorySpendingData) {
            binding.apply {
                tvCategoryName.text = category.categoryName
                tvAmount.text = category.getFormattedAmount()
                tvPercentage.text = category.getFormattedPercentage(totalAmount)
                tvTransactionCount.text = if (category.transactionCount == 1) {
                    "1 transaction"
                } else {
                    "${category.transactionCount} transactions"
                }
                
                // Set category icon
                ivCategoryIcon.setImageResource(category.icon)
                
                // Set category color indicator
                try {
                    val color = Color.parseColor(category.color)
                    val drawable = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(color)
                    }
                    viewCategoryColor.background = drawable
                } catch (e: IllegalArgumentException) {
                    // Fallback to default color
                    val drawable = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#607D8B"))
                    }
                    viewCategoryColor.background = drawable
                }
            }
        }
    }
    
    private class CategoryDiffCallback : DiffUtil.ItemCallback<CategorySpendingData>() {
        override fun areItemsTheSame(
            oldItem: CategorySpendingData,
            newItem: CategorySpendingData
        ): Boolean {
            return oldItem.categoryId == newItem.categoryId
        }
        
        override fun areContentsTheSame(
            oldItem: CategorySpendingData,
            newItem: CategorySpendingData
        ): Boolean {
            return oldItem == newItem
        }
    }
}
