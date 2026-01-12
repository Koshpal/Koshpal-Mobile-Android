package com.koshpal_android.koshpalapp.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koshpal_android.koshpalapp.ui.account.components.KoshpalSurface

@Composable
fun AccountOverviewCard(
    email: String,
    modifier: Modifier = Modifier
) {
    KoshpalSurface(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Email section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mail icon in container
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.8f))
                        .background(Color(0xFF0052FF).copy(alpha = 0.1f)), // Koshpal blue background
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mail,
                        contentDescription = null,
                        tint = Color(0xFF0052FF), // Koshpal blue
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Email details
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Email Address",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Verification badge
            Box(
                modifier = Modifier
                    .background(Color(0xFF10B981).copy(alpha = 0.1f)) // Emerald background
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(50))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 14.sp,
                            color = Color(0xFF10B981), // Emerald
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
