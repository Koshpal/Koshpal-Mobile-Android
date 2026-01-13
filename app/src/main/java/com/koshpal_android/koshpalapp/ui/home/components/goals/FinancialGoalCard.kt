package com.koshpal_android.koshpalapp.ui.home.components.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koshpal_android.koshpalapp.data.remote.dto.FinancialGoalDto
import com.koshpal_android.koshpalapp.ui.goals.GoalsViewModel

/**
 * Financial Goal Card - Material 3 compliant card component
 * Displays goal progress with clean, modern design matching Home UI
 */
@Composable
fun FinancialGoalCard(
    goal: FinancialGoalDto,
    viewModel: GoalsViewModel,
    modifier: Modifier = Modifier
) {
    val progress = viewModel.calculateProgress(goal.saving, goal.goalAmount)
    val formattedDate = viewModel.formatTargetDate(goal.goalDate)

    Card(
        modifier = modifier
            .width(260.dp)
            .height(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Icon, title, and percentage
            GoalHeader(
                icon = goal.icon,
                title = goal.goalName,
                percentage = progress.toInt()
            )

            // Amount section: Current / Target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Current amount (emphasized)
                Text(
                    text = "₹${goal.saving.toInt()}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Separator
                Text(
                    text = "/",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Target amount (muted)
                Text(
                    text = "₹${goal.goalAmount.toInt()}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Progress bar and target date
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Progress bar
                GoalProgressBar(progress = progress)

                // Target date (footer)
                Text(
                    text = "Target: $formattedDate",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
