package dev.rivikauth.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rivikauth.core.crypto.BiometricKeyManager
import dev.rivikauth.core.database.di.VaultPassphraseHolder
import dev.rivikauth.core.datastore.AppPrefsStore
import dev.rivikauth.core.datastore.VaultSlotStore
import dev.rivikauth.core.model.VaultSlot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPrefsStore: AppPrefsStore,
    private val vaultSlotStore: VaultSlotStore,
    private val passphraseHolder: VaultPassphraseHolder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _enrollBiometricEvent = MutableSharedFlow<SecretKey>()
    val enrollBiometricEvent: SharedFlow<SecretKey> = _enrollBiometricEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            appPrefsStore.darkTheme().collect { dark ->
                _uiState.update { it.copy(darkTheme = dark ?: true) }
            }
        }
        viewModelScope.launch {
            appPrefsStore.biometricEnabled().collect { enabled ->
                _uiState.update { it.copy(biometricEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            appPrefsStore.showSuccessOnAuth().collect { show ->
                _uiState.update { it.copy(showSuccessOnAuth = show) }
            }
        }
        viewModelScope.launch {
            appPrefsStore.autoLock().collect { enabled ->
                _uiState.update { it.copy(autoLockEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            appPrefsStore.panicWipe().collect { enabled ->
                _uiState.update { it.copy(panicWipeEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            appPrefsStore.bleEnabled().collect { enabled ->
                _uiState.update { it.copy(bleEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            appPrefsStore.nfcEnabled().collect { enabled ->
                _uiState.update { it.copy(nfcEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            appPrefsStore.linkedDeviceEnabled().collect { enabled ->
                _uiState.update { it.copy(linkedDeviceEnabled = enabled) }
            }
        }
    }

    fun requestBiometricEnrollment() {
        Log.d("BiometricEnroll", "requestBiometricEnrollment called, isUnlocked=${passphraseHolder.isUnlocked()}")
        if (!passphraseHolder.isUnlocked()) return
        val masterKey = SecretKeySpec(passphraseHolder.getPassphrase(), "AES")
        viewModelScope.launch {
            Log.d("BiometricEnroll", "Emitting enrollBiometricEvent")
            _enrollBiometricEvent.emit(masterKey)
        }
    }

    fun onBiometricEnrolled(slot: VaultSlot.Biometric) {
        viewModelScope.launch {
            vaultSlotStore.saveBiometricSlot(slot)
            appPrefsStore.setBiometricEnabled(true)
            _uiState.update { it.copy(biometricEnabled = true) }
        }
    }

    fun onBiometricEnrollmentFailed() {
        _uiState.update { it.copy(biometricEnabled = false) }
    }

    fun disableBiometric() {
        viewModelScope.launch {
            val slot = vaultSlotStore.getBiometricSlot()
            if (slot != null) {
                if (slot.keystoreAlias.startsWith("rivikauth_bio_")) {
                    BiometricKeyManager.deleteKey(slot.keystoreAlias)
                }
                vaultSlotStore.clearBiometricSlot()
            }
            appPrefsStore.setBiometricEnabled(false)
            _uiState.update { it.copy(biometricEnabled = false) }
        }
    }

    fun setAutoLockEnabled(enabled: Boolean) {
        _uiState.update { it.copy(autoLockEnabled = enabled) }
        viewModelScope.launch { appPrefsStore.setAutoLock(enabled) }
    }
    fun setPanicWipeEnabled(enabled: Boolean) {
        _uiState.update { it.copy(panicWipeEnabled = enabled) }
        viewModelScope.launch { appPrefsStore.setPanicWipe(enabled) }
    }
    fun setDarkTheme(enabled: Boolean) {
        _uiState.update { it.copy(darkTheme = enabled) }
        viewModelScope.launch { appPrefsStore.setDarkTheme(enabled) }
    }
    fun setBleEnabled(enabled: Boolean) {
        _uiState.update { it.copy(bleEnabled = enabled) }
        viewModelScope.launch { appPrefsStore.setBleEnabled(enabled) }
    }
    fun setNfcEnabled(enabled: Boolean) {
        _uiState.update { it.copy(nfcEnabled = enabled) }
        viewModelScope.launch { appPrefsStore.setNfcEnabled(enabled) }
    }
    fun setLinkedDeviceEnabled(enabled: Boolean) {
        _uiState.update { it.copy(linkedDeviceEnabled = enabled) }
        viewModelScope.launch { appPrefsStore.setLinkedDeviceEnabled(enabled) }
    }
    fun setShowSuccessOnAuth(enabled: Boolean) {
        _uiState.update { it.copy(showSuccessOnAuth = enabled) }
        viewModelScope.launch { appPrefsStore.setShowSuccessOnAuth(enabled) }
    }
}

data class SettingsUiState(
    val biometricEnabled: Boolean = false,
    val autoLockEnabled: Boolean = true,
    val panicWipeEnabled: Boolean = false,
    val darkTheme: Boolean = true,
    val bleEnabled: Boolean = false,
    val nfcEnabled: Boolean = false,
    val linkedDeviceEnabled: Boolean = false,
    val showSuccessOnAuth: Boolean = true,
)
