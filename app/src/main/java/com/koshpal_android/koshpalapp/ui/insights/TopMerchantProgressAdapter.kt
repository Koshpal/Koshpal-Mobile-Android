package com.koshpal_android.koshpalapp.ui.insights

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.R

data class TopMerchantProgress(
    val merchantName: String,
    val amount: Double,
    val percentageOfMax: Float,  // 0.0 to 1.0
    val sharePercentage: Float   // 0.0 to 100.0
)

class TopMerchantProgressAdapter : RecyclerView.Adapter<TopMerchantProgressAdapter.VH>() {

    private val items = mutableListOf<TopMerchantProgress>()

    fun submitList(newItems: List<TopMerchantProgress>) {
        android.util.Log.d("TopMerchantAdapter", "📥 submitList: Received ${newItems.size} items")
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
        android.util.Log.d("TopMerchantAdapter", "✅ notifyDataSetChanged called, adapter now has ${items.size} items")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        android.util.Log.d("TopMerchantAdapter", "🏭 onCreateViewHolder called")
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_top_merchant_progress, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        android.util.Log.d("TopMerchantAdapter", "🔗 onBindViewHolder [$position] ${item.merchantName}")
        
        holder.tvMerchantLabel.text = item.merchantName
        holder.tvPercent.text = "${item.sharePercentage.toInt()}%"

        // Set background gradient
        holder.viewFill.setBackgroundResource(R.drawable.grad_budget_primary)

        // Set width proportionally after layout - EXACTLY like working budget adapter
        holder.flTrack.post {
            val width = holder.flTrack.width
            val fillWidth = (width * item.percentageOfMax).toInt().coerceIn(0, width)
            val lp = holder.viewFill.layoutParams
            lp.width = fillWidth
            holder.viewFill.layoutParams = lp
            
            android.util.Log.d("TopMerchantAdapter", "  ✅ [$position] ${item.merchantName}: trackWidth=$width, fillWidth=$fillWidth (${(item.percentageOfMax * 100).toInt()}%)")
        }
    }

    override fun getItemCount(): Int = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvMerchantLabel: TextView = v.findViewById(R.id.tvMerchantLabel)
        val flTrack: FrameLayout = v.findViewById(R.id.flTrack)
        val viewFill: View = v.findViewById(R.id.viewFill)
        val tvPercent: TextView = v.findViewById(R.id.tvPercent)
    }
}
