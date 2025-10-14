package com.koshpal_android.koshpalapp.ui.insights

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.R

data class TopExpenseItem(
    val name: String,
    val amount: Double,
    val percent: Double
)

class TopExpenseAdapter : RecyclerView.Adapter<TopExpenseAdapter.VH>() {
    private val items = mutableListOf<TopExpenseItem>()

    fun submitList(newItems: List<TopExpenseItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_top_expense, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvAmount.text = "₹" + String.format("%.2f", item.amount)
        holder.tvPercent.text = String.format("%.0f%%", item.percent)
        holder.ivIcon.setImageResource(R.drawable.ic_category_default)
        holder.ivIcon.setColorFilter(holder.itemView.context.getColor(R.color.primary))
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvPercent: TextView = itemView.findViewById(R.id.tvPercent)
    }
}


