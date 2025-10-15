package com.koshpal_android.koshpalapp.ui.reminders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.FragmentRemindersListBinding
import com.koshpal_android.koshpalapp.model.Reminder
import com.koshpal_android.koshpalapp.model.ReminderType
import com.koshpal_android.koshpalapp.model.getColorResource
import com.koshpal_android.koshpalapp.model.getDisplayName
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class RemindersListFragment : Fragment() {

    private var _binding: FragmentRemindersListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReminderViewModel by viewModels()
    private lateinit var reminderAdapter: ReminderAdapter

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRemindersListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        android.util.Log.d("RemindersListFragment", "✅ Reminders List Fragment created")
    }

    private fun setupRecyclerView() {
        reminderAdapter = ReminderAdapter(
            onReminderClick = { reminder ->
                // Open reminder details or edit
                navigateToSetReminder(reminder)
            },
            onMarkCompleteClick = { reminder ->
                showMarkCompleteDialog(reminder)
            },
            onEditClick = { reminder ->
                navigateToSetReminder(reminder)
            },
            onDeleteClick = { reminder ->
                showDeleteDialog(reminder)
            }
        )

        binding.rvReminders.apply {
            adapter = reminderAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            // Back button
            btnBack.setOnClickListener {
                (activity as? HomeActivity)?.onBackPressed()
            }

            // FAB - Add new reminder
            fabAddReminder.setOnClickListener {
                navigateToSetReminder(null)
            }

            // Search button (placeholder for now)
            btnSearch.setOnClickListener {
                Toast.makeText(requireContext(), "Search feature coming soon!", Toast.LENGTH_SHORT).show()
            }

            // Mark next reminder complete
            btnMarkNextComplete.setOnClickListener {
                viewModel.nextReminder.value?.let { reminder ->
                    showMarkCompleteDialog(reminder)
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe all reminders
            viewModel.allReminders.collect { reminders ->
                android.util.Log.d("RemindersListFragment", "📋 Reminders loaded: ${reminders.size}")
                reminderAdapter.submitList(reminders)

                // Show/hide empty state
                if (reminders.isEmpty()) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.rvReminders.visibility = View.GONE
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.rvReminders.visibility = View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Observe next reminder
            viewModel.nextReminder.collect { nextReminder ->
                if (nextReminder != null) {
                    binding.cardNextReminder.visibility = View.VISIBLE
                    binding.cardNoNextReminder.visibility = View.GONE
                    displayNextReminder(nextReminder)
                } else {
                    binding.cardNextReminder.visibility = View.GONE
                    binding.cardNoNextReminder.visibility = View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Observe UI state for statistics
            viewModel.uiState.collect { uiState ->
                updateStatistics(uiState)

                // Show messages
                uiState.successMessage?.let { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    viewModel.clearMessages()
                }

                uiState.errorMessage?.let { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    viewModel.clearMessages()
                }
            }
        }
    }

    private fun displayNextReminder(reminder: Reminder) {
        binding.apply {
            tvNextPersonName.text = reminder.personName
            tvNextAmount.text = "₹${String.format("%.0f", reminder.amount)}"
            tvNextPurpose.text = reminder.purpose

            // Type badge (now a TextView)
            val typeEmoji = if (reminder.type == ReminderType.GIVE) "💸" else "💰"
            chipNextType.text = "$typeEmoji ${reminder.type.getDisplayName()}"

            // Due date time
            val dueDateTime = Calendar.getInstance().apply {
                timeInMillis = reminder.dueDate + reminder.dueTime
            }

            val now = Calendar.getInstance()
            val dueDateStr = when {
                isSameDay(now, dueDateTime) -> "Today"
                isTomorrow(now, dueDateTime) -> "Tomorrow"
                else -> dateFormat.format(dueDateTime.time)
            }

            tvNextDueDateTime.text = "$dueDateStr at ${timeFormat.format(dueDateTime.time)}"
        }
    }

    private fun updateStatistics(uiState: ReminderUiState) {
        binding.apply {
            tvPendingCount.text = uiState.pendingCount.toString()

            // Format amounts
            tvToGive.text = formatAmount(uiState.totalAmountToGive)
            tvToReceive.text = formatAmount(uiState.totalAmountToReceive)
        }
    }

    private fun formatAmount(amount: Double): String {
        return when {
            amount >= 100000 -> "₹${String.format("%.1f", amount / 100000)}L"
            amount >= 1000 -> "₹${String.format("%.1f", amount / 1000)}K"
            else -> "₹${String.format("%.0f", amount)}"
        }
    }

    private fun showMarkCompleteDialog(reminder: Reminder) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Mark as Completed?")
            .setMessage("Mark reminder for ${reminder.personName} (₹${String.format("%.0f", reminder.amount)}) as completed?")
            .setPositiveButton("Mark Paid") { _, _ ->
                viewModel.markReminderCompleted(reminder.id)
                // Cancel notification
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
                // Cancel notification
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
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack("set_reminder")
                .commit()
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isTomorrow(today: Calendar, date: Calendar): Boolean {
        val tomorrow = today.clone() as Calendar
        tomorrow.add(Calendar.DAY_OF_YEAR, 1)
        return isSameDay(tomorrow, date)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = RemindersListFragment()
    }
}
