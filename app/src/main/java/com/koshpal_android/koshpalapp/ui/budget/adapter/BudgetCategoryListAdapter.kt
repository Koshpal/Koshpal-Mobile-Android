package com.koshpal_android.koshpalapp.ui.budget.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.databinding.ItemBudgetCategoryBinding
import com.koshpal_android.koshpalapp.model.BudgetCategory

class BudgetCategoryListAdapter : ListAdapter<BudgetCategory, BudgetCategoryListAdapter.VH>(Diff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemBudgetCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(private val binding: ItemBudgetCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BudgetCategory) {
            binding.tvCategoryName.text = item.name
            binding.tvLimitAmount.text = "₹${String.format("%,.0f", item.allocatedAmount)}"
            binding.tvSpentAmount.text = "₹${String.format("%,.0f", item.spentAmount)}"
            val remaining = (item.allocatedAmount - item.spentAmount).coerceAtLeast(0.0)
            binding.tvRemainingAmount.text = "₹${String.format("%,.0f", remaining)}"
            val pct = if (item.allocatedAmount > 0) ((item.spentAmount / item.allocatedAmount) * 100).toInt() else 0
            binding.progressCategory.progress = pct
            binding.tvCategoryStatus.text = "$pct%"

            val (iconRes, tintColor) = iconAndColorFor(item.name)
            binding.ivCategoryIcon.setImageResource(iconRes)
            binding.ivCategoryIcon.imageTintList = android.content.res.ColorStateList.valueOf(tintColor)
        }

        private fun iconAndColorFor(name: String): Pair<Int, Int> {
            val n = name.lowercase()
            return when {
                n.contains("rent") -> com.koshpal_android.koshpalapp.R.drawable.ic_home to android.graphics.Color.parseColor("#6366F1")
                n.contains("food") -> com.koshpal_android.koshpalapp.R.drawable.ic_menu_eat to android.graphics.Color.parseColor("#10B981")
                n.contains("entertainment") -> com.koshpal_android.koshpalapp.R.drawable.ic_category_default to android.graphics.Color.parseColor("#F59E0B")
                n.contains("emi") -> com.koshpal_android.koshpalapp.R.drawable.ic_trending_up to android.graphics.Color.parseColor("#8B5CF6")
                n.contains("travel") || n.contains("transport") -> com.koshpal_android.koshpalapp.R.drawable.ic_menu_directions to android.graphics.Color.parseColor("#06B6D4")
                else -> com.koshpal_android.koshpalapp.R.drawable.ic_more_vert to android.graphics.Color.parseColor("#9CA3AF")
            }
        }
    }

    class Diff : DiffUtil.ItemCallback<BudgetCategory>() {
        override fun areItemsTheSame(oldItem: BudgetCategory, newItem: BudgetCategory): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: BudgetCategory, newItem: BudgetCategory): Boolean = oldItem == newItem
    }
}


