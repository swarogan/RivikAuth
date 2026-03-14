package dev.rivikauth.feature.importexport.importer

import dev.rivikauth.core.common.Base32
import dev.rivikauth.core.common.OtpAuthUriParser
import dev.rivikauth.core.model.HashAlgorithm
import dev.rivikauth.core.model.OtpEntry
import dev.rivikauth.core.model.OtpType
import org.json.JSONObject
import java.io.InputStream
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

    override fun parse(input: InputStream): ImportResult {
        val json = JSONObject(input.bufferedReader().readText())

        val items = json.optJSONArray("items")
            ?: throw IllegalArgumentException("Missing 'items' array in Bitwarden export")

        val result = mutableListOf<OtpEntry>()
        var skipped = 0

        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val parsed = parseItem(item)
            if (parsed != null) {
                result.add(parsed)
            } else if (item.optJSONObject("login")?.optString("totp", "")?.isNotBlank() == true) {
                skipped++
            }
        }
        return ImportResult(result, skipped)
    }

    private fun parseItem(item: JSONObject): OtpEntry? {
        val login = item.optJSONObject("login") ?: return null
        val totp = login.optString("totp", "")
        if (totp.isBlank()) return null

        val itemName = item.optString("name", "")
        val username = login.optString("username", "")

        return if (totp.startsWith("otpauth://", ignoreCase = true)) {
            try { OtpAuthUriParser.parse(totp) } catch (_: Exception) { null }
        } else {
            parseBareSecret(totp, itemName, username)
        }
    }

    private fun parseBareSecret(
        secret: String,
        itemName: String,
        username: String,
    ): OtpEntry {
        return OtpEntry(
            id = stableEntryId(itemName, username.ifBlank { itemName }, Base32.decode(secret)),
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
}
