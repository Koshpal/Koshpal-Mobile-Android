package com.koshpal_android.koshpalapp.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Home screen top header - Material 3 compliant header component
 * Combines profile avatar, user greeting, and notification button
 */
@Composable
fun HomeTopHeader(
    greetingText: String,
    userName: String,
    onProfileClick: () -> Unit,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Avatar + Greeting
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp) // gap-4
        ) {
            ProfileAvatar(onClick = onProfileClick)
            UserGreeting(
                greetingText = greetingText,
                userName = userName
            )
        }

        // Right side: Notification button
        NotificationButton(onClick = onNotificationClick)
    }
}
