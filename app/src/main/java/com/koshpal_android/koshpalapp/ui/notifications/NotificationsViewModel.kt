package com.koshpal_android.koshpalapp.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _allNotifications = MutableStateFlow<List<AppNotification>>(emptyList())
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _notificationCounts = MutableStateFlow(NotificationCounts())
    val notificationCounts: StateFlow<NotificationCounts> = _notificationCounts.asStateFlow()

    private val _currentFilter = MutableStateFlow("all")

    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load notifications from local storage or repository
                val notifications = generateSampleNotifications()
                _allNotifications.value = notifications
                applyFilter(_currentFilter.value)
                updateCounts()
            } catch (e: Exception) {
                // Handle error
                _notifications.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterNotifications(filter: String) {
        _currentFilter.value = filter
        applyFilter(filter)
    }

    fun onNotificationClick(notification: AppNotification) {
        // Mark as read and handle click
        markAsRead(notification.id)
        // Navigate to relevant screen based on notification type
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            val updatedNotifications = _allNotifications.value.map { notification ->
                if (notification.id == notificationId) {
                    notification.copy(isRead = true)
                } else {
                    notification
                }
            }
            _allNotifications.value = updatedNotifications
            applyFilter(_currentFilter.value)
            updateCounts()
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val updatedNotifications = _allNotifications.value.map { notification ->
                notification.copy(isRead = true)
            }
            _allNotifications.value = updatedNotifications
            applyFilter(_currentFilter.value)
            updateCounts()
        }
    }

    private fun applyFilter(filter: String) {
        val filtered = when (filter) {
            "alerts" -> _allNotifications.value.filter { it.type == NotificationType.ALERT }
            "updates" -> _allNotifications.value.filter { it.type == NotificationType.UPDATE }
            else -> _allNotifications.value // "all"
        }
        _notifications.value = filtered.sortedByDescending { it.timestamp }
    }

    private fun updateCounts() {
        val unread = _allNotifications.value.count { !it.isRead }
        val total = _allNotifications.value.size
        _notificationCounts.value = NotificationCounts(unread, total)
    }

    private fun generateSampleNotifications(): List<AppNotification> {
        val currentTime = System.currentTimeMillis()
        return listOf(
            AppNotification(
                id = "1",
                title = "Budget Alert: Food & Dining",
                message = "You've spent 80% of your food budget this month (₹4,800 of ₹6,000)",
                type = NotificationType.ALERT,
                timestamp = currentTime - 3600000, // 1 hour ago
                isRead = false
            ),
            AppNotification(
                id = "2",
                title = "Daily Summary",
                message = "You spent ₹450 today across 3 transactions",
                type = NotificationType.UPDATE,
                timestamp = currentTime - 7200000, // 2 hours ago
                isRead = false
            ),
            AppNotification(
                id = "3",
                title = "Savings Goal Achievement",
                message = "Congratulations! You've reached 75% of your vacation savings goal",
                type = NotificationType.UPDATE,
                timestamp = currentTime - 86400000, // 1 day ago
                isRead = true
            ),
            AppNotification(
                id = "4",
                title = "Budget Exceeded: Entertainment",
                message = "You've exceeded your entertainment budget by ₹500 this month",
                type = NotificationType.ALERT,
                timestamp = currentTime - 172800000, // 2 days ago
                isRead = false
            ),
            AppNotification(
                id = "5",
                title = "New Transaction Detected",
                message = "₹1,200 spent at Amazon - Auto-categorized as Shopping",
                type = NotificationType.UPDATE,
                timestamp = currentTime - 259200000, // 3 days ago
                isRead = true
            )
        )
    }
}

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long,
    val isRead: Boolean = false
)

enum class NotificationType {
    ALERT,
    UPDATE
}
