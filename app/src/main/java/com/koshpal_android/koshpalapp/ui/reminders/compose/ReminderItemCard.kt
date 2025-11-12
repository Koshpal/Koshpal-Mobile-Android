package com.koshpal_android.koshpalapp.ui.reminders.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koshpal_android.koshpalapp.model.Reminder
import com.koshpal_android.koshpalapp.model.ReminderPriority
import com.koshpal_android.koshpalapp.model.ReminderType
import com.koshpal_android.koshpalapp.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reminder Item Card with Swipe-to-Action
 * Stateless - receives all data and event handlers as parameters
 */
@Composable
fun ReminderItemCard(
    reminder: Reminder,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMarkAsPaid: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    val maxSwipeDistance = 120.dp
    val density = LocalDensity.current
    val maxSwipeDistancePx = with(density) { maxSwipeDistance.toPx() }
    
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Background Actions (revealed on swipe)
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left swipe action (Mark as Paid)
            if (offsetX > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(maxSwipeDistance)
                        .background(Color(0xFF34C759)), // Green
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Mark as Paid",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Right swipe action (Delete)
            if (offsetX < 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(maxSwipeDistance)
                        .background(Color(0xFFFF3B30)), // Red
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        
        // Main Card
        val animatedOffset by animateFloatAsState(
            targetValue = offsetX,
            animationSpec = tween(300),
            label = "swipeOffset"
        )
        
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = animatedOffset.dp)
                .pointerInput(reminder.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // Snap to action or reset
                            when {
                                offsetX > maxSwipeDistancePx * 0.5f -> {
                                    // Trigger mark as paid
                                    onMarkAsPaid()
                                }
                                offsetX < -maxSwipeDistancePx * 0.5f -> {
                                    // Trigger delete
                                    onDelete()
                                }
                            }
                            // Always reset after action
                            offsetX = 0f
                        }
                    ) { change, dragAmount ->
                        val newOffset = offsetX + dragAmount
                        offsetX = newOffset.coerceIn(
                            -maxSwipeDistancePx,
                            maxSwipeDistancePx
                        )
                    }
                }
                .clickable(onClick = onClick),
            color = AppColors.DarkCard,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Priority Indicator Bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(getPriorityColor(reminder.priority), RoundedCornerShape(2.dp))
                )
                
                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Row 1: Person's Name
                    Text(
                        text = reminder.personName,
                        color = AppColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Row 2: Amount and Purpose
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "₹${String.format("%.0f", reminder.amount)}",
                            color = getTypeColor(reminder.type),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = reminder.purpose,
                            color = AppColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                    
                    // Row 3: Date/Time
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formatDateTime(reminder.dueDate, reminder.dueTime),
                            color = AppColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Get priority color
 */
private fun getPriorityColor(priority: ReminderPriority): Color {
    return when (priority) {
        ReminderPriority.HIGH -> Color(0xFFFF3B30) // Red
        ReminderPriority.MEDIUM -> Color(0xFFFFCC00) // Yellow
        ReminderPriority.LOW -> AppColors.AccentBlue // Blue
    }
}

/**
 * Get type color (Red for GIVE/To Pay, Green for RECEIVE/To Receive)
 */
private fun getTypeColor(type: ReminderType): Color {
    return when (type) {
        ReminderType.GIVE -> Color(0xFFFF3B30) // Red
        ReminderType.RECEIVE -> Color(0xFF34C759) // Green
    }
}

/**
 * Format date and time for display
 */
private fun formatDateTime(dueDate: Long, dueTime: Long): String {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    
    val calendar = Calendar.getInstance().apply {
        timeInMillis = dueDate
    }
    
    val dateStr = dateFormat.format(calendar.time)
    
    // Format time from milliseconds since midnight
    val hours = (dueTime / (1000 * 60 * 60)).toInt()
    val minutes = ((dueTime % (1000 * 60 * 60)) / (1000 * 60)).toInt()
    calendar.set(Calendar.HOUR_OF_DAY, hours)
    calendar.set(Calendar.MINUTE, minutes)
    val timeStr = timeFormat.format(calendar.time).lowercase()
    
    return "$dateStr · $timeStr"
}

