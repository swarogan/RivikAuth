package dev.rivikauth.feature.importexport.importer

import dev.rivikauth.core.model.OtpEntry
import java.io.InputStream

interface Importer {
    val name: String
    fun parse(input: InputStream): List<OtpEntry>
}
