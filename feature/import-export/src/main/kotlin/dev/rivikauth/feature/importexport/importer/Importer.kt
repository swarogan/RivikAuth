package dev.rivikauth.feature.importexport.importer

import dev.rivikauth.core.model.OtpEntry
import java.io.InputStream
import java.util.UUID

data class ImportResult(
    val entries: List<OtpEntry>,
    val skipped: Int,
)

interface Importer {
    val name: String
    fun parse(input: InputStream): ImportResult
}

fun stableEntryId(issuer: String, name: String, secret: ByteArray): String =
    UUID.nameUUIDFromBytes("$issuer:$name:${secret.contentHashCode()}".toByteArray()).toString()
