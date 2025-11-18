package com.koshpal_android.koshpalapp.ui.reminders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.koshpal_android.koshpalapp.model.Reminder
import com.koshpal_android.koshpalapp.model.ReminderStatus
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import com.koshpal_android.koshpalapp.ui.reminders.compose.ReminderFilter
import com.koshpal_android.koshpalapp.ui.reminders.compose.RemindersScreen
import com.koshpal_android.koshpalapp.ui.theme.KoshpalTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RemindersListFragment : Fragment() {

    private val viewModel: ReminderViewModel by viewModels()
    private var selectedFilter by mutableStateOf(ReminderFilter.ALL)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setStatusBarColor()
        
        return ComposeView(requireContext()).apply {
            setContent {
                KoshpalTheme {
                    RemindersScreenContent()
                }
            }
        }
    }

    @Composable
    private fun RemindersScreenContent() {
        val allReminders by viewModel.allReminders.collectAsState()
        val uiState by viewModel.uiState.collectAsState()
        
        // Filter reminders based on selected filter
        val filteredReminders = remember(allReminders, selectedFilter) {
            when (selectedFilter) {
                ReminderFilter.ALL -> allReminders.filter { it.status == ReminderStatus.PENDING }
                ReminderFilter.TO_PAY -> allReminders.filter { 
                    it.type == com.koshpal_android.koshpalapp.model.ReminderType.GIVE && 
                    it.status == ReminderStatus.PENDING 
                }
                ReminderFilter.TO_RECEIVE -> allReminders.filter { 
                    it.type == com.koshpal_android.koshpalapp.model.ReminderType.RECEIVE && 
                    it.status == ReminderStatus.PENDING 
                }
                ReminderFilter.PENDING -> allReminders.filter { it.status == ReminderStatus.PENDING }
            }
        }
        
        // Show messages
        LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
            uiState.successMessage?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
            
            uiState.errorMessage?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        }
        
        // Get pending reminders for summary cards
        val pendingReminders = remember(allReminders) {
            allReminders.filter { it.status == ReminderStatus.PENDING }
        }
        
        RemindersScreen(
            reminders = filteredReminders,
            uiState = uiState,
            selectedFilter = selectedFilter,
            onAddReminderClick = {
                navigateToSetReminder(null)
            },
            onReminderClick = { reminder ->
                navigateToSetReminder(reminder)
            },
            onDeleteReminder = { reminder ->
                showDeleteDialog(reminder)
            },
            onMarkAsPaid = { reminder ->
                showMarkCompleteDialog(reminder)
            },
            onFilterSelected = { filter ->
                selectedFilter = filter
            },
            pendingReminders = pendingReminders
        )
    }

    private fun setStatusBarColor() {
        activity?.window?.let { window ->
            window.statusBarColor = androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.black)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                window.navigationBarColor = androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.black)
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                var flags = window.decorView.systemUiVisibility
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                }
                window.decorView.systemUiVisibility = flags
            }
        }
    }

    private fun showMarkCompleteDialog(reminder: Reminder) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Mark as Completed?")
            .setMessage("Mark reminder for ${reminder.personName} (₹${String.format("%.0f", reminder.amount)}) as completed?")
            .setPositiveButton("Mark Paid") { _, _ ->
                viewModel.markReminderCompleted(reminder.id)
                ReminderNotificationHelper.cancelNotification(requireContext(), reminder.notificationId)
                Toast.makeText(requireContext(), "✅ Reminder completed!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(reminder: Reminder) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Reminder?")
            .setMessage("Are you sure you want to delete this reminder?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteReminder(reminder)
                ReminderNotificationHelper.cancelNotification(requireContext(), reminder.notificationId)
                Toast.makeText(requireContext(), "🗑️ Reminder deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToSetReminder(reminder: Reminder?) {
        val fragment = SetReminderFragment.newInstance(reminder)
        (activity as? HomeActivity)?.let { activity ->
            activity.supportFragmentManager.beginTransaction()
                .replace(com.koshpal_android.koshpalapp.R.id.fragmentContainer, fragment)
                .addToBackStack("set_reminder")
                .commit()
            
            view?.post {
                activity.findViewById<View>(com.koshpal_android.koshpalapp.R.id.bottomNavigationCompose)?.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        setStatusBarColor()
    }

    companion object {
        fun newInstance() = RemindersListFragment()
    }
}
