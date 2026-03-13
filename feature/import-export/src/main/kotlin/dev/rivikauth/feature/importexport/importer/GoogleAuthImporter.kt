package dev.rivikauth.feature.importexport.importer

import dev.rivikauth.core.common.OtpAuthUriParser
import dev.rivikauth.core.model.OtpEntry
import java.io.InputStream

/**
 * Parses Google Authenticator text export.
 *
 * Each line contains one otpauth:// URI:
 * ```
 * otpauth://totp/Example:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Example
 * otpauth://hotp/Counter?secret=JBSWY3DPEHPK3PXP&counter=4
 * ```
 */
class GoogleAuthImporter : Importer {

    override val name: String = "Google Authenticator"

    override fun parse(input: InputStream): ImportResult {
        var skipped = 0
        val entries = input.bufferedReader()
            .lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("otpauth://", ignoreCase = true) }
            .mapNotNull { uri ->
                try {
                    OtpAuthUriParser.parse(uri)
                } catch (_: Exception) {
                    skipped++
                    null
                }
            }
            .toList()
        return ImportResult(entries, skipped)
    }
}
