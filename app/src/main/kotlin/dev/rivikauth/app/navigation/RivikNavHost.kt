package dev.rivikauth.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.rivikauth.feature.fido.CableScanScreen
import dev.rivikauth.feature.fido.FidoListScreen
import dev.rivikauth.feature.importexport.ImportExportScreen
import dev.rivikauth.feature.settings.AboutScreen
import dev.rivikauth.feature.otp.OtpListScreen
import dev.rivikauth.feature.scanner.ScannerScreen
import dev.rivikauth.feature.settings.SettingsScreen
import dev.rivikauth.feature.vault.SetupScreen
import dev.rivikauth.feature.vault.UnlockScreen

@Composable
fun RivikNavHost(
    navController: NavHostController,
    isVaultCreated: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val startDestination: Screen = if (isVaultCreated) Screen.Unlock else Screen.Setup

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<Screen.Setup> {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Screen.OtpList) {
                        popUpTo(Screen.Setup) { inclusive = true }
                    }
                }
            )
        }
        composable<Screen.Unlock> {
            UnlockScreen(
                onUnlocked = {
                    navController.navigate(Screen.OtpList) {
                        popUpTo(Screen.Unlock) { inclusive = true }
                    }
                    // Auto-start linked device listener if enabled
                    dev.rivikauth.service.ble.LinkedDeviceService.start(context)
                }
            )
        }
        composable<Screen.OtpList> {
            OtpListScreen()
        }
        composable<Screen.FidoList> {
            FidoListScreen()
        }
        composable<Screen.CableScan> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.CableScan>()
            CableScanScreen(
                rawUri = args.rawUri,
                onDone = { navController.popBackStack() },
            )
        }
        composable<Screen.Scanner> {
            ScannerScreen(
                onEntryScanned = { _ ->
                    navController.navigate(Screen.OtpList) {
                        popUpTo(Screen.OtpList) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onFidoScanned = { rawUri ->
                    navController.navigate(Screen.CableScan(rawUri = rawUri)) {
                        popUpTo(Screen.Scanner) { inclusive = true }
                    }
                },
            )
        }
        composable<Screen.Settings> {
            SettingsScreen(
                onNavigateToImportExport = { navController.navigate(Screen.ImportExport) }
            )
        }
        composable<Screen.About> {
            AboutScreen()
        }
        composable<Screen.ImportExport> {
            ImportExportScreen()
        }
    }
}
