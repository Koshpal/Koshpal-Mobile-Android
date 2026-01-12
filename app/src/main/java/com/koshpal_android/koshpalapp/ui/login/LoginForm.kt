package com.koshpal_android.koshpalapp.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.koshpal_android.koshpalapp.ui.login.components.KoshpalPrimaryButton
import com.koshpal_android.koshpalapp.ui.login.components.KoshpalTextField

@Composable
fun LoginForm(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Email Field
        KoshpalTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email Address",
            placeholder = "name@example.com",
            keyboardType = KeyboardType.Email,
            modifier = Modifier.fillMaxWidth()
        )

        // Password Field
        KoshpalTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            placeholder = "Enter your password",
            keyboardType = KeyboardType.Password,
            isPassword = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Login Button
        KoshpalPrimaryButton(
            text = "Login",
            onClick = onLoginClick,
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
