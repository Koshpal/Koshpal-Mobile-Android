package com.koshpal_android.koshpalapp.ui.savings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.databinding.ItemSavingsGoalBinding
import com.koshpal_android.koshpalapp.model.SavingsGoal
import com.koshpal_android.koshpalapp.model.GoalCategory
import java.text.SimpleDateFormat
import java.util.*

class SavingsGoalsAdapter(
    private val onAddMoneyClick: (SavingsGoal) -> Unit,
    private val onDetailsClick: (SavingsGoal) -> Unit,
    private val onMoreClick: (SavingsGoal) -> Unit
) : ListAdapter<SavingsGoal, SavingsGoalsAdapter.SavingsGoalViewHolder>(SavingsGoalDiffCallback()) {
    
    private val dateFormatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavingsGoalViewHolder {
        val binding = ItemSavingsGoalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SavingsGoalViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: SavingsGoalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class SavingsGoalViewHolder(
        private val binding: ItemSavingsGoalBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(goal: SavingsGoal) {
            binding.apply {
                tvGoalName.text = goal.name
                tvCurrentAmount.text = goal.getFormattedCurrent()
                tvTargetAmount.text = goal.getFormattedTarget()
                tvProgressPercentage.text = "${goal.progressPercentage.toInt()}% completed"
                tvRemaining.text = "${goal.getFormattedRemaining()} to go"
                
                // Set progress
                progressGoal.progress = goal.progressPercentage.toInt()
                
                // Set goal emoji and category info
                tvGoalEmoji.text = getCategoryEmoji(goal.category)
                val targetDateText = goal.targetDate?.let { targetDate ->
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = targetDate
                    "Target: ${dateFormatter.format(calendar.time)}"
                } ?: "No deadline"
                
                tvGoalCategory.text = "${getCategoryDisplayName(goal.category)} • $targetDateText"
                
                // Click listeners
                btnAddMoney.setOnClickListener { onAddMoneyClick(goal) }
                btnViewDetails.setOnClickListener { onDetailsClick(goal) }
                btnMore.setOnClickListener { onMoreClick(goal) }
            }
        }
        
        private fun getCategoryEmoji(category: GoalCategory): String {
            return when (category) {
                GoalCategory.EMERGENCY_FUND -> "🚨"
                GoalCategory.VACATION -> "🏖️"
                GoalCategory.GADGET -> "📱"
                GoalCategory.EDUCATION -> "🎓"
                GoalCategory.INVESTMENT -> "📈"
                GoalCategory.HEALTH -> "🏥"
                GoalCategory.HOME -> "🏠"
                GoalCategory.VEHICLE -> "🚗"
                GoalCategory.OTHER -> "🎯"
            }
        }
        
        private fun getCategoryDisplayName(category: GoalCategory): String {
            return when (category) {
                GoalCategory.EMERGENCY_FUND -> "Emergency Fund"
                GoalCategory.VACATION -> "Vacation"
                GoalCategory.GADGET -> "Gadget"
                GoalCategory.EDUCATION -> "Education"
                GoalCategory.INVESTMENT -> "Investment"
                GoalCategory.HEALTH -> "Health"
                GoalCategory.HOME -> "Home"
                GoalCategory.VEHICLE -> "Vehicle"
                GoalCategory.OTHER -> "Other"
            }
        }
    }
    
    private class SavingsGoalDiffCallback : DiffUtil.ItemCallback<SavingsGoal>() {
        override fun areItemsTheSame(oldItem: SavingsGoal, newItem: SavingsGoal): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: SavingsGoal, newItem: SavingsGoal): Boolean {
            return oldItem == newItem
        }
    }
}

class CompletedGoalsAdapter(
    private val onGoalClick: (SavingsGoal) -> Unit
) : ListAdapter<SavingsGoal, CompletedGoalsAdapter.CompletedGoalViewHolder>(SavingsGoalDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompletedGoalViewHolder {
        val binding = ItemSavingsGoalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CompletedGoalViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: CompletedGoalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class CompletedGoalViewHolder(
        private val binding: ItemSavingsGoalBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(goal: SavingsGoal) {
            binding.apply {
                tvGoalName.text = "✅ ${goal.name}"
                tvCurrentAmount.text = goal.getFormattedCurrent()
                tvTargetAmount.text = goal.getFormattedTarget()
                tvProgressPercentage.text = "100% completed"
                tvRemaining.text = "Goal achieved!"
                
                progressGoal.progress = 100
                
                // Hide action buttons for completed goals
                btnAddMoney.text = "Celebrate"
                btnViewDetails.text = "View"
                
                root.setOnClickListener { onGoalClick(goal) }
            }
        }
    }
    
    private class SavingsGoalDiffCallback : DiffUtil.ItemCallback<SavingsGoal>() {
        override fun areItemsTheSame(oldItem: SavingsGoal, newItem: SavingsGoal): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: SavingsGoal, newItem: SavingsGoal): Boolean {
            return oldItem == newItem
        }
    }
}
