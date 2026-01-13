package com.koshpal_android.koshpalapp.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AccountScreen(
    email: String = "chaitnykakde517@gmail.com",
    pendingTransactions: Int = 29,
    appVersion: String = "v2.4.0",
    onBackClick: () -> Unit = {},
    onExportStatementClick: () -> Unit = {},
    onSyncTransactionsClick: () -> Unit = {},
    onReportSmsClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onRateUsClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onWebsiteClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Top App Bar
        AccountTopBar(onBackClick = onBackClick)

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Account Overview Card
            AccountOverviewCard(email = email)

            // Expense Tracking Section
            AccountSection(title = "Expense Tracking") {
                AccountRow(
                    icon = Icons.Default.FileUpload,
                    title = "Export statement",
                    onClick = onExportStatementClick
                )

                AccountRow(
                    icon = Icons.Default.Sync,
                    title = "Sync All Transactions",
                    subtitle = if (pendingTransactions > 0) "$pendingTransactions pending transactions" else null,
                    onClick = onSyncTransactionsClick
                )

                AccountRow(
                    icon = Icons.Default.Warning,
                    title = "Report undetected SMS",
                    onClick = onReportSmsClick,
                    showDivider = false
                )
            }

            // Deposits Section
            AccountSection(title = "Deposits") {
                AccountRow(
                    icon = Icons.Default.Help,
                    title = "Help & FAQ",
                    onClick = onHelpClick,
                    showDivider = false
                )
            }

            // General Section
            AccountSection(title = "General") {
                AccountRow(
                    icon = Icons.Default.Star,
                    title = "Rate us",
                    onClick = onRateUsClick
                )

                AccountRow(
                    icon = Icons.Default.Gavel,
                    title = "Terms and Conditions",
                    onClick = onTermsClick
                )

                AccountRow(
                    icon = Icons.Default.Language,
                    title = "Website",
                    onClick = onWebsiteClick
                )

                AccountRow(
                    icon = Icons.Default.Forum,
                    title = "Chat with us",
                    onClick = onChatClick
                )

                LogoutRow(
                    icon = Icons.Default.Logout,
                    title = "Logout",
                    onClick = onLogoutClick
                )
            }
        }

        // Footer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Koshpal $appVersion",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Bottom spacing for navigation
        Spacer(modifier = Modifier.height(80.dp))
    }
}
