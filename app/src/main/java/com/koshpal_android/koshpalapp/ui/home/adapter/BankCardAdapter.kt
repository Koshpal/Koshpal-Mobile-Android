package com.koshpal_android.koshpalapp.ui.home.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.ItemBankCardBinding
import com.koshpal_android.koshpalapp.model.BankSpending

class BankCardAdapter(
    private val onAddCashClick: () -> Unit
) : ListAdapter<BankSpending, BankCardAdapter.BankCardViewHolder>(BankSpendingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankCardViewHolder {
        val binding = ItemBankCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BankCardViewHolder(binding, onAddCashClick)
    }

    override fun onBindViewHolder(holder: BankCardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BankCardViewHolder(
        private val binding: ItemBankCardBinding,
        private val onAddCashClick: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bankSpending: BankSpending) {
            binding.apply {
                tvBankName.text = bankSpending.bankName
                tvSpending.text = "₹${String.format("%,.0f", bankSpending.totalSpending)}"
                tvTransactionCount.text = "${bankSpending.transactionCount} transactions"

                // Show add button only for Cash card
                if (bankSpending.isCash) {
                    btnAddCash.visibility = View.VISIBLE
                    btnAddCash.setOnClickListener { onAddCashClick() }
                    
                    // Set cash card gradient
                    cardContent.setBackgroundResource(R.drawable.gradient_cash_card)
                } else {
                    btnAddCash.visibility = View.GONE
                    
                    // Set bank-specific gradient colors
                    val gradient = getBankGradient(bankSpending.bankName)
                    cardContent.background = gradient
                }
            }
        }

        private fun getBankGradient(bankName: String): GradientDrawable {
            val context = binding.root.context
            val (startColor, endColor) = when (bankName.uppercase()) {
                "SBI", "STATE BANK" -> Pair("#1565C0", "#0D47A1") // Blue
                "HDFC", "HDFC BANK" -> Pair("#D32F2F", "#B71C1C") // Red
                "ICICI", "ICICI BANK" -> Pair("#F57C00", "#E65100") // Orange
                "AXIS", "AXIS BANK" -> Pair("#7B1FA2", "#4A148C") // Purple
                "KOTAK", "KOTAK MAHINDRA" -> Pair("#C62828", "#8E0000") // Dark Red
                "IPPB", "INDIA POST" -> Pair("#00897B", "#00695C") // Teal
                "PAYTM", "PAYTM PAYMENTS" -> Pair("#00B0FF", "#0091EA") // Light Blue
                "PHONEPE" -> Pair("#5F259F", "#3C1361") // Purple
                "GPAY", "GOOGLE PAY" -> Pair("#34A853", "#2D8E47") // Green
                else -> Pair("#6750A4", "#4F378B") // Default Purple
            }

            return GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    android.graphics.Color.parseColor(startColor),
                    android.graphics.Color.parseColor(endColor)
                )
            ).apply {
                cornerRadius = 16 * context.resources.displayMetrics.density
            }
        }
    }

    class BankSpendingDiffCallback : DiffUtil.ItemCallback<BankSpending>() {
        override fun areItemsTheSame(oldItem: BankSpending, newItem: BankSpending): Boolean {
            return oldItem.bankName == newItem.bankName
        }

        override fun areContentsTheSame(oldItem: BankSpending, newItem: BankSpending): Boolean {
            return oldItem == newItem
        }
    }
}
