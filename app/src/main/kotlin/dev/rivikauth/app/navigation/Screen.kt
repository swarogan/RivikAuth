package dev.rivikauth.app.navigation

import kotlinx.serialization.Serializable

@Serializable sealed interface Screen {
    @Serializable data object Setup : Screen
    @Serializable data object Unlock : Screen
    @Serializable data object OtpList : Screen
    @Serializable data object FidoList : Screen
    @Serializable data object Scanner : Screen
    @Serializable data object Settings : Screen
    @Serializable data object About : Screen
    @Serializable data object ImportExport : Screen
    @Serializable data class CableScan(val rawUri: String = "") : Screen
}
