package dev.rivikauth.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rivikauth.core.crypto.AesGcmCrypto
import dev.rivikauth.core.crypto.Argon2KeyDerivation
import dev.rivikauth.core.crypto.MasterKeyManager
import dev.rivikauth.core.datastore.VaultSlotStore
import dev.rivikauth.core.database.di.VaultPassphraseHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val vaultSlotStore: VaultSlotStore,
    private val passphraseHolder: VaultPassphraseHolder,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SetupUiState>(SetupUiState.Input)
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun createVault(password: String) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = SetupUiState.Creating
            try {
                val masterKey = AesGcmCrypto.generateMasterKey()
                val slot = MasterKeyManager.createPasswordSlot(
                    masterKey = masterKey,
                    password = password.toCharArray(),
                    deriveKey = { pw, salt -> Argon2KeyDerivation.deriveKey(pw, salt) },
                )
                vaultSlotStore.savePasswordSlot(slot)
                passphraseHolder.setPassphrase(masterKey.encoded)
                _uiState.value = SetupUiState.Created
            } catch (e: Exception) {
                _uiState.value = SetupUiState.Error(e.message ?: "Błąd tworzenia vault")
            }
        }
    }
}

sealed interface SetupUiState {
    data object Input : SetupUiState
    data object Creating : SetupUiState
    data object Created : SetupUiState
    data class Error(val message: String) : SetupUiState
}
