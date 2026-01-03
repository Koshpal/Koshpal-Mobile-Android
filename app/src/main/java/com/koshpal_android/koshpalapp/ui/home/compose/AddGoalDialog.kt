package com.koshpal_android.koshpalapp.ui.home.compose

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.koshpal_android.koshpalapp.ui.goals.GoalsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalDialog(
    viewModel: GoalsViewModel,
    onDismiss: () -> Unit,
    onGoalCreated: () -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var goalName by remember { mutableStateOf("") }
    var goalAmount by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("") }
    var currentAmount by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("🎯") }

    val isCreating by viewModel.isCreatingGoal.observeAsState(false)
    val createResult by viewModel.createGoalResult.observeAsState()

    // Handle creation result
    LaunchedEffect(createResult) {
        when (createResult) {
            is GoalsViewModel.CreateGoalResult.Success -> {
                onGoalCreated()
                onDismiss()
                viewModel.clearCreateGoalResult()
            }
            is GoalsViewModel.CreateGoalResult.Error -> {
                // Error will be shown in the dialog
            }
            null -> {}
        }
    }

    // Date picker
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, day: Int ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(year, month, day)
            }
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            targetDate = dateFormat.format(selectedCalendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        // Set minimum date to tomorrow
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }
        datePicker.minDate = tomorrow.timeInMillis
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "Add Financial Goal",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Goal Name
                OutlinedTextField(
                    value = goalName,
                    onValueChange = { goalName = it },
                    label = { Text("Goal Name") },
                    placeholder = { Text("e.g., Vacation, Emergency Fund") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = goalName.isBlank() && createResult is GoalsViewModel.CreateGoalResult.Error
                )

                // Goal Amount
                OutlinedTextField(
                    value = goalAmount,
                    onValueChange = { goalAmount = it },
                    label = { Text("Target Amount (₹)") },
                    placeholder = { Text("50000") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = (goalAmount.toDoubleOrNull() ?: 0.0) <= 0 && createResult is GoalsViewModel.CreateGoalResult.Error
                )

                // Current Amount (Optional)
                OutlinedTextField(
                    value = currentAmount,
                    onValueChange = { currentAmount = it },
                    label = { Text("Current Amount (₹) - Optional") },
                    placeholder = { Text("10000") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Target Date
                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { },
                    label = { Text("Target Date") },
                    placeholder = { Text("Select target date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() },
                    singleLine = true,
                    readOnly = true,
                    isError = targetDate.isBlank() && createResult is GoalsViewModel.CreateGoalResult.Error,
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Text("📅", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                )

                // Icon Selection (Simple dropdown)
                val icons = listOf("🎯", "🏠", "🚗", "✈️", "💰", "🏦", "📱", "🎓", "💍", "🏖️")
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = "$selectedIcon ${getIconDescription(selectedIcon)}",
                        onValueChange = { },
                        label = { Text("Icon") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        icons.forEach { icon ->
                            DropdownMenuItem(
                                text = { Text("$icon ${getIconDescription(icon)}") },
                                onClick = {
                                    selectedIcon = icon
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Error message
                if (createResult is GoalsViewModel.CreateGoalResult.Error) {
                    Text(
                        text = (createResult as GoalsViewModel.CreateGoalResult.Error).message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val amount = goalAmount.toDoubleOrNull() ?: 0.0
                            val saving = currentAmount.toDoubleOrNull() ?: 0.0

                            viewModel.createFinancialGoal(
                                goalName = goalName.trim(),
                                goalAmount = amount,
                                goalDate = targetDate,
                                saving = saving,
                                icon = selectedIcon
                            )
                        },
                        enabled = !isCreating && goalName.isNotBlank() &&
                                 (goalAmount.toDoubleOrNull() ?: 0.0) > 0 && targetDate.isNotBlank()
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Create Goal")
                        }
                    }
                }
            }
        }
    }
}

private fun getIconDescription(icon: String): String {
    return when (icon) {
        "🎯" -> "General"
        "🏠" -> "House"
        "🚗" -> "Car"
        "✈️" -> "Travel"
        "💰" -> "Savings"
        "🏦" -> "Investment"
        "📱" -> "Gadget"
        "🎓" -> "Education"
        "💍" -> "Wedding"
        "🏖️" -> "Vacation"
        else -> "Goal"
    }
}
