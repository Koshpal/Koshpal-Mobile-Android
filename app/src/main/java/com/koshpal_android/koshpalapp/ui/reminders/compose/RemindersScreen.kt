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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.model.Reminder
import com.koshpal_android.koshpalapp.ui.reminders.ReminderUiState
import com.koshpal_android.koshpalapp.ui.theme.AppColors
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main Reminders Screen Composable
 * Stateless - receives all data and event handlers as parameters
 */
@Composable
fun RemindersScreen(
    reminders: List<Reminder>,
    uiState: ReminderUiState,
    selectedFilter: ReminderFilter = ReminderFilter.ALL,
    onAddReminderClick: () -> Unit,
    onReminderClick: (Reminder) -> Unit = {},
    onDeleteReminder: (Reminder) -> Unit,
    onMarkAsPaid: (Reminder) -> Unit,
    onFilterSelected: (ReminderFilter) -> Unit,
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
        RemindersAppBar(
            onAddClick = onAddReminderClick,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Summary Cards (Filters)
        SummaryCardsRow(
            uiState = uiState,
            selectedFilter = selectedFilter,
            onFilterSelected = onFilterSelected,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
        
        // Content
        AnimatedVisibility(
            visible = reminders.isEmpty(),
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            )
        }
        
        AnimatedVisibility(
            visible = reminders.isNotEmpty(),
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            RemindersList(
                reminders = reminders,
                onReminderClick = onReminderClick,
                onDeleteReminder = onDeleteReminder,
                onMarkAsPaid = onMarkAsPaid,
                modifier = Modifier.fillMaxSize()
            )
        }
        }
    }
}

/**
 * App Bar with title and add button
 */
@Composable
private fun RemindersAppBar(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Reminders",
            color = AppColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        // Premium ghost button
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onAddClick),
            color = AppColors.DarkCard,
            shape = RoundedCornerShape(20.dp)
        ) {
            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Reminder",
                    tint = AppColors.AccentBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Summary Cards Row - Interactive Filters
 */
@Composable
fun SummaryCardsRow(
    uiState: ReminderUiState,
    selectedFilter: ReminderFilter,
    onFilterSelected: (ReminderFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        item {
            SummaryCard(
                title = "To Pay",
                count = uiState.pendingCount,
                amount = uiState.totalAmountToGive,
                iconColor = Color(0xFFFF3B30), // Red
                iconContent = "↓",
                isSelected = selectedFilter == ReminderFilter.TO_PAY,
                onClick = { onFilterSelected(ReminderFilter.TO_PAY) }
            )
        }
        
        item {
            SummaryCard(
                title = "To Receive",
                count = uiState.pendingCount,
                amount = uiState.totalAmountToReceive,
                iconColor = Color(0xFF34C759), // Green
                iconContent = "↑",
                isSelected = selectedFilter == ReminderFilter.TO_RECEIVE,
                onClick = { onFilterSelected(ReminderFilter.TO_RECEIVE) }
            )
        }
        
        item {
            SummaryCard(
                title = "Pending",
                count = uiState.pendingCount,
                amount = uiState.totalAmountToGive + uiState.totalAmountToReceive,
                iconColor = Color(0xFFFFCC00), // Yellow
                iconContent = "⏰",
                isSelected = selectedFilter == ReminderFilter.PENDING,
                onClick = { onFilterSelected(ReminderFilter.PENDING) }
            )
        }
    }
}

/**
 * Summary Card Component
 */
@Composable
private fun SummaryCard(
    title: String,
    count: Int,
    amount: Double,
    iconColor: Color,
    iconContent: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
            color = if (isSelected) AppColors.AccentBlue.copy(alpha = 0.1f) else AppColors.DarkCard,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = iconContent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
                Text(
                    text = title,
                    color = AppColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text(
                text = count.toString(),
                color = AppColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = formatAmount(amount),
                color = iconColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Empty State
 */
@Composable
private fun EmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.NotificationsOff,
            contentDescription = null,
            tint = AppColors.TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "All Caught Up!",
            color = AppColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "No pending reminders right now",
            color = AppColors.TextSecondary,
            fontSize = 16.sp
        )
    }
}

/**
 * Reminders List
 */
@Composable
private fun RemindersList(
    reminders: List<Reminder>,
    onReminderClick: (Reminder) -> Unit,
    onDeleteReminder: (Reminder) -> Unit,
    onMarkAsPaid: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = reminders,
            key = { it.id }
        ) { reminder ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)),
                exit = fadeOut(tween(300)) + slideOutVertically(tween(300))
            ) {
                ReminderItemCard(
                    reminder = reminder,
                    onClick = { onReminderClick(reminder) },
                    onDelete = { onDeleteReminder(reminder) },
                    onMarkAsPaid = { onMarkAsPaid(reminder) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Format amount for display
 */
private fun formatAmount(amount: Double): String {
    return when {
        amount >= 100000 -> "₹${String.format("%.1f", amount / 100000)}L"
        amount >= 1000 -> "₹${String.format("%.1f", amount / 1000)}K"
        else -> "₹${String.format("%.0f", amount)}"
    }
}

/**
 * Reminder Filter Enum
 */
enum class ReminderFilter {
    ALL,
    TO_PAY,
    TO_RECEIVE,
    PENDING
}

