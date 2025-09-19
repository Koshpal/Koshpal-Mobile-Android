package com.koshpal_android.koshpalapp.ui.alerts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.ItemActiveAlertBinding

class ActiveAlertsAdapter(
    private val onAlertClick: (ActiveAlert) -> Unit
) : ListAdapter<ActiveAlert, ActiveAlertsAdapter.ActiveAlertViewHolder>(ActiveAlertDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveAlertViewHolder {
        val binding = ItemActiveAlertBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ActiveAlertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActiveAlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ActiveAlertViewHolder(
        private val binding: ItemActiveAlertBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(alert: ActiveAlert) {
            binding.apply {
                tvCategoryName.text = alert.categoryName
                tvBudgetAmount.text = alert.getFormattedBudget()
                tvSpentAmount.text = alert.getFormattedSpent()
                tvPercentage.text = "${alert.percentage}%"

                // Set progress bar
                progressBar.progress = alert.percentage.coerceAtMost(100)

                // Set colors based on alert type
                val (progressColor, textColor, backgroundColor) = when (alert.alertType) {
                    AlertType.INFO -> Triple(
                        ContextCompat.getColor(root.context, R.color.success),
                        ContextCompat.getColor(root.context, R.color.success),
                        ContextCompat.getColor(root.context, R.color.success_light)
                    )
                    AlertType.WARNING -> Triple(
                        ContextCompat.getColor(root.context, R.color.warning),
                        ContextCompat.getColor(root.context, R.color.warning),
                        ContextCompat.getColor(root.context, R.color.warning_light)
                    )
                    AlertType.EXCEEDED -> Triple(
                        ContextCompat.getColor(root.context, R.color.error),
                        ContextCompat.getColor(root.context, R.color.error),
                        ContextCompat.getColor(root.context, R.color.error_light)
                    )
                }

                progressBar.progressTintList = android.content.res.ColorStateList.valueOf(progressColor)
                tvPercentage.setTextColor(textColor)
                cardAlert.setCardBackgroundColor(backgroundColor)

                // Set alert icon
                ivAlertIcon.setImageResource(
                    when (alert.alertType) {
                        AlertType.INFO -> R.drawable.ic_info
                        AlertType.WARNING -> R.drawable.ic_warning
                        AlertType.EXCEEDED -> R.drawable.ic_error
                    }
                )

                // Set alert message
                tvAlertMessage.text = when (alert.alertType) {
                    AlertType.INFO -> "On track"
                    AlertType.WARNING -> "Approaching limit"
                    AlertType.EXCEEDED -> "Budget exceeded"
                }

                root.setOnClickListener {
                    onAlertClick(alert)
                }
            }
        }
    }

    private class ActiveAlertDiffCallback : DiffUtil.ItemCallback<ActiveAlert>() {
        override fun areItemsTheSame(oldItem: ActiveAlert, newItem: ActiveAlert): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ActiveAlert, newItem: ActiveAlert): Boolean {
            return oldItem == newItem
        }
    }
}
