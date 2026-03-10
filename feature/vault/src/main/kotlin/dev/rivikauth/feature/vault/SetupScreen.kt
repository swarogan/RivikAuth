package dev.rivikauth.feature.vault

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val errorMinLength = stringResource(R.string.setup_error_min_length)
    val errorMismatch = stringResource(R.string.setup_error_mismatch)

    LaunchedEffect(uiState) {
        if (uiState is SetupUiState.Created) onSetupComplete()
        if (uiState is SetupUiState.Error) {
            errorMessage = (uiState as SetupUiState.Error).message
        }
    }

    val isCreating = uiState is SetupUiState.Creating

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.setup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text(stringResource(R.string.setup_password)) },
            singleLine = true,
            enabled = !isCreating,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; errorMessage = null },
            label = { Text(stringResource(R.string.setup_confirm_password)) },
            singleLine = true,
            enabled = !isCreating,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        if (password.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            val strength = calculatePasswordStrength(password)
            LinearProgressIndicator(
                progress = { strength.progress },
                modifier = Modifier.fillMaxWidth(),
                color = strength.color,
            )
            Spacer(modifier = Modifier.height(4.dp))
            val strengthLabel = when (strength.level) {
                StrengthLevel.WEAK -> stringResource(R.string.strength_weak)
                StrengthLevel.MODERATE -> stringResource(R.string.strength_moderate)
                StrengthLevel.GOOD -> stringResource(R.string.strength_good)
                StrengthLevel.STRONG -> stringResource(R.string.strength_strong)
                StrengthLevel.VERY_STRONG -> stringResource(R.string.strength_very_strong)
            }
            Text(strengthLabel, style = MaterialTheme.typography.labelSmall)
        }

        AnimatedContent(
            targetState = errorMessage,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "error",
        ) { error ->
            if (error != null) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(0.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedContent(
            targetState = isCreating,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "button-state",
        ) { creating ->
            if (creating) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.setup_creating),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Button(
                    onClick = {
                        when {
                            password.length < 8 -> errorMessage = errorMinLength
                            password != confirmPassword -> errorMessage = errorMismatch
                            else -> viewModel.createVault(password)
                        }
                    },
                    enabled = password.isNotEmpty() && confirmPassword.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.setup_create_button))
                }
            }
        }
    }
}

private enum class StrengthLevel { WEAK, MODERATE, GOOD, STRONG, VERY_STRONG }

private data class PasswordStrength(
    val progress: Float,
    val level: StrengthLevel,
    val color: androidx.compose.ui.graphics.Color,
)

private fun calculatePasswordStrength(password: String): PasswordStrength {
    var score = 0
    if (password.length >= 8) score++
    if (password.length >= 12) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++

    return when {
        score <= 1 -> PasswordStrength(0.2f, StrengthLevel.WEAK, androidx.compose.ui.graphics.Color.Red)
        score <= 2 -> PasswordStrength(0.4f, StrengthLevel.MODERATE, androidx.compose.ui.graphics.Color(0xFFFF9800))
        score <= 3 -> PasswordStrength(0.6f, StrengthLevel.GOOD, androidx.compose.ui.graphics.Color(0xFFFFC107))
        score <= 4 -> PasswordStrength(0.8f, StrengthLevel.STRONG, androidx.compose.ui.graphics.Color(0xFF8BC34A))
        else -> PasswordStrength(1.0f, StrengthLevel.VERY_STRONG, androidx.compose.ui.graphics.Color(0xFF4CAF50))
    }
}
