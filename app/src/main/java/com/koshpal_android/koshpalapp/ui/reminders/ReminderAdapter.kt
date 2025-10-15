package com.koshpal_android.koshpalapp.ui.reminders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.ItemReminderBinding
import com.koshpal_android.koshpalapp.model.*
import java.text.SimpleDateFormat
import java.util.*

class ReminderAdapter(
    private val onReminderClick: (Reminder) -> Unit,
    private val onMarkCompleteClick: (Reminder) -> Unit,
    private val onEditClick: (Reminder) -> Unit,
    private val onDeleteClick: (Reminder) -> Unit
) : ListAdapter<Reminder, ReminderAdapter.ReminderViewHolder>(ReminderDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val binding = ItemReminderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReminderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReminderViewHolder(
        private val binding: ItemReminderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(reminder: Reminder) {
            binding.apply {
                // Person name
                tvPersonName.text = reminder.personName

                // Amount with type color
                tvAmount.text = "₹${String.format("%.0f", reminder.amount)}"
                val amountColor = when (reminder.type) {
                    ReminderType.GIVE -> R.color.expense
                    ReminderType.RECEIVE -> R.color.income
                }
                tvAmount.setTextColor(ContextCompat.getColor(root.context, amountColor))

                // Purpose
                tvPurpose.text = reminder.purpose

                // Type label
                tvType.text = if (reminder.type == ReminderType.GIVE) "TO PAY" else "TO RECEIVE"
                
                // Type indicator color
                val indicatorColor = when (reminder.type) {
                    ReminderType.GIVE -> R.color.expense
                    ReminderType.RECEIVE -> R.color.income
                }
                typeIndicator.setBackgroundColor(ContextCompat.getColor(root.context, indicatorColor))

                // Due date and time (combined)
                val dueDateTime = Calendar.getInstance().apply {
                    timeInMillis = reminder.dueDate + reminder.dueTime
                }
                val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(dueDateTime.time)
                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(dueDateTime.time)
                tvDueDateTime.text = "$dateStr • $timeStr"

                // Priority indicator
                if (reminder.priority != ReminderPriority.MEDIUM) {
                    priorityDot.visibility = View.VISIBLE
                    tvPriority.visibility = View.VISIBLE
                    
                    val priorityColor = when (reminder.priority) {
                        ReminderPriority.HIGH -> "#EF4444" // Red
                        ReminderPriority.LOW -> "#3B82F6" // Blue
                        else -> "#F59E0B" // Amber
                    }
                    priorityDot.setBackgroundColor(android.graphics.Color.parseColor(priorityColor))
                    tvPriority.text = reminder.priority.getDisplayName()
                    tvPriority.setTextColor(android.graphics.Color.parseColor(priorityColor))
                } else {
                    priorityDot.visibility = View.GONE
                    tvPriority.visibility = View.GONE
                }

                // Repeat indicator
                if (reminder.repeatType != RepeatType.NONE) {
                    tvRepeat.visibility = View.VISIBLE
                    tvRepeat.text = reminder.repeatType.getDisplayName()
                } else {
                    tvRepeat.visibility = View.GONE
                }

                // Contact
                if (!reminder.contact.isNullOrBlank()) {
                    tvContact.visibility = View.VISIBLE
                    tvContact.text = reminder.contact
                } else {
                    tvContact.visibility = View.GONE
                }

                // Overdue indicator
                val isOverdue = reminder.status == ReminderStatus.OVERDUE ||
                        (reminder.status == ReminderStatus.PENDING && 
                         dueDateTime.timeInMillis < System.currentTimeMillis())
                
                if (isOverdue) {
                    layoutOverdue.visibility = View.VISIBLE
                    cardReminder.strokeWidth = 2
                    cardReminder.strokeColor = ContextCompat.getColor(root.context, R.color.error)
                } else {
                    layoutOverdue.visibility = View.GONE
                    cardReminder.strokeWidth = 0
                }

                // Completed state
                if (reminder.status == ReminderStatus.COMPLETED) {
                    cardReminder.alpha = 0.6f
                    btnMarkComplete.visibility = View.GONE
                } else {
                    cardReminder.alpha = 1.0f
                    btnMarkComplete.visibility = View.VISIBLE
                }

                // Click listeners
                cardReminder.setOnClickListener {
                    onReminderClick(reminder)
                }

                btnMarkComplete.setOnClickListener {
                    onMarkCompleteClick(reminder)
                }

                btnEdit.setOnClickListener {
                    onEditClick(reminder)
                }

                btnDelete.setOnClickListener {
                    onDeleteClick(reminder)
                }
            }
        }
    }

    private class ReminderDiffCallback : DiffUtil.ItemCallback<Reminder>() {
        override fun areItemsTheSame(oldItem: Reminder, newItem: Reminder): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Reminder, newItem: Reminder): Boolean {
            return oldItem == newItem
        }
    }
}
