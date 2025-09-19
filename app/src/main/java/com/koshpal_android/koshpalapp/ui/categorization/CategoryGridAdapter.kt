package com.koshpal_android.koshpalapp.ui.categorization

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.databinding.ItemCategoryGridBinding
import com.koshpal_android.koshpalapp.model.TransactionCategory

class CategoryGridAdapter(
    private val onCategoryClick: (TransactionCategory) -> Unit
) : ListAdapter<TransactionCategory, CategoryGridAdapter.CategoryViewHolder>(CategoryDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryGridBinding.inflate(
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
        private val binding: ItemCategoryGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(category: TransactionCategory) {
            binding.apply {
                tvCategoryName.text = category.name
                ivCategoryIcon.setImageResource(category.icon)
                
                // Set category color
                try {
                    val color = Color.parseColor(category.color)
                    cardIcon.setCardBackgroundColor(color)
                    
                    // Set icon tint to white for better contrast
                    ivCategoryIcon.setColorFilter(Color.WHITE)
                } catch (e: IllegalArgumentException) {
                    // Fallback to default color if parsing fails
                    cardIcon.setCardBackgroundColor(Color.parseColor("#607D8B"))
                    ivCategoryIcon.setColorFilter(Color.WHITE)
                }
                
                root.setOnClickListener {
                    onCategoryClick(category)
                }
                
                // Add ripple effect
                root.isClickable = true
                root.isFocusable = true
            }
        }
    }
    
    private class CategoryDiffCallback : DiffUtil.ItemCallback<TransactionCategory>() {
        override fun areItemsTheSame(
            oldItem: TransactionCategory,
            newItem: TransactionCategory
        ): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(
            oldItem: TransactionCategory,
            newItem: TransactionCategory
        ): Boolean {
            return oldItem == newItem
        }
    }
}
