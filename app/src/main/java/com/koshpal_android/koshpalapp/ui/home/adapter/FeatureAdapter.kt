package com.koshpal_android.koshpalapp.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.databinding.ItemFeatureBinding
import com.koshpal_android.koshpalapp.ui.home.model.FeatureItem

class FeatureAdapter(
    private val onFeatureClick: (FeatureItem) -> Unit
) : ListAdapter<FeatureItem, FeatureAdapter.FeatureViewHolder>(FeatureDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val binding = ItemFeatureBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FeatureViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FeatureViewHolder(
        private val binding: ItemFeatureBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(feature: FeatureItem) {
            binding.apply {
                tvFeatureTitle.text = feature.title
                tvFeatureDescription.text = feature.description
                ivFeatureIcon.setImageResource(feature.icon)
                
                // Set background color
                val color = ContextCompat.getColor(root.context, feature.color)
                cardFeature.setCardBackgroundColor(color)
                
                root.setOnClickListener {
                    onFeatureClick(feature)
                }
            }
        }
    }
}

private class FeatureDiffCallback : DiffUtil.ItemCallback<FeatureItem>() {
    override fun areItemsTheSame(oldItem: FeatureItem, newItem: FeatureItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FeatureItem, newItem: FeatureItem): Boolean {
        return oldItem == newItem
    }
}
