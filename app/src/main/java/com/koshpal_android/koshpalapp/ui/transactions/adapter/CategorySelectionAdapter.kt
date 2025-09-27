package com.koshpal_android.koshpalapp.ui.transactions.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.databinding.ItemCategorySelectionBinding
import com.koshpal_android.koshpalapp.model.TransactionCategory

class CategorySelectionAdapter(
    private val onCategoryClick: (TransactionCategory) -> Unit
) : ListAdapter<TransactionCategory, CategorySelectionAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategorySelectionBinding.inflate(
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
        private val binding: ItemCategorySelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: TransactionCategory) {
            binding.apply {
                tvCategoryName.text = category.name
                ivCategoryIcon.setImageResource(category.icon)
                
                // Set category color
                try {
                    val color = Color.parseColor(category.color)
                    cardIcon.setCardBackgroundColor(color)
                    // Make icon white for better contrast
                    ivCategoryIcon.setColorFilter(Color.WHITE)
                } catch (e: Exception) {
                    // Fallback to default colors if color parsing fails
                    cardIcon.setCardBackgroundColor(
                        binding.root.context.getColor(com.koshpal_android.koshpalapp.R.color.primary_light)
                    )
                    ivCategoryIcon.setColorFilter(
                        binding.root.context.getColor(com.koshpal_android.koshpalapp.R.color.primary)
                    )
                }

                root.setOnClickListener {
                    onCategoryClick(category)
                }
            }
        }
    }

    private class CategoryDiffCallback : DiffUtil.ItemCallback<TransactionCategory>() {
        override fun areItemsTheSame(oldItem: TransactionCategory, newItem: TransactionCategory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TransactionCategory, newItem: TransactionCategory): Boolean {
            return oldItem == newItem
        }
    }
}
