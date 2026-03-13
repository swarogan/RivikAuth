package dev.rivikauth.feature.importexport.importer

import dev.rivikauth.core.model.HashAlgorithm
import dev.rivikauth.core.model.OtpEntry
import dev.rivikauth.core.model.OtpType
import org.json.JSONObject
import java.io.InputStream
import java.net.URI
import java.net.URLDecoder
import java.util.UUID

/**
 * Parses Bitwarden JSON export.
 *
 * Expected structure:
 * ```json
 * {
 *   "items": [
 *     {
 *       "name": "Example",
 *       "login": {
 *         "username": "user@example.com",
 *         "totp": "otpauth://totp/Example:user?secret=BASE32&issuer=Example"
 *       }
 *     }
 *   ]
 * }
 * ```
 *
 * The `login.totp` field may contain a full otpauth:// URI or just a bare base32 secret.
 */
class BitwardenImporter : Importer {

    override val name: String = "Bitwarden"

    override fun parse(input: InputStream): List<OtpEntry> {
        val json = JSONObject(input.bufferedReader().readText())

        val items = json.optJSONArray("items")
            ?: throw IllegalArgumentException("Missing 'items' array in Bitwarden export")

        val result = mutableListOf<OtpEntry>()

        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val parsed = parseItem(item)
            if (parsed != null) {
                result.add(parsed)
            }
        }
        return result
    }

    private fun parseItem(item: JSONObject): OtpEntry? {
        val login = item.optJSONObject("login") ?: return null
        val totp = login.optString("totp", "")
        if (totp.isBlank()) return null

        val itemName = item.optString("name", "")
        val username = login.optString("username", "")

        return if (totp.startsWith("otpauth://", ignoreCase = true)) {
            parseOtpAuthUri(totp)
        } else {
            parseBareSecret(totp, itemName, username)
        }
    }

    private fun parseOtpAuthUri(uriString: String): OtpEntry? {
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

    private fun parseBareSecret(
        secret: String,
        itemName: String,
        username: String,
    ): OtpEntry {
        return OtpEntry(
            id = UUID.randomUUID().toString(),
            name = username.ifBlank { itemName },
            issuer = itemName,
            type = OtpType.TOTP,
            secret = Base32.decode(secret),
            algorithm = HashAlgorithm.SHA1,
            digits = 6,
            period = 30,
            counter = 0,
        )
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
