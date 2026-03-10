package dev.rivikauth.feature.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rivikauth.core.database.dao.OtpEntryDao
import dev.rivikauth.core.database.entity.OtpEntryEntity
import dev.rivikauth.core.model.HashAlgorithm
import dev.rivikauth.core.model.OtpEntry
import dev.rivikauth.core.model.OtpType
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
            }
        }
    }

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
        val codes = entries.associate { entry ->
            entry.id to OtpGenerator.generate(entry)
        }
        _uiState.update { it.copy(codes = codes, currentTime = System.currentTimeMillis()) }
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

private fun OtpEntryEntity.toModel(): OtpEntry = OtpEntry(
    id = id,
    name = name,
    issuer = issuer,
    type = OtpType.valueOf(type),
    secret = secret,
    algorithm = HashAlgorithm.valueOf(algorithm),
    digits = digits,
    period = period,
    counter = counter,
    pin = pin,
    groupIds = if (groupIds.isBlank()) emptySet() else groupIds.split(",").toSet(),
    sortOrder = sortOrder,
    note = note,
    favorite = favorite,
    iconData = iconData,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun OtpEntry.toEntity(): OtpEntryEntity = OtpEntryEntity(
    id = id,
    name = name,
    issuer = issuer,
    type = type.name,
    secret = secret,
    algorithm = algorithm.name,
    digits = digits,
    period = period,
    counter = counter,
    pin = pin,
    groupIds = groupIds.joinToString(","),
    sortOrder = sortOrder,
    note = note,
    favorite = favorite,
    iconData = iconData,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

data class OtpListUiState(
    val entries: List<OtpEntry> = emptyList(),
    val codes: Map<String, String> = emptyMap(),
    val searchQuery: String = "",
    val currentTime: Long = System.currentTimeMillis(),
)
