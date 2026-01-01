package com.koshpal_android.koshpalapp.ui.home.dialog.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.databinding.ItemCategorySelectionNewBinding
import com.koshpal_android.koshpalapp.model.TransactionCategory

class CategoryGridAdapter(
    private val onCategoryClick: (TransactionCategory) -> Unit
) : ListAdapter<TransactionCategory, CategoryGridAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategorySelectionNewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position), onCategoryClick)
    }

    class CategoryViewHolder(
        private val binding: ItemCategorySelectionNewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: TransactionCategory, onCategoryClick: (TransactionCategory) -> Unit) {
            binding.apply {
                // Set category name
                tvCategoryName.text = category.name

                // Set icon
                ivCategoryIcon.setImageResource(category.icon)

                // Set icon color based on category color
                try {
                    val color = Color.parseColor(category.color)
                    ivCategoryIcon.setColorFilter(color)
                } catch (e: Exception) {
                    // Use default color if parsing fails
                    ivCategoryIcon.setColorFilter(Color.parseColor("#607D8B"))
                }

                // Set click listener
                root.setOnClickListener {
                    onCategoryClick(category)
                }
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<TransactionCategory>() {
        override fun areItemsTheSame(oldItem: TransactionCategory, newItem: TransactionCategory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TransactionCategory, newItem: TransactionCategory): Boolean {
            return oldItem == newItem
        }
    }
}

