package com.koshpal_android.koshpalapp.ui.reminders

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.databinding.FragmentSetReminderBinding
import com.koshpal_android.koshpalapp.model.*
import com.koshpal_android.koshpalapp.ui.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class SetReminderFragment : Fragment() {

    private var _binding: FragmentSetReminderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReminderViewModel by viewModels()

    private var editingReminder: Reminder? = null
    private var selectedDate: Calendar = Calendar.getInstance()
    private var selectedTime: Calendar = Calendar.getInstance()

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_REMINDER_ID)?.let { reminderId ->
            lifecycleScope.launch {
                viewModel.getReminderById(reminderId).collect { reminder ->
                    editingReminder = reminder
                    reminder?.let { populateFields(it) }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetReminderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupClickListeners()

        android.util.Log.d("SetReminderFragment", "✅ Set Reminder Fragment created")
    }

    private fun setupUI() {
        binding.apply {
            // Set title based on edit mode
            tvTitle.text = if (editingReminder != null) "Edit Reminder" else "Set Reminder"

            // Set default date and time to current + 1 hour
            selectedDate.add(Calendar.HOUR_OF_DAY, 1)
            selectedTime.timeInMillis = selectedDate.timeInMillis

            updateDateTimeDisplay()
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            // Back button
            btnBack.setOnClickListener {
                navigateBack()
            }

            // Type chips
            chipGroupType.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.chipGive -> {
                        chipGive.setChipBackgroundColorResource(R.color.expense)
                        chipGive.setTextColor(resources.getColor(R.color.white, null))
                        chipReceive.setChipBackgroundColorResource(R.color.background_light)
                        chipReceive.setTextColor(resources.getColor(R.color.text_primary, null))
                    }
                    R.id.chipReceive -> {
                        chipReceive.setChipBackgroundColorResource(R.color.income)
                        chipReceive.setTextColor(resources.getColor(R.color.white, null))
                        chipGive.setChipBackgroundColorResource(R.color.background_light)
                        chipGive.setTextColor(resources.getColor(R.color.text_primary, null))
                    }
                }
            }

            // Date selection
            btnSelectDate.setOnClickListener {
                showDatePicker()
            }

            // Time selection
            btnSelectTime.setOnClickListener {
                showTimePicker()
            }

            // Cancel button
            btnCancel.setOnClickListener {
                navigateBack()
            }

            // Save reminder button
            btnSaveReminder.setOnClickListener {
                saveReminder()
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = selectedDate.get(Calendar.YEAR)
        val month = selectedDate.get(Calendar.MONTH)
        val day = selectedDate.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate.set(Calendar.YEAR, selectedYear)
                selectedDate.set(Calendar.MONTH, selectedMonth)
                selectedDate.set(Calendar.DAY_OF_MONTH, selectedDay)
                selectedDate.set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY))
                selectedDate.set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE))

                updateDateTimeDisplay()
                android.util.Log.d("SetReminderFragment", "📅 Date selected: ${dateFormat.format(selectedDate.time)}")
            },
            year,
            month,
            day
        )

        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val hour = selectedTime.get(Calendar.HOUR_OF_DAY)
        val minute = selectedTime.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                selectedTime.set(Calendar.HOUR_OF_DAY, selectedHour)
                selectedTime.set(Calendar.MINUTE, selectedMinute)
                selectedTime.set(Calendar.SECOND, 0)

                // Update selected date with new time
                selectedDate.set(Calendar.HOUR_OF_DAY, selectedHour)
                selectedDate.set(Calendar.MINUTE, selectedMinute)

                updateDateTimeDisplay()
                android.util.Log.d("SetReminderFragment", "⏰ Time selected: ${timeFormat.format(selectedTime.time)}")
            },
            hour,
            minute,
            false // 12-hour format
        )

        timePickerDialog.show()
    }

    private fun updateDateTimeDisplay() {
        binding.btnSelectDate.text = dateFormat.format(selectedDate.time)
        binding.btnSelectTime.text = timeFormat.format(selectedTime.time)
    }

    private fun saveReminder() {
        val personName = binding.etPersonName.text.toString().trim()
        val contact = binding.etContact.text.toString().trim()
        val amountText = binding.etAmount.text.toString().trim()
        val purpose = binding.etPurpose.text.toString().trim()

        // Validation
        if (personName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter person's name", Toast.LENGTH_SHORT).show()
            binding.etPersonName.requestFocus()
            return
        }

        if (amountText.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter amount", Toast.LENGTH_SHORT).show()
            binding.etAmount.requestFocus()
            return
        }

        if (purpose.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter purpose", Toast.LENGTH_SHORT).show()
            binding.etPurpose.requestFocus()
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            binding.etAmount.requestFocus()
            return
        }

        // Check if date/time is in the past
        if (selectedDate.timeInMillis < System.currentTimeMillis()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Past Date/Time")
                .setMessage("The selected date/time is in the past. Do you want to continue?")
                .setPositiveButton("Continue") { _, _ ->
                    proceedWithSave(personName, contact, amount, purpose)
                }
                .setNegativeButton("Change Date/Time") { _, _ ->
                    showDatePicker()
                }
                .show()
            return
        }

        proceedWithSave(personName, contact, amount, purpose)
    }

    private fun proceedWithSave(personName: String, contact: String?, amount: Double, purpose: String) {
        // Get reminder type
        val type = when (binding.chipGroupType.checkedChipId) {
            R.id.chipGive -> ReminderType.GIVE
            R.id.chipReceive -> ReminderType.RECEIVE
            else -> ReminderType.GIVE
        }

        // Get repeat type
        val repeatType = when (binding.chipGroupRepeat.checkedChipId) {
            R.id.chipRepeatNone -> RepeatType.NONE
            R.id.chipRepeatDaily -> RepeatType.DAILY
            R.id.chipRepeatWeekly -> RepeatType.WEEKLY
            R.id.chipRepeatMonthly -> RepeatType.MONTHLY
            else -> RepeatType.NONE
        }

        // Get priority
        val priority = when (binding.chipGroupPriority.checkedChipId) {
            R.id.chipPriorityLow -> ReminderPriority.LOW
            R.id.chipPriorityMedium -> ReminderPriority.MEDIUM
            R.id.chipPriorityHigh -> ReminderPriority.HIGH
            else -> ReminderPriority.MEDIUM
        }

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
            // Update existing reminder
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
            // Create new reminder
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
            // Cancel old notification and schedule new one
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

    private fun populateFields(reminder: Reminder) {
        binding.apply {
            // Set type
            when (reminder.type) {
                ReminderType.GIVE -> chipGive.isChecked = true
                ReminderType.RECEIVE -> chipReceive.isChecked = true
            }

            // Set fields
            etPersonName.setText(reminder.personName)
            etContact.setText(reminder.contact ?: "")
            etAmount.setText(reminder.amount.toString())
            etPurpose.setText(reminder.purpose)

            // Set date and time
            selectedDate.timeInMillis = reminder.dueDate + reminder.dueTime
            selectedTime.timeInMillis = reminder.dueDate + reminder.dueTime
            updateDateTimeDisplay()

            // Set repeat type
            when (reminder.repeatType) {
                RepeatType.NONE -> chipRepeatNone.isChecked = true
                RepeatType.DAILY -> chipRepeatDaily.isChecked = true
                RepeatType.WEEKLY -> chipRepeatWeekly.isChecked = true
                RepeatType.MONTHLY -> chipRepeatMonthly.isChecked = true
            }

            // Set priority
            when (reminder.priority) {
                ReminderPriority.LOW -> chipPriorityLow.isChecked = true
                ReminderPriority.MEDIUM -> chipPriorityMedium.isChecked = true
                ReminderPriority.HIGH -> chipPriorityHigh.isChecked = true
            }
        }
    }

    private fun navigateBack() {
        (activity as? HomeActivity)?.onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_REMINDER_ID = "reminder_id"

        fun newInstance(reminder: Reminder? = null): SetReminderFragment {
            return SetReminderFragment().apply {
                arguments = Bundle().apply {
                    reminder?.let {
                        putString(ARG_REMINDER_ID, it.id)
                    }
                }
            }
        }
    }
}
