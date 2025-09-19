package com.koshpal_android.koshpalapp.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.*

class NotificationsAdapter(
    private val onNotificationClick: (AppNotification) -> Unit,
    private val onMarkAsRead: (AppNotification) -> Unit
) : ListAdapter<AppNotification, NotificationsAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    private val dateFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: AppNotification) {
            binding.apply {
                tvTitle.text = notification.title
                tvMessage.text = notification.message
                tvTimestamp.text = dateFormatter.format(Date(notification.timestamp))

                // Set icon based on notification type
                ivIcon.setImageResource(
                    when (notification.type) {
                        NotificationType.ALERT -> R.drawable.ic_warning
                        NotificationType.UPDATE -> R.drawable.ic_info
                    }
                )

                // Set read/unread state
                root.alpha = if (notification.isRead) 0.6f else 1.0f
                viewUnreadIndicator.visibility = if (notification.isRead) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }

                // Set background color based on type
                root.setBackgroundResource(
                    when (notification.type) {
                        NotificationType.ALERT -> R.color.error_light
                        NotificationType.UPDATE -> R.color.background_light
                    }
                )

                root.setOnClickListener {
                    onNotificationClick(notification)
                }

                btnMarkAsRead.setOnClickListener {
                    onMarkAsRead(notification)
                }

                // Hide mark as read button if already read
                btnMarkAsRead.visibility = if (notification.isRead) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }
            }
        }
    }

    private class NotificationDiffCallback : DiffUtil.ItemCallback<AppNotification>() {
        override fun areItemsTheSame(
            oldItem: AppNotification,
            newItem: AppNotification
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: AppNotification,
            newItem: AppNotification
        ): Boolean {
            return oldItem == newItem
        }
    }
}
