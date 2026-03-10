package dev.rivikauth.feature.fido

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rivikauth.lib.cable.CableQrCode
import dev.rivikauth.lib.cable.CableSession
import dev.rivikauth.lib.cable.CtapProcessor
import dev.rivikauth.lib.cable.FidoCredentialStore
import dev.rivikauth.service.ble.CableBleAdvertiser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CableScanUiState {
    data object BluetoothOff : CableScanUiState
    data object Scanning : CableScanUiState
    data object Advertising : CableScanUiState
    data object Connecting : CableScanUiState
    data object Handshaking : CableScanUiState
    data object Processing : CableScanUiState
    data object Success : CableScanUiState
    data class Error(val message: String) : CableScanUiState
}

@HiltViewModel
class CableScanViewModel @Inject constructor(
    application: Application,
    private val credentialStore: FidoCredentialStore,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<CableScanUiState>(CableScanUiState.Scanning)
    val uiState: StateFlow<CableScanUiState> = _uiState.asStateFlow()

    private var sessionStarted = false
    private var pendingQrValue: String? = null

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

                // TODO: Get actual master key from vault
                // For now, use a derived key from a placeholder
                val masterKeyBytes = ByteArray(32) // placeholder - needs real vault integration

                val ctapProcessor = CtapProcessor(credentialStore, masterKeyBytes)
                val bleAdvertiser = CableBleAdvertiser(getApplication())

                val session = CableSession(qrData, ctapProcessor, bleAdvertiser)
                session.onStateChanged = { state ->
                    _uiState.value = when (state) {
                        is CableSession.SessionState.Advertising -> CableScanUiState.Advertising
                        is CableSession.SessionState.Connecting -> CableScanUiState.Connecting
                        is CableSession.SessionState.Handshaking -> CableScanUiState.Handshaking
                        is CableSession.SessionState.Processing -> CableScanUiState.Processing
                        is CableSession.SessionState.Success -> CableScanUiState.Success
                        is CableSession.SessionState.Error -> CableScanUiState.Error(state.message)
                        else -> _uiState.value
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
        _uiState.value = CableScanUiState.Scanning
        sessionStarted = false
        pendingQrValue = null
    }

    companion object {
        private const val TAG = "CableScanVM"
    }
}
