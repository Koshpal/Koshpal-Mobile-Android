package com.koshpal_android.koshpalapp.ui.categories.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.databinding.ItemCategorySpendingBinding
import com.koshpal_android.koshpalapp.model.BudgetCategory
import com.koshpal_android.koshpalapp.model.CategorySpending
import com.koshpal_android.koshpalapp.model.TransactionCategory

// Data class to combine spending and budget information
data class CategorySpendingWithBudget(
    val categorySpending: CategorySpending,
    val budgetCategory: BudgetCategory? = null,
    val transactionCount: Int = 0
)

class CategorySpendingAdapter(
    private val onSetBudgetClick: (CategorySpending) -> Unit,
    private val onCategoryClick: (CategorySpending) -> Unit = {}
) : ListAdapter<CategorySpendingWithBudget, CategorySpendingAdapter.CategorySpendingViewHolder>(CategorySpendingDiffCallback()) {

    private var categoriesById: Map<String, TransactionCategory> = emptyMap()

    fun setCategoriesMap(map: Map<String, TransactionCategory>) {
        categoriesById = map
        notifyDataSetChanged()
    }

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

        fun bind(data: CategorySpendingWithBudget) {
            val categorySpending = data.categorySpending
            val budgetCategory = data.budgetCategory
            val transactionCount = data.transactionCount
            
            binding.apply {
                // Get category details from default categories
                val category = getCategoryById(categorySpending.categoryId)
                tvCategoryName.text = category?.name ?: "Unknown Category"
                tvAmount.text = "₹${String.format("%,.0f", categorySpending.totalAmount)}"
                
                // Show real transaction count
                val countText = if (transactionCount == 1) "1 transaction" else "$transactionCount transactions"
                tvTransactionCount.text = countText

                // Set category icon and color
                if (category != null) {
                    ivCategoryIcon.setImageResource(category.icon)
                    try {
                        val color = Color.parseColor(category.color)
                        cardIcon.setCardBackgroundColor(color)
                        ivCategoryIcon.setColorFilter(Color.WHITE)
                    } catch (e: Exception) {
                        // Fallback to light primary color
                        cardIcon.setCardBackgroundColor(
                            binding.root.context.getColor(com.koshpal_android.koshpalapp.R.color.primary_lightest)
                        )
                        ivCategoryIcon.setColorFilter(
                            binding.root.context.getColor(com.koshpal_android.koshpalapp.R.color.primary)
                        )
                    }
                }

                // Handle budget progress
                if (budgetCategory != null && budgetCategory.allocatedAmount > 0) {
                    // Budget is set - show progress
                    layoutProgress.visibility = View.VISIBLE
                    
                    val spentAmount = categorySpending.totalAmount
                    val budgetAmount = budgetCategory.allocatedAmount
                    val percentage = ((spentAmount / budgetAmount) * 100).toInt().coerceIn(0, 100)
                    
                    // Update progress bar
                    progressBar.progress = percentage
                    
                    // Set progress bar color based on percentage
                    val progressColor = when {
                        percentage >= 100 -> binding.root.context.getColor(com.koshpal_android.koshpalapp.R.color.error)
                        percentage >= 80 -> binding.root.context.getColor(com.koshpal_android.koshpalapp.R.color.warning)
                        else -> binding.root.context.getColor(com.koshpal_android.koshpalapp.R.color.primary)
                    }
                    progressBar.setIndicatorColor(progressColor)
                    
                    // Update budget info text
                    tvBudgetInfo.text = "$percentage% of ₹${String.format("%,.0f", budgetAmount)}"
                    tvBudgetInfo.visibility = View.VISIBLE
                } else {
                    // No budget set - hide progress
                    layoutProgress.visibility = View.GONE
                }

                // Click listener for the entire category item - opens details
                root.setOnClickListener {
                    onCategoryClick(categorySpending)
                }
            }
        }

        private fun getCategoryById(categoryId: String): TransactionCategory? {
            return categoriesById[categoryId] ?: TransactionCategory.getDefaultCategories().find { it.id == categoryId }
        }
    }

    private class CategorySpendingDiffCallback : DiffUtil.ItemCallback<CategorySpendingWithBudget>() {
        override fun areItemsTheSame(oldItem: CategorySpendingWithBudget, newItem: CategorySpendingWithBudget): Boolean {
            return oldItem.categorySpending.categoryId == newItem.categorySpending.categoryId
        }

        override fun areContentsTheSame(oldItem: CategorySpendingWithBudget, newItem: CategorySpendingWithBudget): Boolean {
            return oldItem == newItem
        }
    }
}
