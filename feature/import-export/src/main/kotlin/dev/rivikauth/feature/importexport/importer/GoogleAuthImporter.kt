package dev.rivikauth.feature.importexport.importer

import dev.rivikauth.core.model.HashAlgorithm
import dev.rivikauth.core.model.OtpEntry
import dev.rivikauth.core.model.OtpType
import java.io.InputStream
import java.net.URI
import java.net.URLDecoder
import java.util.UUID

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

    override fun parse(input: InputStream): List<OtpEntry> {
        return input.bufferedReader()
            .lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("otpauth://", ignoreCase = true) }
            .mapNotNull { parseUri(it) }
            .toList()
    }

    private fun parseUri(uriString: String): OtpEntry? {
        return try {
            val uri = URI(uriString)
            if (uri.scheme?.lowercase() != "otpauth") return null

            val host = uri.host?.lowercase() ?: return null
            val type = when (host) {
                "totp" -> OtpType.TOTP
                "hotp" -> OtpType.HOTP
                "steam" -> OtpType.STEAM
                else -> return null
            }

            val path = uri.path?.removePrefix("/") ?: ""
            val (issuer, name) = parseLabel(path)
            val params = parseQuery(uri.rawQuery)

            val secretBase32 = params["secret"] ?: return null
            val secret = Base32.decode(secretBase32)

            val algorithm = HashAlgorithm.fromString(params["algorithm"] ?: "SHA1")

            val digits = params["digits"]?.toIntOrNull() ?: when (type) {
                OtpType.STEAM -> 5
                else -> 6
            }
            val period = params["period"]?.toIntOrNull() ?: 30
            val counter = params["counter"]?.toLongOrNull() ?: 0L
            val issuerParam = params["issuer"] ?: issuer

            OtpEntry(
                id = UUID.randomUUID().toString(),
                name = name,
                issuer = issuerParam,
                type = type,
                secret = secret,
                algorithm = algorithm,
                digits = digits,
                period = period,
                counter = counter,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseLabel(label: String): Pair<String, String> {
        val decoded = URLDecoder.decode(label, "UTF-8")
        return if (":" in decoded) {
            val parts = decoded.split(":", limit = 2)
            parts[0].trim() to parts[1].trim()
        } else {
            "" to decoded.trim()
        }
    }

    private fun parseQuery(query: String?): Map<String, String> {
        if (query.isNullOrEmpty()) return emptyMap()
        return query.split("&").associate { param ->
            val parts = param.split("=", limit = 2)
            val key = URLDecoder.decode(parts[0], "UTF-8")
            val value = if (parts.size > 1) URLDecoder.decode(parts[1], "UTF-8") else ""
            key to value
        }
    }
}
