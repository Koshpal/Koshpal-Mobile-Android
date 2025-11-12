package com.koshpal_android.koshpalapp.ui.categories.adapter

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.databinding.ItemSetBudgetCategoryBinding
import com.koshpal_android.koshpalapp.model.BudgetCategory
import com.koshpal_android.koshpalapp.ui.categories.compose.CategoryBudgetItem

class SetBudgetCategoryAdapter(
    private val onBudgetChanged: () -> Unit = {}
) : ListAdapter<CategoryBudgetItem, SetBudgetCategoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSetBudgetCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getCategoryBudgets(): List<CategoryBudgetItem> {
        return currentList.toList()
    }

    fun updateBudgetAmounts(budgetMap: Map<String, BudgetCategory>) {
        val updatedList = currentList.map { item ->
            val existingBudget = budgetMap[item.categoryName]
            if (existingBudget != null) {
                item.copy(budgetAmount = existingBudget.allocatedAmount)
            } else {
                item
            }
        }
        submitList(updatedList) {
            // Notify budget changed after list is updated
            onBudgetChanged()
        }
    }

    inner class ViewHolder(private val binding: ItemSetBudgetCategoryBinding) : RecyclerView.ViewHolder(binding.root) {

        private var textWatcher: TextWatcher? = null

        fun bind(item: CategoryBudgetItem) {
            // Remove previous text watcher
            textWatcher?.let { binding.etBudgetAmount.removeTextChangedListener(it) }

            // Set category info
            binding.tvCategoryName.text = item.categoryName
            binding.tvCurrentSpending.text = "Current spending: ₹${String.format("%.0f", item.currentSpending)}"
            
            // Set category icon
            binding.ivCategoryIcon.setImageResource(item.categoryIcon)
            
            // Set category color
            try {
                val color = Color.parseColor(item.categoryColor)
                binding.ivCategoryIcon.setColorFilter(color)
            } catch (e: Exception) {
                // Use default color if parsing fails
            }

            // Set budget amount
            if (item.budgetAmount > 0) {
                binding.etBudgetAmount.setText(String.format("%.0f", item.budgetAmount))
            } else {
                binding.etBudgetAmount.setText("")
            }

            // Add text watcher for budget amount changes
            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val budgetText = s.toString()
                    val budgetAmount = budgetText.toDoubleOrNull() ?: 0.0
                    
                    // Update the item's budget amount
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION && position < currentList.size) {
                        currentList[position].budgetAmount = budgetAmount
                        // Notify that budget has changed
                        onBudgetChanged()
                    }
                }
            }
            
            binding.etBudgetAmount.addTextChangedListener(textWatcher)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CategoryBudgetItem>() {
        override fun areItemsTheSame(
            oldItem: CategoryBudgetItem,
            newItem: CategoryBudgetItem
        ): Boolean {
            return oldItem.categoryId == newItem.categoryId
        }

        override fun areContentsTheSame(
            oldItem: CategoryBudgetItem,
            newItem: CategoryBudgetItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
