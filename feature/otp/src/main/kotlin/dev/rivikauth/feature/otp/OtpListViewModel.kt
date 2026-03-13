package dev.rivikauth.feature.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rivikauth.core.database.dao.OtpEntryDao
import dev.rivikauth.core.database.toEntity
import dev.rivikauth.core.database.toModel
import dev.rivikauth.core.model.OtpEntry
import dev.rivikauth.core.crypto.otp.OtpGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OtpListViewModel @Inject constructor(
    private val otpEntryDao: OtpEntryDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtpListUiState())
    val uiState: StateFlow<OtpListUiState> = _uiState.asStateFlow()

    init {
        observeEntries()
        startCodeRefresh()
    }

    private fun observeEntries() {
        viewModelScope.launch {
            otpEntryDao.observeAll().collect { entities ->
                val entries = entities.map { it.toModel() }
                _uiState.update { it.copy(entries = entries) }
                refreshCodes()
            }
        }
    }

    private var lastPeriodKeys: Map<String, Long> = emptyMap()

    private fun startCodeRefresh() {
        viewModelScope.launch {
            while (true) {
                refreshCodes()
                delay(1000L)
            }
        }
    }

    private fun refreshCodes() {
        val entries = _uiState.value.entries
        val currentCodes = _uiState.value.codes
        val nowSeconds = System.currentTimeMillis() / 1000

        val newPeriodKeys = entries.associate { entry ->
            val period = if (entry.period > 0) entry.period else 30
            entry.id to (nowSeconds / period)
        }

        if (currentCodes.isEmpty() || newPeriodKeys != lastPeriodKeys) {
            val codes = entries.associate { entry ->
                entry.id to OtpGenerator.generate(entry)
            }
            lastPeriodKeys = newPeriodKeys
            _uiState.update { it.copy(codes = codes) }
        }
    }

    fun toggleFavorite(entryId: String) {
        viewModelScope.launch {
            val entry = _uiState.value.entries.find { it.id == entryId } ?: return@launch
            val updated = entry.copy(favorite = !entry.favorite, updatedAt = System.currentTimeMillis())
            otpEntryDao.upsert(updated.toEntity())
        }
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            val entry = _uiState.value.entries.find { it.id == entryId } ?: return@launch
            otpEntryDao.delete(entry.toEntity())
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}

data class OtpListUiState(
    val entries: List<OtpEntry> = emptyList(),
    val codes: Map<String, String> = emptyMap(),
    val searchQuery: String = "",
)
