package com.koshpal_android.koshpalapp.ui.budget.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.databinding.ItemPieLegendBinding

data class LegendItem(
    val label: String,
    val amount: Double,
    val color: Int
)

class PieLegendAdapter : ListAdapter<LegendItem, PieLegendAdapter.VH>(Diff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPieLegendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
    class VH(private val binding: ItemPieLegendBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LegendItem) {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(item.color)
            }
            binding.viewColor.background = bg
            binding.tvLabel.text = item.label
            binding.tvAmount.text = "₹${String.format("%,.0f", item.amount)}"
        }
    }
    class Diff : DiffUtil.ItemCallback<LegendItem>() {
        override fun areItemsTheSame(oldItem: LegendItem, newItem: LegendItem): Boolean = oldItem.label == newItem.label
        override fun areContentsTheSame(oldItem: LegendItem, newItem: LegendItem): Boolean = oldItem == newItem
    }
}


