package dev.rivikauth.feature.fido

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rivikauth.core.database.dao.FidoCredentialDao
import dev.rivikauth.core.database.entity.FidoCredentialEntity
import dev.rivikauth.core.model.CoseAlgorithm
import dev.rivikauth.core.model.FidoCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.KeyStore
import javax.inject.Inject

@HiltViewModel
class FidoListViewModel @Inject constructor(
    private val fidoCredentialDao: FidoCredentialDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FidoListUiState())
    val uiState: StateFlow<FidoListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            fidoCredentialDao.observeAll()
                .map { entities -> entities.map { it.toDomainModel() } }
                .collect { credentials ->
                    _uiState.update { it.copy(credentials = credentials) }
                }
        }
    }

    fun deleteCredential(credentialId: ByteArray) {
        viewModelScope.launch {
            val cred = _uiState.value.credentials
                .firstOrNull { it.credentialId.contentEquals(credentialId) }
                ?: return@launch

            try {
                val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                ks.deleteEntry(cred.keyAlias)
            } catch (e: Exception) {
                // KeyStore entry may already be missing — proceed with DB deletion
                android.util.Log.w("FidoListVM", "KeyStore cleanup failed for ${cred.keyAlias}", e)
            }

            fidoCredentialDao.deleteByCredentialId(cred.credentialId)
        }
    }
}

data class FidoListUiState(
    val credentials: List<FidoCredential> = emptyList(),
)

private fun FidoCredentialEntity.toDomainModel() = FidoCredential(
    credentialId = credentialId,
    rpId = rpId,
    rpName = rpName,
    userId = userId,
    userName = userName,
    userDisplayName = userDisplayName,
    keyAlias = keyAlias,
    algorithm = CoseAlgorithm.entries.firstOrNull { it.name == algorithm } ?: CoseAlgorithm.ES256,
    discoverable = discoverable,
    signCount = signCount,
    createdAt = createdAt,
    lastUsedAt = lastUsedAt,
)

private fun FidoCredential.toEntity() = FidoCredentialEntity(
    id = Base64.encodeToString(credentialId, Base64.NO_WRAP),
    credentialId = credentialId,
    rpId = rpId,
    rpName = rpName,
    userId = userId,
    userName = userName,
    userDisplayName = userDisplayName,
    keyAlias = keyAlias,
    algorithm = algorithm.name,
    discoverable = discoverable,
    signCount = signCount,
    createdAt = createdAt,
    lastUsedAt = lastUsedAt,
)
