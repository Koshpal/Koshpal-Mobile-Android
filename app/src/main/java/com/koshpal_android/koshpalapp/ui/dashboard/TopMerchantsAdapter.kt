package com.koshpal_android.koshpalapp.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.ItemMerchantSpendingBinding
import com.koshpal_android.koshpalapp.model.MerchantSpendingData

class TopMerchantsAdapter : ListAdapter<MerchantSpendingData, TopMerchantsAdapter.MerchantViewHolder>(
    MerchantDiffCallback()
) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MerchantViewHolder {
        val binding = ItemMerchantSpendingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MerchantViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: MerchantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class MerchantViewHolder(
        private val binding: ItemMerchantSpendingBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(merchant: MerchantSpendingData) {
            binding.apply {
                tvMerchantName.text = merchant.merchantName
                tvAmount.text = merchant.getFormattedAmount()
                tvTransactionCount.text = merchant.getTransactionText()
                
                // Set merchant icon based on name (you can enhance this with a proper mapping)
                ivMerchantIcon.setImageResource(getMerchantIcon(merchant.merchantName))
            }
        }
        
        private fun getMerchantIcon(merchantName: String): Int {
            return when (merchantName.lowercase()) {
                "zomato", "swiggy" -> R.drawable.ic_menu_eat
                "uber", "ola" -> R.drawable.ic_menu_directions
                "amazon", "flipkart" -> R.drawable.ic_add
                "bigbasket", "grofers" -> R.drawable.ic_menu_gallery
                else -> R.drawable.ic_more_vert
            }
        }
    }
    
    private class MerchantDiffCallback : DiffUtil.ItemCallback<MerchantSpendingData>() {
        override fun areItemsTheSame(
            oldItem: MerchantSpendingData,
            newItem: MerchantSpendingData
        ): Boolean {
            return oldItem.merchantName == newItem.merchantName
        }
        
        override fun areContentsTheSame(
            oldItem: MerchantSpendingData,
            newItem: MerchantSpendingData
        ): Boolean {
            return oldItem == newItem
        }
    }
}
