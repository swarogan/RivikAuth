package dev.rivikauth.feature.fido

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rivikauth.core.database.di.VaultPassphraseHolder
import dev.rivikauth.core.datastore.AppPrefsStore
import dev.rivikauth.lib.cable.CableQrCode
import dev.rivikauth.lib.cable.CableSession
import dev.rivikauth.lib.cable.CableSessionMode
import dev.rivikauth.lib.cable.AuthenticatorConfig
import dev.rivikauth.lib.cable.CtapProcessor
import dev.rivikauth.lib.cable.FidoCredentialStore
import dev.rivikauth.lib.cable.LinkedDeviceStore
import dev.rivikauth.service.ble.CableBleAdvertiser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CableScanUiState {
    data object BluetoothOff : CableScanUiState
    data object Scanning : CableScanUiState
    data object Advertising : CableScanUiState
    data object Connecting : CableScanUiState
    data object Handshaking : CableScanUiState
    data object Processing : CableScanUiState
    data class Success(val wasCreation: Boolean) : CableScanUiState
    data class Error(val message: String) : CableScanUiState
}

@HiltViewModel
class CableScanViewModel @Inject constructor(
    application: Application,
    private val credentialStore: FidoCredentialStore,
    private val linkedDeviceStore: LinkedDeviceStore,
    private val passphraseHolder: VaultPassphraseHolder,
    private val appPrefsStore: AppPrefsStore,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<CableScanUiState>(CableScanUiState.Scanning)
    val uiState: StateFlow<CableScanUiState> = _uiState.asStateFlow()

    private val _autoDone = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val autoDone: SharedFlow<Unit> = _autoDone.asSharedFlow()

    private var sessionStarted = false
    private var pendingQrValue: String? = null
    private var activeSession: CableSession? = null

    fun checkBluetooth() {
        val btManager = getApplication<Application>()
            .getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            _uiState.value = CableScanUiState.BluetoothOff
        } else if (_uiState.value is CableScanUiState.BluetoothOff) {
            _uiState.value = CableScanUiState.Scanning
            pendingQrValue?.let { onQrScanned(it) }
        }
    }

    fun onQrScanned(rawValue: String) {
        if (sessionStarted) return
        if (!CableQrCode.isCableQr(rawValue)) return

        val btManager = getApplication<Application>()
            .getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            pendingQrValue = rawValue
            _uiState.value = CableScanUiState.BluetoothOff
            return
        }

        sessionStarted = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.w(TAG, "QR raw value: $rawValue")
                val qrData = CableQrCode.parse(rawValue)
                Log.w(TAG, "caBLE QR parsed OK: tunnel=${qrData.tunnelServerDomain}, pubKey=${qrData.peerPublicKey.size}B, secret=${qrData.qrSecret.size}B")

                if (!passphraseHolder.isUnlocked()) {
                    _uiState.value = CableScanUiState.Error("Vault is locked")
                    sessionStarted = false
                    return@launch
                }
                val masterKeyBytes = passphraseHolder.getPassphrase()

                val showSuccessOnAuth = appPrefsStore.showSuccessOnAuth().first()

                val ctapProcessor = CtapProcessor(
                    credentialStore,
                    masterKeyBytes,
                    AuthenticatorConfig(
                        extensions = listOf("credProtect", "hmac-secret", "largeBlobKey"),
                    ),
                )
                val bleAdvertiser = CableBleAdvertiser(getApplication())

                val linkedEnabled = appPrefsStore.linkedDeviceEnabled().first()

                val session = CableSession(
                    CableSessionMode.Qr(qrData),
                    ctapProcessor,
                    bleAdvertiser,
                    linkedDeviceStore = if (linkedEnabled) linkedDeviceStore else null,
                )
                activeSession = session

                launch {
                    session.state.collect { state ->
                        when (state) {
                            is CableSession.SessionState.Advertising -> _uiState.value = CableScanUiState.Advertising
                            is CableSession.SessionState.Connecting -> _uiState.value = CableScanUiState.Connecting
                            is CableSession.SessionState.Handshaking -> _uiState.value = CableScanUiState.Handshaking
                            is CableSession.SessionState.Processing -> _uiState.value = CableScanUiState.Processing
                            is CableSession.SessionState.Success -> {
                                if (!state.wasCreation && !showSuccessOnAuth) {
                                    _autoDone.tryEmit(Unit)
                                    return@collect
                                }
                                _uiState.value = CableScanUiState.Success(state.wasCreation)
                            }
                            is CableSession.SessionState.Error -> {
                                val msg = state.ctapErrorCode?.let { code ->
                                    ctapErrorToString(code)
                                } ?: state.message
                                _uiState.value = CableScanUiState.Error(msg)
                            }
                            is CableSession.SessionState.Idle -> {}
                        }
                    }
                }

                session.run()

            } catch (e: Exception) {
                Log.e(TAG, "caBLE session failed", e)
                val msg = "${e.javaClass.simpleName}: ${e.message ?: "brak szczegółów"}"
                val cause = e.cause?.let { "\nCause: ${it.javaClass.simpleName}: ${it.message}" } ?: ""
                _uiState.value = CableScanUiState.Error(msg + cause)
            }
        }
    }

    fun resetState() {
        activeSession?.cancel()
        activeSession = null
        _uiState.value = CableScanUiState.Scanning
        sessionStarted = false
        pendingQrValue = null
    }

    private fun ctapErrorToString(code: Byte): String {
        val app = getApplication<Application>()
        return when (code) {
            dev.rivikauth.lib.cable.CableConstants.CTAP2_ERR_NO_CREDENTIALS ->
                app.getString(R.string.ctap_err_no_credentials)
            dev.rivikauth.lib.cable.CableConstants.CTAP2_ERR_CREDENTIAL_EXCLUDED ->
                app.getString(R.string.ctap_err_credential_excluded)
            dev.rivikauth.lib.cable.CableConstants.CTAP2_ERR_OPERATION_DENIED ->
                app.getString(R.string.ctap_err_operation_denied)
            dev.rivikauth.lib.cable.CableConstants.CTAP2_ERR_INVALID_CBOR ->
                app.getString(R.string.ctap_err_invalid_cbor)
            else ->
                app.getString(R.string.ctap_err_unknown, code.toUByte().toString(16))
        }
    }

    companion object {
        private const val TAG = "CableScanVM"
    }
}
