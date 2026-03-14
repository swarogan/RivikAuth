package dev.rivikauth.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPrefsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val SHOW_SUCCESS_ON_AUTH = booleanPreferencesKey("show_success_on_auth")
        val AUTO_LOCK = booleanPreferencesKey("auto_lock")
        val PANIC_WIPE = booleanPreferencesKey("panic_wipe")
        val BLE_ENABLED = booleanPreferencesKey("ble_enabled")
        val NFC_ENABLED = booleanPreferencesKey("nfc_enabled")
        val LINKED_DEVICE_ENABLED = booleanPreferencesKey("linked_device_enabled")
    }

    fun darkTheme(): Flow<Boolean?> =
        context.appPrefsDataStore.data.map { it[Keys.DARK_THEME] }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    fun biometricEnabled(): Flow<Boolean> =
        context.appPrefsDataStore.data.map { it[Keys.BIOMETRIC_ENABLED] ?: false }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    fun showSuccessOnAuth(): Flow<Boolean> =
        context.appPrefsDataStore.data.map { it[Keys.SHOW_SUCCESS_ON_AUTH] ?: true }

    suspend fun setShowSuccessOnAuth(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[Keys.SHOW_SUCCESS_ON_AUTH] = enabled }
    }

    fun autoLock(): Flow<Boolean> =
        context.appPrefsDataStore.data.map { it[Keys.AUTO_LOCK] ?: true }

    suspend fun setAutoLock(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[Keys.AUTO_LOCK] = enabled }
    }

    fun panicWipe(): Flow<Boolean> =
        context.appPrefsDataStore.data.map { it[Keys.PANIC_WIPE] ?: false }

    suspend fun setPanicWipe(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[Keys.PANIC_WIPE] = enabled }
    }

    fun bleEnabled(): Flow<Boolean> =
        context.appPrefsDataStore.data.map { it[Keys.BLE_ENABLED] ?: false }

    suspend fun setBleEnabled(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[Keys.BLE_ENABLED] = enabled }
    }

    fun nfcEnabled(): Flow<Boolean> =
        context.appPrefsDataStore.data.map { it[Keys.NFC_ENABLED] ?: false }

    suspend fun setNfcEnabled(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[Keys.NFC_ENABLED] = enabled }
    }

    fun linkedDeviceEnabled(): Flow<Boolean> =
        context.appPrefsDataStore.data.map { it[Keys.LINKED_DEVICE_ENABLED] ?: false }

    suspend fun setLinkedDeviceEnabled(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[Keys.LINKED_DEVICE_ENABLED] = enabled }
    }
}
