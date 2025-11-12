package com.koshpal_android.koshpalapp.ui.reminders

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.koshpal_android.koshpalapp.model.*
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import com.koshpal_android.koshpalapp.ui.reminders.compose.SetReminderScreen
import com.koshpal_android.koshpalapp.ui.theme.KoshpalTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class SetReminderFragment : Fragment() {

    private val viewModel: ReminderViewModel by viewModels()
    private var editingReminder: Reminder? = null

    companion object {
        private const val ARG_REMINDER_ID = "reminder_id"
        
        fun newInstance(reminder: Reminder?): SetReminderFragment {
            val fragment = SetReminderFragment()
            reminder?.let {
                fragment.arguments = Bundle().apply {
                    putString(ARG_REMINDER_ID, it.id)
                }
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_REMINDER_ID)?.let { reminderId ->
            lifecycleScope.launch {
                viewModel.getReminderById(reminderId).collect { reminder ->
                    editingReminder = reminder
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Hide bottom navigation
        view?.post {
            (activity as? HomeActivity)?.let { homeActivity ->
                homeActivity.findViewById<View>(com.koshpal_android.koshpalapp.R.id.bottomNavigationCompose)?.visibility = View.GONE
            }
        }
        
        return ComposeView(requireContext()).apply {
            setContent {
                KoshpalTheme {
                    SetReminderScreenContent()
                }
            }
        }
    }

    @Composable
    private fun SetReminderScreenContent() {
        // Form state
        var reminderType by remember { mutableStateOf(editingReminder?.type ?: ReminderType.GIVE) }
        var personName by remember { mutableStateOf(editingReminder?.personName ?: "") }
        var contactNumber by remember { mutableStateOf(editingReminder?.contact ?: "") }
        var amount by remember { mutableStateOf(editingReminder?.amount?.toString() ?: "") }
        var purpose by remember { mutableStateOf(editingReminder?.purpose ?: "") }
        
        // Date/Time state
        val selectedDate = remember {
            val cal = Calendar.getInstance()
            editingReminder?.let { reminder ->
                cal.timeInMillis = reminder.dueDate + reminder.dueTime
            } ?: run {
                cal.add(Calendar.HOUR_OF_DAY, 1) // Default: 1 hour from now
            }
            cal
        }
        
        val selectedTime = remember {
            val cal = Calendar.getInstance()
            editingReminder?.let { reminder ->
                cal.timeInMillis = reminder.dueDate + reminder.dueTime
            } ?: run {
                cal.add(Calendar.HOUR_OF_DAY, 1)
            }
            cal
        }
        
        var repeatType by remember { mutableStateOf(editingReminder?.repeatType ?: RepeatType.NONE) }
        var priority by remember { mutableStateOf(editingReminder?.priority ?: ReminderPriority.MEDIUM) }
        
        // Date picker state
        val showDatePicker = remember { mutableStateOf(false) }
        
        // Time picker state
        val showTimePicker = remember { mutableStateOf(false) }
        
        // Show date picker
        LaunchedEffect(showDatePicker.value) {
            if (showDatePicker.value) {
                val year = selectedDate.get(Calendar.YEAR)
                val month = selectedDate.get(Calendar.MONTH)
                val day = selectedDate.get(Calendar.DAY_OF_MONTH)
                
                val dialog = DatePickerDialog(
                    requireContext(),
                    { _, selectedYear, selectedMonth, selectedDay ->
                        selectedDate.set(Calendar.YEAR, selectedYear)
                        selectedDate.set(Calendar.MONTH, selectedMonth)
                        selectedDate.set(Calendar.DAY_OF_MONTH, selectedDay)
                        showDatePicker.value = false
                    },
                    year,
                    month,
                    day
                )
                dialog.datePicker.minDate = System.currentTimeMillis()
                dialog.show()
            }
        }
        
        // Show time picker
        LaunchedEffect(showTimePicker.value) {
            if (showTimePicker.value) {
                val hour = selectedTime.get(Calendar.HOUR_OF_DAY)
                val minute = selectedTime.get(Calendar.MINUTE)
                
                val dialog = TimePickerDialog(
                    requireContext(),
                    { _, selectedHour, selectedMinute ->
                        selectedTime.set(Calendar.HOUR_OF_DAY, selectedHour)
                        selectedTime.set(Calendar.MINUTE, selectedMinute)
                        selectedTime.set(Calendar.SECOND, 0)
                        selectedDate.set(Calendar.HOUR_OF_DAY, selectedHour)
                        selectedDate.set(Calendar.MINUTE, selectedMinute)
                        showTimePicker.value = false
                    },
                    hour,
                    minute,
                    false
                )
                dialog.show()
            }
        }
        
        SetReminderScreen(
            reminderType = reminderType,
            personName = personName,
            contactNumber = contactNumber,
            amount = amount,
            purpose = purpose,
            selectedDate = selectedDate,
            selectedTime = selectedTime,
            repeatType = repeatType,
            priority = priority,
            onReminderTypeChange = { reminderType = it },
            onPersonNameChange = { personName = it },
            onContactNumberChange = { contactNumber = it },
            onAmountChange = { amount = it },
            onPurposeChange = { purpose = it },
            onDateClick = { showDatePicker.value = true },
            onTimeClick = { showTimePicker.value = true },
            onRepeatTypeChange = { repeatType = it },
            onPriorityChange = { priority = it },
            onSaveClick = {
                saveReminder(
                    reminderType,
                    personName,
                    contactNumber,
                    amount,
                    purpose,
                    selectedDate,
                    selectedTime,
                    repeatType,
                    priority
                )
            },
            onCancelClick = {
                navigateBack()
            },
            onBackClick = {
                navigateBack()
            }
        )
    }

    private fun saveReminder(
        type: ReminderType,
        personName: String,
        contact: String,
        amountText: String,
        purpose: String,
        selectedDate: Calendar,
        selectedTime: Calendar,
        repeatType: RepeatType,
        priority: ReminderPriority
    ) {
        // Validation
        if (personName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter person's name", Toast.LENGTH_SHORT).show()
            return
        }

        if (amountText.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (purpose.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter purpose", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if date/time is in the past
        if (selectedDate.timeInMillis < System.currentTimeMillis()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Past Date/Time")
                .setMessage("The selected date/time is in the past. Do you want to continue?")
                .setPositiveButton("Continue") { _, _ ->
                    proceedWithSave(type, personName, contact, amount, purpose, selectedDate, selectedTime, repeatType, priority)
                }
                .setNegativeButton("Change Date/Time", null)
                .show()
            return
        }

        proceedWithSave(type, personName, contact, amount, purpose, selectedDate, selectedTime, repeatType, priority)
    }

    private fun proceedWithSave(
        type: ReminderType,
        personName: String,
        contact: String?,
        amount: Double,
        purpose: String,
        selectedDate: Calendar,
        selectedTime: Calendar,
        repeatType: RepeatType,
        priority: ReminderPriority
    ) {
        // Calculate due date (date only, time of day = 0)
        val dueDate = Calendar.getInstance().apply {
            timeInMillis = selectedDate.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Calculate due time (time from midnight in milliseconds)
        val dueTime = (selectedTime.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000 +
                selectedTime.get(Calendar.MINUTE) * 60 * 1000).toLong()

        val reminder = if (editingReminder != null) {
            editingReminder!!.copy(
                type = type,
                personName = personName,
                contact = contact?.ifEmpty { null },
                amount = amount,
                purpose = purpose,
                dueDate = dueDate,
                dueTime = dueTime,
                repeatType = repeatType,
                priority = priority,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            Reminder(
                type = type,
                personName = personName,
                contact = contact?.ifEmpty { null },
                amount = amount,
                purpose = purpose,
                dueDate = dueDate,
                dueTime = dueTime,
                repeatType = repeatType,
                priority = priority,
                status = ReminderStatus.PENDING
            )
        }

        // Save to database
        if (editingReminder != null) {
            viewModel.updateReminder(reminder)
            ReminderNotificationHelper.cancelNotification(requireContext(), reminder.notificationId)
        } else {
            viewModel.insertReminder(reminder)
        }

        // Schedule notification
        ReminderNotificationHelper.scheduleReminder(requireContext(), reminder)

        Toast.makeText(
            requireContext(),
            if (editingReminder != null) "Reminder updated!" else "Reminder created!",
            Toast.LENGTH_SHORT
        ).show()

        navigateBack()
    }

    private fun navigateBack() {
        (activity as? HomeActivity)?.onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Restore bottom navigation
        (activity as? HomeActivity)?.let { homeActivity ->
            homeActivity.findViewById<View>(com.koshpal_android.koshpalapp.R.id.bottomNavigationCompose)?.visibility = View.VISIBLE
        }
    }
}
