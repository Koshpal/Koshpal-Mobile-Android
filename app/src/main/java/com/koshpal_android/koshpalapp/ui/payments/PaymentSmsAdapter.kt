package com.koshpal_android.koshpalapp.ui.payments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.model.PaymentSms

class PaymentSmsAdapter : ListAdapter<PaymentSms, PaymentSmsAdapter.PaymentSmsViewHolder>(PaymentSmsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentSmsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_sms, parent, false)
        return PaymentSmsViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentSmsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PaymentSmsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSender: TextView = itemView.findViewById(R.id.tvSender)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)

        fun bind(paymentSms: PaymentSms) {
            tvSender.text = paymentSms.address
            tvMessage.text = paymentSms.body
            tvTimestamp.text = paymentSms.date
        }
    }

    private class PaymentSmsDiffCallback : DiffUtil.ItemCallback<PaymentSms>() {
        override fun areItemsTheSame(oldItem: PaymentSms, newItem: PaymentSms): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PaymentSms, newItem: PaymentSms): Boolean {
            return oldItem == newItem
        }
    }
}
