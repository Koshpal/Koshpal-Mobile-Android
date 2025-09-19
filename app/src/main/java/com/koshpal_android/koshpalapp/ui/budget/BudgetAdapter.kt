package com.koshpal_android.koshpalapp.ui.budget

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.ItemBudgetBinding
import com.koshpal_android.koshpalapp.model.Budget
import com.koshpal_android.koshpalapp.model.BudgetStatus
import java.text.SimpleDateFormat
import java.util.*

class BudgetAdapter(
    private val onBudgetClick: (Budget) -> Unit,
    private val onMoreClick: (Budget) -> Unit
) : ListAdapter<Budget, BudgetAdapter.BudgetViewHolder>(BudgetDiffCallback()) {
    
    private val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val binding = ItemBudgetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BudgetViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class BudgetViewHolder(
        private val binding: ItemBudgetBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(budget: Budget) {
            binding.apply {
                // Set category name (simplified - should fetch from CategoryDao)
                tvCategoryName.text = getCategoryDisplayName(budget.categoryId)
                
                // Set budget period
                val calendar = Calendar.getInstance()
                calendar.set(budget.year, budget.month - 1, 1)
                tvBudgetPeriod.text = monthFormatter.format(calendar.time)
                
                // Set amounts
                tvSpent.text = "${budget.getFormattedSpent()} spent"
                tvBudgetLimit.text = "of ${budget.getFormattedLimit()}"
                tvProgressPercentage.text = "${budget.progressPercentage.toInt()}% used"
                tvRemaining.text = "${budget.getFormattedRemaining()} left"
                
                // Set progress
                progressBudget.progress = budget.progressPercentage.toInt()
                
                // Set status chip
                val (statusText, statusColor, progressColor) = when (budget.status) {
                    BudgetStatus.SAFE -> Triple("Safe", "#4CAF50", "#4CAF50")
                    BudgetStatus.WARNING -> Triple("Warning", "#FF9800", "#FF9800")
                    BudgetStatus.CRITICAL -> Triple("Critical", "#FF5722", "#FF5722")
                    BudgetStatus.EXCEEDED -> Triple("Exceeded", "#F44336", "#F44336")
                }
                
                chipStatus.text = statusText
                chipStatus.setChipBackgroundColorResource(android.R.color.transparent)
                chipStatus.setTextColor(Color.parseColor(statusColor))
                
                // Set progress bar color
                progressBudget.setIndicatorColor(Color.parseColor(progressColor))
                
                // Set remaining amount color
                tvRemaining.setTextColor(
                    if (budget.status == BudgetStatus.EXCEEDED) {
                        Color.parseColor("#F44336")
                    } else {
                        Color.parseColor(progressColor)
                    }
                )
                
                // Set category icon and color
                ivCategoryIcon.setImageResource(getCategoryIcon(budget.categoryId))
                cardCategoryIcon.setCardBackgroundColor(Color.parseColor(getCategoryColor(budget.categoryId)))
                
                // Click listeners
                root.setOnClickListener { onBudgetClick(budget) }
                btnMore.setOnClickListener { onMoreClick(budget) }
            }
        }
        
        private fun getCategoryDisplayName(categoryId: String): String {
            return when (categoryId) {
                "food" -> "Food & Dining"
                "grocery" -> "Grocery"
                "transport" -> "Transportation"
                "bills" -> "Bills & Utilities"
                "education" -> "Education"
                "entertainment" -> "Entertainment"
                "healthcare" -> "Healthcare"
                "shopping" -> "Shopping"
                "salary" -> "Salary & Income"
                else -> "Others"
            }
        }
        
        private fun getCategoryIcon(categoryId: String): Int {
            return when (categoryId) {
                "food" -> R.drawable.ic_menu_eat
                "grocery" -> R.drawable.ic_menu_gallery
                "transport" -> R.drawable.ic_menu_directions
                "bills" -> R.drawable.ic_category_default
                "education" -> R.drawable.ic_info
                "entertainment" -> R.drawable.ic_category_default
                "healthcare" -> R.drawable.ic_category_default
                "shopping" -> R.drawable.ic_add
                else -> R.drawable.ic_more_vert
            }
        }
        
        private fun getCategoryColor(categoryId: String): String {
            return when (categoryId) {
                "food" -> "#FF6B35"
                "grocery" -> "#4CAF50"
                "transport" -> "#2196F3"
                "bills" -> "#FF9800"
                "education" -> "#9C27B0"
                "entertainment" -> "#E91E63"
                "healthcare" -> "#F44336"
                "shopping" -> "#795548"
                else -> "#607D8B"
            }
        }
    }
    
    private class BudgetDiffCallback : DiffUtil.ItemCallback<Budget>() {
        override fun areItemsTheSame(oldItem: Budget, newItem: Budget): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Budget, newItem: Budget): Boolean {
            return oldItem == newItem
        }
    }
}
