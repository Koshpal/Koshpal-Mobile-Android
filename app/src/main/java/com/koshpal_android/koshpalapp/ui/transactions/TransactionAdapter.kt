package com.koshpal_android.koshpalapp.ui.transactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.ItemTransactionBinding
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    private val dateFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.apply {
                tvMerchant.text = transaction.merchant
                tvDescription.text = transaction.description
                tvAmount.text = "₹${String.format("%.2f", transaction.amount)}"
                tvTimestamp.text = dateFormatter.format(Date(transaction.timestamp))

                // Set amount color based on transaction type
                val amountColor = if (transaction.type == TransactionType.CREDIT) {
                    ContextCompat.getColor(root.context, R.color.success)
                } else {
                    ContextCompat.getColor(root.context, R.color.error)
                }
                tvAmount.setTextColor(amountColor)

                // Set amount prefix
                tvAmount.text = if (transaction.type == TransactionType.CREDIT) {
                    "+₹${String.format("%.2f", transaction.amount)}"
                } else {
                    "-₹${String.format("%.2f", transaction.amount)}"
                }

                root.setOnClickListener {
                    onTransactionClick(transaction)
                }
            }
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
