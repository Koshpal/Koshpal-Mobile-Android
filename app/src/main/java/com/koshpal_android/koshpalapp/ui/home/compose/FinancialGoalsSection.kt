package com.koshpal_android.koshpalapp.ui.home.compose

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.data.remote.dto.FinancialGoalDto
import com.koshpal_android.koshpalapp.ui.goals.GoalsViewModel

private const val TAG = "FinancialGoalsSection"

@Composable
fun FinancialGoalsSection(
    goalsViewModel: GoalsViewModel,
    onAddGoalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val financialGoals by goalsViewModel.financialGoals.observeAsState(emptyList())
    val isLoading by goalsViewModel.isLoadingGoals.observeAsState(false)
    val error by goalsViewModel.goalsError.observeAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🎯",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Financial Goals",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            TextButton(onClick = onAddGoalClick) {
                Text(
                    text = "+ Add Goal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Content
        when {
            !goalsViewModel.isUserLoggedIn() -> {
                // Not logged in - show login prompt
                LoginPromptCard(
                    onLoginClick = { /* Navigate to login */ },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            isLoading -> {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            error != null -> {
                // Error state
                ErrorCard(
                    error = error!!,
                    onRetryClick = { goalsViewModel.loadFinancialGoals() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            financialGoals.isEmpty() -> {
                // Empty state
                EmptyGoalsCard(
                    onAddGoalClick = onAddGoalClick,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            else -> {
                // Goals list
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(financialGoals) { goal ->
                        GoalCard(
                            goal = goal,
                            viewModel = goalsViewModel,
                            modifier = Modifier.width(260.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalCard(
    goal: FinancialGoalDto,
    viewModel: GoalsViewModel,
    modifier: Modifier = Modifier
) {
    val progress = viewModel.calculateProgress(goal.saving, goal.goalAmount)
    val formattedDate = viewModel.formatTargetDate(goal.goalDate)

    // Premium gradient background
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF6366F1).copy(alpha = 0.1f), // Indigo with low opacity
            Color(0xFF8B5CF6).copy(alpha = 0.08f), // Purple with low opacity
            Color(0xFF06B6D4).copy(alpha = 0.06f)  // Cyan with low opacity
        )
    )

    Card(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .background(
                    Color.White.copy(alpha = 0.05f),
                    RoundedCornerShape(20.dp)
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: Icon and goal name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Emoji icon in a subtle background
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = goal.icon,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Text(
                            text = goal.goalName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1
                        )
                    }

                    // Progress percentage
                    Text(
                        text = "${progress.toInt()}%",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Middle: Amount saved vs goal
                Text(
                    text = "₹${goal.saving.toInt()} / ₹${goal.goalAmount.toInt()}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Bottom: Progress bar and target date
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Premium progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(3.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((progress / 100f).coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF10B981), // Emerald
                                            Color(0xFF06B6D4), // Cyan
                                            Color(0xFF6366F1)  // Indigo
                                        )
                                    ),
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }

                    // Target date
                    Text(
                        text = "Target: $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginPromptCard(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🔐",
                style = MaterialTheme.typography.headlineMedium
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Login to view your goals",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Track your savings and financial targets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onLoginClick) {
                Text(
                    text = "Login",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyGoalsCard(
    onAddGoalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onAddGoalClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎯",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "No financial goals yet",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Tap to add your first goal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.headlineSmall
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Failed to load goals",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    maxLines = 2
                )
            }

            TextButton(onClick = onRetryClick) {
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
