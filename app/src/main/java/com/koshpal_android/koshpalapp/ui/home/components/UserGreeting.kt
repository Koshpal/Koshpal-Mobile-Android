package com.koshpal_android.koshpalapp.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * User greeting component displaying greeting text and user name
 */
@Composable
fun UserGreeting(
    greetingText: String,
    userName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        // Greeting text (e.g., "Good Evening")
        Text(
            text = greetingText,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp, // text-sm
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // User name
        Text(
            text = userName,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 20.sp, // text-xl
                fontWeight = FontWeight.SemiBold // font-semibold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
