package com.koshpal_android.koshpalapp.ui.reminders.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.model.RepeatType
import com.koshpal_android.koshpalapp.model.ReminderPriority
import com.koshpal_android.koshpalapp.model.ReminderType
import com.koshpal_android.koshpalapp.model.getDisplayName
import com.koshpal_android.koshpalapp.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Set Reminder Screen Composable
 * Stateless - receives all form state and event handlers as parameters
 */
@Composable
fun SetReminderScreen(
    reminderType: ReminderType,
    personName: String,
    contactNumber: String,
    amount: String,
    purpose: String,
    selectedDate: Calendar,
    selectedTime: Calendar,
    repeatType: RepeatType,
    priority: ReminderPriority,
    onReminderTypeChange: (ReminderType) -> Unit,
    onPersonNameChange: (String) -> Unit,
    onContactNumberChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onPurposeChange: (String) -> Unit,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    onRepeatTypeChange: (RepeatType) -> Unit,
    onPriorityChange: (ReminderPriority) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.backgroundstrucure2),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // App Bar
        SetReminderAppBar(
            onBackClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        )
        
        // TextField colors (cached)
        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppColors.TextPrimary,
            unfocusedTextColor = AppColors.TextPrimary,
            focusedLabelColor = AppColors.TextSecondary,
            unfocusedLabelColor = AppColors.TextSecondary,
            focusedBorderColor = AppColors.AccentBlue,
            unfocusedBorderColor = AppColors.TextTertiary,
            cursorColor = AppColors.AccentBlue
        )
        
        // Form Content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                // Reminder Type Toggle
                ReminderTypeToggle(
                    selectedType = reminderType,
                    onTypeChange = onReminderTypeChange
                )
            }
            
            item {
                // Person's Name
                OutlinedTextField(
                    value = personName,
                    onValueChange = onPersonNameChange,
                    label = { Text("Person's Name", color = AppColors.TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true
                )
            }
            
            item {
                // Contact Number
                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = onContactNumberChange,
                    label = { Text("Contact Number", color = AppColors.TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true
                )
            }
            
            item {
                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = { Text("Amount", color = AppColors.TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true
                )
            }
            
            item {
                // Purpose
                OutlinedTextField(
                    value = purpose,
                    onValueChange = onPurposeChange,
                    label = { Text("Purpose", color = AppColors.TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true
                )
            }
            
            item {
                // Date Picker Button
                DatePickerButton(
                    selectedDate = selectedDate,
                    onClick = onDateClick
                )
            }
            
            item {
                // Time Picker Button
                TimePickerButton(
                    selectedTime = selectedTime,
                    onClick = onTimeClick
                )
            }
            
            item {
                // Repeat Chips
                ChipGroup(
                    title = "Repeat",
                    options = RepeatType.values().toList(),
                    selectedOption = repeatType,
                    onOptionSelected = onRepeatTypeChange,
                    getDisplayName = { it.getDisplayName() }
                )
            }
            
            item {
                // Priority Chips
                ChipGroup(
                    title = "Priority",
                    options = ReminderPriority.values().toList(),
                    selectedOption = priority,
                    onOptionSelected = onPriorityChange,
                    getDisplayName = { it.getDisplayName() }
                )
            }
            
            // Spacer to push buttons to bottom
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        
        // Action Buttons (Pinned to Bottom)
        ActionButtons(
            onSaveClick = onSaveClick,
            onCancelClick = onCancelClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        }
    }
}

/**
 * App Bar
 */
@Composable
private fun SetReminderAppBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = AppColors.TextPrimary
            )
        }
        
        Text(
            text = "Set Reminder",
            color = AppColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Reminder Type Toggle (Animated)
 */
@Composable
private fun ReminderTypeToggle(
    selectedType: ReminderType,
    onTypeChange: (ReminderType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.DarkCard)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // "I Need to Pay" Button
        ReminderTypeButton(
            text = "I Need to Pay",
            isSelected = selectedType == ReminderType.GIVE,
            onClick = { onTypeChange(ReminderType.GIVE) },
            modifier = Modifier.weight(1f)
        )
        
        // "Someone Owes Me" Button
        ReminderTypeButton(
            text = "Someone Owes Me",
            isSelected = selectedType == ReminderType.RECEIVE,
            onClick = { onTypeChange(ReminderType.RECEIVE) },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Reminder Type Button
 */
@Composable
private fun ReminderTypeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = isSelected,
        transitionSpec = {
            fadeIn(tween(200)) togetherWith fadeOut(tween(200))
        },
        label = "reminderTypeToggle"
    ) { selected ->
        Surface(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
            color = if (selected) AppColors.AccentBlue else Color.Transparent,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = text,
                color = if (selected) Color.White else AppColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * Date Picker Button
 */
@Composable
private fun DatePickerButton(
    selectedDate: Calendar,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(selectedDate.time)
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = AppColors.DarkCard,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Reminder Date",
                color = AppColors.TextSecondary,
                fontSize = 14.sp
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = AppColors.AccentBlue,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = formattedDate.uppercase(),
                    color = AppColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Time Picker Button
 */
@Composable
private fun TimePickerButton(
    selectedTime: Calendar,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(selectedTime.time)
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = AppColors.DarkCard,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Reminder Time",
                color = AppColors.TextSecondary,
                fontSize = 14.sp
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = AppColors.AccentBlue,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = formattedTime.uppercase(),
                    color = AppColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Chip Group (Generic)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChipGroup(
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    getDisplayName: (T) -> String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            color = AppColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { option ->
                FilterChip(
                    selected = option == selectedOption,
                    onClick = { onOptionSelected(option) },
                    label = {
                        Text(
                            text = getDisplayName(option),
                            color = if (option == selectedOption) Color.White else AppColors.TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = if (option == selectedOption) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.AccentBlue,
                        containerColor = AppColors.DarkCard,
                        selectedLabelColor = Color.White,
                        labelColor = AppColors.TextSecondary
                    )
                )
            }
        }
    }
}

/**
 * Action Buttons
 */
@Composable
private fun ActionButtons(
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // SAVE REMINDER Button (Primary)
        Button(
            onClick = onSaveClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.DarkButtonBg
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "SAVE REMINDER",
                color = AppColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // CANCEL Button (Secondary)
        OutlinedButton(
            onClick = onCancelClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AppColors.AccentBlue
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.horizontalGradient(
                    colors = listOf(AppColors.AccentBlue, AppColors.AccentBlueLight)
                )
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "CANCEL",
                color = AppColors.AccentBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


