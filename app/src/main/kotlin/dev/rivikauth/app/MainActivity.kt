package dev.rivikauth.app

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.rivikauth.app.navigation.RivikBottomBar
import dev.rivikauth.app.navigation.RivikNavHost
import dev.rivikauth.app.navigation.Screen
import dev.rivikauth.app.theme.RivikTheme
import dev.rivikauth.core.datastore.AppPrefsStore
import dev.rivikauth.core.datastore.VaultSlotStore
import dev.rivikauth.service.credential.RivikCredentialProviderService
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var vaultSlotStore: VaultSlotStore

    @Inject
    lateinit var appPrefsStore: AppPrefsStore

    private var showProviderDialog by mutableStateOf(false)
    private var providerCheckDone = false

    private val prefs by lazy {
        getSharedPreferences("rivik_prefs", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (!providerCheckDone) {
                    val dismissed = prefs.getBoolean(KEY_PROVIDER_DIALOG_DISMISSED, false)
                    if (!dismissed) {
                        val enabled = isCredentialProviderEnabled()
                        Log.d(TAG, "CredentialProvider enabled: $enabled")
                        if (!enabled) {
                            showProviderDialog = true
                        }
                    }
                    providerCheckDone = true
                }
            }
        }

        setContent {
            val isVaultCreated by vaultSlotStore.isVaultCreated()
                .collectAsState(initial = null)
            val darkThemePref by appPrefsStore.darkTheme()
                .collectAsState(initial = null)
            val darkTheme = darkThemePref ?: true

            RivikTheme(darkTheme = darkTheme) {
                val vaultCreated = isVaultCreated
                if (vaultCreated == null) {
                    Box(Modifier.fillMaxSize())
                } else {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    val showBottomBar = currentDestination?.route?.let { route ->
                        route.startsWith(Screen.OtpList::class.qualifiedName!!) ||
                            route.startsWith(Screen.FidoList::class.qualifiedName!!) ||
                            route.startsWith(Screen.Scanner::class.qualifiedName!!) ||
                            route.startsWith(Screen.Settings::class.qualifiedName!!) ||
                            route.startsWith(Screen.About::class.qualifiedName!!) ||
                            route.startsWith(Screen.ImportExport::class.qualifiedName!!)
                    } ?: false

                    if (showProviderDialog) {
                        AlertDialog(
                            onDismissRequest = { dismissProviderDialog() },
                            title = { Text(stringResource(R.string.provider_dialog_title)) },
                            text = {
                                Text(stringResource(R.string.provider_dialog_message))
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    dismissProviderDialog()
                                    openCredentialProviderSettings()
                                }) {
                                    Text(stringResource(R.string.provider_dialog_confirm))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { dismissProviderDialog() }) {
                                    Text(stringResource(R.string.provider_dialog_dismiss))
                                }
                            },
                        )
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (showBottomBar) RivikBottomBar(navController)
                        },
                    ) { padding ->
                        RivikNavHost(
                            navController = navController,
                            isVaultCreated = vaultCreated,
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }
        }
    }

    private fun isCredentialProviderEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        return try {
            val cm = getSystemService("credential_service") as? android.credentials.CredentialManager
                ?: return true
            cm.isEnabledCredentialProviderService(
                ComponentName(this, RivikCredentialProviderService::class.java)
            )
        } catch (_: Exception) {
            true
        }
    }

    private fun dismissProviderDialog() {
        showProviderDialog = false
        prefs.edit().putBoolean(KEY_PROVIDER_DIALOG_DISMISSED, true).apply()
    }

    private fun openCredentialProviderSettings() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Intent(Settings.ACTION_CREDENTIAL_PROVIDER)
            } else {
                Intent(Settings.ACTION_SETTINGS)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val KEY_PROVIDER_DIALOG_DISMISSED = "provider_dialog_dismissed"
    }
}
