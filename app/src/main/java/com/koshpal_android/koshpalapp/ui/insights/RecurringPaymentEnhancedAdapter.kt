package com.koshpal_android.koshpalapp.ui.insights

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

class RecurringPaymentEnhancedAdapter :
    ListAdapter<RecurringPaymentEnhanced, RecurringPaymentEnhancedAdapter.ViewHolder>(DiffCallback()) {

    private val expandedPositions = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recurring_payment_premium, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, expandedPositions.contains(position))
        
        // Staggered fade-in animation
        PremiumAnimationUtils.fadeInSlideUp(
            holder.itemView,
            duration = 300L,
            startDelay = position * 80L,
            distance = 30f
        )
        
        // Handle expand/collapse with spring animation
        holder.layoutHeader.setOnClickListener {
            val adapterPos = holder.bindingAdapterPosition
            if (adapterPos == RecyclerView.NO_POSITION) return@setOnClickListener
            val currentlyExpanded = expandedPositions.contains(adapterPos)
            if (currentlyExpanded) {
                expandedPositions.remove(adapterPos)
                PremiumAnimationUtils.springCollapse(holder.layoutExpandedDetails, duration = 250L)
                PremiumAnimationUtils.rotateArrow(holder.ivExpandArrow, false, duration = 200L)
            } else {
                expandedPositions.add(adapterPos)
                PremiumAnimationUtils.springExpand(holder.layoutExpandedDetails, duration = 300L)
                PremiumAnimationUtils.rotateArrow(holder.ivExpandArrow, true, duration = 200L)
                // Ensure recent transactions are bound when expanding
                val currentItem = getItem(adapterPos)
                holder.bind(currentItem, true)
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutHeader: View = itemView.findViewById(R.id.layoutHeader)
        private val tvMerchantInitials: TextView = itemView.findViewById(R.id.tvMerchantInitials)
        private val tvMerchantName: TextView = itemView.findViewById(R.id.tvMerchantName)
        private val tvCategoryTag: TextView = itemView.findViewById(R.id.tvCategoryTag)
        private val tvFrequency: TextView = itemView.findViewById(R.id.tvFrequency)
        private val tvConsecutiveMonths: TextView = itemView.findViewById(R.id.tvConsecutiveMonths)
        private val tvCurrentAmount: TextView = itemView.findViewById(R.id.tvCurrentAmount)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val cardStatusBadge: MaterialCardView = itemView.findViewById(R.id.cardStatusBadge)
        private val tvPreviousAmount: TextView = itemView.findViewById(R.id.tvPreviousAmount)
        private val tvCurrentAmountLarge: TextView = itemView.findViewById(R.id.tvCurrentAmountLarge)
        val ivExpandArrow: ImageView = itemView.findViewById(R.id.ivExpandArrow)
        val layoutExpandedDetails: View = itemView.findViewById(R.id.layoutExpandedDetails)
        private val rvRecentTransactions: RecyclerView = itemView.findViewById(R.id.rvRecentTransactions)

        fun bind(item: RecurringPaymentEnhanced, isExpanded: Boolean) {
            // Merchant info
            tvMerchantInitials.text = item.merchantInitials
            tvMerchantName.text = item.merchantName
            tvCategoryTag.text = item.categoryTag
            
            // Frequency and months
            tvFrequency.text = item.frequency
            tvConsecutiveMonths.text = "${item.consecutiveMonths} months"
            
            // Amounts
            tvCurrentAmount.text = "₹${String.format("%.0f", item.currentMonthAmount)}"
            tvPreviousAmount.text = "₹${String.format("%.0f", item.previousMonthAmount)}"
            tvCurrentAmountLarge.text = "₹${String.format("%.0f", item.currentMonthAmount)}"
            
            // Status badge
            when {
                item.isStable -> {
                    tvStatus.text = "Stable"
                    cardStatusBadge.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
                    tvStatus.setTextColor(Color.parseColor("#1565C0"))
                }
                item.hasIncreased -> {
                    tvStatus.text = "↑ ${kotlin.math.abs(item.percentageChange).toInt()}%"
                    cardStatusBadge.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                    tvStatus.setTextColor(Color.parseColor("#D32F2F"))
                }
                item.hasDecreased -> {
                    tvStatus.text = "↓ ${kotlin.math.abs(item.percentageChange).toInt()}%"
                    cardStatusBadge.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                    tvStatus.setTextColor(Color.parseColor("#388E3C"))
                }
            }
            
            // Expanded state
            android.util.Log.d("RecurringAdapter", "=== BIND START ===")
            android.util.Log.d("RecurringAdapter", "Merchant: ${item.merchantName}")
            android.util.Log.d("RecurringAdapter", "IsExpanded: $isExpanded")
            android.util.Log.d("RecurringAdapter", "Recent transactions count: ${item.recentTransactions.size}")
            
            if (isExpanded) {
                android.util.Log.d("RecurringAdapter", "✅ Expanding view...")
                
                layoutExpandedDetails.visibility = View.VISIBLE
                ivExpandArrow.rotation = 180f
                
                android.util.Log.d("RecurringAdapter", "Layout visibility set to VISIBLE")
                android.util.Log.d("RecurringAdapter", "Setting up RecyclerView...")
                
                // Setup recent transactions list
                if (rvRecentTransactions.adapter == null || rvRecentTransactions.adapter !is RecentTransactionMiniAdapter) {
                    android.util.Log.d("RecurringAdapter", "Creating new adapter")
                    rvRecentTransactions.layoutManager = LinearLayoutManager(itemView.context)
                    rvRecentTransactions.adapter = RecentTransactionMiniAdapter()
                } else {
                    android.util.Log.d("RecurringAdapter", "Using existing adapter")
                }
                
                android.util.Log.d("RecurringAdapter", "Submitting ${item.recentTransactions.size} transactions")
                item.recentTransactions.forEachIndexed { index, txn ->
                    android.util.Log.d("RecurringAdapter", "  Transaction $index: ${txn.merchant} - ₹${txn.amount} on ${java.util.Date(txn.date)}")
                }
                
                (rvRecentTransactions.adapter as RecentTransactionMiniAdapter).submitList(item.recentTransactions)
                
                android.util.Log.d("RecurringAdapter", "RecyclerView adapter itemCount: ${rvRecentTransactions.adapter?.itemCount}")
                
                // Animate expansion
                layoutExpandedDetails.alpha = 0f
                layoutExpandedDetails.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
                    
                android.util.Log.d("RecurringAdapter", "✅ Expansion complete")
            } else {
                android.util.Log.d("RecurringAdapter", "❌ Collapsing view...")
                layoutExpandedDetails.visibility = View.GONE
                ivExpandArrow.rotation = 0f
            }
            
            android.util.Log.d("RecurringAdapter", "=== BIND END ===")
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<RecurringPaymentEnhanced>() {
        override fun areItemsTheSame(
            oldItem: RecurringPaymentEnhanced,
            newItem: RecurringPaymentEnhanced
        ): Boolean {
            return oldItem.merchantName == newItem.merchantName
        }

        override fun areContentsTheSame(
            oldItem: RecurringPaymentEnhanced,
            newItem: RecurringPaymentEnhanced
        ): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * Mini adapter for recent transactions in expanded view
 */
class RecentTransactionMiniAdapter :
    RecyclerView.Adapter<RecentTransactionMiniAdapter.MiniViewHolder>() {

    private val transactions = mutableListOf<Transaction>()
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val dayFormatter = SimpleDateFormat("dd", Locale.getDefault())

    fun submitList(newTransactions: List<Transaction>) {
        android.util.Log.d("MiniAdapter", "=== submitList called ===")
        android.util.Log.d("MiniAdapter", "New transactions count: ${newTransactions.size}")
        transactions.clear()
        transactions.addAll(newTransactions)
        notifyDataSetChanged()
        android.util.Log.d("MiniAdapter", "Adapter updated. ItemCount: ${transactions.size}")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiniViewHolder {
        android.util.Log.d("MiniAdapter", "onCreateViewHolder called")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_transaction_mini, parent, false)
        return MiniViewHolder(view)
    }

    override fun onBindViewHolder(holder: MiniViewHolder, position: Int) {
        android.util.Log.d("MiniAdapter", "onBindViewHolder position: $position")
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int {
        android.util.Log.d("MiniAdapter", "getItemCount: ${transactions.size}")
        return transactions.size
    }

    inner class MiniViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)

        fun bind(transaction: Transaction) {
            android.util.Log.d("MiniAdapter", "--- Binding transaction ---")
            android.util.Log.d("MiniAdapter", "Merchant: ${transaction.merchant}")
            android.util.Log.d("MiniAdapter", "Amount: ₹${transaction.amount}")
            android.util.Log.d("MiniAdapter", "Date: ${Date(transaction.date)}")
            
            val date = Date(transaction.date)
            tvDay.text = dayFormatter.format(date)
            tvDate.text = dateFormatter.format(date)
            tvDescription.text = transaction.description.ifEmpty { "Subscription payment" }
            tvAmount.text = "₹${String.format("%.0f", transaction.amount)}"
            
            android.util.Log.d("MiniAdapter", "UI updated - Day: ${tvDay.text}, Date: ${tvDate.text}, Desc: ${tvDescription.text}, Amount: ${tvAmount.text}")
        }
    }
}
