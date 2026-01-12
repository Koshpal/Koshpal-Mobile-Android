package com.koshpal_android.koshpalapp.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AccountTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (MaterialTheme.colorScheme.surface == Color.White) {
        Color.White
    } else {
        Color(0xFF0F1115) // Dark background
    }

    val borderColor = if (MaterialTheme.colorScheme.surface == Color.White) {
        Color(0xFFE2E8F0).copy(alpha = 0.5f)
    } else {
        Color(0xFF23262F).copy(alpha = 0.5f)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onBackClick)
                        .padding(4.dp)
                )

                Text(
                    text = "Account",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Warning icon
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = Color(0xFFF59E0B), // Amber color
                modifier = Modifier.size(20.dp)
            )
        }

        // Subtle divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(borderColor)
        )
    }
}
