package com.koshpal_android.koshpalapp.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.koshpal_android.koshpalapp.ui.login.components.SurfaceCard

@Composable
fun LoginScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    isLoading: Boolean = false
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top spacing (equivalent to iOS status bar + spacing)
            Spacer(modifier = Modifier.height(44.dp))

            // Main content container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .defaultMinSize(minHeight = screenHeight * 0.884f), // Match HTML min-height
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Header Section
                LoginHeader()

                // Login Form Card
                SurfaceCard {
                    LoginForm(
                        email = email,
                        onEmailChange = onEmailChange,
                        password = password,
                        onPasswordChange = onPasswordChange,
                        onLoginClick = onLoginClick,
                        isLoading = isLoading
                    )
                }
            }

            // Bottom indicator line
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .padding(bottom = 16.dp, top = 16.dp)
                    .width(128.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            )
        }
    }
}
