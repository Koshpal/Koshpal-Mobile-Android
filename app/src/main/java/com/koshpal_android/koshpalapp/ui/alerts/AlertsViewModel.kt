package com.koshpal_android.koshpalapp.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.alerts.SpendingAlertManager
import com.koshpal_android.koshpalapp.data.local.UserPreferences
import com.koshpal_android.koshpalapp.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val budgetRepository: BudgetRepository,
    private val spendingAlertManager: SpendingAlertManager
) : ViewModel() {

    private val _alertSettings = MutableStateFlow(AlertSettings())
    val alertSettings: StateFlow<AlertSettings> = _alertSettings.asStateFlow()

    private val _activeAlerts = MutableStateFlow<List<ActiveAlert>>(emptyList())
    val activeAlerts: StateFlow<List<ActiveAlert>> = _activeAlerts.asStateFlow()

    private val _uiEvents = Channel<AlertsUiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    fun loadAlertSettings() {
        viewModelScope.launch {
            try {
                // Load settings from UserPreferences
                val settings = AlertSettings(
                    alertsEnabled = userPreferences.getAlertsEnabled(),
                    threshold50Enabled = userPreferences.getThreshold50Enabled(),
                    threshold80Enabled = userPreferences.getThreshold80Enabled(),
                    threshold100Enabled = userPreferences.getThreshold100Enabled(),
                    dailySummaryEnabled = userPreferences.getDailySummaryEnabled(),
                    weeklySummaryEnabled = userPreferences.getWeeklySummaryEnabled()
                )
                _alertSettings.value = settings
            } catch (e: Exception) {
                _uiEvents.send(AlertsUiEvent.ShowMessage("Failed to load alert settings"))
            }
        }
    }

    fun loadActiveAlerts() {
        viewModelScope.launch {
            try {
                // Load active budget alerts
                val alerts = generateSampleActiveAlerts()
                _activeAlerts.value = alerts
            } catch (e: Exception) {
                _activeAlerts.value = emptyList()
            }
        }
    }

    fun setAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAlertsEnabled(enabled)
            _alertSettings.value = _alertSettings.value.copy(alertsEnabled = enabled)
            
            if (enabled) {
                spendingAlertManager.enableAlerts()
            } else {
                spendingAlertManager.disableAlerts()
            }
        }
    }

    fun setThresholdAlert(threshold: Int, enabled: Boolean) {
        viewModelScope.launch {
            when (threshold) {
                50 -> {
                    userPreferences.setThreshold50Enabled(enabled)
                    _alertSettings.value = _alertSettings.value.copy(threshold50Enabled = enabled)
                }
                80 -> {
                    userPreferences.setThreshold80Enabled(enabled)
                    _alertSettings.value = _alertSettings.value.copy(threshold80Enabled = enabled)
                }
                100 -> {
                    userPreferences.setThreshold100Enabled(enabled)
                    _alertSettings.value = _alertSettings.value.copy(threshold100Enabled = enabled)
                }
            }
            
            spendingAlertManager.updateThresholdSettings()
        }
    }

    fun setDailySummaryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setDailySummaryEnabled(enabled)
            _alertSettings.value = _alertSettings.value.copy(dailySummaryEnabled = enabled)
            
            if (enabled) {
                spendingAlertManager.scheduleDailySummary()
            } else {
                spendingAlertManager.cancelDailySummary()
            }
        }
    }

    fun setWeeklySummaryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setWeeklySummaryEnabled(enabled)
            _alertSettings.value = _alertSettings.value.copy(weeklySummaryEnabled = enabled)
            
            if (enabled) {
                spendingAlertManager.scheduleWeeklySummary()
            } else {
                spendingAlertManager.cancelWeeklySummary()
            }
        }
    }

    fun sendTestNotification() {
        viewModelScope.launch {
            try {
                spendingAlertManager.sendTestNotification()
                _uiEvents.send(AlertsUiEvent.TestNotificationSent)
            } catch (e: Exception) {
                _uiEvents.send(AlertsUiEvent.ShowMessage("Failed to send test notification"))
            }
        }
    }

    fun onActiveAlertClick(alert: ActiveAlert) {
        // Navigate to budget details for this category
        // This could emit a navigation event
    }

    private fun generateSampleActiveAlerts(): List<ActiveAlert> {
        return listOf(
            ActiveAlert(
                id = "1",
                categoryName = "Food & Dining",
                budgetAmount = 6000.0,
                spentAmount = 4800.0,
                percentage = 80,
                alertType = AlertType.WARNING
            ),
            ActiveAlert(
                id = "2",
                categoryName = "Entertainment",
                budgetAmount = 2000.0,
                spentAmount = 2200.0,
                percentage = 110,
                alertType = AlertType.EXCEEDED
            ),
            ActiveAlert(
                id = "3",
                categoryName = "Shopping",
                budgetAmount = 5000.0,
                spentAmount = 2500.0,
                percentage = 50,
                alertType = AlertType.INFO
            )
        )
    }
}

data class ActiveAlert(
    val id: String,
    val categoryName: String,
    val budgetAmount: Double,
    val spentAmount: Double,
    val percentage: Int,
    val alertType: AlertType
) {
    fun getFormattedBudget(): String = "₹${String.format("%.0f", budgetAmount)}"
    fun getFormattedSpent(): String = "₹${String.format("%.0f", spentAmount)}"
}

enum class AlertType {
    INFO,      // 50% threshold
    WARNING,   // 80% threshold
    EXCEEDED   // 100%+ threshold
}
