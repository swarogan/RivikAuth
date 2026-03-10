package dev.rivikauth.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.rivikauth.app.R

private inline fun <reified T : Any> NavDestination.isRoute(): Boolean {
    val qualifiedName = T::class.qualifiedName ?: return false
    return route?.startsWith(qualifiedName) == true
}

@Composable
fun RivikBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val otpSelected = currentDestination?.isRoute<Screen.OtpList>() == true
    val passkeysSelected = currentDestination?.isRoute<Screen.FidoList>() == true
    val settingsSelected = currentDestination?.isRoute<Screen.Settings>() == true ||
        currentDestination?.isRoute<Screen.ImportExport>() == true
    val aboutSelected = currentDestination?.isRoute<Screen.About>() == true

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        // Raised center button
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-9).dp),
        ) {
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, CircleShape, clip = false)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        navController.navigate(Screen.Scanner) {
                            launchSingleTop = true
                        }
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.QrCodeScanner,
                        contentDescription = stringResource(R.string.tab_scan),
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }

        // Navigation items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(
                icon = if (otpSelected) Icons.Filled.Password else Icons.Outlined.Password,
                label = stringResource(R.string.tab_otp),
                selected = otpSelected,
                onClick = {
                    navController.navigate(Screen.OtpList) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.weight(1f),
            )

            NavItem(
                icon = if (passkeysSelected) Icons.Filled.Key else Icons.Outlined.Key,
                label = stringResource(R.string.tab_passkeys),
                selected = passkeysSelected,
                onClick = {
                    navController.navigate(Screen.FidoList) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.weight(1f),
            )

            // Center spacer for scan button
            Box(modifier = Modifier.weight(1f))

            NavItem(
                icon = if (settingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                label = stringResource(R.string.tab_settings),
                selected = settingsSelected,
                onClick = {
                    navController.navigate(Screen.Settings) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.weight(1f),
            )

            NavItem(
                icon = Icons.Outlined.Info,
                label = stringResource(R.string.tab_about),
                selected = aboutSelected,
                onClick = {
                    navController.navigate(Screen.About) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(28.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}
