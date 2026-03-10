package dev.rivikauth.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rivikauth.core.crypto.Argon2KeyDerivation
import dev.rivikauth.core.crypto.MasterKeyManager
import dev.rivikauth.core.datastore.VaultSlotStore
import dev.rivikauth.core.database.di.VaultPassphraseHolder
import dev.rivikauth.core.model.VaultSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.crypto.SecretKey
import javax.inject.Inject

@HiltViewModel
class UnlockViewModel @Inject constructor(
    private val vaultSlotStore: VaultSlotStore,
    private val passphraseHolder: VaultPassphraseHolder,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UnlockUiState>(UnlockUiState.Locked)
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    private val _hasBiometric = MutableStateFlow(false)
    val hasBiometric: StateFlow<Boolean> = _hasBiometric.asStateFlow()

    private val _biometricEvent = MutableSharedFlow<VaultSlot.Biometric>()
    val biometricEvent: SharedFlow<VaultSlot.Biometric> = _biometricEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            _hasBiometric.value = vaultSlotStore.getBiometricSlot() != null
        }
    }

    fun unlock(password: String) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = UnlockUiState.Loading
            try {
                val slot = vaultSlotStore.getPasswordSlot()
                    ?: throw IllegalStateException("Brak vault")
                val masterKey = MasterKeyManager.unlockPasswordSlot(
                    slot = slot,
                    password = password.toCharArray(),
                    deriveKey = { pw, salt -> Argon2KeyDerivation.deriveKey(pw, salt) },
                )
                passphraseHolder.setPassphrase(masterKey.encoded)
                _uiState.value = UnlockUiState.Unlocked
            } catch (e: Exception) {
                _uiState.value = UnlockUiState.Error("Nieprawidłowe hasło")
            }
        }
    }

    fun requestBiometricUnlock() {
        viewModelScope.launch {
            val slot = vaultSlotStore.getBiometricSlot() ?: return@launch
            _biometricEvent.emit(slot)
        }
    }

    fun onBiometricSuccess(masterKey: SecretKey) {
        passphraseHolder.setPassphrase(masterKey.encoded)
        _uiState.value = UnlockUiState.Unlocked
    }
}

sealed interface UnlockUiState {
    data object Locked : UnlockUiState
    data object Unlocked : UnlockUiState
    data object Loading : UnlockUiState
    data class Error(val message: String) : UnlockUiState
}
