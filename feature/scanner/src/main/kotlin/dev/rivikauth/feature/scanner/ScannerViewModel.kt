package dev.rivikauth.feature.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rivikauth.core.common.OtpAuthUriParser
import dev.rivikauth.core.database.dao.OtpEntryDao
import dev.rivikauth.core.database.toEntity
import dev.rivikauth.core.model.OtpEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val otpEntryDao: OtpEntryDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Scanning)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var lastScannedValue: String? = null

    fun onBarcodeScanned(rawValue: String) {
        if (rawValue == lastScannedValue) return
        lastScannedValue = rawValue

        val lower = rawValue.lowercase()
        when {
            lower.startsWith("otpauth://") || lower.startsWith("motp://") -> {
                try {
                    val entry = OtpAuthUriParser.parse(rawValue)
                    viewModelScope.launch {
                        otpEntryDao.upsert(entry.toEntity())
                        _uiState.value = ScannerUiState.OtpScanned(entry)
                    }
                } catch (e: Exception) {
                    _uiState.value = ScannerUiState.Error("Nieprawidłowy kod OTP: ${e.message}")
                }
            }
            lower.startsWith("fido:/") -> {
                _uiState.value = ScannerUiState.FidoScanned(rawValue)
            }
            else -> {
                // Ignoruj nierozpoznane schematy
            }
        }
    }

    fun resetState() {
        lastScannedValue = null
        _uiState.value = ScannerUiState.Scanning
    }
}

sealed interface ScannerUiState {
    data object Scanning : ScannerUiState
    data class OtpScanned(val entry: OtpEntry) : ScannerUiState
    data class FidoScanned(val rawUri: String) : ScannerUiState
    data class Error(val message: String) : ScannerUiState
}
