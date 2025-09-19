package com.koshpal_android.koshpalapp.ui.insights

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.databinding.ItemRecommendationBinding

class RecommendationsAdapter : ListAdapter<Recommendation, RecommendationsAdapter.RecommendationViewHolder>(
    RecommendationDiffCallback()
) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationViewHolder {
        val binding = ItemRecommendationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecommendationViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class RecommendationViewHolder(
        private val binding: ItemRecommendationBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(recommendation: Recommendation) {
            binding.apply {
                tvRecommendationTitle.text = recommendation.title
                tvRecommendationDescription.text = recommendation.description
                
                // Set priority indicator color
                val priorityColor = when (recommendation.priority) {
                    RecommendationPriority.HIGH -> "#F44336"
                    RecommendationPriority.MEDIUM -> "#FF9800"
                    RecommendationPriority.LOW -> "#4CAF50"
                }
                
                viewPriorityIndicator.setBackgroundColor(Color.parseColor(priorityColor))
                
                // Click listener for action button
                btnRecommendationAction.setOnClickListener {
                    // Handle recommendation action
                    handleRecommendationAction(recommendation)
                }
            }
        }
        
        private fun handleRecommendationAction(recommendation: Recommendation) {
            // This could navigate to relevant screens or show more details
            when (recommendation.category) {
                RecommendationCategory.SAVINGS -> {
                    // Navigate to savings goals or create savings goal
                }
                RecommendationCategory.SPENDING -> {
                    // Navigate to spending analysis or categories
                }
                RecommendationCategory.BUDGETING -> {
                    // Navigate to budget creation
                }
                RecommendationCategory.GENERAL -> {
                    // Show more details or tips
                }
            }
        }
    }
    
    private class RecommendationDiffCallback : DiffUtil.ItemCallback<Recommendation>() {
        override fun areItemsTheSame(oldItem: Recommendation, newItem: Recommendation): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Recommendation, newItem: Recommendation): Boolean {
            return oldItem == newItem
        }
    }
}
