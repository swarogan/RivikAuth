package dev.rivikauth.feature.vault

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import dev.rivikauth.core.crypto.BiometricUnlockManager
import kotlinx.coroutines.flow.collectLatest

@Composable
fun UnlockScreen(
    onUnlocked: () -> Unit,
    viewModel: UnlockViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val hasBiometric by viewModel.hasBiometric.collectAsState()
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        if (uiState is UnlockUiState.Unlocked) onUnlocked()
    }

    // Auto-trigger biometric prompt on screen open
    LaunchedEffect(hasBiometric) {
        if (hasBiometric) viewModel.requestBiometricUnlock()
    }

    LaunchedEffect(Unit) {
        viewModel.biometricEvent.collectLatest { slot ->
            val activity = context as? FragmentActivity ?: return@collectLatest
            val manager = BiometricUnlockManager(activity)
            manager.unlockWithBiometric(
                slot = slot,
                onSuccess = { masterKey -> viewModel.onBiometricSuccess(masterKey) },
                onError = { /* biometric dismissed or failed -- user can retry or use password */ },
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.rivikauth_logo),
            contentDescription = null,
            modifier = Modifier.size(96.dp),
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text("RivikAuthenticator", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.unlock_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.unlock_password)) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = stringResource(R.string.unlock_show_password),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState !is UnlockUiState.Loading,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { viewModel.unlock(password) },
            enabled = password.isNotEmpty() && uiState !is UnlockUiState.Loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.unlock_button))
        }

        if (hasBiometric) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.requestBiometricUnlock() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is UnlockUiState.Loading,
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.unlock_biometric))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = uiState,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "unlock-state",
        ) { state ->
            when (state) {
                is UnlockUiState.Error -> {
                    Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                is UnlockUiState.Loading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.unlock_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    Spacer(modifier = Modifier.height(0.dp))
                }
            }
        }
    }
}
