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

                // Type badge (now a TextView)
                val typeEmoji = if (reminder.type == ReminderType.GIVE) "💸" else "💰"
                chipType.text = "$typeEmoji ${reminder.type.getDisplayName()}"

                // Due date and time
                val dueDateTime = Calendar.getInstance().apply {
                    timeInMillis = reminder.dueDate + reminder.dueTime
                }
                tvDueDate.text = dateFormat.format(dueDateTime.time)
                tvDueTime.text = timeFormat.format(dueDateTime.time)

                // Priority badge (now a TextView in a card)
                if (reminder.priority != ReminderPriority.MEDIUM) {
                    chipPriority.visibility = View.VISIBLE
                    val priorityEmoji = when (reminder.priority) {
                        ReminderPriority.HIGH -> "🔥"
                        ReminderPriority.LOW -> "🐌"
                        else -> ""
                    }
                    val tvPriorityText = chipPriority.findViewById<TextView>(R.id.tvPriorityText)
                    tvPriorityText?.text = "$priorityEmoji ${reminder.priority.getDisplayName()}"
                } else {
                    chipPriority.visibility = View.GONE
                }

                // Repeat indicator
                if (reminder.repeatType != RepeatType.NONE) {
                    layoutRepeat.visibility = View.VISIBLE
                    tvRepeat.text = "🔁 ${reminder.repeatType.getDisplayName()}"
                } else {
                    layoutRepeat.visibility = View.GONE
                }

                // Contact
                if (!reminder.contact.isNullOrBlank()) {
                    tvContact.visibility = View.VISIBLE
                    tvContact.text = "📞 ${reminder.contact}"
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
