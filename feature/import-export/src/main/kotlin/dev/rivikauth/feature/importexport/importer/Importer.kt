package dev.rivikauth.feature.importexport.importer

import dev.rivikauth.core.model.OtpEntry
import java.io.InputStream

data class ImportResult(
    val entries: List<OtpEntry>,
    val skipped: Int,
)

interface Importer {
    val name: String
    fun parse(input: InputStream): ImportResult
}
