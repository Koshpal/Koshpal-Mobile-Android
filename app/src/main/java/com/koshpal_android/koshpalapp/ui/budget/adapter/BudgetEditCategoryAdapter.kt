package com.koshpal_android.koshpalapp.ui.budget.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.databinding.ItemBudgetCategoryEditBinding
import com.koshpal_android.koshpalapp.model.BudgetCategory

class BudgetEditCategoryAdapter(
    private val onValueChange: (name: String, value: Double) -> Unit
) : ListAdapter<BudgetCategory, BudgetEditCategoryAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemBudgetCategoryEditBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding, onValueChange)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val binding: ItemBudgetCategoryEditBinding,
        private val onValueChange: (name: String, value: Double) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BudgetCategory) {
            binding.tvName.text = item.name
            val (iconRes, _) = iconAndColorFor(item.name)
            binding.ivIcon.setImageResource(iconRes)
            binding.etAmount.setText(if (item.allocatedAmount > 0) item.allocatedAmount.toInt().toString() else "")
            binding.etAmount.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val value = s?.toString()?.toDoubleOrNull() ?: 0.0
                    onValueChange(item.name, value)
                }
            })
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
        override fun areItemsTheSame(oldItem: BudgetCategory, newItem: BudgetCategory): Boolean = oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: BudgetCategory, newItem: BudgetCategory): Boolean = oldItem == newItem
    }
}


