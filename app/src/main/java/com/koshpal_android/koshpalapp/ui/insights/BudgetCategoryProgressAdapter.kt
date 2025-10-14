package com.koshpal_android.koshpalapp.ui.insights

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.R

class BudgetCategoryProgressAdapterModern : RecyclerView.Adapter<BudgetCategoryProgressAdapterModern.VH>() {

    private val items = mutableListOf<BudgetCategoryProgress>()

    fun submitList(newItems: List<BudgetCategoryProgress>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_budget_category_progress_modern, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvCategoryName.text = item.categoryName
        holder.tvPercent.text = "${(item.percentageUsed * 100).toInt()}%"
        holder.tvAmounts.text = "₹${format(item.spentAmount)} / ₹${format(item.allocatedAmount)}"

        // unified gradient fill
        holder.viewFill.setBackgroundResource(R.drawable.grad_budget_primary)

        // Set width proportionally after layout
        holder.flTrack.post {
            val width = holder.flTrack.width
            val w = (width * item.percentageUsed).toInt().coerceIn(0, width)
            val lp = holder.viewFill.layoutParams
            lp.width = w
            holder.viewFill.layoutParams = lp
        }
    }

    override fun getItemCount(): Int = items.size

    private fun format(value: Double): String = String.format("%.0f", value)

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvCategoryName: TextView = v.findViewById(R.id.tvCategoryName)
        val flTrack: FrameLayout = v.findViewById(R.id.flTrack)
        val viewFill: View = v.findViewById(R.id.viewFill)
        val tvPercent: TextView = v.findViewById(R.id.tvPercent)
        val tvAmounts: TextView = v.findViewById(R.id.tvAmounts)
    }
}
