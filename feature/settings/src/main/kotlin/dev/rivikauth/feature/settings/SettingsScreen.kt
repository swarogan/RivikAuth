package dev.rivikauth.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import android.widget.Toast
import dev.rivikauth.core.crypto.BiometricUnlockManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToImportExport: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.enrollBiometricEvent.collect { masterKey ->
            Log.d("BiometricEnroll", "Event received")
            val activity = context as? FragmentActivity
            if (activity == null) {
                Log.e("BiometricEnroll", "Context is not FragmentActivity")
                viewModel.onBiometricEnrollmentFailed()
                return@collect
            }
            val manager = BiometricUnlockManager(activity)
            if (!manager.canUseBiometric()) {
                Log.e("BiometricEnroll", "canUseBiometric() == false")
                Toast.makeText(context, "Biometria niedostępna na tym urządzeniu", Toast.LENGTH_LONG).show()
                viewModel.onBiometricEnrollmentFailed()
                return@collect
            }
            manager.enrollBiometric(
                masterKey = masterKey,
                onSuccess = { slot ->
                    Log.d("BiometricEnroll", "Enrollment success")
                    viewModel.onBiometricEnrolled(slot)
                },
                onError = { err ->
                    Log.e("BiometricEnroll", "Enrollment error: $err")
                    viewModel.onBiometricEnrollmentFailed()
                },
            )
        }
    }

    val currentLocale = AppCompatDelegate.getApplicationLocales()
        .toLanguageTags().takeIf { it.isNotEmpty() }
        ?: java.util.Locale.getDefault().language

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.settings_title)) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSection(stringResource(R.string.section_general)) {
                SwitchSetting(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.setting_dark_theme),
                    description = stringResource(R.string.setting_dark_theme_desc),
                    checked = uiState.darkTheme,
                    onCheckedChange = viewModel::setDarkTheme,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLanguageDialog = true }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.setting_language),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            if (currentLocale.startsWith("pl")) stringResource(R.string.language_polish)
                            else stringResource(R.string.language_english),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SwitchSetting(
                    icon = Icons.Default.CheckCircle,
                    title = stringResource(R.string.setting_show_success_on_auth),
                    description = stringResource(R.string.setting_show_success_on_auth_desc),
                    checked = uiState.showSuccessOnAuth,
                    onCheckedChange = viewModel::setShowSuccessOnAuth,
                )
            }

            SettingsSection(stringResource(R.string.section_security)) {
                SwitchSetting(
                    icon = Icons.Default.Fingerprint,
                    title = stringResource(R.string.setting_biometric),
                    description = stringResource(R.string.setting_biometric_desc),
                    checked = uiState.biometricEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) viewModel.requestBiometricEnrollment()
                        else viewModel.disableBiometric()
                    },
                )
                SwitchSetting(
                    icon = Icons.Default.ScreenLockPortrait,
                    title = stringResource(R.string.setting_auto_lock),
                    description = stringResource(R.string.setting_auto_lock_desc),
                    checked = uiState.autoLockEnabled,
                    onCheckedChange = viewModel::setAutoLockEnabled,
                )

                SwitchSetting(
                    icon = Icons.Default.Bluetooth,
                    title = stringResource(R.string.setting_ble),
                    description = stringResource(R.string.setting_ble_desc),
                    checked = uiState.bleEnabled,
                    onCheckedChange = {},
                    enabled = false,
                )
            }

            SettingsSection(stringResource(R.string.section_data)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToImportExport)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.setting_import_export),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            stringResource(R.string.setting_import_export_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.setting_language)) },
            text = {
                Column {
                    LanguageOption(
                        label = stringResource(R.string.language_english),
                        selected = !currentLocale.startsWith("pl"),
                        onClick = {
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags("en")
                            )
                            showLanguageDialog = false
                        },
                    )
                    LanguageOption(
                        label = stringResource(R.string.language_polish),
                        selected = currentLocale.startsWith("pl"),
                        onClick = {
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags("pl")
                            )
                            showLanguageDialog = false
                        },
                    )
                }
            },
            confirmButton = {},
        )
    }
}

@Composable
private fun LanguageOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            content()
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun SwitchSetting(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
