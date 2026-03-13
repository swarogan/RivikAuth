package dev.rivikauth.feature.importexport

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rivikauth.core.database.dao.OtpEntryDao
import dev.rivikauth.core.database.toEntity
import dev.rivikauth.core.database.toModel
import dev.rivikauth.core.model.OtpEntry
import dev.rivikauth.feature.importexport.exporter.VaultExporter
import dev.rivikauth.feature.importexport.importer.AegisImporter
import dev.rivikauth.feature.importexport.importer.AndOtpImporter
import dev.rivikauth.feature.importexport.importer.BitwardenImporter
import dev.rivikauth.feature.importexport.importer.GoogleAuthImporter
import dev.rivikauth.feature.importexport.importer.Importer
import dev.rivikauth.feature.importexport.importer.TwoFASImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportExportViewModel @Inject constructor(
    private val otpEntryDao: OtpEntryDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportExportUiState())
    val uiState: StateFlow<ImportExportUiState> = _uiState.asStateFlow()

    private var pendingImporter: Importer? = null

    val importers: List<Importer> = listOf(
        AegisImporter(),
        GoogleAuthImporter(),
        TwoFASImporter(),
        BitwardenImporter(),
        AndOtpImporter(),
    )

    fun selectImporter(importer: Importer) {
        pendingImporter = importer
    }

    fun importFromUri(context: Context, uri: Uri) {
        val importer = pendingImporter ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, message = null) }
            try {
                val result = context.contentResolver.openInputStream(uri)?.use { stream ->
                    importer.parse(stream)
                } ?: throw IllegalStateException("Nie udalo sie otworzyc pliku")

                if (result.entries.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Plik nie zawiera wpisow do zaimportowania",
                        )
                    }
                    return@launch
                }

                val entities = result.entries.map { it.toEntity() }
                otpEntryDao.upsertAll(entities)

                val skippedInfo = if (result.skipped > 0) {
                    " (pominieto ${result.skipped} blednych)"
                } else ""

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "Zaimportowano ${result.entries.size} wpisow z ${importer.name}$skippedInfo",
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Blad importu (${importer.name}): ${e.message}",
                    )
                }
            } finally {
                pendingImporter = null
            }
        }
    }

    fun exportToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, message = null) }
            try {
                val entities = otpEntryDao.observeAll().first()
                val entries = entities.map { it.toModel() }

                if (entries.isEmpty()) {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Brak wpisow do eksportu")
                    }
                    return@launch
                }

                val json = VaultExporter.exportPlaintext(entries)

                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                } ?: throw IllegalStateException("Nie udalo sie zapisac pliku")

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "Wyeksportowano ${entries.size} wpisow",
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Blad eksportu: ${e.message}",
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }

}

data class ImportExportUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)
