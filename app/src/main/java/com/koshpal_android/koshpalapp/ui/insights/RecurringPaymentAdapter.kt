package com.koshpal_android.koshpalapp.ui.insights

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.R
import java.text.SimpleDateFormat
import java.util.*

class RecurringPaymentAdapter(
    private val onMarkEssential: (RecurringPaymentItem) -> Unit,
    private val onCancelSuggestion: (RecurringPaymentItem) -> Unit,
    private val onMarkReimbursable: (RecurringPaymentItem) -> Unit
) : RecyclerView.Adapter<RecurringPaymentAdapter.VH>() {
    
    private val items = mutableListOf<RecurringPaymentItem>()

    fun submitList(newItems: List<RecurringPaymentItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recurring_payment, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        
        holder.tvMerchantName.text = item.merchantName
        holder.tvAvgAmount.text = "₹${String.format("%.0f", item.monthlyAvgAmount)}"
        holder.tvFrequency.text = item.frequency
        holder.tvMonthsBadge.text = "${item.last3MonthsFrequency}/3"
        holder.tvSubscriptionScore.text = "${(item.subscriptionScore * 100).toInt()}%"
        holder.tvLastSeen.text = formatLastSeen(item.lastSeen)
        
        // Set merchant icon based on category
        setMerchantIcon(holder.ivMerchantIcon, item.merchantName)
        
        // Create timeline visualization
        createTimeline(holder.layoutTimeline, item.timelineData)
        
        // Color score pill
        val scoreColor = when {
            item.subscriptionScore >= 0.85f -> R.color.success
            item.subscriptionScore >= 0.6f -> R.color.warning
            else -> R.color.error
        }
        holder.tvSubscriptionScore.setTextColor(holder.itemView.context.getColor(scoreColor))
    }

    override fun getItemCount(): Int = items.size

    private fun setMerchantIcon(imageView: ImageView, merchantName: String) {
        val iconRes = when (merchantName.lowercase()) {
            "netflix" -> R.drawable.ic_netflix
            "spotify" -> R.drawable.ic_spotify
            "amazon" -> R.drawable.ic_amazon
            "google" -> R.drawable.ic_google
            "microsoft" -> R.drawable.ic_microsoft
            "apple" -> R.drawable.ic_apple
            else -> R.drawable.ic_category_default
        }
        imageView.setImageResource(iconRes)
    }

    private fun createTimeline(layout: LinearLayout, timelineData: List<Double>) {
        layout.removeAllViews()
        
        val maxAmount = timelineData.maxOrNull() ?: 1.0
        
        timelineData.forEach { amount ->
            val bar = View(layout.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    layout.context.resources.getDimensionPixelSize(R.dimen.timeline_bar_width),
                    (amount / maxAmount * layout.context.resources.getDimensionPixelSize(R.dimen.timeline_bar_max_height)).toInt()
                ).apply {
                    marginEnd = layout.context.resources.getDimensionPixelSize(R.dimen.timeline_bar_margin)
                }
                setBackgroundColor(layout.context.getColor(
                    if (amount > 0) R.color.success else R.color.text_secondary
                ))
            }
            layout.addView(bar)
        }
    }

    private fun formatLastSeen(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val days = diff / (24 * 60 * 60 * 1000)
        
        return when {
            days == 0L -> "Today"
            days == 1L -> "Yesterday"
            days < 7L -> "$days days ago"
            days < 30L -> "${days / 7} weeks ago"
            else -> "${days / 30} months ago"
        }
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivMerchantIcon: ImageView = itemView.findViewById(R.id.ivMerchantIcon)
        val tvMerchantName: TextView = itemView.findViewById(R.id.tvMerchantName)
        val tvFrequency: TextView = itemView.findViewById(R.id.tvFrequency)
        val tvMonthsBadge: TextView = itemView.findViewById(R.id.tvMonthsBadge)
        val tvAvgAmount: TextView = itemView.findViewById(R.id.tvAvgAmount)
        val tvSubscriptionScore: TextView = itemView.findViewById(R.id.tvSubscriptionScore)
        val tvLastSeen: TextView = itemView.findViewById(R.id.tvLastSeen)
        val layoutTimeline: LinearLayout = itemView.findViewById(R.id.layoutTimeline)
        // buttons removed
    }
}
